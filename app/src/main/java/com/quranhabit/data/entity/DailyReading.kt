package com.quranhabit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "daily_readings")
data class DailyReading(
    @PrimaryKey val date: Date,
    val pagesRead: Int
)