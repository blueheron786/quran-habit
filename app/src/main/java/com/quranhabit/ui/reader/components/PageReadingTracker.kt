package com.quranhabit.ui.reader

import android.os.CountDownTimer
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.data.entity.PagesReadOnDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PageReadingTracker(
    private val statisticsDao: StatisticsDao,
    private val lifecycleScope: CoroutineScope
) {
    private val timeTracker = ReadingTimeTracker()
    private var pageTimer: CountDownTimer? = null
    private var bottomTimer: CountDownTimer? = null

    private var currentPagePosition = -1
    private var isAtBottom = false
    private var pageMarked = false
    private val pageReadStates = mutableMapOf<Int, Boolean>()

    companion object {
        private const val PAGE_READ_DELAY_MS = 3000L
        private const val PAGE_READ_CHECK_INTERVAL = 1000L
    }

    fun startTracking() = timeTracker.start()
    fun pauseTracking() = timeTracker.pause()

    fun handlePageChange(newPage: Int) {
        val secondsRead = timeTracker.pause()
        if (secondsRead > 0) {
            logReadingTime(secondsRead)
        }

        timeTracker.reset()
        timeTracker.start()

        cancelAllTimers()
        resetPageStates(newPage)
    }

    private fun resetPageStates(newPage: Int) {
        pageTimer?.cancel()
        bottomTimer?.cancel()
        currentPagePosition = newPage
        pageMarked = false
        isAtBottom = false

        if (!pageReadStates.getOrDefault(newPage, false)) {
            startPageReadTimer()
        }
    }

    fun startPageReadTimer(onComplete: () -> Unit = { checkPageReadConditions() }) {
        pageTimer?.cancel()
        pageTimer = object : CountDownTimer(PAGE_READ_DELAY_MS, PAGE_READ_CHECK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                pageTimer = null
                onComplete()
            }
        }.start()
    }

    fun handleBottomPositionChange(atBottom: Boolean) {
        isAtBottom = atBottom
        if (atBottom) {
            startBottomTimer()
        } else {
            bottomTimer?.cancel()
            bottomTimer = null
        }
    }

    private fun startBottomTimer() {
        if (bottomTimer == null) {
            bottomTimer = object : CountDownTimer(1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    if (isAtBottom) {
                        checkPageReadConditions()
                    }
                    bottomTimer = null
                }
            }.start()
        }
    }

    fun checkPageReadConditions() {
        if (!pageMarked && isAtBottom && pageTimer == null) {
            pageMarked = true
            pageReadStates[currentPagePosition] = true
            logReadingTime(timeTracker.getTotalSeconds())
            timeTracker.reset()
        }
    }

    fun logReadingTime(seconds: Int) {
        if (seconds <= 0) return

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existingRecord = statisticsDao.getByDate(currentDate)

                if (existingRecord != null) {
                    statisticsDao.upsert(existingRecord.copy(
                        secondsSpendReading = existingRecord.secondsSpendReading + seconds
                    ))
                } else {
                    statisticsDao.upsert(PagesReadOnDay(
                        date = currentDate,
                        pagesRead = 0,
                        secondsSpendReading = seconds
                    ))
                }
            }
        }
    }

    fun cancelAllTimers() {
        pageTimer?.cancel()
        bottomTimer?.cancel()
    }
}