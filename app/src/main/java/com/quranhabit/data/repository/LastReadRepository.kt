package com.quranhabit.data.repository

import com.quranhabit.data.dao.LastReadPositionDao
import com.quranhabit.data.entity.LastReadPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LastReadRepository(private val dao: LastReadPositionDao) {
    suspend fun savePosition(surah: Int, ayah: Int, page: Int, scrollY: Int) {
        dao.upsert(LastReadPosition(
            surah = surah,
            ayah = ayah,
            page = page,
            scrollY = scrollY,
            timestamp = System.currentTimeMillis()
        ))
    }

    suspend fun getLastReadPosition(): LastReadPosition? {
        return withContext(Dispatchers.IO) {
            dao.getLastPosition()
        }
    }
}
