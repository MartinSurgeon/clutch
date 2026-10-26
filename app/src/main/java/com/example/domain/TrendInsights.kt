package com.example.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Turns a chart's numbers into plain-language facts ("down 2.1 from last week",
 * "22 of 30 days in target") so a farmer can read a chart without studying it.
 * Pure Kotlin, no Android imports.
 */
object TrendInsights {

    /** Which direction is good for this measure. */
    enum class Goodness { HIGHER_IS_BETTER, LOWER_IS_BETTER, IN_BAND_IS_BEST }

    enum class Direction { UP, DOWN, FLAT }

    data class Insight(
        val latest: Double,
        val previousAverage: Double?,
        val change: Double?,
        val direction: Direction,
        /** true = the change is good news, false = bad news, null = neutral or unknown. */
        val isImproving: Boolean?,
        val inTargetCount: Int,
        val total: Int,
        val latestInTarget: Boolean?
    )

    fun isInBand(value: Double, bandMin: Double?, bandMax: Double?): Boolean? {
        if (bandMin == null && bandMax == null) return null
        if (bandMin != null && value < bandMin) return false
        if (bandMax != null && value > bandMax) return false
        return true
    }

    /**
     * Compares the latest value with the average of up to [lookback] values before it.
     * Changes smaller than [flatThreshold] count as "no real change".
     */
    fun summarize(
        values: List<Double>,
        bandMin: Double?,
        bandMax: Double?,
        goodness: Goodness,
        lookback: Int = 7,
        flatThreshold: Double = 0.0
    ): Insight? {
        if (values.isEmpty()) return null
        val latest = values.last()
        val previous = values.dropLast(1).takeLast(lookback)
        val previousAverage = previous.takeIf { it.isNotEmpty() }?.average()
        val change = previousAverage?.let { latest - it }

        val direction = when {
            change == null || abs(change) <= flatThreshold -> Direction.FLAT
            change > 0 -> Direction.UP
            else -> Direction.DOWN
        }

        val isImproving: Boolean? = when {
            direction == Direction.FLAT -> null
            goodness == Goodness.HIGHER_IS_BETTER -> direction == Direction.UP
            goodness == Goodness.LOWER_IS_BETTER -> direction == Direction.DOWN
            else -> {
                // In-band measures: moving towards the middle of the band is good.
                val mid = midpoint(bandMin, bandMax)
                if (previousAverage == null || mid == null) null else abs(latest - mid) < abs(previousAverage - mid)
            }
        }

        val inTarget = values.count { isInBand(it, bandMin, bandMax) == true }

        return Insight(
            latest = latest,
            previousAverage = previousAverage,
            change = change,
            direction = direction,
            isImproving = isImproving,
            inTargetCount = inTarget,
            total = values.size,
            latestInTarget = isInBand(latest, bandMin, bandMax)
        )
    }

    private fun midpoint(bandMin: Double?, bandMax: Double?): Double? = when {
        bandMin != null && bandMax != null -> (bandMin + bandMax) / 2.0
        bandMin != null -> bandMin
        bandMax != null -> bandMax
        else -> null
    }

    /**
     * "Nice" axis ticks (1, 2, 2.5, 5 × 10ⁿ steps) covering [min, max], roughly [targetCount] of them.
     */
    fun niceTicks(min: Double, max: Double, targetCount: Int = 4): List<Double> {
        val lo = minOf(min, max)
        var hi = maxOf(min, max)
        if (hi - lo < 1e-9) hi = lo + 1.0
        val rawStep = (hi - lo) / targetCount.coerceAtLeast(1)
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val residual = rawStep / magnitude
        val niceStep = when {
            residual <= 1.0 -> 1.0
            residual <= 2.0 -> 2.0
            residual <= 2.5 -> 2.5
            residual <= 5.0 -> 5.0
            else -> 10.0
        } * magnitude
        val start = floor(lo / niceStep) * niceStep
        val end = ceil(hi / niceStep) * niceStep
        val ticks = mutableListOf<Double>()
        var t = start
        while (t <= end + niceStep * 0.001) {
            ticks.add(if (abs(t) < niceStep * 1e-6) 0.0 else t)
            t += niceStep
        }
        return ticks
    }

    /** Indices of evenly spaced x-axis labels that fit, always including the first and last point. */
    fun labelIndices(count: Int, maxLabels: Int): List<Int> {
        if (count <= 0) return emptyList()
        if (count == 1) return listOf(0)
        val slots = maxLabels.coerceIn(2, count)
        return (0 until slots).map { i -> ((count - 1).toDouble() * i / (slots - 1)).toInt() }.distinct()
    }
}
