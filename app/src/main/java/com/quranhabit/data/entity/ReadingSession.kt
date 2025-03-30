package com.quranhabit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val pagesRead: Int
)