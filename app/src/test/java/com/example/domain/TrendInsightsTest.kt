package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendInsightsTest {

    @Test
    fun `laying drop is flagged as bad news with in-target count`() {
        val values = listOf(92.0, 93.0, 92.5, 92.8, 93.1, 92.6, 92.4, 85.3)
        val insight = TrendInsights.summarize(values, 85.0, 95.0, TrendInsights.Goodness.HIGHER_IS_BETTER)!!

        assertEquals(TrendInsights.Direction.DOWN, insight.direction)
        assertEquals(false, insight.isImproving)
        assertEquals(-7.33, insight.change!!, 0.01)
        assertEquals(8, insight.inTargetCount)
        assertEquals(true, insight.latestInTarget)
    }

    @Test
    fun `fewer deaths is good news`() {
        val insight = TrendInsights.summarize(listOf(0.4, 0.3, 0.1), null, 0.1, TrendInsights.Goodness.LOWER_IS_BETTER)!!
        assertEquals(true, insight.isImproving)
        assertEquals(1, insight.inTargetCount)
    }

    @Test
    fun `feed moving into the band counts as improving`() {
        val insight = TrendInsights.summarize(listOf(100.0, 102.0, 117.0), 110.0, 125.0, TrendInsights.Goodness.IN_BAND_IS_BEST)!!
        assertEquals(true, insight.isImproving)
    }

    @Test
    fun `single value has no change`() {
        val insight = TrendInsights.summarize(listOf(90.0), 85.0, 95.0, TrendInsights.Goodness.HIGHER_IS_BETTER)!!
        assertNull(insight.change)
        assertEquals(TrendInsights.Direction.FLAT, insight.direction)
        assertNull(insight.isImproving)
    }

    @Test
    fun `nice ticks use round steps`() {
        assertEquals(listOf(0.0, 25.0, 50.0, 75.0, 100.0), TrendInsights.niceTicks(0.0, 100.0, 4))
        val feed = TrendInsights.niceTicks(96.0, 131.0, 4)
        assertTrue(feed.first() <= 96.0 && feed.last() >= 131.0)
        assertEquals(10.0, feed[1] - feed[0], 1e-9)
    }

    @Test
    fun `label indices include ends and fit`() {
        assertEquals(listOf(0, 14, 29), TrendInsights.labelIndices(30, 3))
        assertEquals(listOf(0), TrendInsights.labelIndices(1, 4))
        assertFalse(TrendInsights.labelIndices(5, 10).size > 5)
    }
}
