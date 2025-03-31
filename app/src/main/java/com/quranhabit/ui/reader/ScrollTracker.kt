package com.quranhabit.ui.reader

import android.util.Log
import androidx.core.widget.NestedScrollView

class ScrollTracker() {
    var onScrollStateChanged: ((Boolean) -> Unit)? = null
    private var isBottomReached = false

    fun attach(scrollView: NestedScrollView) {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val contentHeight = scrollView.getChildAt(0)?.height ?: 0
            val visibleHeight = scrollView.height
            // Check if we've reached the absolute bottom (with a small buffer of pixels)
            val newState = (scrollY + visibleHeight) >= contentHeight - PIXELS_BUFFER

            if (newState != isBottomReached) {
                isBottomReached = newState
                onScrollStateChanged?.invoke(isBottomReached)
                Log.d("ScrollTracker", "Bottom ${if (newState) "reached" else "lost"}")
            }
        }
    }

    companion object {
        private const val PIXELS_BUFFER = 16
    }
}