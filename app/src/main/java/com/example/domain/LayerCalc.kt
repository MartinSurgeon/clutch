package com.example.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure Kotlin domain calculation engine for laying hen flocks.
 * Contains zero Android imports so it can be unit-tested directly on the JVM.
 */
object LayerCalc {

    enum class TargetState(val label: String) {
        ON_TARGET("On target"),
        WATCH("Watch"),
        OFF_TARGET("Off target"),
        CLIMBING_TO_PEAK("Climbing to peak"),
        PAST_PEAK_WINDOW("Past peak window"),
        NOT_APPLICABLE("Not yet due")
    }

    enum class WarningSeverity(val priority: Int, val label: String) {
        CRITICAL(0, "Critical"),
        SERIOUS(1, "Serious"),
        WARNING(2, "Warning")
    }

    data class FlockInput(
        val id: String,
        val name: String,
        val breed: String,
        val hensHoused: Int,
        val placedDate: String, // YYYY-MM-DD
        val ageWeeksAtPlacement: Int
    )

    data class DailyRecordInput(
        val date: String, // YYYY-MM-DD
        val liveHens: Int,
        val eggs: Int,
        val cracked: Int = 0,
        val dirty: Int = 0,
        val softShell: Int = 0,
        val deaths: Int = 0,
        val culls: Int = 0,
        val feedKg: Double = 0.0,
        val waterL: Double = 0.0,
        val avgEggWeightG: Double? = null,
        val maxTempC: Double? = null,
        val notes: String = ""
    )

    data class MetricExplanation(
        val id: String,
        val title: String,
        val subtitle: String,
        val formattedValue: String,
        val working: String,
        val status: TargetState,
        val formula: String,
        val targetDescription: String,
        val whatToCheck: String
    )

    data class WarningItem(
        val id: String,
        val severity: WarningSeverity,
        val title: String,
        val detail: String,
        val whatToCheck: String
    )

    data class WeeklyMortalityPoint(
        val ageWeeks: Int,
        val deaths: Int,
        val startLiveHens: Int,
        val mortalityPct: Double
    )

    data class Last7DaysSummary(
        val daysCount: Int,
        val totalEggs: Int,
        val fullCratesOf30: Int,
        val remainderEggs: Int,
        val avgHenDayPct: Double,
        val totalFeedKg: Double,
        val totalDeaths: Int
    )

    data class DayMetrics(
        val date: String,
        val ageWeeks: Int,
        val liveHens: Int,
        val closingHens: Int,
        val eggs: Int,
        val henDayPct: Double,
        val henDayChangeVsYesterday: Double?,
        val henDayWorking: String,
        val henHousedPct: Double,
        val feedPerBirdG: Double,
        val waterPerBirdMl: Double,
        val effectiveEggWeightG: Double?,
        val eggWeightDateUsed: String?,
        val eggMassG: Double?,
        val fcr: Double?,
        val crackedPct: Double,
        val dirtyPct: Double,
        val softShellPct: Double,
        val sevenDayMortalityPct: Double,
        val deathsIn7Days: Int,
        val eggsPerHenHousedToDate: Double,
        val cumulativeMortalityPct: Double,
        val totalDeathsToDate: Int,
        // Target statuses
        val henDayStatus: TargetState,
        val feedStatus: TargetState,
        val waterStatus: TargetState,
        val eggMassStatus: TargetState,
        val fcrStatus: TargetState,
        val eggWeightStatus: TargetState,
        val sevenDayMortalityStatus: TargetState,
        val cumulativeMortalityStatus: TargetState,
        val crackedStatus: TargetState,
        val dirtyStatus: TargetState,
        val softShellStatus: TargetState
    )

    // --- Pure Date Math (ISO YYYY-MM-DD <-> Epoch Days) ---

