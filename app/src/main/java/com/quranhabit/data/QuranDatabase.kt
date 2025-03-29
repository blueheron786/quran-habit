package com.quranhabit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranhabit.data.dao.DailyReadingDao
import com.quranhabit.data.dao.ReadingProgressDao
import com.quranhabit.data.entity.DailyReading
import com.quranhabit.data.entity.ReadingProgress
import com.quranhabit.utils.DateConverter

@Database(entities = [ReadingProgress::class, DailyReading::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun dailyReadingDao(): DailyReadingDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}