package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.ui.theme.EggShellCream
import com.example.ui.theme.EggYolkAmber
import com.example.ui.theme.EggYolkHighlight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LedgerBlueLight
import com.example.ui.theme.StatusAmberBgDark
import com.example.ui.theme.StatusAmberBgLight
import com.example.ui.theme.StatusAmberBorderDark
import com.example.ui.theme.StatusAmberBorderLight
import com.example.ui.theme.StatusAmberTextDark
import com.example.ui.theme.StatusAmberTextLight
import com.example.ui.theme.StatusGreenBgDark
import com.example.ui.theme.StatusGreenBgLight
import com.example.ui.theme.StatusGreenBorderDark
import com.example.ui.theme.StatusGreenBorderLight
import com.example.ui.theme.StatusGreenTextDark
import com.example.ui.theme.StatusGreenTextLight
import com.example.ui.theme.StatusOrangeBgDark
import com.example.ui.theme.StatusOrangeBgLight
import com.example.ui.theme.StatusOrangeBorderDark
import com.example.ui.theme.StatusOrangeBorderLight
import com.example.ui.theme.StatusOrangeTextDark
import com.example.ui.theme.StatusOrangeTextLight
import com.example.ui.theme.StatusRedBgDark
import com.example.ui.theme.StatusRedBgLight
import com.example.ui.theme.StatusRedBorderDark
import com.example.ui.theme.StatusRedBorderLight
import com.example.ui.theme.StatusRedTextDark
import com.example.ui.theme.StatusRedTextLight

/**
 * Small egg-yolk logo used in the top app bar and first-run empty state.
 */
@Composable
fun EggYolkLogo(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Clutch egg-yolk logo" }
    ) {
        val w = this.size.width
        val h = this.size.height

        // Deep ledger-blue rounded badge
        drawCircle(
            color = LedgerBlueLight,
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Egg shell silhouette
        val eggPath = Path().apply {
            moveTo(w * 0.5f, h * 0.16f)
            cubicTo(
                w * 0.30f, h * 0.16f,
                w * 0.22f, h * 0.44f,
                w * 0.22f, h * 0.60f
            )
            cubicTo(
                w * 0.22f, h * 0.76f,
                w * 0.34f, h * 0.85f,
                w * 0.5f, h * 0.85f
            )
            cubicTo(
                w * 0.66f, h * 0.85f,
                w * 0.78f, h * 0.76f,
                w * 0.78f, h * 0.60f
            )
            cubicTo(
                w * 0.78f, h * 0.44f,
                w * 0.70f, h * 0.16f,
                w * 0.5f, h * 0.16f
            )
            close()
        }
        drawPath(path = eggPath, color = EggShellCream)

        // Warm egg yolk center
        drawCircle(
            color = EggYolkAmber,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.59f)
        )

        // Yolk specular highlight
        drawCircle(
            color = EggYolkHighlight,
            radius = w * 0.05f,
            center = Offset(w * 0.45f, h * 0.54f)
        )
    }
}

data class StatusVisuals(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun rememberStatusVisuals(state: LayerCalc.TargetState): StatusVisuals {
    val dark = isSystemInDarkTheme()
    return when (state) {
        LayerCalc.TargetState.ON_TARGET -> StatusVisuals(
            backgroundColor = if (dark) StatusGreenBgDark else StatusGreenBgLight,
            borderColor = if (dark) StatusGreenBorderDark else StatusGreenBorderLight,
            textColor = if (dark) StatusGreenTextDark else StatusGreenTextLight,
            icon = Icons.Default.CheckCircle,
            label = state.label
        )
        LayerCalc.TargetState.WATCH -> StatusVisuals(
            backgroundColor = if (dark) StatusAmberBgDark else StatusAmberBgLight,
            borderColor = if (dark) StatusAmberBorderDark else StatusAmberBorderLight,
            textColor = if (dark) StatusAmberTextDark else StatusAmberTextLight,
            icon = Icons.Default.Warning,
            label = state.label
        )
        LayerCalc.TargetState.OFF_TARGET -> StatusVisuals(
            backgroundColor = if (dark) StatusRedBgDark else StatusRedBgLight,
            borderColor = if (dark) StatusRedBorderDark else StatusRedBorderLight,
            textColor = if (dark) StatusRedTextDark else StatusRedTextLight,
            icon = Icons.Default.Error,
            label = state.label
        )
        LayerCalc.TargetState.CLIMBING_TO_PEAK -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            borderColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            label = state.label
        )
        LayerCalc.TargetState.PAST_PEAK_WINDOW -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outline,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Default.Schedule,
            label = state.label
        )
        LayerCalc.TargetState.NOT_APPLICABLE -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Outlined.Info,
            label = state.label
        )
    }
}

/**
 * Accessible status chip: ALWAYS shows both an icon and a word, never colour alone.
 */
