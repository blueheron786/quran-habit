package com.quranhabit.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.R
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.databinding.ItemPageBinding
import com.quranhabit.ui.surah.Ayah
import com.quranhabit.ui.surah.Surah
import com.quranhabit.data.SurahRepository

class QuranReaderFragment : Fragment() {

    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var arabicTypeface: Typeface
    private lateinit var pageAdapter: QuranPageAdapter
    private lateinit var allPages: List<List<PageAyahRange>>
    private lateinit var quranLines: List<String>
    private var currentSurahNumber = 1
    private var currentScrollY = 0

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
            "fonts/kitab.ttf"
        )

        currentSurahNumber = arguments?.getInt("surahNumber") ?: 1
        allPages = cachedPages.flatten()
        quranLines = loadTextFromRaw(R.raw.quran_uthmani).lines()

        setupViewPager()
        updateHeader()
    }

    private fun setupViewPager() {
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
        binding.quranPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateHeader()
                saveCurrentPage(position)
            }
        })

        val initialPage = findFirstPageForSurah(currentSurahNumber)
        binding.quranPager.setCurrentItem(initialPage, false)
    }

    private fun updateHeader() {
        val currentPage = binding.quranPager.currentItem
        val currentSurah = getSurahForPage(currentPage)
        binding.surahInfoTextView.text = "${currentSurah.number}. ${currentSurah.englishName}"
        binding.pageInfoTextView.text = "Page ${currentPage + 1}/${allPages.size}"
    }

    private fun getSurahForPage(pageIndex: Int): Surah {
        val surahNumber = allPages[pageIndex].first().surah
        return SurahRepository.getSurahByNumber(surahNumber) ?:
        Surah(0, "Unknown", "غير معروف")
    }

    private fun findFirstPageForSurah(surahNumber: Int): Int {
        return allPages.indexOfFirst { page ->
            page.any { it.surah == surahNumber }
        }.takeIf { it != -1 } ?: 0
    }

    private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
        return try {
            cachedPages
                .flatten()
                .flatten()
                .first { it.surah == surahNumber }
                .start
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

        Toast.makeText(context, "Saved position: Surah $surahNumber Ayah $ayahNumber", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentPage(page: Int) {
        requireContext().getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE).edit()
            .putInt("lastPage", page)
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
            val pageRanges = allPages[position]
            holder.binding.pageContent.removeAllViews()

            pageRanges.forEach { range ->
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
            )

            ayahBinding.apply {
                ayahNumberTextView.text = ayah.ayahNumber.toString()
                ayahTextView.text = ayah.text
                ayahTextView.typeface = arabicTypeface
                ayahTextView.textDirection = View.TEXT_DIRECTION_RTL

                markButton.setOnClickListener {
                    onAyahMarked(ayah.surahNumber, ayah.ayahNumber)
                }

                ayahNumberTextView.setBackgroundResource(R.drawable.circle_background)
                ayahNumberTextView.setTextColor(container.context.getColor(android.R.color.white))
                ayahNumberTextView.gravity = android.view.Gravity.CENTER
                ayahNumberTextView.textSize = 12f
            }

            container.addView(ayahBinding.root)
        }
    }
}

data class PageAyahRange(
    val surah: Int,
    val start: Int,
    val end: Int
)