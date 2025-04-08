package com.quranhabit.data.repository

import com.quranhabit.data.dao.LastReadPositionDao
import com.quranhabit.data.entity.LastReadPosition

class LastReadRepository(private val dao: LastReadPositionDao) {
    suspend fun savePosition(surah: Int, ayah: Int, scrollY: Int) {
        dao.upsert(LastReadPosition(
            surah = surah,
            ayah = ayah,
            scrollY = scrollY,
            timestamp = System.currentTimeMillis()
        ))
    }

    suspend fun getScrollPosition(surah: Int, ayah: Int) = dao.getScrollPosition(surah, ayah)

    suspend fun getLastReadPosition() = dao.getLastPosition()
}
