package com.quranhabit.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quranhabit.R

class QuranReaderFragment : Fragment() {

    private lateinit var quranTextView: TextView
    private val args: QuranReaderFragmentArgs by navArgs()

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

    private fun loadPageText(pageNumber: Int): String {
        val pagesJson = loadJsonFromRaw(R.raw.pages)
        val pagesList = Gson().fromJson<List<List<PageAyahRange>>>(pagesJson, object : TypeToken<List<List<PageAyahRange>>>() {}.type)

        val quranText = loadTextFromRaw(R.raw.quran_uthmani).split("\n").toTypedArray()

        if (pageNumber in 1..pagesList.size) {
            val ayahRanges = pagesList[pageNumber - 1]
            val allAyahs = mutableListOf<String>()
            for (range in ayahRanges) {
                allAyahs.addAll(quranText.sliceArray(range.start - 1 until range.end).toList()) // Adjusting index to start from 0
            }
            return allAyahs.joinToString("\n")
        } else {
            return "Page text not found."
        }
    }

    private fun loadJsonFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    private fun loadTextFromRaw(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }
}

data class PageAyahRange(val surah: Int, val start: Int, val end: Int)