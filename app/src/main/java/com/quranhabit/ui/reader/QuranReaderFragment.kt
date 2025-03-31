package com.quranhabit.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var arabicTypeface: Typeface

    // What page are we on?
    private lateinit var pageAdapter: QuranPageAdapter
    private lateinit var allPages: List<List<PageAyahRange>>
    private lateinit var quranLines: List<String>
    private var currentSurahNumber = 1

    // Database
    private val database by lazy { QuranDatabase.getDatabase(requireContext()) }
    private val statisticsDao by lazy { database.statisticsDao() }

    // Checking if we read a page
    private var pageScrollState = false
    private var pageTimer: CountDownTimer? = null
    private var currentPagePosition = -1
    private var pageReadStates = mutableMapOf<Int, Boolean>() // Track which pages have been marked as read
    private var isTrackingScroll = false
    private var pageMarked = false

    // Total time read
    private var readingStartTime: Long = 0

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

        arabicTypeface = Typeface.createFromAsset(
            requireContext().assets,
            "fonts/ScheherazadeNewRegular.ttf"
        )

        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        allPages = cachedPages.flatten()
        quranLines = loadTextFromRaw(R.raw.quran_uthmani).lines()

        val initialPage = findFirstPageForSurah(currentSurahNumber)
        setupViewPager(initialPage)
        updateHeader(currentSurahNumber, initialPage)
    }

    private fun setupViewPager(initialPage: Int) {
        pageAdapter = QuranPageAdapter(
            fragment = this,  // Pass fragment reference
            allPages = allPages,
            arabicTypeface = arabicTypeface,
            quranLines = quranLines,
        )

        binding.quranPager.adapter = pageAdapter
        binding.quranPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.quranPager.setCurrentItem(initialPage, false)

        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var scrollState = ViewPager2.SCROLL_STATE_IDLE

            override fun onPageSelected(newPage: Int) {
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

                //// Total time read
                val currentTime = System.currentTimeMillis()
                if (readingStartTime > 0) {
                    val secondsSpendReading = ((currentTime - readingStartTime) / 1000).toInt() // Convert ms to seconds
                    logReadingTime(secondsSpendReading)
                }
                readingStartTime = currentTime // Reset for the new page
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
        if (pageScrollState && !pageMarked && !pageReadStates.getOrDefault(currentPagePosition, false)) {
            // Only mark if timer has completed
            if (pageTimer == null) {
                markPageAsRead(currentPagePosition)
                pageMarked = true
                pageReadStates[currentPagePosition] = true
                Log.d("PageMark", "✔ Marked page $currentPagePosition (after delay)")
            } else {
                Log.d("PageMark", "☑ Page $currentPagePosition ready (waiting for timer)")
            }
        }
    }

    private fun startPageReadTimer(pageNumber: Int) {
        pageTimer?.cancel()
        pageTimer = object : CountDownTimer(PAGE_READ_DELAY_MS, PAGE_READ_CHECK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                // Optional progress updates
            }

            override fun onFinish() {
                pageTimer = null // Clear timer when done
                if (pageScrollState && !pageMarked) {
                    checkPageReadConditions()
                }
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
            .apply()
    }

    private fun loadTextFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setBottomNavVisibility(false)
    }

    override fun onStop() {
        (requireActivity() as MainActivity).setBottomNavVisibility(true)
        super.onStop()
    }

    override fun onDestroyView() {
        pageTimer?.cancel()
        isTrackingScroll = false
        super.onDestroyView()
        _binding = null
    }

    private fun showPageReadFeedback() {
        val snack = Snackbar.make(binding.root, "Page read", Snackbar.LENGTH_SHORT)
        snack.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.green))
        snack.show()
    }

    class QuranPageAdapter(
        private val fragment: QuranReaderFragment,
        private val allPages: List<List<PageAyahRange>>,
        private val arabicTypeface: Typeface,
        private val quranLines: List<String>
    ) : RecyclerView.Adapter<QuranPageAdapter.PageViewHolder>() {

        private val scrollTrackers = mutableMapOf<Int, ScrollTracker>()

        inner class PageViewHolder(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

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
            scrollTrackers.getOrPut(position) { ScrollTracker() }.apply {
                attach(scrollView)
                onScrollStateChanged = { isScrolled ->
                    if (position == fragment.currentPagePosition) {
                        fragment.pageScrollState = isScrolled
                        fragment.checkPageReadConditions()
                    }
                }
            }
        }

        override fun getItemCount() = allPages.size

        private fun addAyahToView(container: ViewGroup, ayah: Ayah) {
            val context = container.context
            val ayahBinding = ItemAyahBinding.inflate(
                LayoutInflater.from(context),
                container,
                false
            ).apply {
                ayahNumberTextView.text = ayah.ayahNumber.toString()
                ayahTextView.text = ayah.text

                // Ensure Arabic font is applied
                arabicTypeface?.let {
                    ayahTextView.typeface = it
                }

                // RTL support
                ayahTextView.textDirection = View.TEXT_DIRECTION_RTL

                // Style ayah number
                ayahNumberTextView.apply {
                    setBackgroundResource(R.drawable.circle_background)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    gravity = Gravity.CENTER
                }

                // Add spacing between ayahs
                (root.layoutParams as? MarginLayoutParams)?.bottomMargin = 16.dpToPx(context)
            }

            container.addView(ayahBinding.root)
        }

        // Helper extension
        fun Int.dpToPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()
    }

    private fun markPageAsRead(pageNumber: Int) {
        val surahNumber = getSurahForPage(pageNumber).number
        val ayahRanges = allPages[pageNumber]

        // Find the last ayah on this page
        val lastAyahRange = ayahRanges.last()
        val lastAyahNumber = lastAyahRange.end - getFirstLineNumberForSurah(lastAyahRange.surah) + 1

        val timeSpent = (System.currentTimeMillis() - readingStartTime) / 1000 // Convert ms to seconds

        // Save reading progress
        saveLastReadAyah(surahNumber, lastAyahNumber)

        // Record in database
        lifecycleScope.launch {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Get existing record for today
            val existingRecord = statisticsDao.getByDate(currentDate)

            if (existingRecord != null) {
                // Update existing record
                statisticsDao.upsert(
                    existingRecord.copy(
                        pagesRead = existingRecord.pagesRead + 1,
                        secondsSpendReading = existingRecord.secondsSpendReading + timeSpent.toInt()
                    )
                )
            } else {
                // Create new record
                statisticsDao.upsert(
                    PagesReadOnDay(
                        date = currentDate,
                        pagesRead = 1,
                        secondsSpendReading = 0
                    )
                )
            }

            withContext(Dispatchers.Main) {
                showPageReadFeedback()
            }

            Log.d("QuranReader", "Marked page $pageNumber as read with $timeSpent seconds spent")
        }
    }
}

data class PageAyahRange(val surah: Int, val start: Int, val end: Int)