    fun parseEpochDay(isoDate: String): Long {
        val parts = isoDate.trim().split("-")
        if (parts.size != 3) return 0L
        val y = parts[0].toLongOrNull() ?: return 0L
        val m = parts[1].toLongOrNull() ?: return 0L
        val d = parts[2].toLongOrNull() ?: return 0L
        val yr = if (m <= 2) y - 1 else y
        val era = if (yr >= 0) yr / 400 else (yr - 399) / 400
        val yoe = yr - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun formatEpochDay(epochDay: Long): String {
        val z = epochDay + 719468
        val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        val year = y + (if (m <= 2) 1 else 0)
        return "${year.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    fun daysBetween(startIsoDate: String, endIsoDate: String): Int {
        return (parseEpochDay(endIsoDate) - parseEpochDay(startIsoDate)).toInt()
    }

    fun addDays(isoDate: String, daysToAdd: Int): String {
        return formatEpochDay(parseEpochDay(isoDate) + daysToAdd)
    }

    // --- Core Formulas ---

    fun ageInWeeks(ageWeeksAtPlacement: Int, placedDate: String, recordDate: String): Int {
        val days = daysBetween(placedDate, recordDate)
        val weeksAdded = if (days >= 0) days / 7 else 0
        return ageWeeksAtPlacement + weeksAdded
    }

    fun henDayPercent(eggs: Int, liveHens: Int): Double {
        if (liveHens <= 0) return 0.0
        return (eggs.toDouble() / liveHens.toDouble()) * 100.0
    }

    fun henHousedPercent(eggs: Int, hensHoused: Int): Double {
        if (hensHoused <= 0) return 0.0
        return (eggs.toDouble() / hensHoused.toDouble()) * 100.0
    }

    fun feedPerBirdGrams(feedKg: Double, liveHens: Int): Double {
        if (liveHens <= 0) return 0.0
        return (feedKg * 1000.0) / liveHens.toDouble()
    }

    fun waterPerBirdMl(waterL: Double, liveHens: Int): Double {
        if (liveHens <= 0) return 0.0
        return (waterL * 1000.0) / liveHens.toDouble()
    }

    /**
     * Egg weight: use today's value, otherwise the most recent one entered within 14 days, otherwise null.
     */
    fun resolveEggWeight(
        targetDate: String,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): Pair<Double, String>? {
        val targetEpoch = parseEpochDay(targetDate)
        val candidate = recordsSortedByDateAsc
            .filter { rec ->
                val epoch = parseEpochDay(rec.date)
                val diff = targetEpoch - epoch
                diff in 0..14 && rec.avgEggWeightG != null && rec.avgEggWeightG > 0.0
            }
            .maxByOrNull { parseEpochDay(it.date) }
        return candidate?.avgEggWeightG?.let { it to candidate.date }
    }

    /**
     * Egg mass (g) = (Hen-day % / 100) * egg weight. Production MUST be a decimal.
     */
    fun eggMassGrams(henDayPercent: Double, eggWeightG: Double?): Double? {
        if (eggWeightG == null || eggWeightG <= 0.0) return null
        val productionDecimal = henDayPercent / 100.0
        return productionDecimal * eggWeightG
    }

    /**
     * FCR = feedKg / (eggs * egg weight / 1000)
     */
    fun feedConversionRatio(feedKg: Double, eggs: Int, eggWeightG: Double?): Double? {
        if (eggs <= 0 || eggWeightG == null || eggWeightG <= 0.0 || feedKg <= 0.0) return null
        val totalEggMassKg = (eggs.toDouble() * eggWeightG) / 1000.0
        if (totalEggMassKg <= 0.0) return null
        return feedKg / totalEggMassKg
    }

    fun defectPercent(defectCount: Int, totalEggs: Int): Double {
        if (totalEggs <= 0) return 0.0
        return (defectCount.toDouble() / totalEggs.toDouble()) * 100.0
    }

    fun closingHens(liveHens: Int, deaths: Int, culls: Int): Int {
        return (liveHens - deaths - culls).coerceAtLeast(0)
    }

    /**
     * 7-day mortality % = deaths in the 7 days ending today / liveHens on the first day of that window * 100
     */
    fun sevenDayMortalityPercent(
        targetDate: String,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): Pair<Double, Int> {
        val targetEpoch = parseEpochDay(targetDate)
        val windowStartEpoch = targetEpoch - 6L
        val windowRecords = recordsSortedByDateAsc.filter {
            val ep = parseEpochDay(it.date)
            ep in windowStartEpoch..targetEpoch
        }
        if (windowRecords.isEmpty()) return 0.0 to 0
        val firstDayLiveHens = windowRecords.first().liveHens
        val deathsInWindow = windowRecords.sumOf { it.deaths }
        if (firstDayLiveHens <= 0) return 0.0 to deathsInWindow
        val pct = (deathsInWindow.toDouble() / firstDayLiveHens.toDouble()) * 100.0
        return pct to deathsInWindow
    }

    fun eggsPerHenHousedToDate(
        targetDate: String,
        hensHoused: Int,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): Double {
        if (hensHoused <= 0) return 0.0
        val targetEpoch = parseEpochDay(targetDate)
        val totalEggs = recordsSortedByDateAsc
            .filter { parseEpochDay(it.date) <= targetEpoch }
            .sumOf { it.eggs }
        return totalEggs.toDouble() / hensHoused.toDouble()
    }

    fun cumulativeMortalityPercent(
        targetDate: String,
        hensHoused: Int,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): Pair<Double, Int> {
        val targetEpoch = parseEpochDay(targetDate)
        val totalDeaths = recordsSortedByDateAsc
            .filter { parseEpochDay(it.date) <= targetEpoch }
            .sumOf { it.deaths }
        if (hensHoused <= 0) return 0.0 to totalDeaths
        val pct = (totalDeaths.toDouble() / hensHoused.toDouble()) * 100.0
        return pct to totalDeaths
    }

    /**
     * Weekly mortality grouped by week of age.
     */
    fun weeklyMortalityByAge(
        flock: FlockInput,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): List<WeeklyMortalityPoint> {
        if (recordsSortedByDateAsc.isEmpty()) return emptyList()
        val grouped = recordsSortedByDateAsc.groupBy { rec ->
            ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, rec.date)
        }
        return grouped.entries
            .sortedBy { it.key }
            .map { (weekAge, recs) ->
                val ordered = recs.sortedBy { parseEpochDay(it.date) }
                val startHens = ordered.first().liveHens
                val deaths = ordered.sumOf { it.deaths }
                val pct = if (startHens > 0) (deaths.toDouble() / startHens.toDouble()) * 100.0 else 0.0
                WeeklyMortalityPoint(
                    ageWeeks = weekAge,
                    deaths = deaths,
                    startLiveHens = startHens,
                    mortalityPct = pct
                )
            }
    }

