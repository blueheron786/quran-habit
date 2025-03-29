package com.quranhabit.ui.reader

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.R
import com.quranhabit.databinding.FragmentQuranReaderBinding
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.ui.surah.Ayah

class QuranReaderFragment : Fragment() {
    private var _binding: FragmentQuranReaderBinding? = null
    private val binding get() = _binding!!
    private lateinit var arabicTypeface: Typeface

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

        // Load Arabic font
        arabicTypeface = Typeface.createFromAsset(
            requireContext().assets,
            "fonts/kitab.ttf"
        )

        // Get surah number from arguments
        val surahNumber = arguments?.getInt("surahNumber") ?: 1

        // Load and display the surah
        displayFirstPageOfSurah(surahNumber)
    }

    private fun displayFirstPageOfSurah(surahNumber: Int) {
        val firstPageAyahs = getFirstPageAyahsForSurah(surahNumber)
        firstPageAyahs.forEach { ayah ->
            addAyahToContainer(ayah)
        }
    }

    private fun getFirstPageAyahsForSurah(surahNumber: Int): List<Ayah> {
        // Get all pages
        val allPages = cachedPages.flatten()

        // Find the first page that contains this surah
        val firstPageWithSurah = allPages.firstOrNull { page ->
            page.any { range -> range.surah == surahNumber }
        } ?: return emptyList()

        // Get all ranges for this surah in the first page
        val surahRanges = firstPageWithSurah.filter { it.surah == surahNumber }

        val quranLines = loadTextFromRaw(R.raw.quran_uthmani).lines()

        return surahRanges.flatMap { range ->
            (range.start..range.end).map { lineNumber ->
                Ayah(
                    surahNumber = surahNumber,
                    ayahNumber = lineNumber - getFirstLineNumberForSurah(surahNumber) + 1,
                    text = quranLines[lineNumber - 1]
                )
            }
        }
    }

    private fun addAyahToContainer(ayah: Ayah) {
        val ayahBinding = ItemAyahBinding.inflate(layoutInflater, binding.quranContainer, false)

        ayahBinding.apply {
            ayahNumberTextView.text = ayah.ayahNumber.toString()
            ayahTextView.text = ayah.text
            ayahTextView.typeface = arabicTypeface
            ayahTextView.textDirection = View.TEXT_DIRECTION_RTL

            markButton.setOnClickListener {
                saveLastReadAyah(ayah.surahNumber, ayah.ayahNumber)
            }

            // Make the circle look better
            ayahNumberTextView.setBackgroundResource(R.drawable.circle_background)
            ayahNumberTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            ayahNumberTextView.gravity = Gravity.CENTER
            ayahNumberTextView.textSize = 12f
        }

        binding.quranContainer.addView(ayahBinding.root)
    }

    private fun getFirstLineNumberForSurah(surahNumber: Int): Int {
        // Implement logic to get the first line number for the given surah
        // This is needed to calculate the correct ayah number
        return cachedPages
            .flatten()
            .flatten()
            .first { it.surah == surahNumber }
            .start
    }

    private fun saveLastReadAyah(surahNumber: Int, ayahNumber: Int) {
        // Implement saving last read position
        // Example using SharedPreferences:
        val sharedPref = requireContext().getSharedPreferences("QuranPrefs", 0)
        with(sharedPref.edit()) {
            putInt("lastSurah", surahNumber)
            putInt("lastAyah", ayahNumber)
            apply()
        }
    }

    // Rest of your existing code (cachedPages, quranLines, loadJsonFromRaw, loadTextFromRaw, PageAyahRange)
    private val cachedPages by lazy {
        val json = loadJsonFromRaw(R.raw.pages_absolute)
        Gson().fromJson<List<List<List<PageAyahRange>>>>(
            json,
            object : TypeToken<List<List<List<PageAyahRange>>>>() {}.type
        )
    }

    private fun loadJsonFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    private fun loadTextFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class PageAyahRange(
    val surah: Int,
    val start: Int,
    val end: Int
)