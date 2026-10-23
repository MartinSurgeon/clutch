package com.example.domain

/**
 * Provides plain-language explanations, formulas, targets, and troubleshooting steps
 * when the farmer taps any number card (Tesler's Law).
 * Written in everyday Ghanaian farm English: birds, pens, crates, feed, water.
 */
object MetricExplanations {

    fun buildForHenDay(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "hen_day",
            title = "Laying %",
            subtitle = "Eggs for every 100 birds today (also called hen-day %)",
            formattedValue = "${LayerCalc.format1(metrics.henDayPct)}%",
            working = metrics.henDayWorking,
            status = metrics.henDayStatus,
            formula = "Eggs picked today ÷ Birds at start of day × 100",
            targetDescription = "85–95% when birds are 25 to 35 weeks old. Before 25 weeks it is still rising. After 35 weeks it slowly drops, about 0.5% each week.",
            whatToCheck = "If laying drops suddenly, check water first, then feed, light hours (keep 14–16 hours, never cut light during lay), heat in the pen, worms and mites, or signs of sickness."
        )
    }

    fun buildForHenHoused(metrics: LayerCalc.DayMetrics, hensHoused: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "hen_housed",
            title = "Laying % (all birds stocked)",
            subtitle = "Eggs for every 100 birds you first stocked",
            formattedValue = "${LayerCalc.format1(metrics.henHousedPct)}%",
            working = "${metrics.eggs} eggs ÷ $hensHoused birds stocked × 100 = ${LayerCalc.format1(metrics.henHousedPct)}%",
            status = metrics.henDayStatus,
            formula = "Eggs picked today ÷ Birds first stocked × 100 (also called hen-housed %)",
            targetDescription = "Should stay close to your daily Laying % when few birds die or are sold.",
            whatToCheck = "If this number falls far below your daily Laying %, you have lost many birds since stocking. Check Reports to see when."
        )
    }

    fun buildForEggMass(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        val weightText = metrics.effectiveEggWeightG?.let { "${LayerCalc.format1(it)} g" } ?: "not weighed in the last 14 days"
        val valText = metrics.eggMassG?.let { "${LayerCalc.format1(it)} g" } ?: "—"
        val workingText = if (metrics.eggMassG != null && metrics.effectiveEggWeightG != null) {
            "(${LayerCalc.format1(metrics.henDayPct)}% ÷ 100) × ${LayerCalc.format1(metrics.effectiveEggWeightG)} g = ${LayerCalc.format1(metrics.eggMassG)} g"
        } else {
            "Weigh some eggs (Record → Extra details) to see this number."
        }
        return LayerCalc.MetricExplanation(
            id = "egg_mass",
            title = "Egg mass",
            subtitle = "Grams of egg each bird gave today",
            formattedValue = valText,
            working = workingText,
            status = metrics.eggMassStatus,
            formula = "(Laying % ÷ 100) × Average egg weight (g)",
            targetDescription = "55 g or more per bird each day from 25 weeks old.",
            whatToCheck = "Low egg mass means low laying or small eggs ($weightText). Check protein in the feed, how much the birds eat, and heat in the pen."
        )
    }

    fun buildForEggWeight(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        val valText = metrics.effectiveEggWeightG?.let { "${LayerCalc.format1(it)} g" } ?: "—"
        val workingText = if (metrics.effectiveEggWeightG != null) {
            "Average weight: ${LayerCalc.format1(metrics.effectiveEggWeightG)} g (weighed on ${metrics.eggWeightDateUsed})"
        } else {
            "No egg weight entered in the last 14 days."
        }
        return LayerCalc.MetricExplanation(
            id = "egg_weight",
            title = "Egg size",
            subtitle = "Average grams per egg (weigh once a week)",
            formattedValue = valText,
            working = workingText,
            status = metrics.eggWeightStatus,
            formula = "Weight of 30 eggs from one crate (g) ÷ 30",
            targetDescription = "55–65 g from 30 weeks old. Young birds lay small eggs at first, so no rating before then.",
            whatToCheck = "Small eggs: check bird weight, heat in the pen (hot birds eat less), and feed quality. Very big eggs (over 65 g) have thin shells and crack easily."
        )
    }

    fun buildForEggsPerHenHoused(metrics: LayerCalc.DayMetrics, hensHoused: Int, totalEggs: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "eggs_per_hh",
            title = "Eggs per bird so far",
            subtitle = "All eggs picked ÷ birds first stocked",
            formattedValue = LayerCalc.format1(metrics.eggsPerHenHousedToDate),
            working = "$totalEggs eggs ÷ $hensHoused birds stocked = ${LayerCalc.format1(metrics.eggsPerHenHousedToDate)} eggs per bird",
            status = LayerCalc.TargetState.ON_TARGET,
            formula = "All eggs picked since stocking ÷ Birds first stocked",
            targetDescription = "Goes up by about 6 eggs a week at peak. Good birds give 300 or more eggs each by 72–80 weeks.",
            whatToCheck = "Keep laying high and deaths low so every bird you paid for keeps giving eggs."
        )
    }

    fun buildForFeedPerBird(metrics: LayerCalc.DayMetrics, feedKg: Double): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "feed_per_bird",
            title = "Feed per bird",
            subtitle = "Grams of feed each bird ate today",
            formattedValue = "${LayerCalc.format0(metrics.feedPerBirdG)} g",
            working = "$feedKg kg × 1,000 ÷ ${metrics.liveHens} birds = ${LayerCalc.format1(metrics.feedPerBirdG)} g each",
            status = metrics.feedStatus,
            formula = "Feed given (kg) × 1,000 ÷ Birds",
            targetDescription = "110–125 g per bird is good. 100–110 g or 125–130 g, watch it. Under 100 g or over 130 g needs attention.",
            whatToCheck = "Too little: heat in the pen (feed in the cool morning and evening), water supply, spoilt feed, or sickness. Too much: feed spilling, rats, or wild birds."
        )
    }

    fun buildForFcr(metrics: LayerCalc.DayMetrics, feedKg: Double): LayerCalc.MetricExplanation {
        val valText = metrics.fcr?.let { LayerCalc.format2(it) } ?: "—"
        val workingText = if (metrics.fcr != null && metrics.effectiveEggWeightG != null) {
            val eggKg = (metrics.eggs * metrics.effectiveEggWeightG) / 1000.0
            "$feedKg kg feed ÷ (${metrics.eggs} eggs × ${LayerCalc.format1(metrics.effectiveEggWeightG)} g ÷ 1,000 = ${LayerCalc.format1(eggKg)} kg eggs) = ${LayerCalc.format2(metrics.fcr)}"
        } else {
            "Needs an egg weight from today or the last 14 days."
        }
        return LayerCalc.MetricExplanation(
            id = "fcr",
            title = "Feed for 1 kg of eggs (FCR)",
            subtitle = "Kg of feed used to get 1 kg of eggs",
            formattedValue = valText,
            working = workingText,
            status = metrics.fcrStatus,
            formula = "Feed given (kg) ÷ (Eggs × Egg weight in g ÷ 1,000)",
            targetDescription = "2.00–2.20 is good. Up to 2.40, watch it. Over 2.40 needs attention. Not rated before 24 weeks.",
            whatToCheck = "A high number means you are spending too much feed for your eggs. Check feed spilling, rats, cold draughts at night, or a drop in laying."
        )
    }

    fun buildForWaterPerBird(metrics: LayerCalc.DayMetrics, waterL: Double): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "water_per_bird",
            title = "Water per bird",
            subtitle = "Millilitres each bird drank today",
            formattedValue = "${LayerCalc.format0(metrics.waterPerBirdMl)} mL",
            working = "$waterL L × 1,000 ÷ ${metrics.liveHens} birds = ${LayerCalc.format0(metrics.waterPerBirdMl)} mL each",
            status = metrics.waterStatus,
            formula = "Water used (L) × 1,000 ÷ Birds",
            targetDescription = "Usually 180–250 mL per bird, more in the hot season. It should not fall below 85% of the last 7 days.",
            whatToCheck = "Birds drink less before they lay less! Check for blocked nipples or drinkers, an empty tank, hot water in the pipes, or early sickness."
        )
    }

    fun buildForSevenDayMortality(metrics: LayerCalc.DayMetrics): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "seven_day_mortality",
            title = "Deaths this week",
            subtitle = "Birds that died in the last 7 days, per 100 birds",
            formattedValue = "${LayerCalc.format2(metrics.sevenDayMortalityPct)}%",
            working = "${metrics.deathsIn7Days} deaths in 7 days (${LayerCalc.format2(metrics.sevenDayMortalityPct)}%)",
            status = metrics.sevenDayMortalityStatus,
            formula = "Deaths in the last 7 days ÷ Birds on the first of those days × 100",
            targetDescription = "Under 0.10% a week is good. 0.10–0.50%, watch it. Over 0.50% needs attention now.",
            whatToCheck = "In a pen of under 1,000 birds, even 1 death goes over 0.1%. Look at dead birds quickly, check heat, air flow and water, and call your vet if deaths continue."
        )
    }

    fun buildForCumulativeMortality(metrics: LayerCalc.DayMetrics, hensHoused: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "cumulative_mortality",
            title = "Deaths since stocking",
            subtitle = "All birds lost, per 100 birds first stocked",
            formattedValue = "${LayerCalc.format1(metrics.cumulativeMortalityPct)}%",
            working = "${metrics.totalDeathsToDate} deaths ÷ $hensHoused birds stocked × 100 = ${LayerCalc.format1(metrics.cumulativeMortalityPct)}%",
            status = metrics.cumulativeMortalityStatus,
            formula = "All deaths since stocking ÷ Birds first stocked × 100",
            targetDescription = "Under 3% up to peak laying. Under 5–7% for the whole laying period.",
            whatToCheck = "Open Reports and look at deaths by week. One big jump points to sickness or heat. A slow rise points to pecking or prolapse."
        )
    }

    fun buildForCracked(metrics: LayerCalc.DayMetrics, crackedCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "cracked",
            title = "Cracked eggs",
            subtitle = "Cracked eggs for every 100 picked",
            formattedValue = "${LayerCalc.format1(metrics.crackedPct)}%",
            working = "$crackedCount cracked ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.crackedPct)}%",
            status = metrics.crackedStatus,
            formula = "Cracked eggs ÷ All eggs picked × 100",
            targetDescription = "2% or less is good.",
            whatToCheck = "Pick eggs at least 3 times a day, give oyster shell or shell grit in the afternoon, reduce heat stress, and handle crates gently."
        )
    }

    fun buildForDirty(metrics: LayerCalc.DayMetrics, dirtyCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "dirty",
            title = "Dirty eggs",
            subtitle = "Eggs with droppings or litter, for every 100 picked",
            formattedValue = "${LayerCalc.format1(metrics.dirtyPct)}%",
            working = "$dirtyCount dirty ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.dirtyPct)}%",
            status = metrics.dirtyStatus,
            formula = "Dirty eggs ÷ All eggs picked × 100",
            targetDescription = "3% or less is good.",
            whatToCheck = "Change nest box litter often, close nests at night so birds don't sleep in them, and fix leaking drinkers that wet the floor."
        )
    }

    fun buildForSoftShell(metrics: LayerCalc.DayMetrics, softShellCount: Int): LayerCalc.MetricExplanation {
        return LayerCalc.MetricExplanation(
            id = "soft_shell",
            title = "Soft-shell eggs",
            subtitle = "Eggs with soft or no shell, for every 100 picked",
            formattedValue = "${LayerCalc.format1(metrics.softShellPct)}%",
            working = "$softShellCount soft-shell ÷ ${metrics.eggs} eggs × 100 = ${LayerCalc.format1(metrics.softShellPct)}%",
            status = metrics.softShellStatus,
            formula = "Soft-shell eggs ÷ All eggs picked × 100",
            targetDescription = "1% or less is good.",
            whatToCheck = "Check calcium in the feed and heat in the pen (panting birds make weak shells). If soft shells jump with a drop in laying, ask your vet about IB or egg drop syndrome."
        )
    }
}
