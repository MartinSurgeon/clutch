package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndOfLayAndReportTest {

    // 60 per crate, 300 per 50 kg bag, 25 per spent hen
    private val prices = EndOfLayCalc.Prices(eggPricePerCrate = 60.0, feedPricePerBag = 300.0, spentHenPrice = 25.0)

    @Test
    fun `break-even laying is feed cost over egg income`() {
        // Feed: 115 g x 7 / 1000 / 50 x 300 = 4.83 per bird-week
        // Income at 100%: 7 / 30 x 60 = 14.0 per bird-week  → 34.5%
        assertEquals(34.5, EndOfLayCalc.breakEvenHenDay(115.0, prices), 0.05)
    }

    @Test
    fun `observed decline uses the flock's own slope`() {
        assertEquals(1.0, EndOfLayCalc.observedDecline(listOf(80.0, 79.0, 78.0, 77.0))!!, 1e-9)
        assertNull(EndOfLayCalc.observedDecline(listOf(80.0, 81.0, 80.5)))
        assertNull(EndOfLayCalc.observedDecline(listOf(80.0, 79.0)))
    }

    @Test
    fun `flock above break-even is kept and the sell week is projected`() {
        val flock = EndOfLayCalc.FlockNow(
            ageWeeks = 70, birds = 450, henDayPct = 44.5, feedPerBirdG = 115.0,
            weeklyMortalityPct = 0.0, recentWeeklyHenDay = listOf(47.5, 46.5, 45.5, 44.5)
        )
        val plan = EndOfLayCalc.plan(flock, prices)

        assertTrue(plan.declineFromFlock)
        assertEquals(1.0, plan.declinePerWeek, 1e-9)
        // 44.5 - w >= 34.5 until w = 10 (34.5 exactly breaks even → first week with margin <= 0)
        assertEquals(10, plan.weeksUntilBreakEven)
        assertEquals(80, plan.sellAtAgeWeeks)
        assertEquals(EndOfLayCalc.Advice.KEEP, plan.advice)
        assertTrue(plan.weeklyMarginNow > 0)
        assertEquals(450 * 25.0, plan.sellNowValue, 1e-6)
        assertTrue(plan.extraFromKeeping > 0)
    }

    @Test
    fun `flock below break-even should be sold now`() {
        val flock = EndOfLayCalc.FlockNow(
            ageWeeks = 85, birds = 400, henDayPct = 30.0, feedPerBirdG = 115.0,
            weeklyMortalityPct = 0.1, recentWeeklyHenDay = emptyList()
        )
        val plan = EndOfLayCalc.plan(flock, prices)
        assertEquals(EndOfLayCalc.Advice.SELL_NOW, plan.advice)
        assertEquals(0, plan.weeksUntilBreakEven)
        assertTrue(plan.weeklyMarginNow < 0)
        assertEquals(EndOfLayCalc.DEFAULT_DECLINE_PER_WEEK, plan.declinePerWeek, 1e-9)
    }

    @Test
    fun `money is written in cedis with thousands separators`() {
        assertEquals("GH₵ 1,240", Money.format(1240.4))
        assertEquals("GH₵ 62.50", Money.format(62.5, decimals = true))
        assertEquals("−GH₵ 3,000", Money.format(-3000.0))
        assertEquals("GH₵ 1,000,000", Money.format(999999.6))
    }

    @Test
    fun `ghana phone numbers normalise like the server`() {
        assertEquals("233241234567", GhanaPhone.normalize("024 123 4567"))
        assertEquals("233241234567", GhanaPhone.normalize("+233 24 123 4567"))
        assertEquals("233501234567", GhanaPhone.normalize("0501234567"))
        assertNull(GhanaPhone.normalize("12345"))
        assertEquals("024 123 4567", GhanaPhone.display("233241234567"))
    }

    @Test
    fun `daily report text reads like a WhatsApp message`() {
        val flock = LayerCalc.FlockInput("f", "Pen A", "Lohmann Brown", 500, "2026-07-01", 17)
        val day = LayerCalc.DailyRecordInput(
            date = "2026-10-20", liveHens = 495, eggs = 452, cracked = 4, dirty = 5, softShell = 1,
            deaths = 1, feedKg = 57.5, avgEggWeightG = 61.2
        )
        val m = LayerCalc.calculateDayMetrics(flock, day, listOf(day))
        val report = ReportBuilder.daily("Pen A", "2026-10-20", m, 57.5, emptyList(), "Vaccinated today")
        val text = ReportBuilder.toText(report)

        assertTrue(text.startsWith("*Pen A — farm report*"))
        assertTrue(text.contains("Tue 20 Oct 2026 • Week ${m.ageWeeks}"))
        assertTrue(text.contains("🥚 Eggs: *452* (15 crates + 2)"))
        assertTrue(text.contains("🐔 Live birds: *494* (1 died or removed)"))
        assertTrue(text.contains("🌾 Feed: *57.5 kg*"))
        assertTrue(text.contains("📝 Vaccinated today"))
        assertTrue(text.endsWith("Sent from Clutch"))
    }
}
