package com.factory.aquacoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestampEpochMillis: Long,
    val dayKey: String
)
