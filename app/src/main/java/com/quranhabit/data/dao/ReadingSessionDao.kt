package com.quranhabit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.quranhabit.data.entity.ReadingSession
import kotlinx.coroutines.flow.Flow

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

//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insert(session: ReadingSession)
//
//    @Update
//    suspend fun update(session: ReadingSession)
//
//    @Query("SELECT * FROM reading_sessions")
//    suspend fun getAllSessions(): List<ReadingSession>?
}