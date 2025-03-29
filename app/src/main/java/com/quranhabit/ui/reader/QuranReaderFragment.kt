package com.quranhabit.ui.reader

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.R

class QuranReaderFragment : Fragment() {

    private lateinit var quranTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quran_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quranTextView = view.findViewById(R.id.quranTextView)
        val pageNumber = 107 // Replace with actual page number
        val quranText = loadPageText(pageNumber)
        quranTextView.text = quranText

        val typeface = Typeface.createFromAsset(requireContext().assets, "fonts/KFGQPC Hafs.otf")
        quranTextView.typeface = typeface
    }

    private val cachedPages by lazy {
        val json = loadJsonFromRaw(R.raw.pages)
        Gson().fromJson<List<List<List<PageAyahRange>>>>(
            json,
            object : TypeToken<List<List<List<PageAyahRange>>>>() {}.type
        )
    }

    private fun loadPageText(pageNumber: Int): String {
        return cachedPages
            .getOrNull(pageNumber - 1)
            ?.flatten()
            ?.joinToString("\n") { range ->
                quranLines.slice(range.start - 1 until range.end).joinToString("\n")
            } ?: "Page not found"
    }

    private fun loadJsonFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    private val quranLines by lazy {
        loadTextFromRaw(R.raw.quran_uthmani).lines()
    }

    private fun loadTextFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }
}

data class PageAyahRange(val surah: Int, val start: Int, val end: Int)