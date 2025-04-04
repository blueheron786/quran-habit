package com.quranhabit.ui.reader.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.databinding.ItemPageBinding
import com.quranhabit.ui.reader.QuranPageRenderer
import com.quranhabit.ui.reader.QuranReaderFragment
import com.quranhabit.ui.reader.model.PageAyahRange

class QuranPageAdapter(
    private val fragment: QuranReaderFragment,
    private val allPages: List<List<PageAyahRange>>,
    private val pageRenderer: QuranPageRenderer
) : RecyclerView.Adapter<QuranPageAdapter.PageViewHolder>() {

    inner class PageViewHolder(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        pageRenderer.renderPage(holder.binding.pageContent, allPages[position])
    }

    override fun getItemCount(): Int = allPages.size
}