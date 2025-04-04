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
        pageContent.removeAllViews() // Clear previous content

        pageRanges.forEach { range ->
            val firstLineForSurah = getFirstLineNumber(range.surah)
            (range.start..range.end).forEach { lineNumber ->
                val lineText = quranLines[lineNumber - 1] // Lines are 1-indexed
                val ayahNumber = lineNumber - firstLineForSurah + 1
                val ayah = Ayah(range.surah, ayahNumber, lineText)

                // Handle Basmalah (for non-Fatiha/Tawba)
                if (isBasmala(ayah)) {
                    addBasmalaView(pageContent, ayah.surahNumber)
                    // Add remaining text if Basmalah isn't the entire line
                    if (lineText.length > basmalaText.length) {
                        val remainingText = lineText.substring(basmalaText.length).trim()
                        if (remainingText.isNotEmpty()) {
                            addAyahView(pageContent, ayah.copy(text = remainingText))
                        }
                    }
                } else {
                    // Normal ayah
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

    private fun addBasmalaView(container: ViewGroup, surahNumber: Int) {
        val binding = ItemBasmalaBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        binding.root.tag = "basmalah_$surahNumber" // Tag with surah number
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