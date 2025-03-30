package com.quranhabit.ui.progress

import androidx.lifecycle.*
import com.quranhabit.data.dao.ReadingSessionDao
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ReadingProgressViewModel(
    private val readingSessionDao: ReadingSessionDao
) : ViewModel() {
    private val today = LocalDate.now().toString()

    val todayProgress = readingSessionDao.getTodayProgress(today)
        .map { it ?: 0 }
        .asLiveData()

    val totalProgress = readingSessionDao.getTotalPagesRead()
        .asLiveData()

    val weeklyStreak = readingSessionDao.getWeeklyStreak()
        .asLiveData()
}