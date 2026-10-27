package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.domain.TrendInsights
import com.example.ui.theme.ChartAccentDark
import com.example.ui.theme.ChartAccentLight
import com.example.ui.theme.ChartLineDark
import com.example.ui.theme.ChartLineLight
import com.example.ui.theme.ClutchIcons
import com.example.ui.theme.StatusAmberBorderDark
import com.example.ui.theme.StatusAmberBorderLight
import com.example.ui.theme.StatusGreenBgDark
import com.example.ui.theme.StatusGreenBgLight
import com.example.ui.theme.StatusGreenBorderDark
import com.example.ui.theme.StatusGreenBorderLight
import com.example.ui.theme.StatusGreenTextDark
import com.example.ui.theme.StatusGreenTextLight
import com.example.ui.theme.StatusRedTextDark
import com.example.ui.theme.StatusRedTextLight
import kotlin.math.floor
import kotlin.math.roundToInt

enum class ChartKind(val label: String) {
    LINE("Line"),
    BARS("Bars"),
    STACKED("Parts")
}

data class ChartDatum(
    /** Short x-axis label, e.g. "12 Oct" or "Wk 30". */
    val shortLabel: String,
    /** Full label for the selected-point callout, e.g. "Mon 12 Oct • week 30". */
    val fullLabel: String,
    val value: Double,
    /** For stacked bars: the parts that add up to [value], bottom to top. */
    val segments: List<Double> = emptyList(),
    val detail: String? = null
)

/**
 * Everything a chart needs to explain itself: what it measures, the target, which way
 * is good, and plain words for its resolution ("each bar is one week").
 */
data class ChartSpec(
    val id: String,
    val title: String,
    val meaning: String,
    val unit: String,
    val decimals: Int,
    val data: List<ChartDatum>,
    val bandMin: Double?,
    val bandMax: Double?,
    val targetText: String?,
    val goodness: TrendInsights.Goodness,
    val yFloor: Double,
    val yCeil: Double,
    val kinds: List<ChartKind>,
    /** "day" or "week" — used in sentences like "22 of 30 days in target". */
    val pointNoun: String,
    val segmentNames: List<String> = emptyList(),
    val footnote: String? = null,
    val emptyMessage: String = "Record a few more days to see this chart.",
    /** Written before numbers, e.g. "GH₵ " for money. */
    val prefix: String = ""
) {
    fun format(value: Double): String {
        val number = when (decimals) {
            0 -> LayerCalc.format0(value)
            1 -> LayerCalc.format1(value)
            else -> LayerCalc.format2(value)
        }
        val sign = if (number.startsWith("-")) "−" else ""
        return sign + prefix + number.removePrefix("-") + unit
    }

    fun formatAxis(value: Double): String {
        val rounded = if (value == floor(value)) value.roundToInt().toString() else LayerCalc.format1(value)
        val sign = if (rounded.startsWith("-")) "−" else ""
        val short = if (prefix.isNotEmpty() && kotlin.math.abs(value) >= 1000) {
            LayerCalc.format1(kotlin.math.abs(value) / 1000.0).removeSuffix(".0") + "k"
        } else rounded.removePrefix("-")
        return sign + prefix.trim() + short + unit.trim().let { if (it == "%") "%" else "" }
    }
}

private data class ChartColors(
    val good: Color,
    val warn: Color,
    val band: Color,
    val bandEdge: Color,
    val grid: Color,
    val axisText: Color,
    val segments: List<Color>,
    val improving: Color,
    val worsening: Color
)

