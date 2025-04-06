package com.quranhabit.ui.reader

import ReadingTimeTracker
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.MainActivity
import com.quranhabit.R
import com.quranhabit.data.QuranDatabase
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.databinding.ItemPageBinding
import com.quranhabit.ui.surah.Ayah
import com.quranhabit.ui.surah.Surah
import com.quranhabit.data.SurahRepository
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.data.repository.LastReadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit
import kotlin.math.max

// Our awesome, flaky way of detecting and inserting the basmalla header
private const val BASMALLA_TEXT = "بِسمِ ٱللَّهِ ٱلرَّحمَـٰنِ ٱلرَّحِيمِ"

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!

    // What page are we on?
    private lateinit var pageAdapter: QuranPageAdapter
    private lateinit var allPages: List<List<PageAyahRange>>
    private lateinit var quranLines: List<String>
    private var currentSurahNumber = 1

    // Database
    private val database by lazy { QuranDatabase.getDatabase(requireContext()) }
    private val statisticsDao by lazy { database.statisticsDao() }
    // The cool new kid repository
    private val lastReadRepo by lazy {
        LastReadRepository(QuranDatabase.getDatabase(requireContext()).lastReadPositionDao())
    }

    // Checking if we read a page
    private var pageScrollState = false
    private var pageTimer: CountDownTimer? = null
    private var currentPagePosition = -1
    private var pageReadStates = mutableMapOf<Int, Boolean>() // Track which pages have been marked as read
    private var isTrackingScroll = false
    private var pageMarked = false
    private var isAtBottom = false
    private var bottomTimer: CountDownTimer? = null
    // Don't mark as read again if we read to  the bottom of a page and reopen and continue
    private var lastPageMarkedAsRead = -1

    // Total time read
    private val readingTimeTracker = ReadingTimeTracker()

    companion object {
        private const val PAGE_READ_DELAY_MS = 3000L
        private const val PAGE_READ_CHECK_INTERVAL = 1000L
    }

    private val cachedPages by lazy {
        val json = loadTextFromRaw(R.raw.pages_absolute)
        Gson().fromJson<List<List<List<PageAyahRange>>>>(
            json,
            object : TypeToken<List<List<List<PageAyahRange>>>>() {}.type
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuranReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        super.onViewCreated(view, savedInstanceState)
        readingTimeTracker.start()

        // Load the last marked page so we don't double-mark as read
        val prefs = requireContext().getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE)
        lastPageMarkedAsRead = prefs.getInt("lastPageMarked", -1)

        // Initialize data
        allPages = cachedPages.flatten()
        quranLines = loadTextFromRaw(R.raw.quran_text).lines()

        // Get the selected surah number from arguments
        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        val ayahNumber = arguments?.getInt("ayahNumber") ?: 1

        // Find the first page of the selected surah
        val initialPage = findFirstPageForSurah(currentSurahNumber)

        setupViewPager(initialPage)
        updateHeader(currentSurahNumber, initialPage)

        // Always scroll to first ayah unless we're continuing from a saved position
        if (arguments?.containsKey("ayahNumber") != true) {
            binding.quranPager.postDelayed({
                scrollToAyah(currentSurahNumber, 1) // Always start at ayah 1 for new surah
            }, 300)
        } else {
            // Only scroll to ayah if we're continuing from a saved position
            binding.quranPager.postDelayed({
                scrollToAyah(currentSurahNumber, ayahNumber)
            }, 300)
        }
    }

    private fun logReadingTime(seconds: Int) {
        if (seconds <= 0) return

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("ReadingTime", "Logging $seconds seconds for $currentDate")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existingRecord = statisticsDao.getByDate(currentDate)
                if (existingRecord != null) {
                    statisticsDao.upsert(existingRecord.copy(
                        secondsSpendReading = existingRecord.secondsSpendReading + seconds
                    ))
                } else {
                    statisticsDao.upsert(PagesReadOnDay(
                        date = currentDate,
                        pagesRead = 0,
                        secondsSpendReading = seconds
                    ))
                }
            }
        }
    }

    private fun scrollToAyah(surah: Int, ayah: Int) {
        try {
            val targetPage = findPageForAyah(surah, ayah).coerceIn(0, allPages.size - 1)

            // Always change page if surah changed, even if same page number
            if (binding.quranPager.currentItem != targetPage || surah != currentSurahNumber) {
                binding.quranPager.setCurrentItem(targetPage, false)
                currentSurahNumber = surah // Update current surah
            }

            binding.quranPager.postDelayed({
                try {
                    val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(targetPage)
                    val scrollView = viewHolder?.itemView?.findViewById<NestedScrollView>(R.id.page_scroll_view)

                    lifecycleScope.launch {
                        // Only use saved position if we're continuing reading
                        val savedPosition = if (arguments?.containsKey("ayahNumber") == true) {
                            lastReadRepo.getScrollPosition(targetPage)
                        } else {
                            null
                        }

                        scrollView?.post {
                            try {
                                if (savedPosition != null) {
                                    scrollView.scrollTo(0, savedPosition)
                                } else {
                                    val ayahView = scrollView.findViewWithTag<View?>("ayah_${surah}_$ayah")
                                        ?: scrollView.findViewWithTag<View?>("ayah_$ayah")

                                    ayahView?.let {
                                        // Simple fixed backup for basmalah (adjust size as needed)
                                        val backup = if (ayah == 1) {
                                            (70 * resources.displayMetrics.density).toInt() // Convert dp to pixels
                                        } else {
                                            0
                                        }
                                        val scrollPosition = max(0, it.top - backup)
                                        scrollView.smoothScrollTo(0, scrollPosition)
                                    } ?: run {
                                        scrollView.smoothScrollTo(0, 0)
                                    }
                                }
                            } catch (e: Exception) {
                                scrollView.scrollTo(0, 0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("QuranReader", "Page setup failed: ${e.message}")
                }
            }, 300) // Delay to allow page to settle
        } catch (e: Exception) {
            Log.e("QuranReader", "Scroll failed: ${e.message}")
            binding.quranPager.setCurrentItem(findFirstPageForSurah(surah), false)
        }
    }

    private fun findPageForAyah(surah: Int, ayah: Int): Int {
        if (!::allPages.isInitialized) return 0

        return allPages.indexOfFirst { page ->
            page.any { range ->
                if (range.surah == surah) {
                    val firstAyah = range.start - getFirstLineNumberForSurah(surah) + 1
                    val lastAyah = range.end - getFirstLineNumberForSurah(surah) + 1
                    // Debug output:
                    Log.d("AyahDebug", "Page ${allPages.indexOf(page)}: Ayahs $firstAyah-$lastAyah")
                    ayah in firstAyah..lastAyah
                } else false
            }
        }.takeIf { it != -1 } ?: 0
    }

    private fun setupViewPager(initialPage: Int) {
        pageAdapter = QuranPageAdapter(
            fragment = this,  // Pass fragment reference
            allPages = allPages,
            quranLines = quranLines
        )

        binding.quranPager.adapter = pageAdapter
        binding.quranPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.quranPager.setCurrentItem(initialPage, false)

        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var scrollState = ViewPager2.SCROLL_STATE_IDLE

            override fun onPageSelected(newPage: Int) {
                saveCurrentPosition()

                // 1. Pause and log time from previous page
                val secondsRead = readingTimeTracker.pause()
                if (secondsRead > 0) {
                    logReadingTime(secondsRead)
                }

                // 2. Reset and start fresh for new page
                readingTimeTracker.reset()
                readingTimeTracker.start()

                // CANCEL ALL TIMERS on page change
                pageTimer?.cancel()
                pageTimer = null
                bottomTimer?.cancel()
                bottomTimer = null

                // Reset states
                pageScrollState = false
                pageMarked = false
                isAtBottom = false

                //// Page position etc
                currentPagePosition = newPage
                updateHeader(getSurahForPage(newPage).number, newPage)

                // Reset states for new page
                pageScrollState = false
                pageMarked = false

                // Cancel any existing timer
                pageTimer?.cancel()

                // Start new timer if not already marked
                if (!pageReadStates.getOrDefault(newPage, false)) {
                    startPageReadTimer(newPage)
                }

                // page/timer stuff
                currentPagePosition = newPage
                updateHeader(getSurahForPage(newPage).number, newPage)

                // Start fresh timer for the new page
                if (!pageReadStates.getOrDefault(newPage, false)) {
                    startPageReadTimer(newPage)
                }
            }

            private fun logReadingTime(secondsSpendReading: Int) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                lifecycleScope.launch {
                    val existingRecord = statisticsDao.getByDate(currentDate)
                    if (existingRecord != null) {
                        statisticsDao.upsert(existingRecord.copy(secondsSpendReading = existingRecord.secondsSpendReading + secondsSpendReading))
                    } else {
                        statisticsDao.upsert(PagesReadOnDay(date = currentDate, pagesRead = 0, secondsSpendReading = secondsSpendReading))
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // Only check conditions, don't mark yet
                    Log.d("PageFlow", "Page settled - scroll: $pageScrollState, timer: ${pageTimer == null}")
                }
            }
        })
    }

    private fun checkPageReadConditions() {
        Log.d("PageConditions", "Checking conditions - scroll:$pageScrollState, marked:$pageMarked, read:${pageReadStates.getOrDefault(currentPagePosition, false)}, bottom:$isAtBottom, timer:${pageTimer == null}, lastMarked:$lastPageMarkedAsRead, current:$currentPagePosition")

        // Only mark as read if we haven't already marked this page
        if (!pageMarked &&
            !pageReadStates.getOrDefault(currentPagePosition, false) &&
            isAtBottom &&
            pageTimer == null &&
            currentPagePosition != lastPageMarkedAsRead) {

            Log.d("PageMark", "All conditions met for page $currentPagePosition")
            markPageAsRead(currentPagePosition)
            pageMarked = true
            pageReadStates[currentPagePosition] = true
            lastPageMarkedAsRead = currentPagePosition // Update last marked page
        }
    }

    private fun startPageReadTimer(pageNumber: Int) {
        pageTimer?.cancel()
        pageTimer = object : CountDownTimer(PAGE_READ_DELAY_MS, PAGE_READ_CHECK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                pageTimer = null
                Log.d("PageTimer", "Page timer completed")
                checkPageReadConditions()
            }
        }.start()
        Log.d("PageFlow", "Page $pageNumber - Timer started")
    }

    private fun updateHeader(currentSurahNumber: Int, pageNumber: Int) {
        val currentSurah = SurahRepository.getSurahByNumber(currentSurahNumber)
        binding.surahInfoTextView.text = "${currentSurah?.number}. ${currentSurah?.englishName}"
        binding.pageInfoTextView.text = "page ${pageNumber + 1}"
    }

    private fun getSurahForPage(position: Int): Surah {
        val surahNumber = allPages[position].first().surah
        return SurahRepository.getSurahByNumber(surahNumber) ?: Surah(0, "Unknown", "???")
    }

    private fun findFirstPageForSurah(surahNumber: Int): Int {
        return allPages.indexOfFirst { page -> page.any { it.surah == surahNumber } }
            .takeIf { it != -1 } ?: 0
    }

    private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
        return try {
            cachedPages.flatten().flatten().first { it.surah == surahNumber }.start
        } catch (e: Exception) {
            Log.e("QuranReader", "Error finding first line for surah $surahNumber", e)
            0
        }
    }

    private fun saveLastReadAyah(surahNumber: Int, ayahNumber: Int) {
        requireContext().getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE).edit()
            .putInt("lastSurah", surahNumber)
            .putInt("lastAyah", ayahNumber)
            .putInt("lastPage", binding.quranPager.currentItem)
            .apply()
    }

    private fun loadTextFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }


    private fun handleBottomPositionChange(atBottom: Boolean) {
        isAtBottom = atBottom

        if (atBottom) {
            // Start fresh 1s timer only if not already running
            if (bottomTimer == null) {
                bottomTimer = object : CountDownTimer(1000L, 1000L) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        if (isAtBottom) { // Double-check we're still at bottom
                            checkPageReadConditions()
                        }
                        bottomTimer = null
                    }
                }.start()
                Log.d("BottomTimer", "Started bottom timer")
            }
        } else {
            // User scrolled up → CANCEL timer aggressively
            bottomTimer?.cancel()
            bottomTimer = null
            Log.d("BottomTimer", "Cancelled bottom timer (user scrolled up)")
        }
    }

    override fun onResume() {
        super.onResume()
        readingTimeTracker.start()
        (requireActivity() as MainActivity).setBottomNavVisibility(false)
    }

    override fun onStop() {
        (requireActivity() as MainActivity).setBottomNavVisibility(true)
        super.onStop()
    }

    override fun onDestroyView() {
        pageTimer?.cancel()
        bottomTimer?.cancel()
        isTrackingScroll = false
        super.onDestroyView()
        _binding = null
    }

    private fun showPageReadFeedback() {
        val snack = Snackbar.make(binding.root, "✔ Page read", Snackbar.LENGTH_SHORT)
        snack.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.green))
        snack.show()
    }

    class QuranPageAdapter(
        private val fragment: QuranReaderFragment,
        private val allPages: List<List<PageAyahRange>>,
        private val quranLines: List<String>
    ) : RecyclerView.Adapter<QuranPageAdapter.PageViewHolder>() {

        private val scrollPositions = SparseArray<Int>()
        private val scrollTrackers = mutableMapOf<Int, ScrollTracker>()

        inner class PageViewHolder(val binding: ItemPageBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val scrollTracker = ScrollTracker() // Add this line
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val binding = ItemPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PageViewHolder(binding)
        }

        private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
            return allPages.flatten() // Flatten all pages' ayah ranges
                .firstOrNull { it.surah == surahNumber } // Find first range for this surah
                ?.start // Get its starting line number
                ?: 0 // Default to 0 if not found (shouldn't happen)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.binding.pageContent.removeAllViews()

            allPages[position].forEach { range ->
                (range.start..range.end).forEach { lineNumber ->
                    val lineText = quranLines[lineNumber - 1]
                    val firstLineForSurah = getFirstLineNumberForSurah(range.surah)
                    val ayahNumber = lineNumber - firstLineForSurah + 1

                    val ayah = Ayah(
                        surahNumber = range.surah,
                        ayahNumber = ayahNumber,
                        text = lineText
                    )

                    addAyahToView(holder.binding.pageContent, ayah)
                }
            }

            holder.binding.pageContent.removeAllViews()

            // Add each ayah to the page
            allPages[position].forEach { range ->
                (range.start..range.end).forEach { lineNumber ->
                    val ayah = Ayah(
                        surahNumber = range.surah,
                        ayahNumber = lineNumber - getFirstLineNumberForSurah(range.surah) + 1,
                        text = quranLines[lineNumber - 1]
                    )
                    addAyahToView(holder.binding.pageContent, ayah)
                }
            }

            // Setup scroll tracking
            val scrollView = holder.binding.root.findViewById<NestedScrollView>(R.id.page_scroll_view)
            holder.scrollTracker.attach(scrollView)
            holder.scrollTracker.onScrollStateChanged = { isScrolled ->
                if (position == fragment.currentPagePosition) {
                    fragment.pageScrollState = isScrolled
                    fragment.checkPageReadConditions()
                }
            }
            holder.scrollTracker.onScrollPositionChanged = { atBottom ->
                if (position == fragment.currentPagePosition) {
                    fragment.handleBottomPositionChange(atBottom)
                }
            }
        }

        override fun onViewRecycled(holder: PageViewHolder) {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                scrollTrackers[position]?.let {
                    scrollPositions.put(position, it.getScrollY())
                    it.detach()
                }
            }
            super.onViewRecycled(holder)
        }

        override fun getItemCount() = allPages.size

        private fun isBasmala(ayah: Ayah): Boolean {
            return ayah.text.startsWith(BASMALLA_TEXT) &&
                    ayah.ayahNumber == 1 &&
                    ayah.surahNumber != 1 &&
                    ayah.surahNumber != 9
        }

        private fun addAyahToView(container: ViewGroup, ayah: Ayah) {
            if (isBasmala(ayah)) {
                // Existing basmala handling...
                val basmalaView = LayoutInflater.from(container.context)
                    .inflate(R.layout.item_basmala, container, false) as TextView
                container.addView(basmalaView)

                ayah.text.removePrefix(BASMALLA_TEXT)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { remainingText ->
                        val ayahBinding = ItemAyahBinding.inflate(
                            LayoutInflater.from(container.context),
                            container,
                            false
                        )
                        ayahBinding.ayahNumberTextView.text = ayah.ayahNumber.toString()
                        ayahBinding.ayahTextView.fontFeatureSettings = "'liga' on, 'clig' on"  // Forces ligature rendering
                        ayahBinding.ayahTextView.text = fixMissingSmallStops(remainingText)
                        // Set both tag formats for compatibility
                        ayahBinding.root.tag = "ayah_${ayah.surahNumber}_${ayah.ayahNumber}"
                        ayahBinding.root.setTag(R.id.ayah_tag, "ayah_${ayah.ayahNumber}")
                        container.addView(ayahBinding.root)
                    }
            } else {
                val binding = ItemAyahBinding.inflate(
                    LayoutInflater.from(container.context),
                    container,
                    false
                )
                binding.ayahNumberTextView.text = ayah.ayahNumber.toString()
                binding.ayahTextView.fontFeatureSettings = "'liga' on, 'clig' on"  // Forces ligature rendering
                binding.ayahTextView.text = fixMissingSmallStops(ayah.text)
                // Set both tag formats for compatibility
                binding.root.tag = "ayah_${ayah.surahNumber}_${ayah.ayahNumber}"
                binding.root.setTag(R.id.ayah_tag, "ayah_${ayah.ayahNumber}")
                container.addView(binding.root)
            }
        }

        private fun fixMissingSmallStops(quranText: String): String {
            var fixedText = quranText

            val salaa = "\u06D6" // U+06D6
            val qala = "\u06D7"  // U+06D7
            val smallJeem = "\u06DA" // U+06DA
            val smallLaa = "\u06D9" // U+06D9

            val smallStops = listOf(salaa, qala, smallJeem, smallLaa)

            for (smallStop in smallStops) {
                // Prepend word joiner so tiny stop doesn't appear at the start of a new line.
                // \u2060 is "Word Joiner" which forces stops to stay attached to the previous word,
                // but without adding visible space.
                fixedText = fixedText.replace(smallStop, "\u2060$smallStop")
            }

            return fixedText
        }
    }

    private fun markPageAsRead(pageNumber: Int) {
        // 1. First pause and capture the reading time
        val secondsRead = readingTimeTracker.pause()

        // Only proceed if we actually spent time reading
        if (secondsRead <= 0) {
            Log.d("PageMark", "Not marking page $pageNumber - no reading time recorded")
            readingTimeTracker.reset()
            readingTimeTracker.start()
            return
        }

        // 2. Get all the page data we need
        val surahNumber = getSurahForPage(pageNumber).number
        val ayahRanges = allPages[pageNumber]
        val lastAyahRange = ayahRanges.last()
        val lastAyahNumber = lastAyahRange.end - getFirstLineNumberForSurah(lastAyahRange.surah) + 1

        // 3. Get scroll position from ViewHolder
        val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(pageNumber) as? QuranPageAdapter.PageViewHolder
        val scrollY = viewHolder?.scrollTracker?.getScrollY() ?: 0

        // 4. Save reading progress immediately to SharedPreferences
        saveLastReadAyah(surahNumber, lastAyahNumber)
        lastPageMarkedAsRead = pageNumber

        // 5. Record in database
        lifecycleScope.launch {
            try {
                // Save position in Room database
                lastReadRepo.savePosition(surahNumber, lastAyahNumber, pageNumber, scrollY)

                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Get existing record for today
                val existingRecord = withContext(Dispatchers.IO) {
                    statisticsDao.getByDate(currentDate)
                }

                // Update statistics
                withContext(Dispatchers.IO) {
                    if (existingRecord != null) {
                        statisticsDao.upsert(
                            existingRecord.copy(
                                pagesRead = existingRecord.pagesRead + 1,
                                secondsSpendReading = existingRecord.secondsSpendReading + secondsRead
                            )
                        )
                    } else {
                        statisticsDao.upsert(
                            PagesReadOnDay(
                                date = currentDate,
                                pagesRead = 1,
                                secondsSpendReading = secondsRead
                            )
                        )
                    }
                }

                // Show feedback and reset timer on main thread
                withContext(Dispatchers.Main) {
                    showPageReadFeedback()
                    readingTimeTracker.reset()
                    readingTimeTracker.start() // Start fresh for next page
                }

                Log.d("QuranReader", "Marked page $pageNumber as read with $secondsRead seconds spent")

            } catch (e: Exception) {
                Log.e("QuranReader", "Error marking page as read", e)
                // Ensure timer gets reset even if there's an error
                withContext(Dispatchers.Main) {
                    readingTimeTracker.reset()
                    readingTimeTracker.start()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        readingTimeTracker.pause()
        saveCurrentPosition()
        // Save the last marked page
        requireContext().getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE).edit() {
            putInt("lastPageMarked", lastPageMarkedAsRead)
        }
    }

    private fun saveCurrentPosition() {
        val currentPage = binding.quranPager.currentItem
        if (currentPage in allPages.indices) {
            val surahNumber = getSurahForPage(currentPage).number
            val ayahRanges = allPages[currentPage]

            // Find the last ayah on this page
            val lastAyahRange = ayahRanges.last()
            val lastAyahNumber = lastAyahRange.end - getFirstLineNumberForSurah(lastAyahRange.surah) + 1
            
            // Get current scroll position
            val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(currentPage) as? QuranPageAdapter.PageViewHolder
            val scrollY = viewHolder?.scrollTracker?.getScrollY() ?: 0

            // Save to both SharedPreferences and database
            saveLastReadAyah(surahNumber, lastAyahNumber)

            lifecycleScope.launch {
                lastReadRepo.savePosition(
                    surah = surahNumber,
                    ayah = lastAyahNumber,
                    page = currentPage,
                    scrollY = scrollY
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. Pause the timer (captures current reading session)
                readingTimeTracker.pause()

                // 2. Log the accumulated time
                val secondsRead = readingTimeTracker.getTotalSeconds()
                if (secondsRead > 0) {
                    logReadingTime(secondsRead)
                }

                // 3. Save position and navigate back
                saveCurrentPosition()
                isEnabled = false
                requireActivity().onBackPressed()

                // 4. Reset after logging (don't reset before logging!)
                readingTimeTracker.reset()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
}

data class PageAyahRange(val surah: Int, val start: Int, val end: Int)