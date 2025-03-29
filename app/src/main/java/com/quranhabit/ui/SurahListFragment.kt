package com.quranhabit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.R

class SurahListFragment : Fragment() {

    private lateinit var surahRecyclerView: RecyclerView
    private lateinit var surahAdapter: SurahAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_surah_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surahRecyclerView = view.findViewById(R.id.surahRecyclerView)
        surahRecyclerView.layoutManager = LinearLayoutManager(context)

        val surahList = getSurahList()
        surahAdapter = SurahAdapter(surahList) { surah ->
            // Manual navigation (no Directions class required)
            findNavController().navigate(
                R.id.action_surahListFragment_to_quranReaderFragment,
                Bundle().apply { putInt("surahNumber", surah.number) }
            )
        }

        surahRecyclerView.adapter = surahAdapter
    }

    private fun getSurahList(): List<Surah> {
        return listOf(
            Surah(1, "Al-Fatiha"),
            Surah(2, "Al-Baqarah"),
            // Add all 114 surahs
        )
    }
}