package com.factory.aquacoach.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class WaterEntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WaterEntryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.waterEntryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insert_persistsEntryAndReturnsGeneratedId() = runTest {
        val id = dao.insert(WaterEntryEntity(amountMl = 250, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))

        val entries = dao.observeEntriesForDay("2026-08-29").first()

        assertEquals(1, entries.size)
        assertEquals(id, entries[0].id)
        assertEquals(250, entries[0].amountMl)
    }

    @Test
    fun observeEntriesForDay_filtersOutOtherDays() = runTest {
        dao.insert(WaterEntryEntity(amountMl = 250, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))
        dao.insert(WaterEntryEntity(amountMl = 300, timestampEpochMillis = 2_000L, dayKey = "2026-08-28"))

        val entries = dao.observeEntriesForDay("2026-08-29").first()

        assertEquals(1, entries.size)
        assertEquals(250, entries[0].amountMl)
    }

    @Test
    fun observeEntriesForDay_ordersByTimestampDescending() = runTest {
        dao.insert(WaterEntryEntity(amountMl = 100, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))
        dao.insert(WaterEntryEntity(amountMl = 200, timestampEpochMillis = 3_000L, dayKey = "2026-08-29"))
        dao.insert(WaterEntryEntity(amountMl = 300, timestampEpochMillis = 2_000L, dayKey = "2026-08-29"))

        val entries = dao.observeEntriesForDay("2026-08-29").first()

        assertEquals(listOf(200, 300, 100), entries.map { it.amountMl })
    }

    @Test
    fun observeEntriesForDay_emitsUpdatedListAfterInsert() = runTest {
        dao.observeEntriesForDay("2026-08-29").first().let { assertTrue(it.isEmpty()) }

        dao.insert(WaterEntryEntity(amountMl = 250, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))

        val entries = dao.observeEntriesForDay("2026-08-29").first()
        assertEquals(1, entries.size)
    }

    @Test
    fun deleteById_removesOnlyMatchingEntry() = runTest {
        val keepId = dao.insert(WaterEntryEntity(amountMl = 250, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))
        val removeId = dao.insert(WaterEntryEntity(amountMl = 300, timestampEpochMillis = 2_000L, dayKey = "2026-08-29"))

        dao.deleteById(removeId)

        val entries = dao.observeEntriesForDay("2026-08-29").first()
        assertEquals(1, entries.size)
        assertEquals(keepId, entries[0].id)
    }

    @Test
    fun observeDailyTotals_sumsAmountsPerDay() = runTest {
        dao.insert(WaterEntryEntity(amountMl = 100, timestampEpochMillis = 1_000L, dayKey = "2026-08-29"))
        dao.insert(WaterEntryEntity(amountMl = 150, timestampEpochMillis = 2_000L, dayKey = "2026-08-29"))
        dao.insert(WaterEntryEntity(amountMl = 500, timestampEpochMillis = 3_000L, dayKey = "2026-08-28"))

        val totals = dao.observeDailyTotals(limit = 30).first()

        assertEquals(
            mapOf("2026-08-29" to 250, "2026-08-28" to 500),
            totals.associate { it.dayKey to it.totalMl }
        )
    }

    @Test
    fun observeDailyTotals_ordersByDayKeyDescendingAndRespectsLimit() = runTest {
        dao.insert(WaterEntryEntity(amountMl = 100, timestampEpochMillis = 1_000L, dayKey = "2026-08-27"))
        dao.insert(WaterEntryEntity(amountMl = 100, timestampEpochMillis = 2_000L, dayKey = "2026-08-28"))
        dao.insert(WaterEntryEntity(amountMl = 100, timestampEpochMillis = 3_000L, dayKey = "2026-08-29"))

        val totals = dao.observeDailyTotals(limit = 2).first()

        assertEquals(listOf("2026-08-29", "2026-08-28"), totals.map { it.dayKey })
    }
}
