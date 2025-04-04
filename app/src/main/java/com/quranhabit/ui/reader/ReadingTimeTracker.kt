package com.quranhabit.ui.reader

class ReadingTimeTracker {
    private var startTime: Long = 0
    private var accumulatedTime: Long = 0
    private var isRunning: Boolean = false

    fun start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis()
            isRunning = true
        }
    }

    fun pause(): Int {
        if (isRunning) {
            accumulatedTime += System.currentTimeMillis() - startTime
            isRunning = false
            return (accumulatedTime / 1000).toInt()
        }
        return 0
    }

    fun reset() {
        accumulatedTime = 0
        isRunning = false
    }

    fun getTotalSeconds(): Int {
        val totalMillis = accumulatedTime + if (isRunning) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
        return (totalMillis / 1000).toInt()
    }
}