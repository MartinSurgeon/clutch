package com.example.domain

import kotlin.math.max
import kotlin.math.pow

/**
 * End-of-lay planning: when does keeping old layers stop paying, and is it better to sell now?
 *
 * Plain model a farmer can check by hand:
 *   weekly income  = birds × laying% × 7 ÷ 30 × price per crate
 *   weekly costs   = birds × feed per bird × 7 ÷ 50 kg × price per bag  +  birds × other costs per bird per week
 *   laying is projected to keep falling at the flock's own recent rate (or 0.5 points a week if there is too little history)
 *   birds are projected to keep dying at the flock's recent weekly rate
 * Pure Kotlin, no Android imports.
 */
object EndOfLayCalc {

    /** Typical fall in laying after peak, in percentage points per week. */
    const val DEFAULT_DECLINE_PER_WEEK = 0.5
    const val MAX_WEEKS_AHEAD = 52

    data class Prices(
        val eggPricePerCrate: Double,
        val feedPricePerBag: Double,
        val spentHenPrice: Double,
        val otherCostPerBirdPerMonth: Double = 0.0
    )

    data class FlockNow(
        val ageWeeks: Int,
        val birds: Int,
        val henDayPct: Double,
        val feedPerBirdG: Double,
        val weeklyMortalityPct: Double,
        /** Recent weekly hen-day values (oldest first) for estimating the decline; may be empty. */
        val recentWeeklyHenDay: List<Double>
    )

    data class WeekProjection(
        val weeksFromNow: Int,
        val ageWeeks: Int,
        val henDayPct: Double,
        val birds: Double,
        val income: Double,
        val costs: Double
    ) {
        val margin: Double get() = income - costs
    }

    enum class Advice { KEEP, PLAN_TO_SELL, SELL_NOW }

    data class Plan(
        val advice: Advice,
        val declinePerWeek: Double,
        val declineFromFlock: Boolean,
        /** Laying % needed just to cover feed and other costs. */
        val breakEvenHenDayPct: Double,
        val weeklyMarginNow: Double,
        /** Weeks until keeping stops paying; null = not within [MAX_WEEKS_AHEAD]. 0 = already not paying. */
        val weeksUntilBreakEven: Int?,
        val sellAtAgeWeeks: Int?,
        val sellNowValue: Double,
        /** Profit from keeping until break-even, plus selling the birds then. */
        val keepThenSellValue: Double,
        val projection: List<WeekProjection>
    ) {
        val extraFromKeeping: Double get() = keepThenSellValue - sellNowValue
    }

    /**
     * Fall in laying per week from recent weekly values (least-squares slope), as a positive number.
     * Returns null when there is not enough history or laying is not falling.
     */
    fun observedDecline(weeklyHenDay: List<Double>): Double? {
        val ys = weeklyHenDay.takeLast(8)
        if (ys.size < 3) return null
        val n = ys.size
        val meanX = (n - 1) / 2.0
        val meanY = ys.average()
        var num = 0.0
        var den = 0.0
        ys.forEachIndexed { i, y ->
            num += (i - meanX) * (y - meanY)
            den += (i - meanX).pow(2)
        }
        val slope = if (den == 0.0) 0.0 else num / den
        return if (slope < -0.05) -slope else null
    }

    fun weeklyIncome(birds: Double, henDayPct: Double, prices: Prices): Double =
        birds * (henDayPct / 100.0) * 7.0 / 30.0 * prices.eggPricePerCrate

    fun weeklyCosts(birds: Double, feedPerBirdG: Double, prices: Prices): Double {
        val feed = birds * feedPerBirdG * 7.0 / 1000.0 / 50.0 * prices.feedPricePerBag
        val other = birds * prices.otherCostPerBirdPerMonth * 12.0 / 52.0
        return feed + other
    }

    /** Laying % at which one bird's eggs exactly pay for its feed and other costs. */
    fun breakEvenHenDay(feedPerBirdG: Double, prices: Prices): Double {
        val incomeAt100 = weeklyIncome(1.0, 100.0, prices)
        if (incomeAt100 <= 0.0) return 100.0
        return weeklyCosts(1.0, feedPerBirdG, prices) / incomeAt100 * 100.0
    }

    fun plan(flock: FlockNow, prices: Prices): Plan {
        val observed = observedDecline(flock.recentWeeklyHenDay)
        val decline = observed ?: DEFAULT_DECLINE_PER_WEEK
        val survival = (1.0 - flock.weeklyMortalityPct.coerceIn(0.0, 100.0) / 100.0)

        val projection = (0..MAX_WEEKS_AHEAD).map { w ->
            val henDay = max(0.0, flock.henDayPct - decline * w)
            val birds = flock.birds * survival.pow(w)
            WeekProjection(
                weeksFromNow = w,
                ageWeeks = flock.ageWeeks + w,
                henDayPct = henDay,
                birds = birds,
                income = weeklyIncome(birds, henDay, prices),
                costs = weeklyCosts(birds, flock.feedPerBirdG, prices)
            )
        }

        val firstLoss = projection.firstOrNull { it.margin <= 0.0 }
        val weeksUntil = firstLoss?.weeksFromNow
        val sellNow = flock.birds * prices.spentHenPrice

        val keepThenSell = if (weeksUntil == null) {
            projection.sumOf { it.margin } + projection.last().birds * prices.spentHenPrice
        } else {
            projection.take(weeksUntil).sumOf { it.margin } + projection[weeksUntil].birds * prices.spentHenPrice
        }

        val advice = when {
            weeksUntil == 0 -> Advice.SELL_NOW
            weeksUntil != null && weeksUntil <= 8 -> Advice.PLAN_TO_SELL
            else -> Advice.KEEP
        }

        return Plan(
            advice = advice,
            declinePerWeek = decline,
            declineFromFlock = observed != null,
            breakEvenHenDayPct = breakEvenHenDay(flock.feedPerBirdG, prices),
            weeklyMarginNow = projection.first().margin,
            weeksUntilBreakEven = weeksUntil,
            sellAtAgeWeeks = firstLoss?.ageWeeks,
            sellNowValue = sellNow,
            keepThenSellValue = keepThenSell,
            projection = projection
        )
    }
}

/** Ghana cedi amounts, e.g. "GH₵ 1,240" or "GH₵ 62.50". */
object Money {
    fun format(amount: Double, decimals: Boolean = false): String {
        val negative = amount < 0
        val abs = kotlin.math.abs(amount)
        val whole = abs.toLong()
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val body = if (decimals) {
            val cents = kotlin.math.round((abs - whole) * 100).toInt().coerceIn(0, 99)
            "$grouped.${cents.toString().padStart(2, '0')}"
        } else {
            val rounded = kotlin.math.round(abs).toLong()
            rounded.toString().reversed().chunked(3).joinToString(",").reversed()
        }
        return (if (negative) "−" else "") + "GH₵ " + body
    }
}

/** Ghana phone numbers, matching the server's rules. */
object GhanaPhone {
    /** Canonical 233XXXXXXXXX, or null if it is not a valid Ghana mobile number. */
    fun normalize(input: String): String? {
        var digits = input.filter { it.isDigit() }
        if (digits.length == 10 && digits.startsWith("0")) digits = "233" + digits.drop(1)
        return if (Regex("^233[235]\\d{8}$").matches(digits)) digits else null
    }

    /** "233241234567" → "024 123 4567". */
    fun display(canonical: String): String {
        if (canonical.length != 12 || !canonical.startsWith("233")) return canonical
        val local = "0" + canonical.drop(3)
        return "${local.take(3)} ${local.substring(3, 6)} ${local.drop(6)}"
    }
}
