package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.RecordWithMetrics
import com.example.ui.TrendWindow
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.StatusChip
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusAmberBorderDark
import com.example.ui.theme.StatusAmberBorderLight
import com.example.ui.theme.StatusGreenBgDark
import com.example.ui.theme.StatusGreenBgLight
import com.example.ui.theme.StatusGreenBorderDark
import com.example.ui.theme.StatusGreenBorderLight
import com.example.ui.theme.StatusRedBorderDark
import com.example.ui.theme.StatusRedBorderLight
import kotlin.math.abs
import kotlin.math.max

@Composable
fun TrendsScreen(
    uiState: ClutchUiState,
    onSelectWindow: (TrendWindow) -> Unit,
    onTapDayToEdit: (String) -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    if (dashboard == null || dashboard.allRecordsAsc.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("trends_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No trend data yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Record daily numbers to see Hen-day %, feed, water, and weekly mortality charts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ClutchPrimaryButton(
                        text = "Record today",
                        onClick = onNavigateToRecord,
                        testTag = "trends_record_today_button"
                    )
                }
            }
        }
        return
    }

    val allRecords = dashboard.allRecordsAsc
    val latestEpoch = LayerCalc.parseEpochDay(allRecords.last().entity.date)
    val filteredRecords = remember(allRecords, uiState.trendWindow, latestEpoch) {
        val daysLimit = uiState.trendWindow.days
        if (daysLimit == null) {
            allRecords
        } else {
            val minEpoch = latestEpoch - (daysLimit - 1)
            allRecords.filter { LayerCalc.parseEpochDay(it.entity.date) >= minEpoch }
        }
    }

    val groupedByWeekDesc = remember(filteredRecords) {
        filteredRecords
            .sortedByDescending { LayerCalc.parseEpochDay(it.entity.date) }
            .groupBy { it.metrics.ageWeeks }
            .entries
            .sortedByDescending { it.key }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("trends_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                WindowFilterRow(
                    selected = uiState.trendWindow,
                    onSelect = onSelectWindow
                )
            }

            // 1. Hen-day % Chart with shaded 85-95% band
            item {
                LineTrendChartCard(
                    title = "Hen-day %",
                    subtitle = "Eggs laid per 100 live hens (target band 85–95%)",
                    points = filteredRecords.map {
                        ChartPoint(
                            label = "${it.entity.date} (${it.metrics.ageWeeks}w)",
                            value = it.metrics.henDayPct,
                            formatted = "${LayerCalc.format1(it.metrics.henDayPct)}%"
                        )
                    },
                    bandMin = 85.0,
                    bandMax = 95.0,
                    bandLabel = "85–95% target band",
                    yMinFloor = 0.0,
                    yMaxCeiling = 100.0,
                    testTag = "chart_henday"
                )
            }

            // 2. Feed per bird Chart with shaded 110-125 g band
            item {
                LineTrendChartCard(
                    title = "Feed per bird (g)",
                    subtitle = "Grams of feed per live hen (target band 110–125 g)",
                    points = filteredRecords.map {
                        ChartPoint(
                            label = "${it.entity.date} (${it.metrics.ageWeeks}w)",
                            value = it.metrics.feedPerBirdG,
                            formatted = "${LayerCalc.format0(it.metrics.feedPerBirdG)} g"
                        )
                    },
                    bandMin = 110.0,
                    bandMax = 125.0,
                    bandLabel = "110–125 g target band",
                    yMinFloor = 60.0,
                    yMaxCeiling = 150.0,
                    testTag = "chart_feed"
                )
            }

            // 3. Water per bird Chart
            item {
                LineTrendChartCard(
                    title = "Water per bird (mL)",
                    subtitle = "Millilitres of water drunk per live hen",
                    points = filteredRecords.map {
                        ChartPoint(
                            label = "${it.entity.date} (${it.metrics.ageWeeks}w)",
                            value = it.metrics.waterPerBirdMl,
                            formatted = "${LayerCalc.format0(it.metrics.waterPerBirdMl)} mL"
                        )
                    },
                    bandMin = null,
                    bandMax = null,
                    bandLabel = null,
                    yMinFloor = 100.0,
                    yMaxCeiling = 280.0,
                    testTag = "chart_water"
                )
            }

            // 4. Weekly mortality bars with 0.1% line
            item {
                WeeklyMortalityBarChartCard(
                    points = dashboard.weeklyMortality,
                    testTag = "chart_weekly_mortality"
                )
            }

            item {
                Text(
                    text = "Daily ledger by week of age",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Tap any day to open and edit its record",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            groupedByWeekDesc.forEach { (ageWeeks, weekRecords) ->
                item(key = "week_$ageWeeks") {
                    WeekDailyGroupCard(
                        ageWeeks = ageWeeks,
                        records = weekRecords,
                        onTapDayToEdit = onTapDayToEdit
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WindowFilterRow(
    selected: TrendWindow,
    onSelect: (TrendWindow) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrendWindow.entries.forEach { window ->
            val isSelected = window == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(window) },
                label = {
                    Text(
                        text = window.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                modifier = Modifier.testTag("filter_chip_${window.name.lowercase()}")
            )
        }
    }
}

private data class ChartPoint(
    val label: String,
    val value: Double,
    val formatted: String
)

@Composable
private fun LineTrendChartCard(
    title: String,
    subtitle: String,
    points: List<ChartPoint>,
    bandMin: Double?,
    bandMax: Double?,
    bandLabel: String?,
    yMinFloor: Double,
    yMaxCeiling: Double,
    testTag: String
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]

    val dark = isSystemInDarkTheme()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bandFill = if (dark) StatusGreenBgDark.copy(alpha = 0.65f) else StatusGreenBgLight
    val bandStroke = if (dark) StatusGreenBorderDark.copy(alpha = 0.7f) else StatusGreenBorderLight.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selected point callout (tap-to-see-value)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = "Tap chart to inspect day",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedPoint.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = selectedPoint.formatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val minVal = minOf(yMinFloor, points.minOf { it.value } * 0.9)
            val maxVal = maxOf(yMaxCeiling, points.maxOf { it.value } * 1.05)
            val span = (maxVal - minVal).coerceAtLeast(1.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val count = points.size
                            if (count == 1) {
                                selectedIndex = 0
                            } else {
                                val fraction = (tapOffset.x / w).coerceIn(0f, 1f)
                                selectedIndex = (fraction * (count - 1) + 0.5f).toInt().coerceIn(0, count - 1)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

                fun valueToY(v: Double): Float {
                    val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                    return padTop + plotH * (1f - norm)
                }

                // Horizontal grid rules
                listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                    val y = padTop + plotH * frac
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Target shaded band if present
                if (bandMin != null && bandMax != null) {
                    val yTop = valueToY(bandMax)
                    val yBottom = valueToY(bandMin)
                    val bandHeight = (yBottom - yTop).coerceAtLeast(2f)
                    drawRect(
                        color = bandFill,
                        topLeft = Offset(0f, yTop),
                        size = Size(w, bandHeight)
                    )
                    drawLine(
                        color = bandStroke,
                        start = Offset(0f, yTop),
                        end = Offset(w, yTop),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                    drawLine(
                        color = bandStroke,
                        start = Offset(0f, yBottom),
                        end = Offset(w, yBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }

                // Data path
                val path = Path()
                points.forEachIndexed { idx, pt ->
                    val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
                    val y = valueToY(pt.value)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Selected point marker & vertical guide
                val selX = if (points.size == 1) w / 2f else (safeIndex.toFloat() / (points.size - 1)) * w
                val selY = valueToY(selectedPoint.value)

                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(selX, padTop),
                    end = Offset(selX, h - padBottom),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(
                    color = lineColor,
                    radius = 5.5.dp.toPx(),
                    center = Offset(selX, selY)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bandLabel != null) {
                    Text(
                        text = bandLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = points.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyMortalityBarChartCard(
    points: List<LayerCalc.WeeklyMortalityPoint>,
    testTag: String
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedBar = points[safeIndex]

    val dark = isSystemInDarkTheme()
    val goodColor = if (dark) StatusGreenBorderDark else StatusGreenBorderLight
    val watchColor = if (dark) StatusAmberBorderDark else StatusAmberBorderLight
    val highColor = if (dark) StatusRedBorderDark else StatusRedBorderLight
    val thresholdColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
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
                text = "Weekly mortality %",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Deaths per week of age with 0.1% target line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = "Tap bar to inspect week",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Week ${selectedBar.ageWeeks} (${selectedBar.deaths} ${if (selectedBar.deaths == 1) "death" else "deaths"})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${LayerCalc.format2(selectedBar.mortalityPct)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val maxPct = max(0.35, points.maxOf { it.mortalityPct } * 1.25)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val slotW = w / points.size
                            val idx = (tapOffset.x / slotW).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 12.dp.toPx()
                val padBottom = 10.dp.toPx()
                val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

                fun pctToY(pct: Double): Float {
                    val norm = (pct / maxPct).toFloat().coerceIn(0f, 1f)
                    return padTop + plotH * (1f - norm)
                }

                val slotW = w / points.size
                val barW = (slotW * 0.62f).coerceAtLeast(4.dp.toPx())

                points.forEachIndexed { idx, pt ->
                    val barX = idx * slotW + (slotW - barW) / 2f
                    val topY = pctToY(pt.mortalityPct)
                    val barH = (h - padBottom - topY).coerceAtLeast(3.dp.toPx())
                    val color = when {
                        pt.mortalityPct <= 0.1 -> goodColor
                        pt.mortalityPct <= 0.5 -> watchColor
                        else -> highColor
                    }
                    val alpha = if (idx == safeIndex) 1.0f else 0.7f
                    drawRoundRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(barX, h - padBottom - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // 0.1% target dashed line
                val lineY = pctToY(0.1)
                drawLine(
                    color = thresholdColor,
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wk ${points.first().ageWeeks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dashed line: 0.10% target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Wk ${points.last().ageWeeks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekDailyGroupCard(
    ageWeeks: Int,
    records: List<RecordWithMetrics>,
    onTapDayToEdit: (String) -> Unit
) {
    val weekAvgHenDay = remember(records) {
        records.map { it.metrics.henDayPct }.average()
    }
    val weekEggs = remember(records) {
        records.sumOf { it.entity.eggs }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("week_group_$ageWeeks"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Week $ageWeeks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${records.size} ${if (records.size == 1) "day" else "days"} • $weekEggs eggs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Avg ${LayerCalc.format1(weekAvgHenDay)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            records.forEachIndexed { idx, rec ->
                if (idx > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                DailyRecordLedgerRow(
                    record = rec,
                    onClick = { onTapDayToEdit(rec.entity.date) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyRecordLedgerRow(
    record: RecordWithMetrics,
    onClick: () -> Unit
) {
    val e = record.entity
    val m = record.metrics

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("trend_day_row_${e.date}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = e.date,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${e.eggs} eggs • ${LayerCalc.format0(m.feedPerBirdG)} g feed • ${LayerCalc.format0(m.waterPerBirdMl)} mL water",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.Center
        ) {
            StatusChip(state = m.henDayStatus)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${LayerCalc.format1(m.henDayPct)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Edit ${e.date}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
