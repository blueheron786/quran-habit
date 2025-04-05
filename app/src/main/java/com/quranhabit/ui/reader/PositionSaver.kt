package com.quranhabit.ui.reader

import android.content.SharedPreferences
import com.quranhabit.data.repository.LastReadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PositionSaver(
    private val sharedPrefs: SharedPreferences,
    private val lastReadRepo: LastReadRepository,
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
}