@Composable
private fun rememberChartColors(): ChartColors {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    return ChartColors(
        good = if (dark) ChartLineDark else ChartLineLight,
        warn = if (dark) StatusAmberBorderDark else StatusAmberBorderLight,
        band = if (dark) StatusGreenBgDark.copy(alpha = 0.7f) else StatusGreenBgLight.copy(alpha = 0.85f),
        bandEdge = if (dark) StatusGreenBorderDark.copy(alpha = 0.6f) else StatusGreenBorderLight.copy(alpha = 0.55f),
        grid = scheme.outlineVariant.copy(alpha = 0.7f),
        axisText = scheme.onSurfaceVariant,
        segments = listOf(
            if (dark) ChartLineDark else ChartLineLight,
            if (dark) ChartAccentDark else ChartAccentLight,
            if (dark) Color(0xFF9FB3AA) else Color(0xFF6B7C74)
        ),
        improving = if (dark) StatusGreenTextDark else StatusGreenTextLight,
        worsening = if (dark) StatusRedTextDark else StatusRedTextLight
    )
}

/**
 * The one chart card used everywhere. Reads top to bottom like a sentence:
 * 1) the latest number and whether it is in target, 2) which way it is moving,
 * 3) the chart itself with labelled axes and target band, 4) a legend.
 * Tap or drag across the chart to read any day.
 */
