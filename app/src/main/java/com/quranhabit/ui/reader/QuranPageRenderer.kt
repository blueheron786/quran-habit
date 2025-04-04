package com.quranhabit.ui.reader

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.quranhabit.R
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.databinding.ItemBasmalaBinding
import com.quranhabit.ui.reader.model.PageAyahRange
import com.quranhabit.ui.surah.Ayah

class QuranPageRenderer(
    private val context: Context,
    private val quranLines: List<String>,
    private val getFirstLineNumber: (Int) -> Int
) {
    private val basmalaText = context.getString(R.string.basmala)

    fun renderPage(pageContent: ViewGroup, pageRanges: List<PageAyahRange>) {
        pageContent.removeAllViews()

        pageRanges.forEach { range ->
            val firstLineForSurah = getFirstLineNumber(range.surah)
            (range.start..range.end).forEach { lineNumber ->
                val lineText = quranLines[lineNumber - 1]
                val ayahNumber = lineNumber - firstLineForSurah + 1
                val ayah = Ayah(range.surah, ayahNumber, lineText)

                if (isBasmala(ayah)) {
                    addBasmalaView(pageContent)
                    if (lineText.length > basmalaText.length) {
                        addAyahView(pageContent, ayah.copy(text = lineText.substring(basmalaText.length).trim()))
                    }
                } else {
                    addAyahView(pageContent, ayah)
                }
            }
        }
    }

    private fun isBasmala(ayah: Ayah): Boolean {
        return ayah.ayahNumber == 1 &&
                ayah.surahNumber != 1 &&
                ayah.surahNumber != 9 &&
                ayah.text.startsWith(basmalaText)
    }

    private fun addBasmalaView(container: ViewGroup) {
        val binding = ItemBasmalaBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        container.addView(binding.root)
    }

    private fun addAyahView(container: ViewGroup, ayah: Ayah) {
        val binding = ItemAyahBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        binding.ayahNumberTextView.text = ayah.ayahNumber.toString()
        binding.ayahTextView.text = ayah.text
        binding.root.tag = "ayah_${ayah.surahNumber}_${ayah.ayahNumber}"
        container.addView(binding.root)
    }
}