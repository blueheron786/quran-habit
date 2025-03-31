package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.ui.statistics.DailyPages

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PagesReadOnDay)

    @Query("SELECT * FROM pages_read_on_day WHERE date = :date")
    suspend fun getByDate(date: String): PagesReadOnDay?

    @Query("SELECT SUM(pagesRead) FROM pages_read_on_day WHERE date = :date")
    suspend fun getPagesReadOnDay(date: String): Int?

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM pages_read_on_day")
    suspend fun getTotalPagesRead(): Int

    @Query("SELECT SUM(secondsSpendReading) FROM pages_read_on_day WHERE date = :date")
    suspend fun getTimeReadToday(date: String): Int?

    @Query("SELECT COALESCE(SUM(secondsSpendReading), 0) FROM pages_read_on_day")
    suspend fun getTotalTimeRead(): Int

    @Query("SELECT COALESCE(SUM(secondsSpendReading), 0) FROM pages_read_on_day WHERE date = :date")
    suspend fun getTimeSpentToday(date: String): Int

    @Query("SELECT COALESCE(SUM(secondsSpendReading), 0) FROM pages_read_on_day")
    suspend fun getTotalTimeSpent(): Int

    @Query("DELETE FROM pages_read_on_day")
    suspend fun resetAllStatistics()

    @Query("""
    WITH dates AS (
        SELECT date('now', '-' || (rowid-1) || ' days') AS date 
        FROM (
            SELECT 1 AS rowid UNION SELECT 2 UNION SELECT 3 
            UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7
        )
    )
    SELECT 
        dates.date,
        COALESCE(pages_read_on_day.pagesRead, 0) AS pagesRead
    FROM dates
    LEFT JOIN pages_read_on_day ON dates.date = pages_read_on_day.date
    ORDER BY dates.date ASC
""")
    suspend fun getWeeklyPagesRead(): List<DailyPages>
}


