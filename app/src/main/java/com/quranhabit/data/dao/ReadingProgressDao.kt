package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.quranhabit.data.entity.ReadingProgress

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress ORDER BY id DESC LIMIT 1")
    fun getLastReadingProgress(): ReadingProgress?

    @Insert
    fun insertReadingProgress(readingProgress: ReadingProgress)

    @Query("DELETE FROM reading_progress")
    fun clearReadingProgress()
}