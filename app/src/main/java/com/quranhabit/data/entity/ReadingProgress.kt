package com.quranhabit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    val ayah: Int,
    val page: Int
)