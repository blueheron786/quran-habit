package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranhabit.data.entity.PagesReadOnDay

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PagesReadOnDay)

    @Query("SELECT * FROM pages_read_on_day WHERE date = :date")
    suspend fun getByDate(date: String): PagesReadOnDay?

    @Query("SELECT SUM(pagesRead) FROM pages_read_on_day WHERE date = :date")
    suspend fun getDaysProgress(date: String): Int?

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM pages_read_on_day")
    suspend fun getTotalPagesRead(): Int

    @Query("DELETE FROM pages_read_on_day")
    suspend fun resetAllStatistics()
}