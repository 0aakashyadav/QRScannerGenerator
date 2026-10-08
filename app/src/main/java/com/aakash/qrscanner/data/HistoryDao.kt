package com.aakash.qrscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert suspend fun insert(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id") suspend fun delete(id: Long)

    @Query("DELETE FROM history") suspend fun clear()

    @Query("DELETE FROM history WHERE id IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT -1 OFFSET :keep)")
    suspend fun trimTo(keep: Int)
}
