package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.quranhabit.data.entity.DailyReading
import java.util.Date

@Dao
interface DailyReadingDao {
    @Query("SELECT * FROM daily_readings WHERE date = :date")
    fun getDailyReading(date: Date): DailyReading?

    @Insert
    fun insertDailyReading(dailyReading: DailyReading)

    @Query("SELECT * FROM daily_readings WHERE date BETWEEN :startDate AND :endDate")
    fun getDailyReadings(startDate: Date, endDate: Date): List<DailyReading>
}