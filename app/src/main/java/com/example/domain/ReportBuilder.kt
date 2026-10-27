package com.example.domain

/**
 * Builds the farm report a caretaker sends to the owner, vet or buyer on WhatsApp.
 * The same [Report] drives both the text message and the shareable image, so they always agree.
 * Pure Kotlin, no Android imports.
 */
object ReportBuilder {

    enum class Tone { GOOD, WATCH, BAD, NEUTRAL }

    data class Stat(
        val emoji: String,
        val label: String,
        val value: String,
        val note: String? = null,
        val tone: Tone = Tone.NEUTRAL
    )

    data class Report(
        val title: String,
        val subtitle: String,
        val stats: List<Stat>,
        val alerts: List<String>,
        val notes: String?
    )

    private fun toneOf(state: LayerCalc.TargetState): Tone = when (state) {
        LayerCalc.TargetState.ON_TARGET -> Tone.GOOD
        LayerCalc.TargetState.WATCH -> Tone.WATCH
        LayerCalc.TargetState.OFF_TARGET -> Tone.BAD
        else -> Tone.NEUTRAL
    }

    private fun statusWord(state: LayerCalc.TargetState): String? = when (state) {
        LayerCalc.TargetState.ON_TARGET,
        LayerCalc.TargetState.WATCH,
        LayerCalc.TargetState.OFF_TARGET -> state.label
        else -> null
    }

    fun crates(eggs: Int): String {
        val crates = eggs / 30
        val loose = eggs % 30
        return when {
            crates == 0 -> "$loose eggs"
            loose == 0 -> "$crates ${if (crates == 1) "crate" else "crates"}"
            else -> "$crates ${if (crates == 1) "crate" else "crates"} + $loose"
        }
    }

    fun daily(
        flockName: String,
        date: String,
        metrics: LayerCalc.DayMetrics,
        feedKg: Double,
        warnings: List<LayerCalc.WarningItem>,
        notes: String
    ): Report {
        val stats = buildList {
            add(Stat("🥚", "Eggs", "${metrics.eggs}", crates(metrics.eggs)))
            add(Stat("📈", "Hen-day", "${LayerCalc.format1(metrics.henDayPct)}%", statusWord(metrics.henDayStatus), toneOf(metrics.henDayStatus)))
            add(
                Stat(
                    "🐔", "Live birds", "${metrics.closingHens}",
                    if (metrics.closingHens != metrics.liveHens) "${metrics.liveHens - metrics.closingHens} died or removed" else "No deaths",
                    if (metrics.closingHens == metrics.liveHens) Tone.GOOD else Tone.WATCH
                )
            )
            add(Stat("🌾", "Feed", "$feedKg kg", "${LayerCalc.format0(metrics.feedPerBirdG)} g per bird", toneOf(metrics.feedStatus)))
            metrics.fcr?.let { add(Stat("⚖️", "FCR", LayerCalc.format2(it), statusWord(metrics.fcrStatus), toneOf(metrics.fcrStatus))) }
            val bad = metrics.crackedPct + metrics.dirtyPct + metrics.softShellPct
            if (metrics.eggs > 0) {
                val state = LayerCalc.evaluateEggQualityTarget(bad)
                add(Stat("🛡️", "Bad eggs", "${LayerCalc.format1(bad)}%", statusWord(state), toneOf(state)))
            }
        }
        return Report(
            title = flockName,
            subtitle = "${LayerCalc.friendlyDate(date, withYear = true)} • Week ${metrics.ageWeeks}",
            stats = stats,
            alerts = warnings.take(2).map { it.title },
            notes = notes.ifBlank { null }
        )
    }

    fun weekly(
        flockName: String,
        week: WeeklyCalc.WeekSummary,
        notes: String?,
        actions: String?
    ): Report {
        val stats = buildList {
            add(Stat("🥚", "Eggs this week", "${week.totalEggs}", crates(week.totalEggs)))
            add(Stat("📈", "Hen-day", "${LayerCalc.format1(week.henDayPct)}%", statusWord(week.henDayStatus), toneOf(week.henDayStatus)))
            add(Stat("🐔", "Average birds", LayerCalc.format0(week.avgLiveHens), "${week.deaths} died", toneOf(week.mortalityStatus)))
            add(Stat("🌾", "Feed used", "${LayerCalc.format1(week.totalFeedKg)} kg", "${LayerCalc.format0(week.feedPerBirdG)} g per bird a day", toneOf(week.feedStatus)))
            week.fcr?.let { add(Stat("⚖️", "FCR", LayerCalc.format2(it), statusWord(week.fcrStatus), toneOf(week.fcrStatus))) }
            if (week.totalEggs > 0) {
                add(Stat("🛡️", "Bad eggs", "${LayerCalc.format1(week.badEggPct)}%", statusWord(week.qualityStatus), toneOf(week.qualityStatus)))
            }
        }
        val combinedNotes = listOfNotNull(
            notes?.takeIf { it.isNotBlank() }?.let { "What happened: $it" },
            actions?.takeIf { it.isNotBlank() }?.let { "Actions: $it" }
        ).joinToString("\n").ifBlank { null }
        val range = if (week.startDate == week.endDate) {
            LayerCalc.friendlyDate(week.startDate)
        } else {
            "${LayerCalc.friendlyDate(week.startDate)} – ${LayerCalc.friendlyDate(week.endDate)}"
        }
        return Report(
            title = flockName,
            subtitle = "Week ${week.ageWeek} of age • $range (${week.daysRecorded} of 7 days)",
            stats = stats,
            alerts = emptyList(),
            notes = combinedNotes
        )
    }

    /** WhatsApp-friendly text: *bold* headings, one fact per line, emoji as visual anchors. */
    fun toText(report: Report): String = buildString {
        appendLine("*${report.title} — farm report*")
        appendLine(report.subtitle)
        appendLine()
        report.stats.forEach { s ->
            val mark = when (s.tone) {
                Tone.GOOD -> " ✅"
                Tone.WATCH -> " ⚠️"
                Tone.BAD -> " ❗"
                Tone.NEUTRAL -> ""
            }
            append("${s.emoji} ${s.label}: *${s.value}*")
            s.note?.let { append(" ($it)") }
            appendLine(mark)
        }
        if (report.alerts.isNotEmpty()) {
            appendLine()
            appendLine("*Things to check*")
            report.alerts.forEach { appendLine("• $it") }
        }
        report.notes?.let {
            appendLine()
            appendLine("📝 $it")
        }
        appendLine()
        append("Sent from Clutch")
    }
}
