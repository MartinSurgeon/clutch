package com.example.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.domain.ReportBuilder
import com.example.domain.WeeklyCalc
import com.example.ui.ActiveFlockDashboard
import kotlinx.coroutines.launch

/** The latest day's report for the pen, or null before the first record. */
fun latestDayReport(dashboard: ActiveFlockDashboard): ReportBuilder.Report? {
    val latest = dashboard.latestRecord ?: return null
    return ReportBuilder.daily(
        flockName = dashboard.flock.name,
        date = latest.entity.date,
        metrics = latest.metrics,
        feedKg = latest.entity.feedKg,
        warnings = dashboard.warnings,
        notes = latest.entity.notes
    )
}

fun weekReport(dashboard: ActiveFlockDashboard, week: WeeklyCalc.WeekSummary): ReportBuilder.Report {
    val note = dashboard.weeklyNotes[week.ageWeek]
    return ReportBuilder.weekly(dashboard.flock.name, week, note?.notes, note?.actions)
}

fun latestWeekReport(dashboard: ActiveFlockDashboard): ReportBuilder.Report? =
    dashboard.weeklySummaries.firstOrNull()?.let { weekReport(dashboard, it) }

/** Returns a function that shares a report — straight to WhatsApp when [toWhatsApp] is true. */
@Composable
fun rememberShareReport(): (ReportBuilder.Report, Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        { report, toWhatsApp -> scope.launch { ReportSharer.share(context, report, toWhatsApp) } }
    }
}

@Composable
fun rememberHasWhatsApp(): Boolean {
    val context = LocalContext.current
    return remember(context) { ReportSharer.hasWhatsApp(context) }
}
