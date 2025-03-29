package com.quranhabit.ui.surah

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.R

class SurahAdapter(private val surahList: List<Surah>, private val onSurahClick: (Surah) -> Unit) :
    RecyclerView.Adapter<SurahAdapter.SurahViewHolder>() {

    class SurahViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val surahNameTextView: TextView = itemView.findViewById(R.id.surahNameTextView)
        val surahNumberTextView: TextView = itemView.findViewById(R.id.surahNumberTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurahViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_surah, parent, false)
        return SurahViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SurahViewHolder, position: Int) {
        val currentSurah = surahList[position]
        holder.surahNameTextView.text = currentSurah.name
        holder.surahNumberTextView.text = "Surah ${currentSurah.number}"

        holder.itemView.setOnClickListener {
            onSurahClick(currentSurah)
        }
    }

    override fun getItemCount() = surahList.size
}