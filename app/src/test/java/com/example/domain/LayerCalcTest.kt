package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerCalcTest {

    @Test
    fun `required benchmark - 500 hens 420 eggs 58kg feed 60g eggs gives Hen-day 84_0 feed 116 egg mass 50_4 FCR 2_30`() {
        val flock = LayerCalc.FlockInput(
            id = "flock_1",
            name = "Pen A",
            breed = "Lohmann Brown",
            hensHoused = 500,
            placedDate = "2026-08-01",
            ageWeeksAtPlacement = 17
        )
        val record = LayerCalc.DailyRecordInput(
            date = "2026-10-03", // 9 weeks after placement -> 26 weeks old
            liveHens = 500,
            eggs = 420,
            feedKg = 58.0,
            waterL = 110.0,
            avgEggWeightG = 60.0
        )

        val metrics = LayerCalc.calculateDayMetrics(
            flock = flock,
            record = record,
            allRecordsSortedAsc = listOf(record)
        )

        // Hen-day 84.0%
        assertEquals(84.0, metrics.henDayPct, 0.001)
        assertEquals("84.0", LayerCalc.format1(metrics.henDayPct))

        // Feed 116 g/bird
        assertEquals(116.0, metrics.feedPerBirdG, 0.001)
        assertEquals("116", LayerCalc.format0(metrics.feedPerBirdG))

        // Egg mass 50.4 g
        assertNotNull(metrics.eggMassG)
        assertEquals(50.4, metrics.eggMassG!!, 0.001)
        assertEquals("50.4", LayerCalc.format1(metrics.eggMassG!!))

        // FCR 2.30
        assertNotNull(metrics.fcr)
        assertEquals(2.301587, metrics.fcr!!, 0.01)
        assertEquals("2.30", LayerCalc.format2(metrics.fcr!!))
    }

    @Test
    fun `egg weight uses most recent within 14 days and expires after 14 days`() {
        val records = listOf(
            LayerCalc.DailyRecordInput(date = "2026-10-01", liveHens = 500, eggs = 440, avgEggWeightG = 59.5),
            LayerCalc.DailyRecordInput(date = "2026-10-10", liveHens = 500, eggs = 445, avgEggWeightG = null),
            LayerCalc.DailyRecordInput(date = "2026-10-20", liveHens = 500, eggs = 450, avgEggWeightG = null)
        )

        // 9 days later -> uses 59.5 g
        val resolvedWithin14 = LayerCalc.resolveEggWeight("2026-10-10", records)
        assertNotNull(resolvedWithin14)
        assertEquals(59.5, resolvedWithin14!!.first, 0.001)
        assertEquals("2026-10-01", resolvedWithin14.second)

        // 19 days later (>14 days) -> null
        val resolvedAfter14 = LayerCalc.resolveEggWeight("2026-10-20", records)
        assertNull(resolvedAfter14)
    }

    @Test
    fun `seven day mortality and closing hens calculation`() {
        assertEquals(497, LayerCalc.closingHens(liveHens = 500, deaths = 2, culls = 1))

        val records = (0..6).map { dayOffset ->
            val date = LayerCalc.addDays("2026-10-01", dayOffset)
            LayerCalc.DailyRecordInput(
                date = date,
                liveHens = 500 - dayOffset,
                eggs = 450,
                deaths = if (dayOffset == 2 || dayOffset == 5) 1 else 0
            )
        }
        val (mortPct, deathsCount) = LayerCalc.sevenDayMortalityPercent("2026-10-07", records)
        assertEquals(2, deathsCount)
        // 2 deaths / 500 live hens on first day of window * 100 = 0.40%
        assertEquals(0.40, mortPct, 0.001)
    }

    @Test
    fun `warnings fire for two-day hen-day drop and water drop`() {
        val flock = LayerCalc.FlockInput(
            id = "flock_1",
            name = "Pen A",
            breed = "Lohmann Brown",
            hensHoused = 500,
            placedDate = "2026-08-01",
            ageWeeksAtPlacement = 17
        )
        // 7 baseline days at ~92% hen-day (460 eggs / 500 hens) and 220 mL water/bird (110 L)
        val baseline = (0..6).map { d ->
            LayerCalc.DailyRecordInput(
                date = LayerCalc.addDays("2026-10-01", d),
                liveHens = 500,
                eggs = 460,
                feedKg = 58.0,
                waterL = 110.0,
                avgEggWeightG = 60.0
            )
        }
        // 2 days of >3 point hen-day drop + water drop on final day
        val dropDays = listOf(
            LayerCalc.DailyRecordInput(
                date = "2026-10-08",
                liveHens = 500,
                eggs = 430, // 86.0% (6 pts below 92%)
                feedKg = 57.5,
                waterL = 108.0
            ),
            LayerCalc.DailyRecordInput(
                date = "2026-10-09",
                liveHens = 500,
                eggs = 425, // 85.0% (7 pts below 92%)
                feedKg = 57.0,
                waterL = 85.0 // 170 mL/bird (<85% of 220 mL)
            )
        )

        val warnings = LayerCalc.evaluateWarnings(flock, baseline + dropDays)
        assertTrue(warnings.any { it.id == "henday_drop_2d" && it.severity == LayerCalc.WarningSeverity.CRITICAL })
        assertTrue(warnings.any { it.id == "water_drop" && it.severity == LayerCalc.WarningSeverity.SERIOUS })
        // Ordered most severe first: CRITICAL before SERIOUS
        assertEquals(LayerCalc.WarningSeverity.CRITICAL, warnings.first().severity)
    }

    @Test
    fun `friendlyDate writes day before month with weekday`() {
        assertEquals("Fri 23 Oct", LayerCalc.friendlyDate("2026-10-23"))
        assertEquals("Thu 1 Jan 1970", LayerCalc.friendlyDate("1970-01-01", withYear = true))
        assertEquals("Sun 1 Mar", LayerCalc.friendlyDate("2026-03-01"))
        assertEquals("not-a-date", LayerCalc.friendlyDate("not-a-date"))
    }

    @Test
    fun `crates helper counts in crates of 30`() {
        assertEquals("0 crates", LayerCalc.formatCratesHelper(0))
        assertEquals("12 eggs (less than a crate)", LayerCalc.formatCratesHelper(12))
        assertEquals("1 crate", LayerCalc.formatCratesHelper(30))
        assertEquals("14 crates and 5 eggs", LayerCalc.formatCratesHelper(425))
    }
}
