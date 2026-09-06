package com.factory.aquacoach.data.repository

import app.cash.turbine.test
import com.factory.aquacoach.data.local.DailyTotalRow
import com.factory.aquacoach.data.local.WaterEntryDao
import com.factory.aquacoach.data.local.WaterEntryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WaterRepositoryTest {

    private lateinit var dao: WaterEntryDao
    private lateinit var repository: WaterRepository

    @Before
    fun setup() {
        dao = mockk()
        repository = WaterRepository(dao)
    }

    @Test
    fun `observeEntriesForDay delegates to dao`() = runTest {
        val entries = listOf(
            WaterEntryEntity(id = 1, amountMl = 250, timestampEpochMillis = 1000L, dayKey = "2026-08-29")
        )
        every { dao.observeEntriesForDay("2026-08-29") } returns flowOf(entries)

        repository.observeEntriesForDay("2026-08-29").test {
            assertEquals(entries, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeDailyTotals delegates to dao with default limit`() = runTest {
        val totals = listOf(DailyTotalRow("2026-08-29", 1500))
        every { dao.observeDailyTotals(30) } returns flowOf(totals)

        repository.observeDailyTotals().test {
            assertEquals(totals, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeDailyTotals honors custom limit`() = runTest {
        every { dao.observeDailyTotals(7) } returns flowOf(emptyList())

        repository.observeDailyTotals(7).test {
            assertEquals(emptyList<DailyTotalRow>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `todayTotalMl sums amounts for today`() = runTest {
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val entries = listOf(
            WaterEntryEntity(id = 1, amountMl = 250, timestampEpochMillis = 1000L, dayKey = todayKey),
            WaterEntryEntity(id = 2, amountMl = 300, timestampEpochMillis = 2000L, dayKey = todayKey)
        )
        every { dao.observeEntriesForDay(todayKey) } returns flowOf(entries)

        val total = repository.todayTotalMl()

        assertEquals(550, total)
    }

    @Test
    fun `todayTotalMl returns zero when no entries`() = runTest {
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        every { dao.observeEntriesForDay(todayKey) } returns flowOf(emptyList())

        val total = repository.todayTotalMl()

        assertEquals(0, total)
    }

    @Test
    fun `addEntry inserts entity with today's dayKey and given amount`() = runTest {
        val captured = slot<WaterEntryEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        repository.addEntry(500)

        assertEquals(500, captured.captured.amountMl)
        assertEquals(todayKey, captured.captured.dayKey)
    }

    @Test
    fun `deleteEntry delegates to dao deleteById`() = runTest {
        coEvery { dao.deleteById(42L) } returns Unit

        repository.deleteEntry(42L)

        coVerify(exactly = 1) { dao.deleteById(42L) }
    }
}