    // --- Target Evaluations ---

    fun evaluateHenDayTarget(henDayPct: Double, ageWeeks: Int): TargetState {
        return when {
            ageWeeks < 25 -> TargetState.CLIMBING_TO_PEAK
            ageWeeks > 35 -> TargetState.PAST_PEAK_WINDOW
            henDayPct in 85.0..95.0 || henDayPct > 95.0 -> TargetState.ON_TARGET
            henDayPct >= 80.0 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateFeedTarget(feedPerBirdG: Double): TargetState {
        return when {
            feedPerBirdG in 110.0..125.0 -> TargetState.ON_TARGET
            feedPerBirdG in 100.0..130.0 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateEggMassTarget(eggMassG: Double?, ageWeeks: Int): TargetState {
        if (ageWeeks < 25 || eggMassG == null) return TargetState.NOT_APPLICABLE
        return when {
            eggMassG >= 55.0 -> TargetState.ON_TARGET
            eggMassG >= 50.0 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateFcrTarget(fcr: Double?, ageWeeks: Int): TargetState {
        if (ageWeeks < 24 || fcr == null) return TargetState.NOT_APPLICABLE
        return when {
            fcr <= 2.2 -> TargetState.ON_TARGET
            fcr <= 2.4 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateMortalityTarget(sevenDayMortalityPct: Double): TargetState {
        return when {
            sevenDayMortalityPct <= 0.1 -> TargetState.ON_TARGET
            sevenDayMortalityPct <= 0.5 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateCrackedTarget(crackedPct: Double): TargetState {
        return when {
            crackedPct <= 2.0 -> TargetState.ON_TARGET
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateDirtyTarget(dirtyPct: Double): TargetState {
        return when {
            dirtyPct <= 3.0 -> TargetState.ON_TARGET
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateSoftShellTarget(softShellPct: Double): TargetState {
        return when {
            softShellPct <= 1.0 -> TargetState.ON_TARGET
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateEggWeightTarget(eggWeightG: Double?, ageWeeks: Int): TargetState {
        if (ageWeeks < 30 || eggWeightG == null) return TargetState.NOT_APPLICABLE
        return when {
            eggWeightG in 55.0..65.0 -> TargetState.ON_TARGET
            eggWeightG in 52.0..68.0 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    fun evaluateWaterTarget(
        targetDate: String,
        waterPerBirdMl: Double,
        recordsSortedByDateAsc: List<DailyRecordInput>
    ): TargetState {
        val targetEpoch = parseEpochDay(targetDate)
        val prev7 = recordsSortedByDateAsc.filter {
            val ep = parseEpochDay(it.date)
            ep in (targetEpoch - 7)..(targetEpoch - 1)
        }
        if (prev7.isNotEmpty()) {
            val prevAvg = prev7.map { waterPerBirdMl(it.waterL, it.liveHens) }.average()
            if (prevAvg > 0.0 && waterPerBirdMl < 0.85 * prevAvg) {
                return TargetState.OFF_TARGET
            }
        }
        return when {
            waterPerBirdMl in 180.0..320.0 -> TargetState.ON_TARGET
            waterPerBirdMl in 140.0..360.0 -> TargetState.WATCH
            else -> TargetState.OFF_TARGET
        }
    }

    // --- Full Day Calculation ---

    fun calculateDayMetrics(
        flock: FlockInput,
        record: DailyRecordInput,
        allRecordsSortedAsc: List<DailyRecordInput>
    ): DayMetrics {
        val ageWks = ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, record.date)
        val hdPct = henDayPercent(record.eggs, record.liveHens)
        val hhPct = henHousedPercent(record.eggs, flock.hensHoused)
        val feedG = feedPerBirdGrams(record.feedKg, record.liveHens)
        val waterMl = waterPerBirdMl(record.waterL, record.liveHens)
        val resolvedWeight = resolveEggWeight(record.date, allRecordsSortedAsc)
        val effWeight = resolvedWeight?.first
        val weightDate = resolvedWeight?.second
        val massG = eggMassGrams(hdPct, effWeight)
        val fcrVal = feedConversionRatio(record.feedKg, record.eggs, effWeight)
        val crackedP = defectPercent(record.cracked, record.eggs)
        val dirtyP = defectPercent(record.dirty, record.eggs)
        val softShellP = defectPercent(record.softShell, record.eggs)
        val closing = closingHens(record.liveHens, record.deaths, record.culls)
        val (sevenDayMortPct, deaths7d) = sevenDayMortalityPercent(record.date, allRecordsSortedAsc)
        val eggsPerHH = eggsPerHenHousedToDate(record.date, flock.hensHoused, allRecordsSortedAsc)
        val (cumMortPct, totalDeaths) = cumulativeMortalityPercent(record.date, flock.hensHoused, allRecordsSortedAsc)

        val recordEpoch = parseEpochDay(record.date)
        val yesterdayRec = allRecordsSortedAsc.find { parseEpochDay(it.date) == recordEpoch - 1L }
        val changeVsYesterday = yesterdayRec?.let {
            hdPct - henDayPercent(it.eggs, it.liveHens)
        }

        val working = "${record.eggs} eggs ÷ ${record.liveHens} live hens × 100 = ${format1(hdPct)}%"

        return DayMetrics(
            date = record.date,
            ageWeeks = ageWks,
            liveHens = record.liveHens,
            closingHens = closing,
            eggs = record.eggs,
            henDayPct = hdPct,
            henDayChangeVsYesterday = changeVsYesterday,
            henDayWorking = working,
            henHousedPct = hhPct,
            feedPerBirdG = feedG,
            waterPerBirdMl = waterMl,
            effectiveEggWeightG = effWeight,
            eggWeightDateUsed = weightDate,
            eggMassG = massG,
            fcr = fcrVal,
            crackedPct = crackedP,
            dirtyPct = dirtyP,
            softShellPct = softShellP,
            sevenDayMortalityPct = sevenDayMortPct,
            deathsIn7Days = deaths7d,
            eggsPerHenHousedToDate = eggsPerHH,
            cumulativeMortalityPct = cumMortPct,
            totalDeathsToDate = totalDeaths,
            henDayStatus = evaluateHenDayTarget(hdPct, ageWks),
            feedStatus = evaluateFeedTarget(feedG),
            waterStatus = evaluateWaterTarget(record.date, waterMl, allRecordsSortedAsc),
            eggMassStatus = evaluateEggMassTarget(massG, ageWks),
            fcrStatus = evaluateFcrTarget(fcrVal, ageWks),
            eggWeightStatus = evaluateEggWeightTarget(effWeight, ageWks),
            sevenDayMortalityStatus = evaluateMortalityTarget(sevenDayMortPct),
            cumulativeMortalityStatus = if (cumMortPct <= 3.0) TargetState.ON_TARGET else if (cumMortPct <= 6.0) TargetState.WATCH else TargetState.OFF_TARGET,
            crackedStatus = evaluateCrackedTarget(crackedP),
            dirtyStatus = evaluateDirtyTarget(dirtyP),
            softShellStatus = evaluateSoftShellTarget(softShellP)
        )
    }

    /**
     * Last 7 days summary ending on latest record date.
     */
    fun calculateLast7DaysSummary(
        targetDate: String,
        allRecordsSortedAsc: List<DailyRecordInput>
    ): Last7DaysSummary {
        val targetEpoch = parseEpochDay(targetDate)
        val window = allRecordsSortedAsc.filter {
            val ep = parseEpochDay(it.date)
            ep in (targetEpoch - 6L)..targetEpoch
        }
        if (window.isEmpty()) {
            return Last7DaysSummary(0, 0, 0, 0, 0.0, 0.0, 0)
        }
        val totalEggs = window.sumOf { it.eggs }
        val crates = totalEggs / 30
        val rem = totalEggs % 30
        val avgHenDay = window.map { henDayPercent(it.eggs, it.liveHens) }.average()
        val totalFeed = window.sumOf { it.feedKg }
        val totalDeaths = window.sumOf { it.deaths }
        return Last7DaysSummary(
            daysCount = window.size,
            totalEggs = totalEggs,
            fullCratesOf30 = crates,
            remainderEggs = rem,
            avgHenDayPct = avgHenDay,
            totalFeedKg = totalFeed,
            totalDeaths = totalDeaths
        )
    }

    /**
     * Warnings for the latest recorded day, sorted most severe first.
     */
    fun evaluateWarnings(
        flock: FlockInput,
        allRecordsSortedAsc: List<DailyRecordInput>
    ): List<WarningItem> {
        if (allRecordsSortedAsc.isEmpty()) return emptyList()
        val latest = allRecordsSortedAsc.last()
        val latestEpoch = parseEpochDay(latest.date)
        val dayMetrics = calculateDayMetrics(flock, latest, allRecordsSortedAsc)
        val warnings = mutableListOf<WarningItem>()

        // 1. Hen-day drop vs previous week's average (2 days running -> Critical, today only -> Warning)
        val yesterday = allRecordsSortedAsc.find { parseEpochDay(it.date) == latestEpoch - 1L }
        val prevWeekForTwoDay = allRecordsSortedAsc.filter {
            val ep = parseEpochDay(it.date)
            ep in (latestEpoch - 8L)..(latestEpoch - 2L)
        }
        val prevWeekForOneDay = allRecordsSortedAsc.filter {
            val ep = parseEpochDay(it.date)
            ep in (latestEpoch - 7L)..(latestEpoch - 1L)
        }

        var twoDayDropFired = false
        if (yesterday != null && prevWeekForTwoDay.isNotEmpty()) {
            val prevAvg = prevWeekForTwoDay.map { henDayPercent(it.eggs, it.liveHens) }.average()
            val yesterdayHd = henDayPercent(yesterday.eggs, yesterday.liveHens)
            val todayHd = dayMetrics.henDayPct
            if ((prevAvg - yesterdayHd) > 3.0 && (prevAvg - todayHd) > 3.0) {
                twoDayDropFired = true
                warnings.add(
                    WarningItem(
                        id = "henday_drop_2d",
                        severity = WarningSeverity.CRITICAL,
                        title = "Hen-day down >3 pts for two days running (${format1(todayHd)}% vs ${format1(prevAvg)}% avg)",
                        detail = "Production dropped to ${format1(yesterdayHd)}% yesterday and ${format1(todayHd)}% today.",
                        whatToCheck = "Check feed, water, light, disease, stress."
                    )
                )
            }
        }
        if (!twoDayDropFired && prevWeekForOneDay.isNotEmpty()) {
            val prevAvg = prevWeekForOneDay.map { henDayPercent(it.eggs, it.liveHens) }.average()
            val todayHd = dayMetrics.henDayPct
            if ((prevAvg - todayHd) > 3.0) {
                warnings.add(
                    WarningItem(
                        id = "henday_drop_1d",
                        severity = WarningSeverity.WARNING,
                        title = "Hen-day down >3 pts today (${format1(todayHd)}% vs ${format1(prevAvg)}% avg)",
                        detail = "Today's lay is ${format1(prevAvg - todayHd)} points below the previous 7-day average.",
                        whatToCheck = "Check feed, water, light, disease, stress."
                    )
                )
            }
        }

        // 2. Serious: water per bird below 85% of previous 7-day average
        if (prevWeekForOneDay.isNotEmpty()) {
            val prevWaterAvg = prevWeekForOneDay.map { waterPerBirdMl(it.waterL, it.liveHens) }.average()
            if (prevWaterAvg > 0.0 && dayMetrics.waterPerBirdMl < 0.85 * prevWaterAvg) {
                val pctOfAvg = (dayMetrics.waterPerBirdMl / prevWaterAvg) * 100.0
                warnings.add(
                    WarningItem(
                        id = "water_drop",
                        severity = WarningSeverity.SERIOUS,
                        title = "Water per bird below 85% of 7-day average (${format0(dayMetrics.waterPerBirdMl)} mL, ${format0(pctOfAvg)}% of avg)",
                        detail = "Previous 7-day average was ${format0(prevWaterAvg)} mL/bird. Drinking often falls before eggs do.",
                        whatToCheck = "Check drinker lines, airlocks, nipple flow rate, water tank level, water temperature, and early disease."
                    )
                )
            }
        }

        // 3. 7-day mortality over 0.5% (Critical) or over 0.1% (Warning)
        if (dayMetrics.sevenDayMortalityPct > 0.5) {
            warnings.add(
                WarningItem(
                    id = "mortality_critical",
                    severity = WarningSeverity.CRITICAL,
                    title = "7-day mortality over 0.5% (${format2(dayMetrics.sevenDayMortalityPct)}%)",
                    detail = "${dayMetrics.deathsIn7Days} birds died in the last 7 days.",
                    whatToCheck = "Check for acute disease, heat stress, water failure, smothering, or predators; call a vet if deaths continue."
                )
            )
        } else if (dayMetrics.sevenDayMortalityPct > 0.1) {
            val singleBirdNote = if (dayMetrics.deathsIn7Days == 1) {
                " Note: in small flocks one bird crosses the 0.1% line."
            } else ""
            warnings.add(
                WarningItem(
                    id = "mortality_warning",
                    severity = WarningSeverity.WARNING,
                    title = "7-day mortality over 0.1% (${format2(dayMetrics.sevenDayMortalityPct)}%)",
                    detail = "${dayMetrics.deathsIn7Days} ${if (dayMetrics.deathsIn7Days == 1) "bird" else "birds"} died in the last 7 days.$singleBirdNote",
                    whatToCheck = "Check dead birds for prolapse, pecking, weight loss, or respiratory signs; verify ventilation and water."
                )
            )
        }

        // 4. Cracked over 2%
        if (dayMetrics.crackedPct > 2.0) {
            warnings.add(
                WarningItem(
                    id = "cracked_high",
                    severity = WarningSeverity.WARNING,
                    title = "Cracked eggs over 2% (${format1(dayMetrics.crackedPct)}%)",
                    detail = "${latest.cracked} cracked out of ${latest.eggs} eggs collected.",
                    whatToCheck = "Check calcium, heat, collection frequency, handling, hen age."
                )
            )
        }

        // 5. Dirty over 3%
        if (dayMetrics.dirtyPct > 3.0) {
            warnings.add(
                WarningItem(
                    id = "dirty_high",
                    severity = WarningSeverity.WARNING,
                    title = "Dirty eggs over 3% (${format1(dayMetrics.dirtyPct)}%)",
                    detail = "${latest.dirty} dirty out of ${latest.eggs} eggs collected.",
                    whatToCheck = "Check nest boxes and litter condition."
                )
            )
        }

        // 6. Soft-shell over 1%
        if (dayMetrics.softShellPct > 1.0) {
            warnings.add(
                WarningItem(
                    id = "soft_shell_high",
                    severity = WarningSeverity.WARNING,
                    title = "Soft-shell eggs over 1% (${format1(dayMetrics.softShellPct)}%)",
                    detail = "${latest.softShell} soft-shell out of ${latest.eggs} eggs collected.",
                    whatToCheck = "Check calcium and heat; ask a vet about infectious bronchitis or egg drop syndrome."
                )
            )
        }

        // 7. Feed under 100 g or over 130 g
        if (dayMetrics.feedPerBirdG < 100.0) {
            warnings.add(
                WarningItem(
                    id = "feed_low",
                    severity = WarningSeverity.WARNING,
                    title = "Feed per bird under 100 g (${format0(dayMetrics.feedPerBirdG)} g)",
                    detail = "Target is 110–125 g per bird per day.",
                    whatToCheck = "Check heat, water, feed quality, health."
                )
            )
        } else if (dayMetrics.feedPerBirdG > 130.0) {
            warnings.add(
                WarningItem(
                    id = "feed_high",
                    severity = WarningSeverity.WARNING,
                    title = "Feed per bird over 130 g (${format0(dayMetrics.feedPerBirdG)} g)",
                    detail = "Target is 110–125 g per bird per day.",
                    whatToCheck = "Check wastage, rats, wild birds."
                )
            )
        }

        // 8. FCR over 2.4
        val fcrVal = dayMetrics.fcr
        if (fcrVal != null && fcrVal > 2.4 && dayMetrics.ageWeeks >= 24) {
            warnings.add(
                WarningItem(
                    id = "fcr_high",
                    severity = WarningSeverity.WARNING,
                    title = "FCR over 2.4 (${format2(fcrVal)} kg feed / kg eggs)",
                    detail = "More than 2.4 kg of feed is being used per kg of egg mass.",
                    whatToCheck = "Check feeder spillage, low rate of lay, small egg size, or rodent feed loss."
                )
            )
        }

        // 9. 7-day average hen-day under 85% between 25 and 35 weeks
        val last7 = calculateLast7DaysSummary(latest.date, allRecordsSortedAsc)
        if (dayMetrics.ageWeeks in 25..35 && last7.daysCount > 0 && last7.avgHenDayPct < 85.0) {
            warnings.add(
                WarningItem(
                    id = "peak_window_low",
                    severity = WarningSeverity.WARNING,
                    title = "7-day average hen-day under 85% (${format1(last7.avgHenDayPct)}%)",
                    detail = "Flock is ${dayMetrics.ageWeeks} weeks old (peak window is 25–35 weeks).",
                    whatToCheck = "Check light hours (14–16h), feed intake, water availability, and flock uniformity."
                )
            )
        }

        // 10. No eggs in 7 days at 21+ weeks
        if (dayMetrics.ageWeeks >= 21 && last7.daysCount >= 7 && last7.totalEggs == 0) {
            warnings.add(
                WarningItem(
                    id = "no_eggs_21w",
                    severity = WarningSeverity.WARNING,
                    title = "No eggs in 7 days at ${dayMetrics.ageWeeks} weeks",
                    detail = "Hens are past 21 weeks with zero eggs recorded this week.",
                    whatToCheck = "Check body weight, light hours, rearing feed."
                )
            )
        }

        return warnings.sortedBy { it.severity.priority }
    }

    // ---Formatting Helpers (pure Kotlin, locale-independent) ---

    fun format0(value: Double): String {
        return value.roundToInt().toString()
    }

    fun format1(value: Double): String {
        val scaled = (value * 10.0).roundToInt()
        val sign = if (scaled < 0) "-" else ""
        val absScaled = abs(scaled)
        return "$sign${absScaled / 10}.${absScaled % 10}"
    }

    fun format2(value: Double): String {
        val scaled = (value * 100.0).roundToInt()
        val sign = if (scaled < 0) "-" else ""
        val absScaled = abs(scaled)
        val whole = absScaled / 100
        val frac = (absScaled % 100).toString().padStart(2, '0')
        return "$sign$whole.$frac"
    }

    fun formatCratesHelper(eggs: Int): String {
        if (eggs <= 0) return "0 crates of 30"
        val crates = eggs / 30
        val rem = eggs % 30
        return when {
            crates == 0 -> "$rem ${if (rem == 1) "egg" else "eggs"} (under 1 crate of 30)"
            rem == 0 -> "$crates ${if (crates == 1) "crate" else "crates"} of 30"
            else -> "$crates ${if (crates == 1) "crate" else "crates"} of 30 + $rem ${if (rem == 1) "egg" else "eggs"}"
        }
    }
}