@Composable
fun StatusChip(
    state: LayerCalc.TargetState,
    modifier: Modifier = Modifier
) {
    if (state == LayerCalc.TargetState.NOT_APPLICABLE) return
    val visuals = rememberStatusVisuals(state)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = visuals.backgroundColor,
        border = BorderStroke(1.dp, visuals.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = visuals.label,
                tint = visuals.textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = visuals.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = visuals.textColor
            )
        }
    }
}

/**
 * Primary full-width 56dp button following Fitts's Law and Hick's Law.
 */
@Composable
fun ClutchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Secondary full-width 56dp outlined button (at most one secondary action per section).
 */
@Composable
fun ClutchSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Grouped card for up to 5 metrics (Miller's Law, Gestalt proximity & similarity).
 * Tapping any metric row opens the Tesler's Law bottom sheet with its formula, target, and what to check.
 */
@Composable
fun GroupedMetricsCard(
    heading: String,
    metrics: List<LayerCalc.MetricExplanation>,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Tap any number for formula and targets",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tap for formula",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            metrics.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                MetricRowItem(
                    item = item,
                    onClick = { onMetricClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricRowItem(
    item: LayerCalc.MetricExplanation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("metric_card_${item.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.Center
        ) {
            StatusChip(state = item.status)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.formattedValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "See formula for ${item.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Warnings section for Today screen:
 * - Shows most severe first, at most 5 shown with "Show all" for the rest;
 * - Each one says what to check;
 * - If nothing fires, shows one green "All clear" card.
 */
@Composable
fun WarningsSectionCard(
    warnings: List<LayerCalc.WarningItem>,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    var showAll by rememberSaveable { mutableStateOf(false) }

    if (warnings.isEmpty()) {
        val bg = if (dark) StatusGreenBgDark else StatusGreenBgLight
        val border = if (dark) StatusGreenBorderDark else StatusGreenBorderLight
        val fg = if (dark) StatusGreenTextDark else StatusGreenTextLight

        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("all_clear_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.5.dp, border)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "All clear",
                    tint = fg,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "All clear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        text = "Production, feed, water, mortality, and egg quality are within target ranges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg
                    )
                }
            }
        }
        return
    }

    val visibleWarnings = if (showAll || warnings.size <= 5) warnings else warnings.take(5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warnings_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Warnings (${warnings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Most severe first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            visibleWarnings.forEach { warning ->
                WarningItemBlock(warning = warning)
            }

            if (warnings.size > 5) {
                TextButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("warnings_show_all_button")
                ) {
                    Text(
                        text = if (showAll) "Show fewer" else "Show all (${warnings.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WarningItemBlock(
    warning: LayerCalc.WarningItem,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val (bg, border, fg, icon) = when (warning.severity) {
        LayerCalc.WarningSeverity.CRITICAL -> listOf(
            if (dark) StatusRedBgDark else StatusRedBgLight,
            if (dark) StatusRedBorderDark else StatusRedBorderLight,
            if (dark) StatusRedTextDark else StatusRedTextLight,
            Icons.Default.Error
        )
        LayerCalc.WarningSeverity.SERIOUS -> listOf(
            if (dark) StatusOrangeBgDark else StatusOrangeBgLight,
            if (dark) StatusOrangeBorderDark else StatusOrangeBorderLight,
            if (dark) StatusOrangeTextDark else StatusOrangeTextLight,
            Icons.Default.PriorityHigh
        )
        LayerCalc.WarningSeverity.WARNING -> listOf(
            if (dark) StatusAmberBgDark else StatusAmberBgLight,
            if (dark) StatusAmberBorderDark else StatusAmberBorderLight,
            if (dark) StatusAmberTextDark else StatusAmberTextLight,
            Icons.Default.Warning
        )
    }
    val bgColor = bg as Color
    val borderColor = border as Color
    val textColor = fg as Color
    val iconVec = icon as ImageVector

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warning_item_${warning.id}"),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = iconVec,
                    contentDescription = warning.severity.label,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = warning.severity.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text(
                text = warning.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = warning.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "What to check: ${warning.whatToCheck}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * Tesler's Law bottom sheet showing a metric's formula, target, working, and what to check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricExplanationBottomSheet(
    explanation: LayerCalc.MetricExplanation,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("metric_explanation_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = explanation.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = explanation.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = explanation.formattedValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            StatusChip(state = explanation.status)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TODAY'S WORKING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = explanation.working,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Formula",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = explanation.formula,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Target range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = explanation.targetDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "What to check if off target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = explanation.whatToCheck,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ClutchPrimaryButton(
                text = "Done",
                onClick = onDismiss,
                testTag = "sheet_done_button"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Banner shown when example data is active, offering one-tap "Remove example".
 */
@Composable
fun ExampleDataBanner(
    onRemoveExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("example_data_banner"),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Example flock active (12 wks for Pen A)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRemoveExample,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("remove_example_button")
            ) {
                Text(
                    text = "Remove example",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
