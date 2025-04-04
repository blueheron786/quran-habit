package com.quranhabit.ui.reader

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import com.quranhabit.R
import com.quranhabit.databinding.ItemAyahBinding
import com.quranhabit.databinding.ItemBasmalaBinding
import com.quranhabit.ui.reader.model.PageAyahRange
import com.quranhabit.ui.surah.Ayah

class QuranPageRenderer(
    private val context: Context,
    private val quranLines: List<String>
) {
    private val basmalaText = context.getString(R.string.basmala)

    fun renderPage(pageContent: ViewGroup, pageRanges: List<PageAyahRange>) {
        pageContent.removeAllViews()
        pageRanges.forEach { range ->
            renderAyahRange(pageContent, range)
        }
    }

    private fun renderAyahRange(container: ViewGroup, range: PageAyahRange) {
        (range.start..range.end).forEach { lineNumber ->
            val ayah = createAyah(range, lineNumber)
            addAyahToView(container, ayah)
        }
    }

    private fun createAyah(range: PageAyahRange, lineNumber: Int): Ayah {
        val lineText = quranLines[lineNumber - 1]
        return Ayah(
            surahNumber = range.surah,
            ayahNumber = lineNumber - range.start + 1,
            text = lineText
        )
    }

    private fun addAyahToView(container: ViewGroup, ayah: Ayah) {
        if (isBasmala(ayah)) {
            addBasmalaView(container)
            addRemainingAyahText(container, ayah)
        } else {
            addRegularAyahView(container, ayah)
        }
    }

    private fun isBasmala(ayah: Ayah): Boolean {
        return ayah.text.startsWith(basmalaText) &&
                ayah.ayahNumber == 1 &&
                ayah.surahNumber != 1 &&
                ayah.surahNumber != 9
    }

    private fun addBasmalaView(container: ViewGroup) {
        val basmalaBinding = ItemBasmalaBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        container.addView(basmalaBinding.root)
    }

    private fun addRemainingAyahText(container: ViewGroup, ayah: Ayah) {
        ayah.text.removePrefix(basmalaText).trim().takeIf { it.isNotEmpty() }?.let { text ->
            addAyahView(container, ayah.copy(text = fixTextFormatting(text)))
        }
    }

    private fun addRegularAyahView(container: ViewGroup, ayah: Ayah) {
        addAyahView(container, ayah.copy(text = fixTextFormatting(ayah.text)))
    }

    private fun addAyahView(container: ViewGroup, ayah: Ayah) {
        val binding = ItemAyahBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        binding.ayahNumberTextView.text = ayah.ayahNumber.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.ayahTextView.fontFeatureSettings = "'liga' on, 'clig' on"
        }
        binding.ayahTextView.text = ayah.text
        binding.root.tag = "ayah_${ayah.surahNumber}_${ayah.ayahNumber}"
        binding.root.setTag(R.id.ayah_tag, "ayah_${ayah.ayahNumber}")
        container.addView(binding.root)
    }

    private fun fixTextFormatting(text: String): String {
        val smallStops = listOf("\u06D6", "\u06D7", "\u06DA", "\u06D9")
        var fixedText = text
        smallStops.forEach { stop ->
            fixedText = fixedText.replace(stop, "\u2060$stop")
        }
        return fixedText
    }
}