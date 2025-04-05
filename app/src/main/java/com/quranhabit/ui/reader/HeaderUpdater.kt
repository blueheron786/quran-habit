package com.quranhabit.ui.reader

import com.quranhabit.data.SurahRepository
import com.quranhabit.databinding.FragmentQuranReaderBinding

class HeaderUpdater(private val binding: FragmentQuranReaderBinding) {
    fun update(surahNumber: Int, pageNumber: Int) {
        val surah = SurahRepository.getSurahByNumber(surahNumber)
        binding.surahInfoTextView.text = "${surah?.number}. ${surah?.englishName}"
        binding.pageInfoTextView.text = "page ${pageNumber + 1}"
    }
}