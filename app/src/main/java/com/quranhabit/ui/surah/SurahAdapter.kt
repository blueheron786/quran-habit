package com.quranhabit.ui.surah

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.R

class SurahAdapter(
    private val surahList: List<Surah>,
    private val onItemClick: (Surah) -> Unit
) : RecyclerView.Adapter<SurahAdapter.SurahViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurahViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_surah, parent, false)
        return SurahViewHolder(view)
    }

    override fun onBindViewHolder(holder: SurahViewHolder, position: Int) {
        val surah = surahList[position]
        holder.bind(surah)
        holder.itemView.setOnClickListener { onItemClick(surah) }
    }

    override fun getItemCount() = surahList.size

    class SurahViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val englishNameTextView: TextView = itemView.findViewById(R.id.englishNameTextView)
        private val arabicNameTextView: TextView = itemView.findViewById(R.id.arabicNameTextView)

        fun bind(surah: Surah) {
            englishNameTextView.text = "${surah.number}. ${surah.englishName}"
            arabicNameTextView.text = surah.arabicName

            // Load Arabic font if needed
            val typeface = Typeface.createFromAsset(itemView.context.assets, "fonts/kitab.ttf")
            arabicNameTextView.typeface = typeface
        }
    }
}