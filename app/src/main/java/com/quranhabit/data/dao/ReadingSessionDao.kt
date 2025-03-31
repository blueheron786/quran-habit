package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranhabit.data.entity.ReadingSession

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ReadingSession)

    @Query("SELECT * FROM reading_sessions WHERE date = :date")
    suspend fun getByDate(date: String): ReadingSession?

    @Query("SELECT SUM(pagesRead) FROM reading_sessions WHERE date = :date")
    suspend fun getDaysProgress(date: String): Int?

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions")
    suspend fun getTotalPagesRead(): Int

    @Query("DELETE FROM reading_sessions")
    suspend fun resetAllStatistics()
}