@Composable
fun PerformanceChartCard(
    spec: ChartSpec,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val colors = rememberChartColors()
    var kind by rememberSaveable(spec.id) { mutableStateOf(spec.kinds.first()) }
    val activeKind = if (kind in spec.kinds) kind else spec.kinds.first()

    ClutchCard(modifier = modifier.testTag("chart_${spec.id}"), contentPadding = 18.dp) {
        header?.invoke()

        if (spec.data.isEmpty()) {
            ChartTitleRow(spec = spec, kind = activeKind, onKind = { kind = it })
            Text(
                text = spec.emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ClutchCard
        }

        var selectedIndex by remember(spec.id, spec.data.size) { mutableIntStateOf(spec.data.lastIndex) }
        val safeIndex = selectedIndex.coerceIn(0, spec.data.lastIndex)

        ChartTitleRow(spec = spec, kind = activeKind, onKind = { kind = it })
        InsightBlock(spec = spec, colors = colors)

        SelectedPointRow(spec = spec, datum = spec.data[safeIndex], isLatest = safeIndex == spec.data.lastIndex)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Responsive: about half as tall as wide, within comfortable limits
            val chartHeight = (maxWidth * 0.52f).coerceIn(190.dp, 300.dp)
            ChartCanvas(
                spec = spec,
                kind = activeKind,
                colors = colors,
                selectedIndex = safeIndex,
                onSelect = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            )
        }

        ChartLegend(spec = spec, kind = activeKind, colors = colors)

        val resolution = "Each ${if (activeKind == ChartKind.LINE) "point" else "bar"} is one ${spec.pointNoun}. Tap or slide across the chart to read it."
        Text(
            text = resolution,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        spec.footnote?.let { InfoNote(text = it) }
    }
}

@Composable
private fun ChartTitleRow(spec: ChartSpec, kind: ChartKind, onKind: (ChartKind) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = spec.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = spec.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (spec.kinds.size > 1) {
            ChartKindToggle(kinds = spec.kinds, selected = kind, onSelect = onKind)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartKindToggle(kinds: List<ChartKind>, selected: ChartKind, onSelect: (ChartKind) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.testTag("chart_kind_toggle")) {
        kinds.forEachIndexed { index, k ->
            SegmentedButton(
                selected = k == selected,
                onClick = { onSelect(k) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
                icon = {},
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Text(k.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun InsightBlock(spec: ChartSpec, colors: ChartColors) {
    val values = spec.data.map { it.value }
    val insight = TrendInsights.summarize(
        values = values,
        bandMin = spec.bandMin,
        bandMax = spec.bandMax,
        goodness = spec.goodness,
        flatThreshold = 0.05 * (spec.yCeil - spec.yFloor) / 10.0
    ) ?: return

    val inTarget = insight.latestInTarget
    val (chipState, chipLabel) = when {
        inTarget == null -> null to null
        inTarget -> LayerCalc.TargetState.ON_TARGET to "In target"
        spec.bandMin != null && insight.latest < spec.bandMin -> LayerCalc.TargetState.WATCH to "Below target"
        else -> LayerCalc.TargetState.WATCH to "Above target"
    }

    val trendText = when {
        insight.change == null -> "First ${spec.pointNoun} in this period."
        insight.direction == TrendInsights.Direction.FLAT -> "About the same as the ${spec.pointNoun}s before."
        else -> {
            val word = if (insight.direction == TrendInsights.Direction.UP) "Up" else "Down"
            "$word ${spec.format(kotlin.math.abs(insight.change))} on the last ${minOf(7, spec.data.size - 1)} ${spec.pointNoun}s' average"
        }
    }
    val trendColor = when (insight.isImproving) {
        true -> colors.improving
        false -> colors.worsening
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val trendIcon = when (insight.direction) {
        TrendInsights.Direction.UP -> ClutchIcons.TrendUp
        TrendInsights.Direction.DOWN -> ClutchIcons.TrendDown
        TrendInsights.Direction.FLAT -> ClutchIcons.TrendFlat
    }

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = spec.format(insight.latest),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (chipState != null && chipLabel != null) {
                StatusChip(state = chipState, label = chipLabel)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(11.dp))
            Text(text = trendText, style = MaterialTheme.typography.labelLarge, color = trendColor)
        }
        if (spec.bandMin != null || spec.bandMax != null) {
            Text(
                text = "${insight.inTargetCount} of ${insight.total} ${spec.pointNoun}s in target" +
                    (spec.targetText?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedPointRow(spec: ChartSpec, datum: ChartDatum, isLatest: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_selected_point")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLatest) "Latest • ${datum.fullLabel}" else datum.fullLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = spec.format(datum.value),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            datum.detail?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    spec: ChartSpec,
    kind: ChartKind,
    colors: ChartColors,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = spec.data
    val n = data.size
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = colors.axisText, fontFeatureSettings = "tnum")
    val targetStyle = MaterialTheme.typography.labelSmall.copy(color = colors.bandEdge.copy(alpha = 1f), fontWeight = FontWeight.SemiBold)
    val surface = MaterialTheme.colorScheme.surface

    val isBars = kind != ChartKind.LINE
    val dataMin = data.minOf { it.value }
    val dataMax = data.maxOf { it.value }
    val lo = minOf(dataMin, spec.bandMin ?: dataMin, spec.yFloor).let { if (isBars) minOf(it, 0.0) else it }
    val hi = maxOf(dataMax, spec.bandMax ?: dataMax, spec.yCeil)
    val ticks = remember(lo, hi) { TrendInsights.niceTicks(lo, hi, 4) }
    val axisMin = ticks.first()
    val axisMax = ticks.last()
    val span = (axisMax - axisMin).coerceAtLeast(1e-6)

    // Geometry shared by drawing and touch so taps land on the right point
    // [0] = plot left edge, [1] = plot width, written while drawing and read on touch
    val plotGeometry = remember { floatArrayOf(0f, 1f) }

    fun indexAt(x: Float): Int {
        if (n <= 1) return 0
        val rel = ((x - plotGeometry[0]) / plotGeometry[1]).coerceIn(0f, 1f)
        return if (isBars) floor(rel * n).toInt().coerceIn(0, n - 1) else (rel * (n - 1)).roundToInt().coerceIn(0, n - 1)
    }

    val latest = data.last()
    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = "${spec.title} chart. Latest ${spec.format(latest.value)} on ${latest.fullLabel}." +
                    (spec.targetText?.let { " $it." } ?: "")
            }
            .pointerInput(n, kind) { detectTapGestures { onSelect(indexAt(it.x)) } }
            .pointerInput(n, kind) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSelect(indexAt(change.position.x))
                }
            }
    ) {
        val tickLabels = ticks.map { spec.formatAxis(it) }
        val labelWidths = tickLabels.map { textMeasurer.measure(it, axisStyle).size.width }
        val gutterLeft = (labelWidths.maxOrNull() ?: 0) + 8.dp.toPx()
        val xLabelHeight = textMeasurer.measure("0", axisStyle).size.height
        val top = 8.dp.toPx()
        val bottom = size.height - xLabelHeight - 8.dp.toPx()
        val plotH = (bottom - top).coerceAtLeast(1f)
        val left = gutterLeft
        val width = (size.width - left).coerceAtLeast(1f)
        plotGeometry[0] = left
        plotGeometry[1] = width

        fun y(v: Double): Float = top + plotH * (1f - ((v - axisMin) / span).toFloat().coerceIn(0f, 1f))
        val slot = width / n.coerceAtLeast(1)
        fun xCenter(i: Int): Float = if (isBars) left + slot * (i + 0.5f) else if (n == 1) left + width / 2f else left + width * i / (n - 1)

        // Grid + y-axis labels
        ticks.forEachIndexed { i, t ->
            val yy = y(t)
            drawLine(colors.grid, Offset(left, yy), Offset(size.width, yy), 1.dp.toPx())
            val layout = textMeasurer.measure(tickLabels[i], axisStyle)
            drawText(layout, topLeft = Offset(left - 8.dp.toPx() - layout.size.width, yy - layout.size.height / 2f))
        }

        // Target band (or one-sided zone) with a label
        if (spec.bandMin != null || spec.bandMax != null) {
            val yTop = y(spec.bandMax ?: axisMax)
            val yBottom = y(spec.bandMin ?: axisMin)
            drawRect(colors.band, topLeft = Offset(left, yTop), size = Size(width, (yBottom - yTop).coerceAtLeast(2f)))
            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            spec.bandMax?.let { drawLine(colors.bandEdge, Offset(left, yTop), Offset(size.width, yTop), 1.5.dp.toPx(), pathEffect = dash) }
            spec.bandMin?.let { drawLine(colors.bandEdge, Offset(left, yBottom), Offset(size.width, yBottom), 1.5.dp.toPx(), pathEffect = dash) }
            val tag = textMeasurer.measure("Target", targetStyle)
            val tagY = (yTop + 3.dp.toPx()).coerceAtMost(yBottom - tag.size.height.toFloat()).coerceAtLeast(top)
            drawText(tag, topLeft = Offset(size.width - tag.size.width - 4.dp.toPx(), tagY))
        }

        fun inTarget(v: Double) = TrendInsights.isInBand(v, spec.bandMin, spec.bandMax) != false

        when (kind) {
            ChartKind.LINE -> {
                val pts = data.mapIndexed { i, d -> Offset(xCenter(i), y(d.value)) }
                if (pts.size > 1) {
                    val area = Path().apply {
                        moveTo(pts.first().x, bottom)
                        pts.forEach { lineTo(it.x, it.y) }
                        lineTo(pts.last().x, bottom)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(colors.good.copy(alpha = 0.22f), colors.good.copy(alpha = 0f)), startY = top, endY = bottom))
                    val line = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(line, colors.good, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                // Points outside target always get an amber dot, so problems jump out
                pts.forEachIndexed { i, p ->
                    val off = !inTarget(data[i].value)
                    if (off || n <= 31) {
                        drawCircle(surface, radius = (if (off) 4.5 else 3.0).dp.toPx(), center = p)
                        drawCircle(if (off) colors.warn else colors.good, radius = (if (off) 3.2 else 2.0).dp.toPx(), center = p)
                    }
                }
                val sel = pts[selectedIndex]
                drawLine(colors.axisText.copy(alpha = 0.5f), Offset(sel.x, top), Offset(sel.x, bottom), 1.dp.toPx())
                drawCircle(surface, radius = 7.dp.toPx(), center = sel)
                drawCircle(if (inTarget(data[selectedIndex].value)) colors.good else colors.warn, radius = 5.dp.toPx(), center = sel)
            }

            ChartKind.BARS, ChartKind.STACKED -> {
                val barW = (slot * 0.66f).coerceIn(2.dp.toPx(), 32.dp.toPx())
                val radius = CornerRadius((barW / 3f).coerceAtMost(6.dp.toPx()))
                data.forEachIndexed { i, d ->
                    val x = xCenter(i) - barW / 2f
                    val dim = i != selectedIndex
                    if (kind == ChartKind.STACKED && d.segments.isNotEmpty()) {
                        var base = 0.0
                        d.segments.forEachIndexed { s, part ->
                            val yTop = y(base + part)
                            val yBottom = y(base)
                            if (yBottom - yTop > 0.5f) {
                                val c = colors.segments[s % colors.segments.size]
                                drawRoundRect(
                                    color = if (dim) c.copy(alpha = 0.75f) else c,
                                    topLeft = Offset(x, yTop),
                                    size = Size(barW, yBottom - yTop),
                                    cornerRadius = if (s == d.segments.lastIndex) radius else CornerRadius.Zero
                                )
                            }
                            base += part
                        }
                    } else {
                        val c = if (inTarget(d.value)) colors.good else colors.warn
                        val yValue = y(d.value)
                        val yZero = y(maxOf(0.0, axisMin))
                        // Bars grow up from zero for gains and down from zero for losses
                        val barTop = minOf(yValue, yZero)
                        val barHeight = kotlin.math.abs(yZero - yValue).coerceAtLeast(2.dp.toPx())
                        drawRoundRect(
                            color = if (dim) c.copy(alpha = 0.72f) else c,
                            topLeft = Offset(x, if (yValue <= yZero) barTop else yZero),
                            size = Size(barW, barHeight),
                            cornerRadius = radius
                        )
                    }
                }
                val selX = xCenter(selectedIndex)
                drawLine(colors.axisText.copy(alpha = 0.35f), Offset(selX, top), Offset(selX, bottom), 1.dp.toPx())
                if (kind == ChartKind.STACKED) {
                    // Total target as a line over stacked parts
                    spec.bandMax?.let {
                        drawLine(colors.warn, Offset(left, y(it)), Offset(size.width, y(it)), 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f))
                    }
                }
            }
        }

        // X-axis labels — as many as fit the width
        val maxLabels = (width / 64.dp.toPx()).toInt().coerceAtLeast(2)
        TrendInsights.labelIndices(n, maxLabels).forEach { i ->
            val layout = textMeasurer.measure(data[i].shortLabel, axisStyle)
            val cx = xCenter(i)
            val lx = (cx - layout.size.width / 2f).coerceIn(left, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(lx, bottom + 6.dp.toPx()))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(spec: ChartSpec, kind: ChartKind, colors: ChartColors) {
    val hasBand = spec.bandMin != null || spec.bandMax != null
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (kind == ChartKind.STACKED) {
            spec.segmentNames.forEachIndexed { i, name ->
                LegendItem(color = colors.segments[i % colors.segments.size], label = name)
            }
            if (spec.bandMax != null) LegendItem(color = colors.warn, label = "Target line", isLine = true)
        } else {
            LegendItem(color = colors.good, label = if (hasBand) "In target" else spec.title, isLine = kind == ChartKind.LINE)
            if (hasBand) {
                LegendItem(color = colors.warn, label = "Outside target")
                LegendItem(color = colors.band, label = "Target range", outline = colors.bandEdge)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isLine: Boolean = false, outline: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(width = if (isLine) 14.dp else 10.dp, height = if (isLine) 3.dp else 10.dp)
                .background(color, if (isLine) RoundedCornerShape(2.dp) else RoundedCornerShape(3.dp))
                .then(
                    if (outline != null) Modifier.border(1.dp, outline, RoundedCornerShape(3.dp)) else Modifier
                )
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Horizontally scrolling chips to choose which measure the chart shows (one chart at a time —
 * Hick's Law). Each chip has a status dot so problems are visible before tapping.
 */
@Composable
fun ChartMetricSelector(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    attention: Set<String> = emptySet()
) {
    val warn = rememberChartColors().warn
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("chart_metric_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, maxLines = 1)
                        if (id in attention) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(warn, CircleShape)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("chart_metric_$id")
            )
        }
    }
}

/**
 * Period switch for charts (last 30 days / 3 months / all time).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PeriodSwitch(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth().testTag("period_switch")) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {},
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(label(option), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}
