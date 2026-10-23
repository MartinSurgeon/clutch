package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.domain.MetricExplanations
import com.example.ui.ActiveFlockDashboard
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EggYolkLogo
import com.example.ui.components.GroupedMetricsCard
import com.example.ui.components.MetricExplanationBottomSheet
import com.example.ui.components.StatusChip
import com.example.ui.components.WarningsSectionCard
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun TodayScreen(
    uiState: ClutchUiState,
    onAddFirstFlock: () -> Unit,
    onTryExampleData: () -> Unit,
    onRecordToday: () -> Unit,
    onEditToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExplanation by remember { mutableStateOf<LayerCalc.MetricExplanation?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val dashboard = uiState.activeDashboard
        when {
            uiState.flocks.isEmpty() || dashboard == null -> {
                FirstRunEmptyState(
                    onAddFirstFlock = onAddFirstFlock,
                    onTryExampleData = onTryExampleData
                )
            }
            dashboard.latestRecord == null -> {
                FlockNoRecordsEmptyState(
                    flockName = dashboard.flock.name,
                    breed = dashboard.flock.breed,
                    hensHoused = dashboard.flock.hensHoused,
                    ageWeeks = dashboard.ageWeeksToday,
                    onRecordToday = onRecordToday
                )
            }
            else -> {
                TodayDashboardContent(
                    dashboard = dashboard,
                    onMetricClick = { selectedExplanation = it },
                    onRecordToday = onRecordToday,
                    onEditToday = onEditToday
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
private fun FirstRunEmptyState(
    onAddFirstFlock: () -> Unit,
    onTryExampleData: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("first_run_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        EggYolkLogo(size = 80.dp)

        Text(
            text = "Welcome to Clutch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your offline daily layer ledger. Record eggs, feed, water, and bird counts in seconds — Clutch calculates performance and warns you early when something slips.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClutchPrimaryButton(
                    text = "Add your first flock",
                    icon = Icons.Default.Add,
                    onClick = onAddFirstFlock,
                    testTag = "add_first_flock_button"
                )
                ClutchSecondaryButton(
                    text = "Try with example data",
                    icon = Icons.Outlined.Science,
                    onClick = onTryExampleData,
                    testTag = "try_example_data_button"
                )
            }
        }
    }
}

@Composable
private fun FlockNoRecordsEmptyState(
    flockName: String,
    breed: String,
    hensHoused: Int,
    ageWeeks: Int,
    onRecordToday: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .testTag("no_records_empty_state"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "$flockName is ready",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$breed • $hensHoused hens housed • $ageWeeks weeks old",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Enter your first daily count of eggs, feed, water, and live hens to see Hen-day %, FCR, and flock health warnings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ClutchPrimaryButton(
                    text = "Record today",
                    onClick = onRecordToday,
                    testTag = "record_today_empty_button"
                )
            }
        }
    }
}

