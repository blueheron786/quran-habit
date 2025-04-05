package com.quranhabit.ui.reader

import android.view.View
import androidx.core.widget.NestedScrollView

class ScrollTracker {
    private var scrollView: NestedScrollView? = null
    private var isTracking = false

    private var onScrollStateChanged: ((Boolean) -> Unit)? = null
    private var onScrollPositionChanged: ((Boolean) -> Unit)? = null
    var onReachedBottom: (() -> Unit)? = null
    var onScrolledUpFromBottom: (() -> Unit)? = null

    private val scrollListener = NestedScrollView.OnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
        (v as? NestedScrollView)?.let { view ->
            val atBottom = isAtBottom(view)

            // Notify position changes
            onScrollPositionChanged?.invoke(atBottom)

            // Notify scroll state (whether scrolling occurred)
            val isScrolling = scrollY != oldScrollY
            onScrollStateChanged?.invoke(isScrolling)

            // Edge detection callbacks
            if (atBottom) {
                onReachedBottom?.invoke()
            } else if (isScrolling && wasAtBottom && !atBottom) {
                onScrolledUpFromBottom?.invoke()
            }

            wasAtBottom = atBottom
        }
    }

    private var wasAtBottom = false

    fun attach(scrollView: NestedScrollView) {
        if (this.scrollView == scrollView && isTracking) return

        detach()
        this.scrollView = scrollView
        scrollView.setOnScrollChangeListener(scrollListener)
        isTracking = true
    }

    fun detach() {
        scrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        scrollView = null
        isTracking = false
        wasAtBottom = false
    }

    private fun isAtBottom(scrollView: NestedScrollView): Boolean {
        return scrollView.getChildAt(0)?.let { child ->
            val scrollRange = child.height - scrollView.height
            scrollView.scrollY >= scrollRange - SCROLL_THRESHOLD
        } ?: false
    }

    companion object {
        private const val SCROLL_THRESHOLD = 10 // pixels
    }
}