package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyCalcTest {

    private val flock = LayerCalc.FlockInput(
        id = "f1",
        name = "Pen A",
        breed = "Lohmann Brown",
        hensHoused = 500,
        placedDate = "2026-10-01",
        ageWeeksAtPlacement = 30
    )

    private fun day(offset: Int, eggs: Int = 450, feedKg: Double = 57.5, deaths: Int = 0, liveHens: Int = 500, eggWeight: Double? = null) =
        LayerCalc.DailyRecordInput(
            date = LayerCalc.addDays("2026-10-01", offset),
            liveHens = liveHens,
            eggs = eggs,
            cracked = 5,
            dirty = 4,
            deaths = deaths,
            feedKg = feedKg,
            avgEggWeightG = eggWeight
        )

    @Test
    fun `full week totals, production and feed efficiency`() {
        val week = (0..6).map { day(it, deaths = if (it == 3) 1 else 0, eggWeight = if (it == 6) 60.0 else null) }
        val summary = WeeklyCalc.summarizeWeeks(flock, week).single()

        assertEquals(30, summary.ageWeek)
        assertEquals(7, summary.daysRecorded)
        assertEquals(3150, summary.totalEggs)
        assertEquals(402.5, summary.totalFeedKg, 0.001)
        assertEquals(1, summary.deaths)
        assertEquals(500.0, summary.avgLiveHens, 0.001)
        // 3150 eggs / (500 birds x 7 days) = 90.0%
        assertEquals(90.0, summary.henDayPct, 0.001)
        assertEquals(90.0, summary.henHousedPct, 0.001)
        // 402.5 kg x 1000 / 3500 bird-days = 115 g
        assertEquals(115.0, summary.feedPerBirdG, 0.001)
        // 3150 eggs x 60 g = 189 kg eggs; 402.5 / 189 = 2.13
        assertEquals(2.13, summary.fcr!!, 0.005)
        // 402.5 kg / 105 crates = 3.83 kg per crate
        assertEquals(3.83, summary.feedPerCrateKg!!, 0.005)
        // 1 death / 500 = 0.2%
        assertEquals(0.2, summary.mortalityPct, 0.001)
        assertEquals(LayerCalc.TargetState.ON_TARGET, summary.henDayStatus)
        assertEquals(LayerCalc.TargetState.WATCH, summary.mortalityStatus)
    }

    @Test
    fun `weeks are newest first and partial weeks use bird-days`() {
        val records = (0..9).map { day(it, eggs = if (it >= 7) 400 else 450) }
        val weeks = WeeklyCalc.summarizeWeeks(flock, records)

        assertEquals(listOf(31, 30), weeks.map { it.ageWeek })
        val partial = weeks.first()
        assertEquals(3, partial.daysRecorded)
        assertEquals(80.0, partial.henDayPct, 0.001)
    }

    @Test
    fun `fcr is empty without any egg weight`() {
        val summary = WeeklyCalc.summarizeWeeks(flock, (0..6).map { day(it) }).single()
        assertNull(summary.fcr)
        assertNotNull(summary.feedPerCrateKg)
    }

    @Test
    fun `uniformity rating thresholds`() {
        assertEquals(LayerCalc.TargetState.ON_TARGET, LayerCalc.evaluateUniformityTarget(85.0))
        assertEquals(LayerCalc.TargetState.WATCH, LayerCalc.evaluateUniformityTarget(75.0))
        assertEquals(LayerCalc.TargetState.OFF_TARGET, LayerCalc.evaluateUniformityTarget(60.0))
        assertEquals(LayerCalc.TargetState.NOT_APPLICABLE, LayerCalc.evaluateUniformityTarget(null))
    }

    @Test
    fun `guide targets cover every group`() {
        assertEquals(ReferenceTargets.Group.entries.size, ReferenceTargets.groups.size)
        assertTrue(ReferenceTargets.dailyMonitoringNotes.size in 5..7)
    }
}
