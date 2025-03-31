package com.quranhabit.ui.reader

import android.util.Log
import androidx.core.widget.NestedScrollView

class ScrollTracker(private val threshold: Float = 0.9f) {
    var onScrollStateChanged: ((Boolean) -> Unit)? = null
    private var isThresholdReached = false

    fun attach(scrollView: NestedScrollView) {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val contentHeight = scrollView.getChildAt(0)?.height ?: 0
            val visibleHeight = scrollView.height
            val newState = (scrollY + visibleHeight) >= contentHeight * threshold

            if (newState != isThresholdReached) {
                isThresholdReached = newState
                onScrollStateChanged?.invoke(isThresholdReached)
                Log.d("ScrollTracker", "Threshold ${if (newState) "reached" else "lost"}")
            }
        }
    }
}