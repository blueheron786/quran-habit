package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.ui.statistics.DailyData

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PagesReadOnDay)

    @Query("SELECT * FROM pages_read_on_day WHERE date = :date")
    suspend fun getByDate(date: String): PagesReadOnDay?

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
    SELECT strftime('%Y-%m-%d', date('now', '-' || (CAST(:daysParam AS INTEGER) - 1) || ' days')) AS date, 0 AS pagesRead, 0 AS secondsReading
    UNION ALL
    SELECT strftime('%Y-%m-%d', date) AS date, pagesRead, secondsSpendReading
        FROM pages_read_on_day
        WHERE date >= strftime('%Y-%m-%d', date('now', '-' || (CAST(:daysParam AS INTEGER) - 1) || ' days'))
    ORDER BY date ASC
""")
    suspend fun getTimeRangeStatsData(daysParam: Int): List<DailyData>
}


