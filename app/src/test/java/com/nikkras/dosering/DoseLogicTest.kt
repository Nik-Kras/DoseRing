package com.nikkras.dosering

import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.domain.Stats
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseLogicTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val ibuprofen = Medication(id = 1, name = "Ibuprofen", intervalMinutes = 240, doseAmount = 400.0, unit = "mg", maxPer24h = 1200.0, color = 0)

    private fun intake(at: Long, med: Medication = ibuprofen) =
        Intake(medicationId = med.id, takenAt = at, name = med.name, doseAmount = med.doseAmount, unit = med.unit, color = 0)

    @Test fun `never taken is ready with a full ring`() {
        val status = DoseLogic.status(ibuprofen, null, now = 0)
        assertTrue(status.isReady)
        assertEquals(1f, status.progress)
        assertNull(status.lastTakenAt)
    }

    @Test fun `ring fills linearly over the interval`() {
        val status = DoseLogic.status(ibuprofen, intake(0), now = hour)
        assertEquals(0.25f, status.progress, 1e-6f)
        assertEquals(3 * hour, status.remainingMillis)
        assertFalse(status.isReady)
    }

    @Test fun `ready exactly when the interval has passed`() {
        assertTrue(DoseLogic.status(ibuprofen, intake(0), now = 4 * hour).isReady)
        assertFalse(DoseLogic.status(ibuprofen, intake(0), now = 4 * hour - 1).isReady)
    }

    @Test fun `ready time is last dose plus interval`() {
        assertEquals(5 * hour, DoseLogic.readyAt(ibuprofen, intake(hour)))
        assertNull(DoseLogic.readyAt(ibuprofen, null))
    }

    @Test fun `24h total uses a rolling window`() {
        val now = 30 * hour
        val intakes = listOf(intake(now - 25 * hour), intake(now - 23 * hour), intake(now - hour))
        assertEquals(800.0, DoseLogic.amountInLast24h(intakes, ibuprofen.id, now), 1e-9)
    }

    @Test fun `24h total ignores other medicines`() {
        val other = ibuprofen.copy(id = 2)
        val intakes = listOf(intake(0, other), intake(0))
        assertEquals(400.0, DoseLogic.amountInLast24h(intakes, ibuprofen.id, hour), 1e-9)
    }

    @Test fun `tap on a ready medicine within limits needs no confirmation`() {
        val now = 10 * hour
        val check = DoseLogic.checkTap(ibuprofen, intake(now - 5 * hour), listOf(intake(now - 5 * hour)), now)
        assertFalse(check.needsConfirmation)
        assertEquals(800.0, check.amountAfter, 1e-9)
    }

    @Test fun `early tap needs confirmation`() {
        val now = 10 * hour
        val check = DoseLogic.checkTap(ibuprofen, intake(now - hour), listOf(intake(now - hour)), now)
        assertTrue(check.isEarly)
        assertTrue(check.needsConfirmation)
    }

    @Test fun `tap over the daily maximum needs confirmation even when ready`() {
        val now = 20 * hour
        val recent = listOf(intake(now - 15 * hour), intake(now - 10 * hour), intake(now - 5 * hour))
        val check = DoseLogic.checkTap(ibuprofen, recent.last(), recent, now)
        assertFalse(check.isEarly)
        assertTrue(check.exceedsMax)
        assertEquals(1600.0, check.amountAfter, 1e-9)
    }

    @Test fun `reaching the maximum exactly is allowed`() {
        val now = 20 * hour
        val recent = listOf(intake(now - 10 * hour), intake(now - 5 * hour))
        assertFalse(DoseLogic.checkTap(ibuprofen, recent.last(), recent, now).exceedsMax)
    }

    @Test fun `remaining time rounds up and elapsed rounds down`() {
        assertEquals("1m", DoseLogic.formatRemaining(1_000))
        assertEquals("1h 12m", DoseLogic.formatRemaining(71 * minute + 1))
        assertEquals("4h", DoseLogic.formatRemaining(4 * hour))
        assertEquals("1h 11m", DoseLogic.formatElapsed(71 * minute + 59_000))
        assertEquals("2d 3h", DoseLogic.formatMinutes(51 * 60))
    }

    @Test fun `initials`() {
        assertEquals("DS", DoseLogic.initials("Difflam Spray"))
        assertEquals("I", DoseLogic.initials("  ibuprofen "))
        assertEquals("", DoseLogic.initials(" "))
    }

    @Test fun `dose formatting`() {
        assertEquals("400 mg", Doses.format(400.0, "mg"))
        assertEquals("1 spray", Doses.format(1.0, "spray"))
        assertEquals("4 sprays", Doses.format(4.0, "spray"))
        assertEquals("2.5 ml", Doses.format(2.5, "ml"))
        assertEquals("0.33 tablets", Doses.format(1.0 / 3, "tablet"))
    }

    @Test fun `dose parsing`() {
        assertEquals(2.5, Doses.parse("2,5")!!, 1e-9)
        assertEquals(400.0, Doses.parse(" 400 ")!!, 1e-9)
        assertNull(Doses.parse("0"))
        assertNull(Doses.parse("abc"))
    }

    @Test fun `daily summaries include empty days and total per medicine`() {
        val zone = ZoneOffset.UTC
        val today = LocalDate.of(2026, 10, 15)
        val day = { d: LocalDate, h: Int -> d.atStartOfDay(zone).plusHours(h.toLong()).toInstant().toEpochMilli() }
        val intakes = listOf(intake(day(today, 9)), intake(day(today, 14)), intake(day(today.minusDays(2), 10)))

        val days = Stats.lastDays(intakes, zone, today, 3)
        assertEquals(listOf(today.minusDays(2), today.minusDays(1), today), days.map { it.date })
        assertEquals(listOf(1, 0, 2), days.map { it.doseCount })
        assertEquals(800.0, days.last().totals.single().amount, 1e-9)
    }
}
