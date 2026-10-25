package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.ui.theme.ChartLineDark
import com.example.ui.theme.ChartLineLight
import com.example.ui.theme.ClutchIcons
import com.example.ui.theme.StatusAmberBorderDark
import com.example.ui.theme.StatusAmberBorderLight
import com.example.ui.theme.StatusGreenBgDark
import com.example.ui.theme.StatusGreenBgLight
import com.example.ui.theme.StatusGreenBorderDark
import com.example.ui.theme.StatusGreenBorderLight
import com.example.ui.theme.StatusRedBorderDark
import com.example.ui.theme.StatusRedBorderLight
import kotlin.math.max

data class ChartPoint(
    val label: String,
    val value: Double,
    val formatted: String,
    /** Optional extra line shown when this point is selected, e.g. a breakdown. */
    val detail: String? = null
)

/**
 * One line chart card used for every daily trend. Tap anywhere to inspect a day.
 * The green band marks the good range; either edge may be open (e.g. "55 g or more").
 */
@Composable
fun LineTrendChartCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    points: List<ChartPoint>,
    bandMin: Double?,
    bandMax: Double?,
    bandLabel: String?,
    yMinFloor: Double,
    yMaxCeiling: Double,
    testTag: String,
    emptyMessage: String = "Not enough records yet."
) {
    val dark = isSystemInDarkTheme()
    val lineColor = if (dark) ChartLineDark else ChartLineLight
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bandFill = if (dark) StatusGreenBgDark.copy(alpha = 0.65f) else StatusGreenBgLight
    val bandStroke = if (dark) StatusGreenBorderDark.copy(alpha = 0.7f) else StatusGreenBorderLight.copy(alpha = 0.7f)

    ClutchCard(modifier = Modifier.testTag(testTag)) {
        ChartHeader(title = title, subtitle = subtitle, icon = icon)

        if (points.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ClutchCard
        }

        var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
        val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
        val selectedPoint = points[safeIndex]

        SelectedValueCallout(
            label = selectedPoint.label,
            value = selectedPoint.formatted,
            detail = selectedPoint.detail
        )

        val minVal = minOf(yMinFloor, points.minOf { it.value } * 0.9)
        val maxVal = maxOf(yMaxCeiling, points.maxOf { it.value } * 1.05)
        val span = (maxVal - minVal).coerceAtLeast(1.0)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics {
                    contentDescription = "$title chart. Latest ${points.last().formatted}. Tap to see each day."
                }
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val count = points.size
                        selectedIndex = if (count == 1) {
                            0
                        } else {
                            val fraction = (tapOffset.x / w).coerceIn(0f, 1f)
                            (fraction * (count - 1) + 0.5f).toInt().coerceIn(0, count - 1)
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

            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = padTop + plotH * frac
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            if (bandMin != null || bandMax != null) {
                val yTop = valueToY(bandMax ?: maxVal)
                val yBottom = valueToY(bandMin ?: minVal)
                drawRect(
                    color = bandFill,
                    topLeft = Offset(0f, yTop),
                    size = Size(w, (yBottom - yTop).coerceAtLeast(2f))
                )
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                if (bandMax != null) {
                    drawLine(bandStroke, Offset(0f, yTop), Offset(w, yTop), 1.dp.toPx(), pathEffect = dash)
                }
                if (bandMin != null) {
                    drawLine(bandStroke, Offset(0f, yBottom), Offset(w, yBottom), 1.dp.toPx(), pathEffect = dash)
                }
            }

            val path = Path()
            points.forEachIndexed { idx, pt ->
                val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
                val y = valueToY(pt.value)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val selX = if (points.size == 1) w / 2f else (safeIndex.toFloat() / (points.size - 1)) * w
            val selY = valueToY(selectedPoint.value)
            drawLine(
                color = lineColor.copy(alpha = 0.45f),
                start = Offset(selX, padTop),
                end = Offset(selX, h - padBottom),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = Offset(selX, selY))
        }

        ChartFooter(
            start = points.first().label.substringBefore(" •"),
            middle = bandLabel,
            end = points.last().label.substringBefore(" •")
        )
    }
}

/**
 * Weekly deaths as bars, coloured by rating, with the 0.1% line dashed.
 */
@Composable
fun WeeklyMortalityBarChartCard(
    points: List<LayerCalc.WeeklyMortalityPoint>,
    testTag: String
) {
    val dark = isSystemInDarkTheme()
    val goodColor = if (dark) StatusGreenBorderDark else StatusGreenBorderLight
    val watchColor = if (dark) StatusAmberBorderDark else StatusAmberBorderLight
    val highColor = if (dark) StatusRedBorderDark else StatusRedBorderLight
    val thresholdColor = MaterialTheme.colorScheme.onSurfaceVariant

    ClutchCard(modifier = Modifier.testTag(testTag)) {
        ChartHeader(
            title = "Deaths by week",
            subtitle = "Bars under the dashed line (0.1%) are good.",
            icon = ClutchIcons.Deaths
        )

        if (points.isEmpty()) {
            Text(
                text = "Not enough records yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ClutchCard
        }

        var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
        val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
        val selectedBar = points[safeIndex]

        SelectedValueCallout(
            label = "Week ${selectedBar.ageWeeks}: ${selectedBar.deaths} ${if (selectedBar.deaths == 1) "bird" else "birds"} died",
            value = "${LayerCalc.format2(selectedBar.mortalityPct)}%"
        )

        val maxPct = max(0.35, points.maxOf { it.mortalityPct } * 1.25)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics {
                    contentDescription = "Deaths by week chart. Latest week ${LayerCalc.format2(points.last().mortalityPct)}%. Tap a bar to see the week."
                }
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val slotW = w / points.size
                        selectedIndex = (tapOffset.x / slotW).toInt().coerceIn(0, points.lastIndex)
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
                drawRoundRect(
                    color = color.copy(alpha = if (idx == safeIndex) 1.0f else 0.65f),
                    topLeft = Offset(barX, h - padBottom - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            val lineY = pctToY(0.1)
            drawLine(
                color = thresholdColor,
                start = Offset(0f, lineY),
                end = Offset(w, lineY),
                strokeWidth = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        ChartFooter(
            start = "Wk ${points.first().ageWeeks}",
            middle = "Tap a bar",
            end = "Wk ${points.last().ageWeeks}"
        )
    }
}

@Composable
private fun ChartHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedValueCallout(label: String, value: String, detail: String? = null) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartFooter(start: String, middle: String?, end: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (middle != null) {
            Text(text = middle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