@Composable
private fun TodayDashboardContent(
    dashboard: ActiveFlockDashboard,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit,
    onRecordToday: () -> Unit,
    onEditToday: () -> Unit
) {
    val latest = dashboard.latestRecord ?: return
    val m = latest.metrics
    val rec = latest.entity
    val flock = dashboard.flock

    val productionMetrics = remember(m, flock.hensHoused, dashboard.totalEggsToDate) {
        listOf(
            MetricExplanations.buildForHenHoused(m, flock.hensHoused),
            MetricExplanations.buildForEggMass(m),
            MetricExplanations.buildForEggWeight(m),
            MetricExplanations.buildForEggsPerHenHoused(m, flock.hensHoused, dashboard.totalEggsToDate)
        )
    }

    val feedWaterMetrics = remember(m, rec.feedKg, rec.waterL) {
        listOf(
            MetricExplanations.buildForFeedPerBird(m, rec.feedKg),
            MetricExplanations.buildForFcr(m, rec.feedKg),
            MetricExplanations.buildForWaterPerBird(m, rec.waterL)
        )
    }

    val healthQualityMetrics = remember(m, flock.hensHoused, rec.cracked, rec.dirty, rec.softShell) {
        listOf(
            MetricExplanations.buildForSevenDayMortality(m),
            MetricExplanations.buildForCumulativeMortality(m, flock.hensHoused),
            MetricExplanations.buildForCracked(m, rec.cracked),
            MetricExplanations.buildForDirty(m, rec.dirty),
            MetricExplanations.buildForSoftShell(m, rec.softShell)
        )
    }

    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hen-day Hero Card
        HenDayHeroCard(
            flockName = flock.name,
            breed = flock.breed,
            metrics = m,
            onClick = { onMetricClick(MetricExplanations.buildForHenDay(m)) }
        )

        // 2. Warnings Card
        WarningsSectionCard(warnings = dashboard.warnings)

        // 3. Three grouped number cards (at most 5 numbers each)
        GroupedMetricsCard(
            heading = "Production",
            metrics = productionMetrics,
            onMetricClick = onMetricClick
        )

        GroupedMetricsCard(
            heading = "Feed and water",
            metrics = feedWaterMetrics,
            onMetricClick = onMetricClick
        )

        GroupedMetricsCard(
            heading = "Health and egg quality",
            metrics = healthQualityMetrics,
            onMetricClick = onMetricClick
        )

        // 4. Last 7 days card
        Last7DaysCard(summary = dashboard.last7Days)

        // 5. Primary button "Record today" at the end (or "Today is recorded ✓" with "Edit today")
        TodayActionFooter(
            isTodayRecorded = dashboard.isTodayRecorded,
            onRecordToday = onRecordToday,
            onEditToday = onEditToday
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HenDayHeroCard(
    flockName: String,
    breed: String,
    metrics: LayerCalc.DayMetrics,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("henday_hero_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$flockName • ${metrics.ageWeeks} wks • $breed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Hen-day % (${metrics.date})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Eggs laid per 100 live hens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(state = metrics.henDayStatus)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "${LayerCalc.format1(metrics.henDayPct)}%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("hero_henday_value")
                )

                val delta = metrics.henDayChangeVsYesterday
                if (delta != null) {
                    val (deltaIcon, signText) = when {
                        delta > 0.05 -> Icons.AutoMirrored.Filled.TrendingUp to "+${LayerCalc.format1(delta)} pts vs yesterday"
                        delta < -0.05 -> Icons.AutoMirrored.Filled.TrendingDown to "${LayerCalc.format1(delta)} pts vs yesterday"
                        else -> Icons.AutoMirrored.Filled.TrendingFlat to "0.0 pts vs yesterday"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = deltaIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = signText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metrics.henDayWorking,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Tap to view formula and target",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Last7DaysCard(
    summary: LayerCalc.Last7DaysSummary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_7_days_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            SummaryLine(
                label = "Eggs collected",
                subtitle = LayerCalc.formatCratesHelper(summary.totalEggs),
                value = "${summary.totalEggs} eggs"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryLine(
                label = "Average hen-day",
                subtitle = "Across ${summary.daysCount} recorded ${if (summary.daysCount == 1) "day" else "days"}",
                value = "${LayerCalc.format1(summary.avgHenDayPct)}%"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryLine(
                label = "Feed used",
                subtitle = "Total feed given over 7 days",
                value = "${LayerCalc.format1(summary.totalFeedKg)} kg"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryLine(
                label = "Deaths",
                subtitle = "Total birds lost over 7 days",
                value = "${summary.totalDeaths}"
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TodayActionFooter(
    isTodayRecorded: Boolean,
    onRecordToday: () -> Unit,
    onEditToday: () -> Unit
) {
    if (isTodayRecorded) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("today_recorded_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Today is recorded ✓",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                ClutchSecondaryButton(
                    text = "Edit today",
                    icon = Icons.Default.Edit,
                    onClick = onEditToday,
                    testTag = "edit_today_button"
                )
            }
        }
    } else {
        ClutchPrimaryButton(
            text = "Record today",
            onClick = onRecordToday,
            testTag = "record_today_button"
        )
    }
}
