package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.ui.theme.EggShellCream
import com.example.ui.theme.EggYolkAmber
import com.example.ui.theme.EggYolkHighlight
import com.example.ui.theme.BrandDeepGreen
import com.example.ui.theme.ClutchIcons
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
 * Shared layout tokens so every screen has the same rhythm (Gestalt similarity,
 * aesthetic-usability effect).
 */
object ClutchDimens {
    val ScreenPadding = 16.dp
    val SectionGap = 20.dp
    val CardPadding = 16.dp
    val CardRadius = 16.dp
    val ContentMaxWidth = 640.dp
    val TouchTarget = 56.dp
}

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
            .semantics { contentDescription = "Clutch logo" }
    ) {
        val w = this.size.width
        val h = this.size.height

        // Deep ledger-blue rounded badge
        drawCircle(
            color = BrandDeepGreen,
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

/**
 * Centred, width-capped scroll column used by every screen so content never
 * stretches too wide on tablets and keeps the same side gutters on phones.
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ClutchDimens.ContentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ClutchDimens.ScreenPadding, vertical = ClutchDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap),
            content = content
        )
    }
}

/**
 * Section heading with an optional single text action on the right
 * (at most one secondary action per section — Hick's Law).
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * The one card style used across the app. Pass [onClick] to make the whole card a tap target.
 */
@Composable
fun ClutchCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: Dp = ClutchDimens.CardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(ClutchDimens.CardRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
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
            icon = ClutchIcons.Good,
            label = state.label
        )
        LayerCalc.TargetState.WATCH -> StatusVisuals(
            backgroundColor = if (dark) StatusAmberBgDark else StatusAmberBgLight,
            borderColor = if (dark) StatusAmberBorderDark else StatusAmberBorderLight,
            textColor = if (dark) StatusAmberTextDark else StatusAmberTextLight,
            icon = ClutchIcons.Warning,
            label = state.label
        )
        LayerCalc.TargetState.OFF_TARGET -> StatusVisuals(
            backgroundColor = if (dark) StatusRedBgDark else StatusRedBgLight,
            borderColor = if (dark) StatusRedBorderDark else StatusRedBorderLight,
            textColor = if (dark) StatusRedTextDark else StatusRedTextLight,
            icon = ClutchIcons.Critical,
            label = state.label
        )
        LayerCalc.TargetState.CLIMBING_TO_PEAK -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            borderColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = ClutchIcons.TrendUp,
            label = state.label
        )
        LayerCalc.TargetState.PAST_PEAK_WINDOW -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outline,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = ClutchIcons.Waiting,
            label = state.label
        )
        LayerCalc.TargetState.NOT_APPLICABLE -> StatusVisuals(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = ClutchIcons.Info,
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
        shape = RoundedCornerShape(50),
        color = visuals.backgroundColor,
        border = BorderStroke(1.dp, visuals.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = null,
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
    loading: Boolean = false,
    icon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ClutchDimens.TouchTarget)
            .testTag(testTag)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ClutchDimens.TouchTarget)
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
 * Round icon badge used to lead cards and list rows.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * Compact number tile for a 2-column quick-stats grid. Tappable when [onClick] is set.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    status: LayerCalc.TargetState? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "stat_tile"
) {
    ClutchCard(
        modifier = modifier.testTag(testTag),
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (status != null && status != LayerCalc.TargetState.NOT_APPLICABLE) {
            StatusChip(state = status)
        }
    }
}

/**
 * Label + value row used in summary cards.
 */
@Composable
fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * One empty-state pattern for every screen: icon, short title, one sentence, one primary
 * action and an optional secondary action.
 */
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryTestTag: String = "empty_primary_button",
    secondaryTestTag: String = "empty_secondary_button"
) {
    ClutchCard(modifier = modifier, contentPadding = 24.dp) {
        IconBadge(icon = icon, size = 56.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ClutchPrimaryButton(
            text = primaryLabel,
            icon = primaryIcon,
            onClick = onPrimary,
            testTag = primaryTestTag
        )
        if (secondaryLabel != null && onSecondary != null) {
            ClutchSecondaryButton(
                text = secondaryLabel,
                onClick = onSecondary,
                testTag = secondaryTestTag
            )
        }
    }
}

/**
 * Full-screen loading state shown while the local database opens.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = "Opening your records…"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Text field with helper text that turns into an error message, and a "touched" callback
 * when the farmer leaves the field (validation shows only after they move on).
 */
@Composable
fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helperText: String?,
    errorText: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onBlur: () -> Unit = {},
    singleLine: Boolean = true,
    suffix: String? = null,
    placeholder: String? = null,
    testTag: String
) {
    var hadFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        isError = errorText != null,
        supportingText = when {
            errorText != null -> {
                {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("${testTag}_error")
                    )
                }
            }
            helperText != null -> {
                { Text(text = helperText) }
            }
            else -> null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    onBlur()
                }
            }
            .testTag(testTag)
    )
}

/**
 * Big − / + stepper for small daily counts (deaths, cracked eggs…). The number in the
 * middle can still be typed. 48dp buttons keep taps easy with one thumb (Fitts's Law).
 */
