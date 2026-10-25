package com.example.domain

/**
 * Weekly summary engine: rolls daily records up into one row per week of age,
 * the way a farm workbook's weekly sheet does. Pure Kotlin, no Android imports.
 */
object WeeklyCalc {

    data class WeekSummary(
        val ageWeek: Int,
        val startDate: String,
        val endDate: String,
        val daysRecorded: Int,
        val totalEggs: Int,
        val totalFeedKg: Double,
        val deaths: Int,
        val culls: Int,
        val startLiveHens: Int,
        val avgLiveHens: Double,
        val henDayPct: Double,
        val henHousedPct: Double,
        val feedPerBirdG: Double,
        val avgEggWeightG: Double?,
        val eggMassKg: Double?,
        val fcr: Double?,
        val feedPerCrateKg: Double?,
        val mortalityPct: Double,
        val badEggPct: Double,
        val bodyWeightG: Double?,
        val uniformityPct: Double?,
        val henDayStatus: LayerCalc.TargetState,
        val feedStatus: LayerCalc.TargetState,
        val fcrStatus: LayerCalc.TargetState,
        val mortalityStatus: LayerCalc.TargetState,
        val qualityStatus: LayerCalc.TargetState,
        val uniformityStatus: LayerCalc.TargetState
    )

    /**
     * One [WeekSummary] per week of age, newest week first.
     *
     * - Weekly hen-day % = eggs in the week ÷ sum of daily live hens × 100 (bird-days),
     *   so a week with missing days is still fair.
     * - Weekly FCR uses the week's average egg weight, or the latest weight from the last 14 days.
     */
    fun summarizeWeeks(
        flock: LayerCalc.FlockInput,
        recordsSortedByDateAsc: List<LayerCalc.DailyRecordInput>
    ): List<WeekSummary> {
        if (recordsSortedByDateAsc.isEmpty()) return emptyList()

        return recordsSortedByDateAsc
            .groupBy { LayerCalc.ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, it.date) }
            .map { (ageWeek, recs) -> summarizeWeek(flock, ageWeek, recs, recordsSortedByDateAsc) }
            .sortedByDescending { it.ageWeek }
    }

    private fun summarizeWeek(
        flock: LayerCalc.FlockInput,
        ageWeek: Int,
        weekRecords: List<LayerCalc.DailyRecordInput>,
        allRecords: List<LayerCalc.DailyRecordInput>
    ): WeekSummary {
        val ordered = weekRecords.sortedBy { LayerCalc.parseEpochDay(it.date) }
        val days = ordered.size
        val totalEggs = ordered.sumOf { it.eggs }
        val totalFeedKg = ordered.sumOf { it.feedKg }
        val birdDays = ordered.sumOf { it.liveHens }
        val deaths = ordered.sumOf { it.deaths }
        val culls = ordered.sumOf { it.culls }
        val startHens = ordered.first().liveHens
        val badEggs = ordered.sumOf { it.cracked + it.dirty + it.softShell }

        val henDayPct = if (birdDays > 0) totalEggs.toDouble() / birdDays * 100.0 else 0.0
        val henHousedPct = if (flock.hensHoused > 0 && days > 0) {
            totalEggs.toDouble() / (flock.hensHoused.toDouble() * days) * 100.0
        } else 0.0
        val feedPerBirdG = if (birdDays > 0) totalFeedKg * 1000.0 / birdDays else 0.0

        val weighedInWeek = ordered.mapNotNull { it.avgEggWeightG }.filter { it > 0.0 }
        val avgEggWeight = if (weighedInWeek.isNotEmpty()) {
            weighedInWeek.average()
        } else {
            LayerCalc.resolveEggWeight(ordered.last().date, allRecords)?.first
        }
        val eggMassKg = avgEggWeight?.let { totalEggs * it / 1000.0 }
        val fcr = if (eggMassKg != null && eggMassKg > 0.0 && totalFeedKg > 0.0) totalFeedKg / eggMassKg else null
        val feedPerCrateKg = if (totalEggs > 0 && totalFeedKg > 0.0) totalFeedKg / (totalEggs / 30.0) else null

        val mortalityPct = if (startHens > 0) deaths.toDouble() / startHens * 100.0 else 0.0
        val badEggPct = if (totalEggs > 0) badEggs.toDouble() / totalEggs * 100.0 else 0.0

        val bodyWeight = ordered.lastOrNull { it.bodyWeightG != null }?.bodyWeightG
        val uniformity = ordered.lastOrNull { it.uniformityPct != null }?.uniformityPct

        return WeekSummary(
            ageWeek = ageWeek,
            startDate = ordered.first().date,
            endDate = ordered.last().date,
            daysRecorded = days,
            totalEggs = totalEggs,
            totalFeedKg = totalFeedKg,
            deaths = deaths,
            culls = culls,
            startLiveHens = startHens,
            avgLiveHens = if (days > 0) birdDays.toDouble() / days else 0.0,
            henDayPct = henDayPct,
            henHousedPct = henHousedPct,
            feedPerBirdG = feedPerBirdG,
            avgEggWeightG = avgEggWeight,
            eggMassKg = eggMassKg,
            fcr = fcr,
            feedPerCrateKg = feedPerCrateKg,
            mortalityPct = mortalityPct,
            badEggPct = badEggPct,
            bodyWeightG = bodyWeight,
            uniformityPct = uniformity,
            henDayStatus = LayerCalc.evaluateHenDayTarget(henDayPct, ageWeek),
            feedStatus = if (feedPerBirdG > 0.0) LayerCalc.evaluateFeedTarget(feedPerBirdG) else LayerCalc.TargetState.NOT_APPLICABLE,
            fcrStatus = LayerCalc.evaluateFcrTarget(fcr, ageWeek),
            mortalityStatus = LayerCalc.evaluateMortalityTarget(mortalityPct),
            qualityStatus = if (totalEggs > 0) LayerCalc.evaluateEggQualityTarget(badEggPct) else LayerCalc.TargetState.NOT_APPLICABLE,
            uniformityStatus = LayerCalc.evaluateUniformityTarget(uniformity)
        )
    }
}
