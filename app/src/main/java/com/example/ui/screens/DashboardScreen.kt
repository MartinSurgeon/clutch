package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.domain.TrendInsights
import com.example.ui.ActiveFlockDashboard
import com.example.ui.ClutchUiState
import com.example.ui.RecordWithMetrics
import com.example.ui.TrendWindow
import com.example.ui.components.BrandHeroCard
import com.example.ui.components.ChartDatum
import com.example.ui.components.ChartKind
import com.example.ui.components.ChartMetricSelector
import com.example.ui.components.ChartSpec
import com.example.ui.components.ClutchDimens
import com.example.ui.components.PerformanceChartCard
import com.example.ui.components.PeriodSwitch
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EggYolkLogo
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IconBadge
import com.example.ui.components.MetricExplanationBottomSheet
import com.example.ui.components.OfflineReassurance
import com.example.ui.components.ScreenColumn
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.StatusChip
import com.example.ui.components.WarningsSectionCard
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

    ScreenColumn(modifier = modifier, testTag = "today_screen", maxContentWidth = ClutchDimens.WideContentMaxWidth) {
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val twoPane = maxWidth >= ClutchDimens.TwoPaneMinWidth

        val hero: @Composable () -> Unit = {
            LayingHeroCard(
                metrics = m,
                isLatestToday = rec.date == todayDate,
                onClick = { onMetricClick(MetricExplanations.buildForHenDay(m)) }
            )
        }
        val trends: @Composable () -> Unit = {
            TrendsSection(dashboard = dashboard, trendWindow = trendWindow, onSelectWindow = onSelectWindow)
        }
        val task: @Composable () -> Unit = {
            TodayTaskCard(isTodayRecorded = dashboard.isTodayRecorded, onRecordToday = onRecordToday)
        }
        val indicators: @Composable () -> Unit = {
            LatestIndicators(dashboard = dashboard, latest = latest, onMetricClick = onMetricClick)
        }
        val warnings: @Composable () -> Unit = {
            WarningsSectionCard(warnings = dashboard.warnings)
        }

        if (twoPane) {
            // Tablet / landscape: what is happening (left) beside what to do about it (right)
            Column(verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)) {
                Greeting(todayDate = todayDate)
                Row(horizontalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)
                    ) {
                        hero()
                        trends()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)
                    ) {
                        task()
                        indicators()
                        warnings()
                        OfflineReassurance()
                    }
                }
            }
        } else {
            // Phone: one column, most important first and a clear action last (serial position)
            Column(verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)) {
                Greeting(todayDate = todayDate)
                task()
                hero()
                trends()
                indicators()
                warnings()
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
        }
    }
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
    val badEggPct = m.crackedPct + m.dirtyPct + m.softShellPct
    val weighIn = dashboard.latestWeighIn

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Latest performance", subtitle = "Tap a number to see what it means and what to do")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Feed intake",
                value = "${LayerCalc.format0(m.feedPerBirdG)} g",
                caption = "per bird",
                icon = ClutchIcons.Feed,
                status = m.feedStatus,
                onClick = { onMetricClick(MetricExplanations.buildForFeedPerBird(m, rec.feedKg)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_feed"
            )
            StatTile(
                label = "Egg mass",
                value = m.eggMassG?.let { "${LayerCalc.format1(it)} g" } ?: "—",
                caption = "Weigh eggs to see this",
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
                label = "Deaths (7 days)",
                value = "${m.deathsIn7Days}",
                caption = "${LayerCalc.format2(m.sevenDayMortalityPct)}% of flock",
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
                caption = "No weigh-in yet",
                icon = ClutchIcons.Weight,
                status = LayerCalc.evaluateUniformityTarget(weighIn?.uniformityPct),
                onClick = { onMetricClick(MetricExplanations.buildForUniformity(weighIn?.uniformityPct, weighIn?.date)) },
                modifier = Modifier.weight(1f),
                testTag = "stat_uniformity"
            )
        }
    }
}

// --- Trends: one chart at a time, chosen with chips (Hick's Law) ---

private const val METRIC_LAYING = "laying"
private const val METRIC_FEED = "feed"
private const val METRIC_EGG_MASS = "egg_mass"
private const val METRIC_DEATHS = "deaths"
private const val METRIC_QUALITY = "quality"

private val METRIC_OPTIONS = listOf(
    METRIC_LAYING to "Hen-day",
    METRIC_FEED to "Feed intake",
    METRIC_EGG_MASS to "Egg mass",
    METRIC_DEATHS to "Deaths",
    METRIC_QUALITY to "Egg quality"
)

