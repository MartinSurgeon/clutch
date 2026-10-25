package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.domain.ReferenceTargets
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.IconBadge
import com.example.ui.components.InfoNote
import com.example.ui.components.LedgerTextField
import com.example.ui.components.OfflineReassurance
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ClutchIcons

@Composable
fun GuideScreen(
    uiState: ClutchUiState,
    onSaveFlockSize: (FlockEntity, Int) -> Unit,
    onManagePens: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenColumn(modifier = modifier, testTag = "guide_screen") {
        ScreenTitle(
            title = "Guide & targets",
            subtitle = "What good looks like, and what to check every day"
        )

        // Most important first (serial position): the caveats the client asked for
        InfoNote(
            title = "Targets are a guide",
            text = ReferenceTargets.DISCLAIMER_TARGETS,
            icon = ClutchIcons.Info
        )
        InfoNote(
            title = "Check weekly deaths too",
            text = ReferenceTargets.DISCLAIMER_MORTALITY,
            icon = ClutchIcons.Warning
        )

        uiState.activeDashboard?.let { dashboard ->
            FlockSizeCard(
                flock = dashboard.flock,
                ageWeeks = dashboard.ageWeeksToday,
                onSave = { onSaveFlockSize(dashboard.flock, it) },
                onManagePens = onManagePens
            )
        }

        ReferenceTargets.groups.forEach { group ->
            TargetGroupCard(group = group)
        }

        MonitoringNotesCard()

        OfflineReassurance()
    }
}

@Composable
private fun FlockSizeCard(
    flock: FlockEntity,
    ageWeeks: Int,
    onSave: (Int) -> Unit,
    onManagePens: () -> Unit
) {
    var sizeText by rememberSaveable(flock.id, flock.hensHoused) { mutableStateOf(flock.hensHoused.toString()) }
    var attempted by rememberSaveable(flock.id) { mutableStateOf(false) }
    val sizeVal = sizeText.trim().toIntOrNull()
    val error = if (sizeVal == null || sizeVal <= 0) "Enter how many birds you stocked." else null
    val changed = sizeVal != null && sizeVal != flock.hensHoused

    ClutchCard(modifier = Modifier.testTag("flock_size_card")) {
        SectionHeader(
            title = "Your flock",
            subtitle = "${flock.name} • ${flock.breed} • $ageWeeks weeks old",
            actionLabel = "Pen details",
            onAction = onManagePens
        )
        LedgerTextField(
            value = sizeText,
            onValueChange = { sizeText = it.filter { c -> c.isDigit() }.take(7) },
            label = "Initial flock size",
            suffix = "birds",
            helperText = "Birds stocked on ${LayerCalc.friendlyDate(flock.placedDate, withYear = true)}. Used for hen-housed % and total deaths.",
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
                if (error == null && sizeVal != null) onSave(sizeVal)
            },
            testTag = "save_flock_size"
        )
    }
}

private fun iconFor(group: ReferenceTargets.Group): ImageVector = when (group) {
    ReferenceTargets.Group.PRODUCTION -> ClutchIcons.Dashboard
    ReferenceTargets.Group.FEED -> ClutchIcons.Feed
    ReferenceTargets.Group.EGG_WEIGHT -> ClutchIcons.Egg
    ReferenceTargets.Group.QUALITY -> ClutchIcons.Quality
    ReferenceTargets.Group.UNIFORMITY -> ClutchIcons.Uniformity
    ReferenceTargets.Group.MORTALITY -> ClutchIcons.Deaths
}

@Composable
private fun TargetGroupCard(group: ReferenceTargets.TargetGroup) {
    ClutchCard(modifier = Modifier.testTag("targets_${group.group.name.lowercase()}")) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = iconFor(group.group), size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = group.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column {
            group.rows.forEachIndexed { idx, row ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = row.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        row.note?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = row.target,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoringNotesCard() {
    ClutchCard(modifier = Modifier.testTag("monitoring_notes_card")) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = ClutchIcons.Tip,
                size = 36.dp,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Daily monitoring", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Good habits that catch problems early", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ReferenceTargets.dailyMonitoringNotes.forEachIndexed { idx, note ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(26.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 3.dp)
                )
            }
        }
    }
}
