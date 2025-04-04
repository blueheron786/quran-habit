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
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.data.repository.LastReadRepository
import com.quranhabit.ui.reader.adapter.QuranPageAdapter
import com.quranhabit.ui.reader.model.PageAyahRange
import com.quranhabit.ui.surah.Surah

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var readingTracker: PageReadingTracker
    private lateinit var pageRenderer: QuranPageRenderer
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var positionSaver: PositionSaver
    private lateinit var headerUpdater: HeaderUpdater

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

    // Getter for adapter access
    internal fun getReadingTracker(): PageReadingTracker = readingTracker
    internal fun getCurrentPagePosition(): Int = binding.quranPager.currentItem

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

        // Initialize data
        allPages = loadPagesData()
        quranLines = loadQuranText()

        // Get initial surah number from arguments
        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        val ayahNumber = arguments?.getInt("ayahNumber") ?: 1

        // Initialize components AFTER we have the surah number
        readingTracker = PageReadingTracker(statisticsDao, lifecycleScope)
        pageRenderer = QuranPageRenderer(requireContext(), quranLines)
        navigationHandler = NavigationHandler(
            binding.quranPager,
            allPages,
            lastReadRepo,
            ::getFirstLineNumberForSurah
        )
        positionSaver = PositionSaver(prefs, lastReadRepo, statisticsDao)
        headerUpdater = HeaderUpdater(binding)

        // Setup view pager and handle navigation in one go
        setupViewPagerAndNavigate(currentSurahNumber, ayahNumber)
    }

    private fun setupViewPagerAndNavigate(surah: Int, ayah: Int) {
        binding.quranPager.adapter = QuranPageAdapter(this, allPages, pageRenderer)
        binding.quranPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.quranPager.offscreenPageLimit = 2

        // Set the page first
        val targetPage = navigationHandler.findFirstPageForSurah(surah)
        binding.quranPager.setCurrentItem(targetPage, false)

        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(newPage: Int) {
                handlePageChange(newPage)
            }
        })

        // Then handle ayah navigation after layout is complete
        binding.quranPager.post {
            if (arguments?.containsKey("ayahNumber") == true) {
                navigationHandler.scrollToAyah(targetPage, surah, ayah)
            }
            headerUpdater.update(surah, targetPage)
        }
    }

    private fun handleInitialNavigation(surah: Int, ayah: Int) {
        val targetPage = navigationHandler.findPageForAyah(surah, ayah)
        if (binding.quranPager.currentItem != targetPage) {
            binding.quranPager.setCurrentItem(targetPage, false)
        }
    }

    private fun scrollToAyah(surah: Int, ayah: Int) {
        val recyclerView = binding.quranPager.getChildAt(0) as? RecyclerView ?: return
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(binding.quranPager.currentItem) ?: return
        val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view)

        scrollView.post {
            try {
                val ayahView = scrollView.findViewWithTag<View?>("ayah_${surah}_$ayah")
                    ?: scrollView.findViewWithTag<View?>("ayah_$ayah")

                ayahView?.let {
                    scrollView.smoothScrollTo(0, it.top)
                }
            } catch (e: Exception) {
                Log.e("QuranReader", "Ayah scroll failed", e)
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
        headerUpdater.update(currentSurahNumber, newPage)
    }

    private fun getSurahForPage(position: Int): Surah {
        val surahNumber = allPages[position].first().surah
        return SurahRepository.getSurahByNumber(surahNumber) ?: Surah(0, "Unknown", "???")
    }

    private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
        return allPages.flatten().firstOrNull { it.surah == surahNumber }?.start ?: 0
    }

    override fun onResume() {
        super.onResume()
        readingTracker.startTracking()
        (requireActivity() as MainActivity).setBottomNavVisibility(false)
    }

    override fun onPause() {
        super.onPause()
        readingTracker.pauseTracking()
        saveCurrentPosition()
        (requireActivity() as MainActivity).setBottomNavVisibility(true)
    }

    override fun onDestroyView() {
        readingTracker.cancelAllTimers()
        super.onDestroyView()
        _binding = null
    }

    private fun saveCurrentPosition() {
        val currentPage = binding.quranPager.currentItem
        if (currentPage in allPages.indices) {
            val surah = getSurahForPage(currentPage).number
            val ayahRanges = allPages[currentPage]
            val lastAyahRange = ayahRanges.last()
            val lastAyahNumber = lastAyahRange.end - getFirstLineNumberForSurah(lastAyahRange.surah) + 1

            positionSaver.saveLastReadAyah(surah, lastAyahNumber, currentPage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                readingTracker.pauseTracking()
                saveCurrentPosition()
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }
}