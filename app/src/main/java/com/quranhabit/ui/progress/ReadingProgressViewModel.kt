package com.quranhabit.ui.progress

import androidx.lifecycle.*
import com.quranhabit.data.dao.ReadingSessionDao
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReadingProgressViewModel(
    private val readingSessionDao: ReadingSessionDao
) : ViewModel() {
    private val today = LocalDate.now().toString()

    val daysProgress = readingSessionDao.getDaysProgress(today)
        .map { it ?: 0 }
        .asLiveData()

    val totalProgress = readingSessionDao.getTotalPagesRead()
        .map { it }
        .asLiveData()

    fun resetStatistics() {
        viewModelScope.launch {
            readingSessionDao.resetAllStatistics()
            // You might want to refresh the stats after resetting
            // Add any necessary refresh logic here
        }
    }
}