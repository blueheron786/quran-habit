package com.quranhabit.ui.reader

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.quranhabit.R
import androidx.core.widget.NestedScrollView
import com.quranhabit.data.repository.LastReadRepository
import com.quranhabit.ui.reader.model.PageAyahRange

class NavigationHandler(
    private val viewPager: ViewPager2,
    private val allPages: List<List<PageAyahRange>>,
    private val lastReadRepo: LastReadRepository,
    private val getFirstLineNumber: (Int) -> Int
) {
    fun findFirstPageForSurah(surahNumber: Int): Int {
        // Find the first page containing any ayah from this surah
        return allPages.indexOfFirst { pageRanges ->
            pageRanges.any { it.surah == surahNumber }
        }.takeIf { it != -1 } ?: 0 // Default to first page if not found
    }

    fun findPageForAyah(surah: Int, ayah: Int): Int {
        return allPages.indexOfFirst { page ->
            page.any { range ->
                if (range.surah == surah) {
                    val firstAyah = range.start - getFirstLineNumber(surah) + 1
                    val lastAyah = range.end - getFirstLineNumber(surah) + 1
                    ayah in firstAyah..lastAyah
                } else false
            }
        }.coerceIn(0, allPages.size - 1)
    }

    fun scrollToAyah(page: Int, surah: Int, ayah: Int) {
        try {
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(page) ?: return
            val scrollView = viewHolder.itemView.findViewById<NestedScrollView>(R.id.page_scroll_view)

            scrollView.post {
                // Try both tag formats
                val ayahView = scrollView.findViewWithTag<View>("ayah_${surah}_$ayah")
                    ?: scrollView.findViewWithTag<View>("ayah_$ayah")

                ayahView?.let {
                    scrollView.smoothScrollTo(0, it.top)
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to scroll to ayah", e)
        }
    }
}