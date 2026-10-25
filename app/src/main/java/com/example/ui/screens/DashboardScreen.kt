package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.domain.MetricExplanations
import com.example.domain.ReferenceTargets
import com.example.ui.ActiveFlockDashboard
import com.example.ui.ClutchUiState
import com.example.ui.RecordWithMetrics
import com.example.ui.TrendWindow
import com.example.ui.components.BrandHeroCard
import com.example.ui.components.ChartPoint
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EggYolkLogo
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IconBadge
import com.example.ui.components.InfoNote
import com.example.ui.components.LineTrendChartCard
import com.example.ui.components.MetricExplanationBottomSheet
import com.example.ui.components.OfflineReassurance
import com.example.ui.components.ScreenColumn
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.StatusChip
import com.example.ui.components.WarningsSectionCard
import com.example.ui.components.WeeklyMortalityBarChartCard
import com.example.ui.theme.ClutchIcons
import com.example.ui.theme.EggYolkHighlight
import com.example.ui.theme.OnBrandDeepGreen
import com.example.ui.theme.OnBrandDeepGreenMuted
import java.util.Calendar

@Composable
fun DashboardScreen(
    uiState: ClutchUiState,
    onAddFirstFlock: () -> Unit,
    onTryExampleData: () -> Unit,
    onRecordToday: () -> Unit,
    onSelectWindow: (TrendWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExplanation by remember { mutableStateOf<LayerCalc.MetricExplanation?>(null) }
    val dashboard = uiState.activeDashboard

    ScreenColumn(modifier = modifier, testTag = "today_screen") {
        when {
            uiState.flocks.isEmpty() || dashboard == null -> {
                FirstRunWelcome(
                    onAddFirstFlock = onAddFirstFlock,
                    onTryExampleData = onTryExampleData
                )
            }
            dashboard.latestRecord == null -> {
                Greeting(todayDate = uiState.todayDate)
                EmptyStateCard(
                    icon = ClutchIcons.Record,
                    title = "${dashboard.flock.name} is ready",
                    body = "${dashboard.flock.hensHoused} ${dashboard.flock.breed} birds, ${dashboard.ageWeeksToday} weeks old. " +
                        "Record your first day to see your performance dashboard.",
                    primaryLabel = "Record today's count",
                    onPrimary = onRecordToday,
                    primaryTestTag = "record_today_empty_button",
                    modifier = Modifier.testTag("no_records_empty_state")
                )
                OfflineReassurance()
            }
            else -> {
                DashboardContent(
                    dashboard = dashboard,
                    todayDate = uiState.todayDate,
                    trendWindow = uiState.trendWindow,
                    onSelectWindow = onSelectWindow,
                    onMetricClick = { selectedExplanation = it },
                    onRecordToday = onRecordToday
                )
            }
        }
    }

    selectedExplanation?.let { explanation ->
        MetricExplanationBottomSheet(
            explanation = explanation,
            onDismiss = { selectedExplanation = null }
        )
    }
}

@Composable
private fun Greeting(todayDate: String) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = LayerCalc.friendlyDate(todayDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FirstRunWelcome(
    onAddFirstFlock: () -> Unit,
    onTryExampleData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("first_run_empty_state"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        EggYolkLogo(size = 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Akwaaba! Welcome to Clutch",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your layer farm records, weekly summaries and targets in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ClutchCard {
            BenefitRow(
                icon = ClutchIcons.Daily,
                title = "Daily records in a minute",
                body = "Eggs in crates, feed, deaths and egg weight. The sums are done for you."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BenefitRow(
                icon = ClutchIcons.Dashboard,
                title = "See your performance",
                body = "Laying %, feed, egg mass, deaths and egg quality as simple charts."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BenefitRow(
                icon = ClutchIcons.Weekly,
                title = "Weekly summary and notes",
                body = "Totals for each week, with space for what you did about it."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BenefitRow(
                icon = ClutchIcons.Offline,
                title = "Works without data",
                body = "Everything is saved on your phone."
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ClutchPrimaryButton(
                text = "Add my first pen",
                icon = ClutchIcons.Add,
                onClick = onAddFirstFlock,
                testTag = "add_first_flock_button"
            )
            ClutchSecondaryButton(
                text = "Try with a sample pen",
                onClick = onTryExampleData,
                testTag = "try_example_data_button"
            )
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, body: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: ActiveFlockDashboard,
    todayDate: String,
    trendWindow: TrendWindow,
    onSelectWindow: (TrendWindow) -> Unit,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit,
    onRecordToday: () -> Unit
) {
    val latest = dashboard.latestRecord ?: return
    val m = latest.metrics
    val rec = latest.entity

    // 1. Greeting + today's one job (serial position: most important first)
    Greeting(todayDate = todayDate)
    TodayTaskCard(isTodayRecorded = dashboard.isTodayRecorded, onRecordToday = onRecordToday)

    // 2. Headline number
    LayingHeroCard(
        metrics = m,
        isLatestToday = rec.date == todayDate,
        onClick = { onMetricClick(MetricExplanations.buildForHenDay(m)) }
    )

    // 3. Latest flock performance indicators (6 tiles, 2 per row)
    LatestIndicators(dashboard = dashboard, latest = latest, onMetricClick = onMetricClick)

    // 4. Things to check
    WarningsSectionCard(warnings = dashboard.warnings)

    // 5. Trends
    TrendCharts(dashboard = dashboard, trendWindow = trendWindow, onSelectWindow = onSelectWindow)

    // 6. Strong, calm ending (peak-end rule)
    if (!dashboard.isTodayRecorded) {
        ClutchPrimaryButton(
            text = "Record today's count",
            icon = ClutchIcons.Record,
            onClick = onRecordToday,
            testTag = "record_today_button_end"
        )
    }
    OfflineReassurance()
}

@Composable
private fun TodayTaskCard(
    isTodayRecorded: Boolean,
    onRecordToday: () -> Unit
) {
    if (isTodayRecorded) {
        ClutchCard(
            modifier = Modifier.testTag("today_recorded_card"),
            contentPadding = 12.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = ClutchIcons.Good,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's count is saved",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Well done. See you tomorrow!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onRecordToday,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_today_button")
                ) {
                    Text("Change", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        ClutchCard(
            modifier = Modifier.testTag("record_today_card"),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
        ) {
            Column {
                Text(
                    text = "Today's count is not in yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "It takes about a minute. Do it after the last egg pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            ClutchPrimaryButton(
                text = "Record today's count",
                icon = ClutchIcons.Record,
                onClick = onRecordToday,
                testTag = "record_today_button"
            )
        }
    }
}

@Composable
private fun LayingHeroCard(
    metrics: LayerCalc.DayMetrics,
    isLatestToday: Boolean,
    onClick: () -> Unit
) {
    BrandHeroCard(
        modifier = Modifier.testTag("henday_hero_card"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hen-day production",
                    style = MaterialTheme.typography.titleSmall,
                    color = OnBrandDeepGreen
                )
                Text(
                    text = if (isLatestToday) "Today • Week ${metrics.ageWeeks}" else "Last count ${LayerCalc.friendlyDate(metrics.date)} • Week ${metrics.ageWeeks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBrandDeepGreenMuted
                )
            }
            StatusChip(state = metrics.henDayStatus)
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${LayerCalc.format1(metrics.henDayPct)}%",
                style = MaterialTheme.typography.displayLarge,
                color = OnBrandDeepGreen,
                modifier = Modifier.testTag("hero_henday_value")
            )
            metrics.henDayChangeVsYesterday?.let { delta ->
                val (deltaIcon, deltaText) = when {
                    delta > 0.05 -> ClutchIcons.TrendUp to "Up ${LayerCalc.format1(delta)}"
                    delta < -0.05 -> ClutchIcons.TrendDown to "Down ${LayerCalc.format1(-delta)}"
                    else -> ClutchIcons.TrendFlat to "Same as"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = deltaIcon,
                        contentDescription = null,
                        tint = EggYolkHighlight,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "$deltaText yesterday",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnBrandDeepGreen
                    )
                }
            }
        }

        HorizontalDivider(color = OnBrandDeepGreen.copy(alpha = 0.2f))

        Row(modifier = Modifier.fillMaxWidth()) {
            HeroFact(label = "Hen-housed", value = "${LayerCalc.format1(metrics.henHousedPct)}%", modifier = Modifier.weight(1f))
            HeroFact(label = "Eggs", value = "${metrics.eggs}", modifier = Modifier.weight(1f))
            HeroFact(label = "Live hens", value = "${metrics.liveHens}", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = OnBrandDeepGreenMuted)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = OnBrandDeepGreen)
    }
}

@Composable
private fun LatestIndicators(
    dashboard: ActiveFlockDashboard,
    latest: RecordWithMetrics,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit
) {
    val m = latest.metrics
    val rec = latest.entity
    val birdsNow = m.closingHens
    val badEggPct = m.crackedPct + m.dirtyPct + m.softShellPct
    val weighIn = dashboard.latestWeighIn

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Latest performance", subtitle = "Tap any number to see what it means")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Feed intake",
                value = "${LayerCalc.format0(m.feedPerBirdG)} g",
                caption = "per bird • ${rec.feedKg} kg given",
                icon = ClutchIcons.Feed,
                status = m.feedStatus,
                onClick = { onMetricClick(MetricExplanations.buildForFeedPerBird(m, rec.feedKg)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_feed"
            )
            StatTile(
                label = "Egg mass",
                value = m.eggMassG?.let { "${LayerCalc.format1(it)} g" } ?: "—",
                caption = m.effectiveEggWeightG?.let { "egg weight ${LayerCalc.format1(it)} g" } ?: "weigh eggs to see",
                icon = ClutchIcons.Egg,
                status = m.eggMassStatus,
                onClick = { onMetricClick(MetricExplanations.buildForEggMass(m)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_egg_mass"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "FCR",
                value = m.fcr?.let { LayerCalc.format2(it) } ?: "—",
                caption = "kg feed per kg eggs",
                icon = ClutchIcons.Uniformity,
                status = m.fcrStatus,
                onClick = { onMetricClick(MetricExplanations.buildForFcr(m, rec.feedKg)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_fcr"
            )
            StatTile(
                label = "Deaths this week",
                value = "${m.deathsIn7Days}",
                caption = "${LayerCalc.format2(m.sevenDayMortalityPct)}% • $birdsNow birds now",
                icon = ClutchIcons.Deaths,
                status = m.sevenDayMortalityStatus,
                onClick = { onMetricClick(MetricExplanations.buildForSevenDayMortality(m)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_deaths"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Bad eggs",
                value = "${LayerCalc.format1(badEggPct)}%",
                caption = "cracked, dirty, soft",
                icon = ClutchIcons.Quality,
                status = if (m.eggs > 0) LayerCalc.evaluateEggQualityTarget(badEggPct) else null,
                onClick = { onMetricClick(MetricExplanations.buildForBadEggs(m, rec.cracked, rec.dirty, rec.softShell)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_bad_eggs"
            )
            StatTile(
                label = "Uniformity",
                value = weighIn?.uniformityPct?.let { "${LayerCalc.format0(it)}%" } ?: "—",
                caption = weighIn?.bodyWeightG?.let { "body weight ${LayerCalc.format2(it / 1000.0)} kg" } ?: "no weigh-in yet",
                icon = ClutchIcons.Weight,
                status = LayerCalc.evaluateUniformityTarget(weighIn?.uniformityPct),
                onClick = { onMetricClick(MetricExplanations.buildForUniformity(weighIn?.uniformityPct, weighIn?.date)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_uniformity"
            )
        }
    }
}

@Composable
private fun TrendCharts(
    dashboard: ActiveFlockDashboard,
    trendWindow: TrendWindow,
    onSelectWindow: (TrendWindow) -> Unit
) {
    val allRecords = dashboard.allRecordsAsc
    val latestEpoch = LayerCalc.parseEpochDay(allRecords.last().entity.date)
    val records = remember(allRecords, trendWindow, latestEpoch) {
        val daysLimit = trendWindow.days
        if (daysLimit == null) {
            allRecords
        } else {
            val minEpoch = latestEpoch - (daysLimit - 1)
            allRecords.filter { LayerCalc.parseEpochDay(it.entity.date) >= minEpoch }
        }
    }
    val minWeek = records.minOfOrNull { it.metrics.ageWeeks } ?: 0
    val weeklyMortality = dashboard.weeklyMortality.filter { it.ageWeeks >= minWeek }

    fun label(r: RecordWithMetrics) = "${LayerCalc.friendlyDate(r.entity.date)} • wk ${r.metrics.ageWeeks}"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Trends", subtitle = "Tap a chart to see each day")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendWindow.entries.forEach { window ->
                val isSelected = window == trendWindow
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectWindow(window) },
                    label = { Text(window.label, maxLines = 1) },
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("filter_chip_${window.name.lowercase()}")
                )
            }
        }

        LineTrendChartCard(
            title = "Hen-day production",
            subtitle = "Laying %. Green band is the peak target (85–95%).",
            icon = ClutchIcons.Dashboard,
            points = records.map {
                ChartPoint(label(it), it.metrics.henDayPct, "${LayerCalc.format1(it.metrics.henDayPct)}%")
            },
            bandMin = 85.0,
            bandMax = 95.0,
            bandLabel = "Target 85–95%",
            yMinFloor = 0.0,
            yMaxCeiling = 100.0,
            testTag = "chart_henday"
        )

        LineTrendChartCard(
            title = "Feed intake",
            subtitle = "Grams per bird per day. Target band 110–125 g.",
            icon = ClutchIcons.Feed,
            points = records.map {
                ChartPoint(label(it), it.metrics.feedPerBirdG, "${LayerCalc.format0(it.metrics.feedPerBirdG)} g")
            },
            bandMin = 110.0,
            bandMax = 125.0,
            bandLabel = "Target 110–125 g",
            yMinFloor = 60.0,
            yMaxCeiling = 150.0,
            testTag = "chart_feed"
        )

        LineTrendChartCard(
            title = "Egg mass",
            subtitle = "Grams of egg per bird per day. Aim for 55 g or more.",
            icon = ClutchIcons.Egg,
            points = records.mapNotNull { r ->
                r.metrics.eggMassG?.let { ChartPoint(label(r), it, "${LayerCalc.format1(it)} g") }
            },
            bandMin = 55.0,
            bandMax = null,
            bandLabel = "Target 55 g+",
            yMinFloor = 30.0,
            yMaxCeiling = 65.0,
            testTag = "chart_egg_mass",
            emptyMessage = "Weigh your eggs once a week to see egg mass here."
        )

        WeeklyMortalityBarChartCard(
            points = weeklyMortality.ifEmpty { dashboard.weeklyMortality },
            testTag = "chart_weekly_mortality"
        )
        InfoNote(text = ReferenceTargets.DISCLAIMER_MORTALITY, icon = ClutchIcons.Info)

        LineTrendChartCard(
            title = "Egg quality",
            subtitle = "Bad eggs (cracked + dirty + soft) per 100. Aim for 4% or less.",
            icon = ClutchIcons.Quality,
            points = records.filter { it.entity.eggs > 0 }.map {
                val m = it.metrics
                val total = m.crackedPct + m.dirtyPct + m.softShellPct
                ChartPoint(
                    label = label(it),
                    value = total,
                    formatted = "${LayerCalc.format1(total)}%",
                    detail = "Cracked ${LayerCalc.format1(m.crackedPct)}% • Dirty ${LayerCalc.format1(m.dirtyPct)}% • Soft ${LayerCalc.format1(m.softShellPct)}%"
                )
            },
            bandMin = null,
            bandMax = 4.0,
            bandLabel = "Target 4% or less",
            yMinFloor = 0.0,
            yMaxCeiling = 8.0,
            testTag = "chart_egg_quality"
        )
    }
}