/** Daily points up to this many; beyond it the chart shows weekly averages so bars stay readable. */
private const val MAX_DAILY_POINTS = 45

/** One week of records rolled up for charting. */
private data class WeekBucket(
    val ageWeek: Int,
    val days: List<RecordWithMetrics>
) {
    val eggs = days.sumOf { it.entity.eggs }
    val birdDays = days.sumOf { it.entity.liveHens }
    val henDay = if (birdDays > 0) eggs * 100.0 / birdDays else 0.0
    val feedPerBird = if (birdDays > 0) days.sumOf { it.entity.feedKg } * 1000.0 / birdDays else 0.0
    val eggMass: Double? = days.mapNotNull { it.metrics.eggMassG }.takeIf { it.isNotEmpty() }?.average()
    fun pctOf(count: (RecordWithMetrics) -> Int) = if (eggs > 0) days.sumOf(count) * 100.0 / eggs else 0.0
    val label = "Wk $ageWeek"
    val fullLabel = "Week $ageWeek of age • ${days.size} ${if (days.size == 1) "day" else "days"}"
}

private fun shortDay(iso: String) = LayerCalc.friendlyDate(iso).substringAfter(" ")

@Composable
private fun TrendsSection(
    dashboard: ActiveFlockDashboard,
    trendWindow: TrendWindow,
    onSelectWindow: (TrendWindow) -> Unit
) {
    var metric by rememberSaveable { mutableStateOf(METRIC_LAYING) }

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
    val weekly = records.size > MAX_DAILY_POINTS
    val weeks = remember(records) {
        records.groupBy { it.metrics.ageWeeks }.toSortedMap().map { (w, days) -> WeekBucket(w, days) }
    }

    val specs = remember(records, weekly, dashboard.weeklyMortality) {
        buildChartSpecs(records, weeks, weekly, dashboard)
    }
    val attention = specs.filterValues { spec ->
        spec.data.lastOrNull()?.let {
            TrendInsights.isInBand(it.value, spec.bandMin, spec.bandMax) == false
        } == true
    }.keys

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Trends", subtitle = "Pick what to look at. A dot means it needs attention.")
        ChartMetricSelector(
            options = METRIC_OPTIONS,
            selectedId = metric,
            onSelect = { metric = it },
            attention = attention
        )
        PeriodSwitch(
            options = TrendWindow.entries,
            selected = trendWindow,
            label = { it.label },
            onSelect = onSelectWindow
        )
        specs[metric]?.let { PerformanceChartCard(spec = it) }
    }
}

