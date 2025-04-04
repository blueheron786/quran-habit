package com.quranhabit.ui.reader.components

import android.util.Log
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.quranhabit.R
import com.quranhabit.ui.reader.model.PageAyahRange

class NavigationHandler(
    private val viewPager: ViewPager2,
    private val allPages: List<List<PageAyahRange>>,
    private val getFirstLineNumber: (Int) -> Int
) {
    // Remove LastReadRepository parameter if not needed
    // Keep only the essential navigation methods

    fun findFirstPageForSurah(surahNumber: Int): Int {
        return allPages.indexOfFirst { pageRanges ->
            pageRanges.any { it.surah == surahNumber }
        }.coerceAtLeast(0)
    }

    fun scrollToAyah(page: Int, surah: Int, ayah: Int) {
        try {
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(page) ?: return
            val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view) ?: return

            scrollView.post {
                try {
                    val ayahView = scrollView.findViewWithTag<View>("ayah_${surah}_$ayah")
                        ?: scrollView.findViewWithTag<View>("ayah_$ayah")

                    ayahView?.let {
                        scrollView.smoothScrollTo(0, it.top)
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Scroll failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Scroll setup failed", e)
        }
    }
}