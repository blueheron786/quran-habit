package com.quranhabit.ui.reader

import androidx.core.widget.NestedScrollView

class ScrollTracker {
    private var scrollView: NestedScrollView? = null
    var onScrollStateChanged: ((Boolean) -> Unit)? = null
    var onScrollPositionChanged: ((Boolean) -> Unit)? = null
    private var isScrolling = false
    private var isBottomReached = false

    fun attach(view: NestedScrollView) {
        scrollView = view
        view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            // Track scrolling state
            val newScrollingState = scrollY != oldScrollY
            if (newScrollingState != isScrolling) {
                isScrolling = newScrollingState
                onScrollStateChanged?.invoke(isScrolling)
            }

            // Track bottom position
            val contentHeight = scrollView?.getChildAt(0)?.height ?: 0
            val visibleHeight = scrollView?.height ?: 0
            val newBottomState = (scrollY + visibleHeight) >= contentHeight - PIXELS_BUFFER

            if (newBottomState != isBottomReached) {
                isBottomReached = newBottomState
                onScrollPositionChanged?.invoke(isBottomReached)
            }
        })
    }

    fun detach() {
        scrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        scrollView = null
    }

    companion object {
        private const val PIXELS_BUFFER = 16
    }
}