package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranhabit.data.entity.ReadingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ReadingSession)

    @Query("SELECT * FROM reading_sessions WHERE date = :date")
    suspend fun getByDate(date: String): ReadingSession?

    @Query("SELECT pagesRead FROM reading_sessions WHERE date = :date")
    fun getTodayProgress(date: String): Flow<Int?>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions")
    fun getTotalPagesRead(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM (
            SELECT date FROM reading_sessions 
            WHERE pagesRead > 0 
            ORDER BY date DESC 
            LIMIT 7
        )
    """)
    fun getWeeklyStreak(): Flow<Int>
}