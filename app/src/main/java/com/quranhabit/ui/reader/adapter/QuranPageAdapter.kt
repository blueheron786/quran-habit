package com.quranhabit.ui.reader.adapter

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.databinding.ItemPageBinding
import com.quranhabit.ui.reader.QuranPageRenderer
import com.quranhabit.ui.reader.QuranReaderFragment
import com.quranhabit.ui.reader.ScrollTracker
import com.quranhabit.ui.reader.model.PageAyahRange

class QuranPageAdapter(
    private val fragment: QuranReaderFragment,
    private val allPages: List<List<PageAyahRange>>,
    private val pageRenderer: QuranPageRenderer
) : RecyclerView.Adapter<QuranPageAdapter.PageViewHolder>() {

    private val scrollPositions = SparseArray<Int>()

    inner class PageViewHolder(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root) {
        val scrollTracker = ScrollTracker()
    }

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

        val scrollView = holder.binding.pageScrollView
        scrollView.scrollTo(0, scrollPositions.get(position, 0))

        holder.scrollTracker.attach(scrollView)
        holder.scrollTracker.onScrollStateChanged = { isScrolled ->
            if (position == fragment.getCurrentPagePosition()) {
                fragment.getReadingTracker().checkPageReadConditions()
            }
        }
        holder.scrollTracker.onScrollPositionChanged = { atBottom ->
            if (position == fragment.getCurrentPagePosition()) {
                fragment.getReadingTracker().handleBottomPositionChange(atBottom)
            }
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            scrollPositions.put(position, holder.scrollTracker.getScrollY())
            holder.scrollTracker.detach()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = allPages.size
}