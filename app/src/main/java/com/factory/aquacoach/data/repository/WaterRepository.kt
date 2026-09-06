package com.factory.aquacoach.data.repository

import com.factory.aquacoach.data.local.DailyTotalRow
import com.factory.aquacoach.data.local.WaterEntryDao
import com.factory.aquacoach.data.local.WaterEntryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WaterRepository(private val dao: WaterEntryDao) {

    fun observeEntriesForDay(dayKey: String): Flow<List<WaterEntryEntity>> =
        dao.observeEntriesForDay(dayKey)

    fun observeDailyTotals(limit: Int = 30): Flow<List<DailyTotalRow>> =
        dao.observeDailyTotals(limit)

    suspend fun todayTotalMl(): Int {
        val dayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return dao.observeEntriesForDay(dayKey).map { entries -> entries.sumOf { it.amountMl } }.first()
    }

    suspend fun addEntry(amountMl: Int) {
        val now = System.currentTimeMillis()
        val dayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        dao.insert(WaterEntryEntity(amountMl = amountMl, timestampEpochMillis = now, dayKey = dayKey))
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