@Composable
fun NumberStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    isError: Boolean = false,
    testTag: String
) {
    val current = value.trim().toIntOrNull() ?: 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (helperText != null) {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        FilledTonalIconButton(
            onClick = { onValueChange((current - 1).coerceAtLeast(0).toString()) },
            enabled = current > 0,
            modifier = Modifier
                .size(48.dp)
                .testTag("${testTag}_minus")
        ) {
            Icon(imageVector = ClutchIcons.Remove, contentDescription = "Reduce $label", modifier = Modifier.size(16.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(6)) },
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(76.dp)
                .semantics { contentDescription = label }
                .testTag("${testTag}_value")
        )
        FilledTonalIconButton(
            onClick = { onValueChange((current + 1).toString()) },
            modifier = Modifier
                .size(48.dp)
                .testTag("${testTag}_plus")
        ) {
            Icon(imageVector = ClutchIcons.Add, contentDescription = "Add one to $label", modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Grouped card for up to 5 metrics (Miller's Law, Gestalt proximity & similarity).
 * Tapping any metric row opens the Tesler's Law bottom sheet with its meaning and advice.
 */
@Composable
fun GroupedMetricsCard(
    heading: String,
    metrics: List<LayerCalc.MetricExplanation>,
    onMetricClick: (LayerCalc.MetricExplanation) -> Unit,
    modifier: Modifier = Modifier
) {
    ClutchCard(modifier = modifier) {
        SectionHeader(title = heading)
        Column {
            metrics.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                MetricRowItem(
                    item = item,
                    onClick = { onMetricClick(item) }
                )
            }
        }
    }
}

@Composable
fun MetricRowItem(
    item: LayerCalc.MetricExplanation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 10.dp)
            .testTag("metric_card_${item.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            StatusChip(state = item.status)
        }
        Text(
            text = item.formattedValue,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = ClutchIcons.ChevronRight,
            contentDescription = "What ${item.title} means",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * "Things to check" section for the Home screen:
 * - Most severe first, at most 3 shown with "See all" for the rest (Miller's Law);
 * - Each one says what to do;
 * - If nothing fires, one calm "All is well" card.
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

        ClutchCard(
            modifier = modifier.testTag("all_clear_card"),
            containerColor = bg,
            borderColor = border
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = ClutchIcons.Good,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "All is well",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        text = "Laying, feed, water, deaths and egg quality all look normal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg
                    )
                }
            }
        }
        return
    }

    val maxVisible = 3
    val visibleWarnings = if (showAll || warnings.size <= maxVisible) warnings else warnings.take(maxVisible)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warnings_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Things to check (${warnings.size})",
            subtitle = "Most urgent first",
            actionLabel = if (warnings.size > maxVisible) {
                if (showAll) "Show less" else "See all"
            } else null,
            onAction = { showAll = !showAll },
            modifier = Modifier.testTag("warnings_show_all_button")
        )
        visibleWarnings.forEach { warning ->
            WarningItemBlock(warning = warning)
        }
    }
}

@Composable
fun WarningItemBlock(
    warning: LayerCalc.WarningItem,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val bgColor: Color
    val borderColor: Color
    val textColor: Color
    val iconVec: ImageVector
    when (warning.severity) {
        LayerCalc.WarningSeverity.CRITICAL -> {
            bgColor = if (dark) StatusRedBgDark else StatusRedBgLight
            borderColor = if (dark) StatusRedBorderDark else StatusRedBorderLight
            textColor = if (dark) StatusRedTextDark else StatusRedTextLight
            iconVec = ClutchIcons.Critical
        }
        LayerCalc.WarningSeverity.SERIOUS -> {
            bgColor = if (dark) StatusOrangeBgDark else StatusOrangeBgLight
            borderColor = if (dark) StatusOrangeBorderDark else StatusOrangeBorderLight
            textColor = if (dark) StatusOrangeTextDark else StatusOrangeTextLight
            iconVec = ClutchIcons.Warning
        }
        LayerCalc.WarningSeverity.WARNING -> {
            bgColor = if (dark) StatusAmberBgDark else StatusAmberBgLight
            borderColor = if (dark) StatusAmberBorderDark else StatusAmberBorderLight
            textColor = if (dark) StatusAmberTextDark else StatusAmberTextLight
            iconVec = ClutchIcons.Warning
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warning_item_${warning.id}"),
        shape = RoundedCornerShape(ClutchDimens.CardRadius),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = iconVec,
                    contentDescription = null,
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
                text = "What to do: ${warning.whatToCheck}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * Tesler's Law bottom sheet. Order follows what a farmer needs: the number and rating,
 * what it means, the good range, what to do — and only then how it was worked out.
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = explanation.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = explanation.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = explanation.formattedValue,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(state = explanation.status)
            }

            SheetSection(title = "Good range", body = explanation.targetDescription)
            SheetSection(title = "What to do if it is not good", body = explanation.whatToCheck)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How we worked it out",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = explanation.working,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = explanation.formula,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ClutchPrimaryButton(
                text = "Got it",
                onClick = onDismiss,
                testTag = "sheet_done_button"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Banner shown when sample data is active, offering one-tap "Clear sample".
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
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ClutchIcons.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "You are viewing a sample pen",
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
                    text = "Clear sample",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Page title used at the top of every tab, so each screen opens the same way (Jakob's Law).
 */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Deep-green brand card for the single most important number on a screen.
 */
@Composable
fun BrandHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(ClutchDimens.CardRadius + 4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(BrandDeepGreen, BrandMidGreen))
            )
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private val BrandMidGreen = Color(0xFF136F4A)

/**
 * Gold information note for guidance and caveats (e.g. "targets are indicative").
 */
@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = ClutchIcons.Info
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ClutchDimens.CardRadius),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Folding section header (Tesler's Law): shows a title and one-line summary, expands on tap.
 */
@Composable
fun ExpandableHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onToggle)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (expanded) ClutchIcons.ChevronUp else ClutchIcons.ChevronDown,
            contentDescription = if (expanded) "Hide $title" else "Show $title",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * "Saved on this phone" reassurance line used at the end of screens (peak-end rule).
 */
@Composable
fun OfflineReassurance(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ClutchIcons.Offline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Saved on this phone. Works without data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
