package com.quranhabit.ui.reader

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.MainActivity
import com.quranhabit.R
import com.quranhabit.data.QuranDatabase
import com.quranhabit.data.SurahRepository
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.data.repository.LastReadRepository
import com.quranhabit.ui.reader.adapter.QuranPageAdapter
import com.quranhabit.ui.reader.components.PageReadingTracker
import com.quranhabit.ui.reader.model.PageAyahRange
import com.quranhabit.ui.surah.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var readingTracker: PageReadingTracker
    private lateinit var pageRenderer: QuranPageRenderer
    private lateinit var positionSaver: PositionSaver
    private lateinit var headerUpdater: HeaderUpdater
    private lateinit var scrollTracker: ScrollTracker

    private lateinit var allPages: List<List<PageAyahRange>>
    private lateinit var quranLines: List<String>
    private var currentSurahNumber = 1

    // Database
    private val database by lazy { QuranDatabase.getDatabase(requireContext()) }
    private val statisticsDao by lazy { database.statisticsDao() }
    private val lastReadRepo by lazy {
        LastReadRepository(QuranDatabase.getDatabase(requireContext()).lastReadPositionDao())
    }
    private val prefs by lazy { requireContext().getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuranReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        // Initialize data first
        allPages = loadPagesData()
        quranLines = loadQuranText()

        // Get initial surah number from arguments
        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        val ayahNumber = arguments?.getInt("ayahNumber") ?: 1
        Log.d("Navigation", "Initial surah: $currentSurahNumber, ayah: $ayahNumber")

        // Initialize components
        readingTracker = PageReadingTracker(statisticsDao, lifecycleScope).apply {
            onPageMarkedRead = { page, seconds ->
                // Proper coroutine launch
                lifecycleScope.launch {
                    try {
                        logPageRead(seconds)
                        val surah = getSurahForPage(page).number
                        positionSaver.savePosition(surah, 1, page, 0)
                    } catch (e: Exception) {
                        Log.e("PositionSaver", "Error saving position", e)
                    }
                }
            }
        }

        positionSaver = PositionSaver(prefs, lastReadRepo)
        headerUpdater = HeaderUpdater(binding)
        pageRenderer = QuranPageRenderer(
            requireContext(),
            quranLines,
            ::getFirstLineNumberForSurah
        )
        scrollTracker = ScrollTracker().apply {
            onReachedBottom = { readingTracker.handleBottomPositionChange(true) }
            onScrolledUpFromBottom = { readingTracker.handleBottomPositionChange(false) }
        }

        // Disable ViewPager2's initial page set
        binding.quranPager.isSaveEnabled = false

        // Setup view pager with initial page
        Log.d("Navigation", "Going in: $currentSurahNumber, ayah: $ayahNumber")
        setupViewPager(currentSurahNumber, ayahNumber)

        binding.quranPager.post {
            val targetPage = findFirstPageForSurah(currentSurahNumber)
            binding.quranPager.setCurrentItem(targetPage, false)

            // NEW: Force-scroll to the surah's first ayah
            scrollToSurahStart(targetPage, currentSurahNumber)
        }
    }

    private suspend fun logPageRead(secondsRead: Int) {
        withContext(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingRecord = statisticsDao.getByDate(currentDate)

            if (existingRecord != null) {
                statisticsDao.upsert(existingRecord.copy(
                    pagesRead = existingRecord.pagesRead + 1,
                    secondsSpendReading = existingRecord.secondsSpendReading + secondsRead
                ))
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
    }

    private fun setupViewPager(initialSurah: Int, initialAyah: Int) {
        binding.quranPager.adapter = QuranPageAdapter(allPages, pageRenderer)
        binding.quranPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.quranPager.offscreenPageLimit = 2

        // Set initial page before registering callbacks
        val initialPage = findFirstPageForSurah(initialSurah)
        binding.quranPager.setCurrentItem(initialPage, false)

        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val ranges = allPages.getOrNull(position) ?: return
                if (ranges.isNotEmpty()) {
                    currentSurahNumber = ranges.first().surah
                    headerUpdater.update(currentSurahNumber, position + 1)
                    handlePageChange(position) // Add this line
                }

                // Attach scroll tracker to current page
                binding.quranPager.post {
                    val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView ?: return@post
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return@post
                    val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view) ?: return@post
                    scrollTracker.attach(scrollView)
                }
            }
        })

        // Handle ayah scrolling after layout
        if (initialAyah > 1) {
            binding.quranPager.post {
                scrollToAyah(initialPage, initialSurah, initialAyah)
            }
        }
    }

    private fun findFirstPageForSurah(surahNumber: Int): Int {
        // Find the page where the surah ACTUALLY begins (first ayah)
        return allPages.indexOfFirst { pageRanges ->
            pageRanges.any { range ->
                range.surah == surahNumber &&
                        range.start == getFirstLineNumberForSurah(surahNumber) // Exact start line
            }
        }.takeIf { it != -1 } ?: 0 // Fallback to page 0 if not found
    }

    private fun scrollToAyah(page: Int, surah: Int, ayah: Int) {
        try {
            val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView ?: return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(page) ?: return
            val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view) ?: return

            scrollView.post {
                // Find the first ayah of the surah (even if ayahNumber > 1 was passed)
                val firstAyahTag = "ayah_${surah}_1" // Force "1" to target the surah's start
                val ayahView = scrollView.findViewWithTag<View>(firstAyahTag)
                    ?: scrollView.findViewWithTag<View>("ayah_$ayah") // Fallback

                ayahView?.let {
                    scrollView.smoothScrollTo(0, it.top) // Snap to top of the surah
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Scroll to surah start failed", e)
        }
    }

    private fun scrollToSurahStart(page: Int, surah: Int) {
        binding.quranPager.post {
            try {
                val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView ?: return@post
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(page) ?: return@post
                val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view) ?: return@post

                // Find the Basmalah or first ayah
                val basmalahView = scrollView.findViewWithTag<View>("basmalah_$surah") // Tag your Basmalah view!
                    ?: scrollView.findViewWithTag<View>("ayah_${surah}_1")
                    ?: scrollView.findViewWithTag<View>("ayah_1")

                // Scroll 16dp ABOVE the Basmalah (adjust this value as needed)
                val scrollPadding = (16 * resources.displayMetrics.density).toInt()
                basmalahView?.let {
                    scrollView.smoothScrollTo(0, it.top - scrollPadding)
                }
            } catch (e: Exception) {
                Log.e("Scroll", "Failed to scroll to Basmalah", e)
            }
        }
    }

    private fun loadPagesData(): List<List<PageAyahRange>> {
        val json = resources.openRawResource(R.raw.pages_absolute).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<List<List<PageAyahRange>>>>() {}.type
        return Gson().fromJson<List<List<List<PageAyahRange>>>>(json, type).flatten()
    }

    private fun loadQuranText(): List<String> {
        return resources.openRawResource(R.raw.quran_text).bufferedReader().use { it.readText() }.lines()
    }

    private fun handlePageChange(newPage: Int) {
        readingTracker.handlePageChange(newPage)
        currentSurahNumber = getSurahForPage(newPage).number
        Log.d("Navigation", "HandpePageChange(): surah: $currentSurahNumber")
        headerUpdater.update(currentSurahNumber, newPage)
    }

    private fun getSurahForPage(position: Int): Surah {
        val surahNumber = allPages[position].first().surah
        return SurahRepository.getSurahByNumber(surahNumber) ?: Surah(0, "Unknown", "???")
    }

    private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
        // Find the absolute starting line number for this surah
        return allPages.flatten()
            .firstOrNull { it.surah == surahNumber }
            ?.start ?: 0
    }



    private suspend fun saveCurrentPosition() {
        withContext(Dispatchers.IO) {
            val currentPage = binding.quranPager.currentItem
            if (currentPage in allPages.indices) {
                val surah = getSurahForPage(currentPage).number
                val ayahRanges = allPages[currentPage]
                val firstAyahNumber = ayahRanges.first().start - getFirstLineNumberForSurah(surah) + 1

                positionSaver.saveLastReadAyah(surah, firstAyahNumber, currentPage)

                val scrollY = try {
                    (binding.quranPager.getChildAt(0) as? RecyclerView)
                        ?.findViewHolderForAdapterPosition(currentPage)
                        ?.itemView?.findViewById<NestedScrollView>(R.id.page_scroll_view)
                        ?.scrollY ?: 0
                } catch (e: Exception) {
                    0
                }

                positionSaver.savePosition(surah, firstAyahNumber, currentPage, scrollY)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        readingTracker.startTracking()
        (requireActivity() as MainActivity).setBottomNavVisibility(false)
    }

    override fun onPause() {
        super.onPause()
        readingTracker.pauseTracking()
        lifecycleScope.launch {
            saveCurrentPosition()
        }
    }

    override fun onDestroyView() {
        readingTracker.cancelAllTimers()
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    readingTracker.pauseTracking()
                    saveCurrentPosition()
                    remove()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
}