package com.factory.aquacoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {

    @Insert
    suspend fun insert(entry: WaterEntryEntity): Long

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM water_entries WHERE dayKey = :dayKey ORDER BY timestampEpochMillis DESC")
    fun observeEntriesForDay(dayKey: String): Flow<List<WaterEntryEntity>>

    @Query("SELECT dayKey, SUM(amountMl) AS totalMl FROM water_entries GROUP BY dayKey ORDER BY dayKey DESC LIMIT :limit")
    fun observeDailyTotals(limit: Int): Flow<List<DailyTotalRow>>
}
