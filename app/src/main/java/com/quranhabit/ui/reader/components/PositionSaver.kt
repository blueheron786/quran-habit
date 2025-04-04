package com.quranhabit.ui.reader

import android.content.SharedPreferences
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.data.repository.LastReadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PositionSaver(
    private val sharedPrefs: SharedPreferences,
    private val lastReadRepo: LastReadRepository,
    private val statisticsDao: StatisticsDao
) {
    fun saveLastReadAyah(surah: Int, ayah: Int, page: Int) {
        sharedPrefs.edit().apply {
            putInt("lastSurah", surah)
            putInt("lastAyah", ayah)
            putInt("lastPage", page)
            apply()
        }
    }

    suspend fun savePosition(surah: Int, ayah: Int, page: Int, scrollY: Int) {
        withContext(Dispatchers.IO) {
            lastReadRepo.savePosition(surah, ayah, page, scrollY)
        }
    }

    suspend fun logPageRead(page: Int, secondsRead: Int) {
        withContext(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingRecord = statisticsDao.getByDate(currentDate)

            if (existingRecord != null) {
                statisticsDao.upsert(existingRecord.copy(
                    pagesRead = existingRecord.pagesRead + 1,
                    secondsSpendReading = existingRecord.secondsSpendReading + secondsRead
                ))
            } else {
                statisticsDao.upsert(PagesReadOnDay(
                    date = currentDate,
                    pagesRead = 1,
                    secondsSpendReading = secondsRead
                ))
            }
        }
    }
}