private fun buildChartSpecs(
    records: List<RecordWithMetrics>,
    weeks: List<WeekBucket>,
    weekly: Boolean,
    dashboard: ActiveFlockDashboard
): Map<String, ChartSpec> {
    val noun = if (weekly) "week" else "day"
    val minWeek = records.minOfOrNull { it.metrics.ageWeeks } ?: 0

    fun daily(value: (RecordWithMetrics) -> Double?, detail: (RecordWithMetrics) -> String? = { null }) =
        records.mapNotNull { r ->
            value(r)?.let {
                ChartDatum(
                    shortLabel = shortDay(r.entity.date),
                    fullLabel = "${LayerCalc.friendlyDate(r.entity.date)} • week ${r.metrics.ageWeeks}",
                    value = it,
                    detail = detail(r)
                )
            }
        }

    fun weeklyData(value: (WeekBucket) -> Double?) =
        weeks.mapNotNull { w -> value(w)?.let { ChartDatum(w.label, w.fullLabel, it) } }

    val laying = ChartSpec(
        id = METRIC_LAYING,
        title = "Hen-day production",
        meaning = "Eggs for every 100 live hens",
        unit = "%",
        decimals = 1,
        data = if (weekly) weeklyData { it.henDay } else daily({ it.metrics.henDayPct }) { "${it.entity.eggs} eggs from ${it.entity.liveHens} hens" },
        bandMin = 85.0,
        bandMax = 95.0,
        targetText = "Target 85–95% at peak (25–35 weeks)",
        goodness = TrendInsights.Goodness.HIGHER_IS_BETTER,
        yFloor = 50.0,
        yCeil = 100.0,
        kinds = listOf(ChartKind.LINE, ChartKind.BARS),
        pointNoun = noun
    )

    val feed = ChartSpec(
        id = METRIC_FEED,
        title = "Feed intake",
        meaning = "Grams of feed per bird per day",
        unit = " g",
        decimals = 0,
        data = if (weekly) weeklyData { it.feedPerBird } else daily({ it.metrics.feedPerBirdG }) { "${it.entity.feedKg} kg for ${it.entity.liveHens} hens" },
        bandMin = 110.0,
        bandMax = 125.0,
        targetText = "Target 110–125 g",
        goodness = TrendInsights.Goodness.IN_BAND_IS_BEST,
        yFloor = 90.0,
        yCeil = 135.0,
        kinds = listOf(ChartKind.BARS, ChartKind.LINE),
        pointNoun = noun
    )

    val eggMass = ChartSpec(
        id = METRIC_EGG_MASS,
        title = "Egg mass",
        meaning = "Grams of egg each bird gives per day",
        unit = " g",
        decimals = 1,
        data = if (weekly) weeklyData { it.eggMass } else daily({ it.metrics.eggMassG }) { r ->
            r.metrics.effectiveEggWeightG?.let { "Egg weight ${LayerCalc.format1(it)} g × ${LayerCalc.format1(r.metrics.henDayPct)}% laying" }
        },
        bandMin = 55.0,
        bandMax = null,
        targetText = "Target 55 g or more from 25 weeks",
        goodness = TrendInsights.Goodness.HIGHER_IS_BETTER,
        yFloor = 40.0,
        yCeil = 62.0,
        kinds = listOf(ChartKind.LINE, ChartKind.BARS),
        pointNoun = noun,
        emptyMessage = "Weigh 30 eggs once a week (Daily → record) to see egg mass here."
    )

    // Deaths are always weekly: one day on its own says little (client caveat)
    val weeklyDeaths = dashboard.weeklyMortality.filter { it.ageWeeks >= minWeek }
    val deaths = ChartSpec(
        id = METRIC_DEATHS,
        title = "Deaths by week",
        meaning = "Birds that died each week, per 100 birds",
        unit = "%",
        decimals = 2,
        data = weeklyDeaths.map {
            ChartDatum(
                shortLabel = "Wk ${it.ageWeeks}",
                fullLabel = "Week ${it.ageWeeks} of age",
                value = it.mortalityPct,
                detail = "${it.deaths} ${if (it.deaths == 1) "bird" else "birds"} died out of ${it.startLiveHens}"
            )
        },
        bandMin = null,
        bandMax = 0.1,
        targetText = "Target under 0.1% a week",
        goodness = TrendInsights.Goodness.LOWER_IS_BETTER,
        yFloor = 0.0,
        yCeil = 0.3,
        kinds = listOf(ChartKind.BARS, ChartKind.LINE),
        pointNoun = "week",
        footnote = ReferenceTargets.DISCLAIMER_MORTALITY
    )

    val qualityData = if (weekly) {
        weeks.filter { it.eggs > 0 }.map { w ->
            val c = w.pctOf { it.entity.cracked }
            val d = w.pctOf { it.entity.dirty }
            val s = w.pctOf { it.entity.softShell }
            ChartDatum(w.label, w.fullLabel, c + d + s, listOf(c, d, s),
                "Cracked ${LayerCalc.format1(c)}% • Dirty ${LayerCalc.format1(d)}% • Soft ${LayerCalc.format1(s)}%")
        }
    } else {
        records.filter { it.entity.eggs > 0 }.map { r ->
            val mm = r.metrics
            ChartDatum(
                shortLabel = shortDay(r.entity.date),
                fullLabel = "${LayerCalc.friendlyDate(r.entity.date)} • week ${mm.ageWeeks}",
                value = mm.crackedPct + mm.dirtyPct + mm.softShellPct,
                segments = listOf(mm.crackedPct, mm.dirtyPct, mm.softShellPct),
                detail = "Cracked ${LayerCalc.format1(mm.crackedPct)}% • Dirty ${LayerCalc.format1(mm.dirtyPct)}% • Soft ${LayerCalc.format1(mm.softShellPct)}%"
            )
        }
    }
    val quality = ChartSpec(
        id = METRIC_QUALITY,
        title = "Egg quality",
        meaning = "Bad eggs for every 100 picked",
        unit = "%",
        decimals = 1,
        data = qualityData,
        bandMin = null,
        bandMax = 4.0,
        targetText = "Target 4% or less in total",
        goodness = TrendInsights.Goodness.LOWER_IS_BETTER,
        yFloor = 0.0,
        yCeil = 5.0,
        kinds = listOf(ChartKind.STACKED, ChartKind.LINE),
        pointNoun = noun,
        segmentNames = listOf("Cracked", "Dirty", "Soft-shell")
    )

    return listOf(laying, feed, eggMass, deaths, quality).associateBy { it.id }
}
