package com.quranhabit.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.R
import com.quranhabit.data.QuranDatabase
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.databinding.ItemPageBinding
import com.quranhabit.ui.surah.Ayah
import com.quranhabit.ui.surah.Surah
import com.quranhabit.data.SurahRepository
import com.quranhabit.data.entity.LastReadPosition
import com.quranhabit.data.entity.ReadingSession
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var arabicTypeface: Typeface

    // Page tracking
    private lateinit var pageAdapter: QuranPageAdapter
    private lateinit var allPages: List<List<PageAyahRange>>
    private lateinit var quranLines: List<String>
    private var currentSurahNumber = 1

    // Database
    private val database by lazy { QuranDatabase.getDatabase(requireContext()) }
    private val lastReadDao by lazy { database.lastReadPositionDao() }
    private val readingSessionDao by lazy { database.readingSessionDao() }

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
        super.onViewCreated(view, savedInstanceState)

        arabicTypeface = Typeface.createFromAsset(
            requireContext().assets,
            "fonts/ScheherazadeNewRegular.ttf"
        )

        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        allPages = cachedPages.flatten()
        quranLines = loadTextFromRaw(R.raw.quran_uthmani).lines()

        val initialPage = findFirstPageForSurah(currentSurahNumber)
        setupViewPager(initialPage)
        updateHeader(initialPage)
    }

    private fun setupViewPager(initialPage: Int) {
        pageAdapter = QuranPageAdapter(
            allPages = allPages,
            arabicTypeface = arabicTypeface,
            quranLines = quranLines,
            onAyahMarked = { surahNumber, ayahNumber ->
                saveLastReadAyah(surahNumber, ayahNumber)
            },
            getFirstLineNumber = ::getFirstLineNumberForSurah
        )

        binding.quranPager.adapter = pageAdapter
        binding.quranPager.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.quranPager.setCurrentItem(initialPage, false)

        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var currentPage = -1

            override fun onPageSelected(newPage: Int) {
                updateHeader(newPage)

                if (currentPage != -1 && currentPage != newPage) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        trackPageRead(newPage)
                    }
                }
                currentPage = newPage
            }
        })
    }

    private suspend fun trackPageRead(page: Int) {
        try {
            // 1. Update daily reading
            val today = LocalDate.now().toString()
            val session = readingSessionDao.getByDate(today) ?: ReadingSession(today, 0)
            readingSessionDao.upsert(session.copy(pagesRead = session.pagesRead + 1))

            // 2. Update last read position
            val firstAyah = allPages[page].first().start
            lastReadDao.upsert(
                LastReadPosition(
                    surah = getSurahForPage(page).number,
                    ayah = firstAyah,
                    page = page,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e("QuranReader", "Error tracking page", e)
        }
    }

    private fun updateHeader(position: Int) {
        val currentSurah = getSurahForPage(position)
        binding.surahInfoTextView.text = "${currentSurah.number}. ${currentSurah.englishName}"
        binding.pageInfoTextView.text = "صفحة ${position + 1}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class QuranPageAdapter(
        private val allPages: List<List<PageAyahRange>>,
        private val arabicTypeface: Typeface,
        private val quranLines: List<String>,
        private val onAyahMarked: (surahNumber: Int, ayahNumber: Int) -> Unit,
        private val getFirstLineNumber: (Int) -> Int
    ) : RecyclerView.Adapter<QuranPageAdapter.PageViewHolder>() {

        inner class PageViewHolder(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val binding = ItemPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.binding.pageContent.removeAllViews()
            allPages[position].forEach { range ->
                (range.start..range.end).forEach { lineNumber ->
                    val ayah = Ayah(
                        surahNumber = range.surah,
                        ayahNumber = lineNumber - getFirstLineNumber(range.surah) + 1,
                        text = quranLines[lineNumber - 1]
                    )
                    addAyahToView(holder.binding.pageContent, ayah)
                }
            }
        }

        override fun getItemCount() = allPages.size

        private fun addAyahToView(container: ViewGroup, ayah: Ayah) {
            val ayahBinding = ItemAyahBinding.inflate(
                LayoutInflater.from(container.context),
                container,
                false
            ).apply {
                ayahNumberTextView.text = ayah.ayahNumber.toString()
                ayahTextView.text = ayah.text
                ayahTextView.typeface = arabicTypeface
                ayahTextView.textDirection = View.TEXT_DIRECTION_RTL

                markButton.setOnClickListener { onAyahMarked(ayah.surahNumber, ayah.ayahNumber) }

                ayahNumberTextView.apply {
                    setBackgroundResource(R.drawable.circle_background)
                    setTextColor(container.context.getColor(android.R.color.white))
                    gravity = android.view.Gravity.CENTER
                    textSize = 12f
                }
            }
            container.addView(ayahBinding.root)
        }
    }
}

data class PageAyahRange(val surah: Int, val start: Int, val end: Int)