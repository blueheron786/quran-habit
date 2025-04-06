package com.quranhabit.ui.reader

import android.util.Log
import androidx.core.widget.NestedScrollView

class ScrollTracker {
    private var scrollView: NestedScrollView? = null
    var onScrollStateChanged: ((Boolean) -> Unit)? = null
    var onScrollPositionChanged: ((Boolean) -> Unit)? = null
    private var isScrolling = false
    private var isBottomReached = false
    private var lastScrollY: Int = 0

    fun attach(view: NestedScrollView) {
        Log.d("ScrollTracker", "Attaching to scroll view")

        scrollView = view
        onScrollStateChanged?.invoke(false) // Initial state is not scrolling

        view.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            Log.d("ScrollTracker", "Scroll detected - Y: $scrollY, oldY: $oldScrollY")

            // Track scrolling state
            val newScrollingState = scrollY != oldScrollY
            if (newScrollingState != isScrolling) {
                isScrolling = newScrollingState
                Log.d("ScrollTracker", "Scroll state changed: $isScrolling")
                onScrollStateChanged?.invoke(isScrolling)
            }

            // Track bottom position
            val contentHeight = scrollView?.getChildAt(0)?.height ?: 0
            val visibleHeight = scrollView?.height ?: 0
            val newBottomState = (scrollY + visibleHeight) >= contentHeight - PIXELS_BUFFER

            if (newBottomState != isBottomReached) {
                isBottomReached = newBottomState
                Log.d("ScrollTracker", "Bottom state changed: $isBottomReached")
                onScrollPositionChanged?.invoke(isBottomReached)
            }
        })
    }

    fun getScrollY(): Int {
        return scrollView?.scrollY ?: 0
    }

    fun saveScrollPosition() {
        lastScrollY = scrollView?.scrollY ?: 0
    }

    fun detach() {
        saveScrollPosition()

        scrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        scrollView = null
        onScrollStateChanged = null
        onScrollPositionChanged = null  // Prevent stale callbacks
    }
    
    companion object {
        const val PIXELS_BUFFER = 16
    }
}