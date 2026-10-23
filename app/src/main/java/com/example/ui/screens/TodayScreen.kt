package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.domain.MetricExplanations
import com.example.ui.ActiveFlockDashboard
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EggYolkLogo
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GroupedMetricsCard
import com.example.ui.components.IconBadge
import com.example.ui.components.MetricExplanationBottomSheet
import com.example.ui.components.ScreenColumn
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.StatusChip
import com.example.ui.components.SummaryRow
import com.example.ui.components.WarningsSectionCard
import java.util.Calendar

@Composable
fun TodayScreen(
    uiState: ClutchUiState,
    onAddFirstFlock: () -> Unit,
    onTryExampleData: () -> Unit,
    onRecordToday: () -> Unit,
    onOpenReports: () -> Unit,
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
                    icon = Icons.Default.EditCalendar,
                    title = "${dashboard.flock.name} is ready",
                    body = "${dashboard.flock.hensHoused} ${dashboard.flock.breed} birds, ${dashboard.ageWeeksToday} weeks old. " +
                        "Record your first day of eggs, feed and water to see how the birds are doing.",
                    primaryLabel = "Record today's count",
                    onPrimary = onRecordToday,
                    primaryTestTag = "record_today_empty_button",
                    modifier = Modifier.testTag("no_records_empty_state")
                )
                OfflineReassurance()
            }
            else -> {
                TodayDashboardContent(
                    dashboard = dashboard,
                    todayDate = uiState.todayDate,
                    onMetricClick = { selectedExplanation = it },
                    onRecordToday = onRecordToday,
                    onOpenReports = onOpenReports
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = LayerCalc.friendlyDate(todayDate),
            style = MaterialTheme.typography.bodyLarge,
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Keep your layer farm records in one place, and know early when something is wrong.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ClutchCard {
            BenefitRow(
                icon = Icons.Outlined.Timer,
                title = "Record a day in about a minute",
                body = "Eggs in crates, feed, water and dead birds."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BenefitRow(
                icon = Icons.Outlined.NotificationsActive,
                title = "Get early warnings",
                body = "Know when laying, feed or water drops — before you lose money."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BenefitRow(
                icon = Icons.Outlined.CloudOff,
                title = "Works without data",
                body = "Everything is saved on your phone. No internet needed."
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ClutchPrimaryButton(
                text = "Add my first pen",
                icon = Icons.Default.Add,
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
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
private fun TodayDashboardContent(
    dashboard: ActiveFlockDashboard,
    todayDate: String,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit,
    onRecordToday: () -> Unit,
    onOpenReports: () -> Unit
) {
    val latest = dashboard.latestRecord ?: return
    val m = latest.metrics
    val rec = latest.entity
    val flock = dashboard.flock
    val birdsNow = LayerCalc.closingHens(rec.liveHens, rec.deaths, rec.culls)

    // 1. Greeting + today's one job (serial position: most important first)
    Greeting(todayDate = todayDate)
    TodayTaskCard(
        isTodayRecorded = dashboard.isTodayRecorded,
        onRecordToday = onRecordToday
    )

    // 2. The one number that matters most
    LayingHeroCard(
        metrics = m,
        isLatestToday = rec.date == todayDate,
        onClick = { onMetricClick(MetricExplanations.buildForHenDay(m)) }
    )

    // 3. Quick stats — four tiles, two per row
    QuickStatsGrid(
        eggs = rec.eggs,
        birdsNow = birdsNow,
        hensHoused = flock.hensHoused,
        deathsToday = rec.deaths,
        metrics = m,
        onFeedClick = { onMetricClick(MetricExplanations.buildForFeedPerBird(m, rec.feedKg)) },
        onWaterClick = { onMetricClick(MetricExplanations.buildForWaterPerBird(m, rec.waterL)) }
    )

    // 4. Things to check
    WarningsSectionCard(warnings = dashboard.warnings)

    // 5. This week
    ThisWeekCard(summary = dashboard.last7Days, onOpenReports = onOpenReports)

    // 6. All the detail, folded away until wanted (Tesler's Law)
    MoreNumbersSection(
        dashboard = dashboard,
        onMetricClick = onMetricClick
    )

    // 7. Strong, calm ending (peak-end rule)
    if (!dashboard.isTodayRecorded) {
        ClutchPrimaryButton(
            text = "Record today's count",
            icon = Icons.Default.EditCalendar,
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's count is saved",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Well done. See you tomorrow!",
                        style = MaterialTheme.typography.bodyMedium,
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
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ) {
            Column {
                Text(
                    text = "Today's count is not in yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "It takes about a minute. Do it after the last egg pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            ClutchPrimaryButton(
                text = "Record today's count",
                icon = Icons.Default.EditCalendar,
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
    ClutchCard(
        modifier = Modifier.testTag("henday_hero_card"),
        onClick = onClick,
        contentPadding = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Laying %",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isLatestToday) "Today" else "Last count: ${LayerCalc.friendlyDate(metrics.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(state = metrics.henDayStatus)
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${LayerCalc.format1(metrics.henDayPct)}%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("hero_henday_value")
            )
            metrics.henDayChangeVsYesterday?.let { delta ->
                val (deltaIcon, deltaText) = when {
                    delta > 0.05 -> Icons.AutoMirrored.Filled.TrendingUp to "Up ${LayerCalc.format1(delta)}"
                    delta < -0.05 -> Icons.AutoMirrored.Filled.TrendingDown to "Down ${LayerCalc.format1(-delta)}"
                    else -> Icons.AutoMirrored.Filled.TrendingFlat to "Same as"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = deltaIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$deltaText yesterday",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "${metrics.eggs} eggs from ${metrics.liveHens} birds. Tap to see what this means.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickStatsGrid(
    eggs: Int,
    birdsNow: Int,
    hensHoused: Int,
    deathsToday: Int,
    metrics: LayerCalc.DayMetrics,
    onFeedClick: () -> Unit,
    onWaterClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Eggs",
                value = "${eggs / 30} crates",
                caption = if (eggs % 30 == 0) "$eggs eggs" else "and ${eggs % 30} eggs ($eggs total)",
                icon = Icons.Outlined.Egg,
                modifier = Modifier.weight(1f),
                testTag = "stat_eggs"
            )
            StatTile(
                label = "Birds",
                value = "$birdsNow",
                caption = if (deathsToday > 0) "$deathsToday died today" else "of $hensHoused stocked",
                icon = Icons.Outlined.Pets,
                modifier = Modifier.weight(1f),
                testTag = "stat_birds"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Feed",
                value = "${LayerCalc.format0(metrics.feedPerBirdG)} g",
                caption = "per bird",
                icon = Icons.Outlined.Grass,
                status = metrics.feedStatus,
                onClick = onFeedClick,
                modifier = Modifier.weight(1f),
                testTag = "stat_feed"
            )
            StatTile(
                label = "Water",
                value = "${LayerCalc.format0(metrics.waterPerBirdMl)} mL",
                caption = "per bird",
                icon = Icons.Outlined.WaterDrop,
                status = metrics.waterStatus,
                onClick = onWaterClick,
                modifier = Modifier.weight(1f),
                testTag = "stat_water"
            )
        }
    }
}

@Composable
private fun ThisWeekCard(
    summary: LayerCalc.Last7DaysSummary,
    onOpenReports: () -> Unit
) {
    ClutchCard(modifier = Modifier.testTag("last_7_days_card")) {
        SectionHeader(
            title = "Last 7 days",
            actionLabel = "See reports",
            onAction = onOpenReports
        )
        SummaryRow(
            label = "Eggs picked",
            caption = LayerCalc.formatCratesHelper(summary.totalEggs),
            value = "${summary.totalEggs}"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(
            label = "Average laying",
            caption = "Over ${summary.daysCount} recorded ${if (summary.daysCount == 1) "day" else "days"}",
            value = "${LayerCalc.format1(summary.avgHenDayPct)}%"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(
            label = "Feed used",
            caption = "About ${LayerCalc.format1(summary.totalFeedKg / 50.0)} bags of 50 kg",
            value = "${LayerCalc.format1(summary.totalFeedKg)} kg"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(
            label = "Birds that died",
            value = "${summary.totalDeaths}"
        )
    }
}

@Composable
private fun MoreNumbersSection(
    dashboard: ActiveFlockDashboard,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val latest = dashboard.latestRecord ?: return
    val m = latest.metrics
    val rec = latest.entity
    val flock = dashboard.flock

    val eggMetrics = remember(m, flock.hensHoused, dashboard.totalEggsToDate) {
        listOf(
            MetricExplanations.buildForHenHoused(m, flock.hensHoused),
            MetricExplanations.buildForEggWeight(m),
            MetricExplanations.buildForEggMass(m),
            MetricExplanations.buildForEggsPerHenHoused(m, flock.hensHoused, dashboard.totalEggsToDate)
        )
    }
    val feedMetrics = remember(m, rec.feedKg, rec.waterL) {
        listOf(
            MetricExplanations.buildForFeedPerBird(m, rec.feedKg),
            MetricExplanations.buildForFcr(m, rec.feedKg),
            MetricExplanations.buildForWaterPerBird(m, rec.waterL)
        )
    }
    val healthMetrics = remember(m, flock.hensHoused, rec.cracked, rec.dirty, rec.softShell) {
        listOf(
            MetricExplanations.buildForSevenDayMortality(m),
            MetricExplanations.buildForCumulativeMortality(m, flock.hensHoused),
            MetricExplanations.buildForCracked(m, rec.cracked),
            MetricExplanations.buildForDirty(m, rec.dirty),
            MetricExplanations.buildForSoftShell(m, rec.softShell)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { expanded = !expanded }
                .heightIn(min = 56.dp)
                .testTag("more_numbers_toggle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All numbers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Egg size, FCR, deaths, cracked and dirty eggs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide all numbers" else "Show all numbers",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GroupedMetricsCard(heading = "Eggs", metrics = eggMetrics, onMetricClick = onMetricClick)
                GroupedMetricsCard(heading = "Feed and water", metrics = feedMetrics, onMetricClick = onMetricClick)
                GroupedMetricsCard(heading = "Health and egg quality", metrics = healthMetrics, onMetricClick = onMetricClick)
            }
        }
    }
}

@Composable
private fun OfflineReassurance() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "  Saved on this phone. Works without data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
