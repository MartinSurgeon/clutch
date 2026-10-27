package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.domain.ReferenceTargets
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IconBadge
import com.example.ui.components.InfoNote
import com.example.ui.components.LedgerTextField
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SummaryRow
import com.example.ui.theme.ClutchIcons

private enum class GuideTab(val label: String) {
    TARGETS("Targets"),
    CHECKS("Daily checks"),
    FLOCK("My flock")
}

/**
 * Guide & targets, split into three short views instead of one long page (Hick's Law):
 * the targets to aim for, today's monitoring checklist, and the flock's starting size.
 */
@Composable
fun GuideScreen(
    uiState: ClutchUiState,
    onSaveFlockSize: (FlockEntity, Int) -> Unit,
    onManagePens: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(GuideTab.TARGETS) }

    ScreenColumn(modifier = modifier, testTag = "guide_screen") {
        ScreenTitle(
            title = "Guide & targets",
            subtitle = "What good looks like, and what to check each day"
        )
        GuideTabs(selected = tab, onSelect = { tab = it })

        when (tab) {
            GuideTab.TARGETS -> TargetsView()
            GuideTab.CHECKS -> DailyChecksView(todayDate = uiState.todayDate)
            GuideTab.FLOCK -> FlockView(
                uiState = uiState,
                onSaveFlockSize = onSaveFlockSize,
                onManagePens = onManagePens
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideTabs(selected: GuideTab, onSelect: (GuideTab) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_tabs")
    ) {
        GuideTab.entries.forEachIndexed { index, t ->
            SegmentedButton(
                selected = t == selected,
                onClick = { onSelect(t) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = GuideTab.entries.size),
                icon = {},
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(t.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

// --- Targets ---

private fun iconFor(group: ReferenceTargets.Group): ImageVector = when (group) {
    ReferenceTargets.Group.PRODUCTION -> ClutchIcons.Dashboard
    ReferenceTargets.Group.FEED -> ClutchIcons.Feed
    ReferenceTargets.Group.EGG_WEIGHT -> ClutchIcons.Egg
    ReferenceTargets.Group.QUALITY -> ClutchIcons.Quality
    ReferenceTargets.Group.UNIFORMITY -> ClutchIcons.Uniformity
    ReferenceTargets.Group.MORTALITY -> ClutchIcons.Deaths
}

@Composable
private fun TargetsView() {
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }

    // The client's two caveats, together and short, before any number
    InfoNote(
        title = "Use these as a guide",
        text = "• ${ReferenceTargets.DISCLAIMER_TARGETS}\n• ${ReferenceTargets.DISCLAIMER_MORTALITY}"
    )

    ClutchCard(contentPadding = 0.dp, modifier = Modifier.testTag("targets_list")) {
        Column {
            ReferenceTargets.groups.forEachIndexed { index, group ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 66.dp)
                    )
                }
                TargetGroupRow(
                    group = group,
                    expanded = openGroup == group.group.name,
                    onToggle = { openGroup = if (openGroup == group.group.name) null else group.group.name }
                )
            }
        }
    }

    Text(
        text = "Tap a topic to see all its targets.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TargetGroupRow(
    group: ReferenceTargets.TargetGroup,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("targets_${group.group.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = iconFor(group.group), size = 38.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text = group.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = group.headline,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
            Icon(
                imageVector = if (expanded) ClutchIcons.ChevronUp else ClutchIcons.ChevronDown,
                contentDescription = if (expanded) "Hide ${group.title} targets" else "Show ${group.title} targets",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 52.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                group.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = row.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            row.note?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = row.target,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

// --- Daily checks: a real checklist, remembered for today only ---

private const val CHECKS_PREFS = "clutch_daily_checks"

@Composable
private fun DailyChecksView(todayDate: String) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(CHECKS_PREFS, Context.MODE_PRIVATE) }
    val notes = ReferenceTargets.dailyMonitoringNotes
    var done by remember(todayDate) {
        mutableStateOf(
            prefs.getStringSet(todayDate, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        )
    }

    fun toggle(index: Int) {
        done = if (index in done) done - index else done + index
        // Keep only today's ticks; older days are cleared so the list starts fresh each morning
        prefs.edit().clear().putStringSet(todayDate, done.map { it.toString() }.toSet()).apply()
    }

    ClutchCard(modifier = Modifier.testTag("daily_checks_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's checks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${LayerCalc.friendlyDate(todayDate)} • ${done.size} of ${notes.size} done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (done.size == notes.size) {
                Icon(ClutchIcons.Good, contentDescription = "All done", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        LinearProgressIndicator(
            progress = { done.size.toFloat() / notes.size },
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 6.dp)
        )
        Column {
            notes.forEachIndexed { index, note ->
                CheckRow(text = note, checked = index in done, onToggle = { toggle(index) }, index = index)
            }
        }
    }
    InfoNote(text = "Ticks reset every morning. They are just for you and are not part of your records.", icon = ClutchIcons.Tip)
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onToggle: () -> Unit, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { stateDescription = if (checked) "Done" else "Not done" }
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp)
            .testTag("daily_check_$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (checked) {
                        Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(ClutchIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}

// --- My flock ---

@Composable
private fun FlockView(
    uiState: ClutchUiState,
    onSaveFlockSize: (FlockEntity, Int) -> Unit,
    onManagePens: () -> Unit
) {
    val dashboard = uiState.activeDashboard
    if (dashboard == null) {
        EmptyStateCard(
            icon = ClutchIcons.Pens,
            title = "No pen yet",
            body = "Add a pen to set its starting flock size.",
            primaryLabel = "Add a pen",
            primaryIcon = ClutchIcons.Add,
            onPrimary = onManagePens
        )
        return
    }
    val flock = dashboard.flock

    var sizeText by rememberSaveable(flock.id, flock.hensHoused) { mutableStateOf(flock.hensHoused.toString()) }
    var attempted by rememberSaveable(flock.id) { mutableStateOf(false) }
    val sizeVal = sizeText.trim().toIntOrNull()
    val error = if (sizeVal == null || sizeVal <= 0) "Enter how many birds you stocked." else null
    val changed = sizeVal != null && sizeVal != flock.hensHoused

    ClutchCard(modifier = Modifier.testTag("flock_size_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon = ClutchIcons.Flock, size = 40.dp)
            Column {
                Text(text = flock.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${flock.breed} • ${dashboard.ageWeeksToday} weeks old",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LedgerTextField(
            value = sizeText,
            onValueChange = { sizeText = it.filter { c -> c.isDigit() }.take(7) },
            label = "Initial flock size",
            suffix = "birds",
            helperText = "Birds stocked at the start. Used for hen-housed % and total deaths.",
            errorText = if (attempted) error else null,
            keyboardType = KeyboardType.Number,
            testTag = "input_initial_flock_size"
        )
        ClutchPrimaryButton(
            text = if (changed) "Save flock size" else "Flock size is saved",
            icon = ClutchIcons.Save,
            enabled = changed,
            onClick = {
                attempted = true
                if (error == null && sizeVal != null) onSaveFlockSize(flock, sizeVal)
            },
            testTag = "save_flock_size"
        )
    }

    ClutchCard {
        SummaryRow(label = "Birds alive now", value = "${dashboard.latestRecord?.metrics?.closingHens ?: flock.hensHoused}")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(label = "Stocked on", value = LayerCalc.friendlyDate(flock.placedDate, withYear = true))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(label = "Age when stocked", value = "${flock.ageWeeksAtPlacement} weeks")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(label = "Days recorded", value = "${dashboard.allRecordsAsc.size}")
    }

    ClutchSecondaryButton(
        text = "Edit pen details",
        icon = ClutchIcons.Edit,
        onClick = onManagePens,
        testTag = "guide_manage_pens"
    )
}
