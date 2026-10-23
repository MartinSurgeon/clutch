package com.example.domain

/**
 * Provides plain-language explanations, formulas, targets, and troubleshooting steps
 * when the farmer taps any number card (Tesler's Law).
 */
object MetricExplanations {

    fun buildForHenDay(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "hen_day",
            title = "Hen-day %",
            subtitle = "Eggs laid per 100 live hens today",
            formattedValue = "${LayerCalc.format1(metrics.henDayPct)}%",
            working = metrics.henDayWorking,
            status = metrics.henDayStatus,
            formula = "Eggs collected today ÷ Live hens at start of day × 100",
            targetDescription = "85–95% between 25 and 35 weeks of age. Before 25 weeks the flock is climbing to peak; after 35 weeks lay eases gradually by ~0.5% per week.",
            whatToCheck = "If hen-day drops suddenly: check water flow first, then feed intake, light hours (keep 14–16 hrs steady, never shorten day length in lay), house temperature, parasites, or signs of illness."
        )
    }

    fun buildForHenHoused(metrics: LayerCalc.DayMetrics, hensHoused: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "hen_housed",
            title = "Hen-housed %",
            subtitle = "Eggs laid per 100 hens originally placed",
            formattedValue = "${LayerCalc.format1(metrics.henHousedPct)}%",
            working = "${metrics.eggs} eggs ÷ $hensHoused hens housed × 100 = ${LayerCalc.format1(metrics.henHousedPct)}%",
            status = metrics.henDayStatus,
            formula = "Eggs collected today ÷ Hens originally housed × 100",
            targetDescription = "Stays close to Hen-day % when mortality and culling are low (<3% total loss).",
            whatToCheck = "A widening gap between Hen-day % and Hen-housed % means cumulative mortality or culls have reduced your active flock size."
        )
    }

    fun buildForEggMass(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        val weightText = metrics.effectiveEggWeightG?.let { "${LayerCalc.format1(it)} g" } ?: "not weighed in last 14 days"
        val valText = metrics.eggMassG?.let { "${LayerCalc.format1(it)} g" } ?: "—"
        val workingText = if (metrics.eggMassG != null && metrics.effectiveEggWeightG != null) {
            "(${LayerCalc.format1(metrics.henDayPct)}% ÷ 100) × ${LayerCalc.format1(metrics.effectiveEggWeightG)} g = ${LayerCalc.format1(metrics.eggMassG)} g"
        } else {
            "Weigh a sample of eggs (in Record → More details) to calculate daily egg mass."
        }
        return LayerCalc.MetricExplanation(
            id = "egg_mass",
            title = "Egg mass",
            subtitle = "Grams of egg produced per live hen today",
            formattedValue = valText,
            working = workingText,
            status = metrics.eggMassStatus,
            formula = "(Hen-day % ÷ 100) × Average egg weight (g)",
            targetDescription = "55 g or more per bird per day from 25 weeks onward (skipped before 25 weeks).",
            whatToCheck = "Low egg mass comes from either low lay % or small egg weight ($weightText). Check protein/methionine levels in feed, daily feed intake, and house heat."
        )
    }

    fun buildForEggWeight(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        val valText = metrics.effectiveEggWeightG?.let { "${LayerCalc.format1(it)} g" } ?: "—"
        val workingText = if (metrics.effectiveEggWeightG != null) {
            "Average weight: ${LayerCalc.format1(metrics.effectiveEggWeightG)} g (recorded on ${metrics.eggWeightDateUsed})"
        } else {
            "No egg weight entered in the last 14 days."
        }
        return LayerCalc.MetricExplanation(
            id = "egg_weight",
            title = "Average egg weight",
            subtitle = "Grams per egg (weighed weekly, valid for 14 days)",
            formattedValue = valText,
            working = workingText,
            status = metrics.eggWeightStatus,
            formula = "Total weight of 30 random eggs (g) ÷ 30",
            targetDescription = "55–65 g from 30 weeks onward (no status chip before 30 weeks while pullet eggs size up).",
            whatToCheck = "Small eggs: check pullet body weight at point of lay, house heat reducing feed intake, or dietary linoleic acid and amino acids. Oversized eggs (>65 g) have thinner shells and crack more easily."
        )
    }

    fun buildForEggsPerHenHoused(metrics: LayerCalc.DayMetrics, hensHoused: Int, totalEggs: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "eggs_per_hh",
            title = "Eggs per hen housed",
            subtitle = "Cumulative eggs collected per bird placed to date",
            formattedValue = LayerCalc.format1(metrics.eggsPerHenHousedToDate),
            working = "$totalEggs total eggs ÷ $hensHoused hens housed = ${LayerCalc.format1(metrics.eggsPerHenHousedToDate)} eggs/hen",
            status = LayerCalc.TargetState.ON_TARGET,
            formula = "Total eggs collected since placement ÷ Hens housed",
            targetDescription = "Climbs by ~6 eggs per week in peak lay, aiming for 300+ eggs per hen housed by 72–80 weeks.",
            whatToCheck = "Keep both daily lay rate high and mortality low so every housed space keeps producing."
        )
    }

    fun buildForFeedPerBird(metrics: LayerCalc.DayMetrics, feedKg: Double): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "feed_per_bird",
            title = "Feed per bird",
            subtitle = "Grams of feed used per live hen today",
            formattedValue = "${LayerCalc.format0(metrics.feedPerBirdG)} g",
            working = "${feedKg} kg × 1,000 ÷ ${metrics.liveHens} live hens = ${LayerCalc.format1(metrics.feedPerBirdG)} g/bird",
            status = metrics.feedStatus,
            formula = "Feed given (kg) × 1,000 ÷ Live hens",
            targetDescription = "110–125 g per bird is on target. 100–110 g or 125–130 g is Watch. Under 100 g or over 130 g is Off target.",
            whatToCheck = "Under 100 g: check house heat (feed birds in cooler morning/evening hours), water supply, spoiled feed, or illness. Over 130 g: check feeder depth/spillage, rats, or wild birds."
        )
    }

    fun buildForFcr(metrics: LayerCalc.DayMetrics, feedKg: Double): LayerCalc.MetricExplanation {
        val valText = metrics.fcr?.let { LayerCalc.format2(it) } ?: "—"
        val workingText = if (metrics.fcr != null && metrics.effectiveEggWeightG != null) {
            val eggKg = (metrics.eggs * metrics.effectiveEggWeightG) / 1000.0
            "$feedKg kg feed ÷ (${metrics.eggs} eggs × ${LayerCalc.format1(metrics.effectiveEggWeightG)} g ÷ 1,000 = ${LayerCalc.format1(eggKg)} kg eggs) = ${LayerCalc.format2(metrics.fcr)}"
        } else {
            "Requires today's or a recent (≤14 days) average egg weight."
        }
        return LayerCalc.MetricExplanation(
            id = "fcr",
            title = "FCR",
            subtitle = "kg of feed per kg of eggs",
            formattedValue = valText,
            working = workingText,
            status = metrics.fcrStatus,
            formula = "Feed given (kg) ÷ (Eggs × Egg weight in g ÷ 1,000)",
            targetDescription = "2.00–2.20 is On target; up to 2.40 is Watch; over 2.40 is Off target (skipped before 24 weeks).",
            whatToCheck = "High FCR means too much feed is spent per kg of eggs. Check feed wastage at troughs, rodent control, cold drafts, or drops in egg production."
        )
    }

    fun buildForWaterPerBird(metrics: LayerCalc.DayMetrics, waterL: Double): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "water_per_bird",
            title = "Water per bird",
            subtitle = "Millilitres of water drunk per live hen today",
            formattedValue = "${LayerCalc.format0(metrics.waterPerBirdMl)} mL",
            working = "${waterL} L × 1,000 ÷ ${metrics.liveHens} live hens = ${LayerCalc.format0(metrics.waterPerBirdMl)} mL/bird",
            status = metrics.waterStatus,
            formula = "Water used (L) × 1,000 ÷ Live hens",
            targetDescription = "Typically 180–250 mL/bird (about 1.8–2.0× feed weight in temperate weather, more in heat). Must stay above 85% of the previous 7-day average.",
            whatToCheck = "Drinking often falls before egg production drops! If water falls below 85% of the 7-day average, check blocked nipples/bell drinkers, airlocks, header tank ball valves, hot water pipes, or early flock sickness."
        )
    }

    fun buildForSevenDayMortality(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "seven_day_mortality",
            title = "7-day mortality",
            subtitle = "Deaths in the last 7 days per 100 hens",
            formattedValue = "${LayerCalc.format2(metrics.sevenDayMortalityPct)}%",
            working = "${metrics.deathsIn7Days} deaths in 7-day window (${LayerCalc.format2(metrics.sevenDayMortalityPct)}%)",
            status = metrics.sevenDayMortalityStatus,
            formula = "Deaths in the 7 days ending today ÷ Live hens on the first day of that window × 100",
            targetDescription = "Under 0.10% per week is On target. 0.10–0.50% is Watch. Over 0.50% is Off target (Critical).",
            whatToCheck = "In small flocks (<1,000 birds), even 1 dead hen exceeds 0.1%. Inspect dead birds immediately, check house temperature, ventilation, water, and call a vet if deaths cluster."
        )
    }

    fun buildForCumulativeMortality(metrics: LayerCalc.DayMetrics, hensHoused: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "cumulative_mortality",
            title = "Total mortality",
            subtitle = "All deaths since placement per 100 hens housed",
            formattedValue = "${LayerCalc.format1(metrics.cumulativeMortalityPct)}%",
            working = "${metrics.totalDeathsToDate} total deaths ÷ $hensHoused hens housed × 100 = ${LayerCalc.format1(metrics.cumulativeMortalityPct)}%",
            status = metrics.cumulativeMortalityStatus,
            formula = "Total deaths since placement ÷ Hens housed × 100",
            targetDescription = "Under 3% through peak lay, and under 5–7% across a full laying cycle.",
            whatToCheck = "Review weekly mortality bars in Trends to see whether losses happened in one spike (disease/heat event) or crept up steadily (pecking/prolapse)."
        )
    }

    fun buildForCracked(metrics: LayerCalc.DayMetrics, crackedCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "cracked",
            title = "Cracked %",
            subtitle = "Cracked eggs per 100 eggs collected",
            formattedValue = "${LayerCalc.format1(metrics.crackedPct)}%",
            working = "$crackedCount cracked ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.crackedPct)}%",
            status = metrics.crackedStatus,
            formula = "Cracked count ÷ Total eggs collected × 100",
            targetDescription = "2.0% or under is On target.",
            whatToCheck = "Collect eggs more often (at least 3× a day), add coarse limestone/oyster shell grit in the afternoon, check vitamin D3/calcium, reduce heat stress, and pad nest roll-out trays."
        )
    }

    fun buildForDirty(metrics: LayerCalc.DayMetrics, dirtyCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "dirty",
            title = "Dirty %",
            subtitle = "Manure or litter-stained eggs per 100 eggs",
            formattedValue = "${LayerCalc.format1(metrics.dirtyPct)}%",
            working = "$dirtyCount dirty ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.dirtyPct)}%",
            status = metrics.dirtyStatus,
            formula = "Dirty count ÷ Total eggs collected × 100",
            targetDescription = "3.0% or under is On target.",
            whatToCheck = "Refresh nest box litter/astro-turf pads, close nests at night so hens don't roost and defecate inside, fix leaking drinkers causing wet floor litter, and check gut health."
        )
    }

    fun buildForSoftShell(metrics: LayerCalc.DayMetrics, softShellCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "soft_shell",
            title = "Soft-shell %",
            subtitle = "Shell-less or thin membrane eggs per 100 eggs",
            formattedValue = "${LayerCalc.format1(metrics.softShellPct)}%",
            working = "$softShellCount soft-shell ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.softShellPct)}%",
            status = metrics.softShellStatus,
            formula = "Soft-shell count ÷ Total eggs collected × 100",
            targetDescription = "1.0% or under is On target.",
            whatToCheck = "Check calcium/phosphorus/vitamin D3 balance and house heat (panting lowers blood carbonate for shells). If soft shells jump suddenly with a drop in lay, ask a vet about infectious bronchitis (IB) or egg drop syndrome (EDS)."
        )
    }
}
