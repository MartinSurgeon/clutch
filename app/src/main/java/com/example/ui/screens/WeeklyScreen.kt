package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.WeeklyNoteEntity
import com.example.domain.LayerCalc
import com.example.domain.WeeklyCalc
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchDimens
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExpandableHeader
import com.example.ui.components.LedgerTextField
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.StatusChip
import com.example.ui.components.SummaryRow
import com.example.ui.theme.ClutchIcons

@Composable
fun WeeklyScreen(
    uiState: ClutchUiState,
    onSaveNote: (ageWeek: Int, notes: String, actions: String) -> Unit,
    onRecordToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    val weeks = dashboard?.weeklySummaries.orEmpty()
    if (dashboard == null || weeks.isEmpty()) {
        ScreenColumn(modifier = modifier, testTag = "weekly_screen") {
            ScreenTitle(title = "Weekly summary")
            EmptyStateCard(
                icon = ClutchIcons.Weekly,
                title = "No weeks yet",
                body = "Once you record a few days, each week's eggs, feed, deaths and production will add up here.",
                primaryLabel = "Record today's count",
                primaryIcon = ClutchIcons.Record,
                onPrimary = onRecordToday,
                primaryTestTag = "weekly_record_today"
            )
        }
        return
    }

    var editingWeek by remember { mutableStateOf<Int?>(null) }
    var expandedWeek by rememberSaveable(dashboard.flock.id) { mutableStateOf(weeks.first().ageWeek) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ClutchDimens.ContentMaxWidth)
                .fillMaxWidth(),
            contentPadding = PaddingValues(ClutchDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenTitle(
                    title = "Weekly summary",
                    subtitle = "${dashboard.flock.name} • ${weeks.size} ${if (weeks.size == 1) "week" else "weeks"}, newest first"
                )
            }

            items(weeks, key = { it.ageWeek }) { week ->
                WeekCard(
                    week = week,
                    note = dashboard.weeklyNotes[week.ageWeek],
                    expanded = expandedWeek == week.ageWeek,
                    onToggle = { expandedWeek = if (expandedWeek == week.ageWeek) -1 else week.ageWeek },
                    onEditNotes = { editingWeek = week.ageWeek }
                )
            }
        }
    }

    editingWeek?.let { ageWeek ->
        WeeklyNoteSheet(
            ageWeek = ageWeek,
            existing = dashboard.weeklyNotes[ageWeek],
            onDismiss = { editingWeek = null },
            onSave = { notes, actions ->
                editingWeek = null
                onSaveNote(ageWeek, notes, actions)
            }
        )
    }
}

private fun dateRange(start: String, end: String): String =
    if (start == end) LayerCalc.friendlyDate(start) else "${LayerCalc.friendlyDate(start)} – ${LayerCalc.friendlyDate(end)}"

@Composable
private fun WeekCard(
    week: WeeklyCalc.WeekSummary,
    note: WeeklyNoteEntity?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditNotes: () -> Unit
) {
    ClutchCard(modifier = Modifier.testTag("week_summary_${week.ageWeek}")) {
        ExpandableHeader(
            title = "Week ${week.ageWeek} of age",
            subtitle = if (expanded) {
                "${dateRange(week.startDate, week.endDate)} • ${week.daysRecorded} of 7 days"
            } else {
                "${LayerCalc.format1(week.henDayPct)}% laying • ${week.totalEggs} eggs • ${week.deaths} died"
            },
            expanded = expanded,
            onToggle = onToggle
        )

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(state = week.henDayStatus)
                    if (week.daysRecorded < 7) {
                        Text(
                            text = "${7 - week.daysRecorded} ${if (7 - week.daysRecorded == 1) "day" else "days"} missing",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                WeekGroup(title = "Totals", icon = ClutchIcons.Layers) {
                    SummaryRow(label = "Eggs collected", caption = LayerCalc.formatCratesHelper(week.totalEggs), value = "${week.totalEggs}")
                    SummaryRow(
                        label = "Feed used",
                        caption = "About ${LayerCalc.format1(week.totalFeedKg / 50.0)} bags of 50 kg",
                        value = "${LayerCalc.format1(week.totalFeedKg)} kg"
                    )
                    SummaryRow(
                        label = "Deaths",
                        caption = "${LayerCalc.format2(week.mortalityPct)}% of the flock" + if (week.culls > 0) " • ${week.culls} removed" else "",
                        value = "${week.deaths}"
                    )
                    SummaryRow(label = "Average live flock", value = LayerCalc.format0(week.avgLiveHens))
                }

                WeekGroup(title = "Production and feed efficiency", icon = ClutchIcons.Dashboard) {
                    SummaryRow(label = "Hen-day production", value = "${LayerCalc.format1(week.henDayPct)}%")
                    SummaryRow(label = "Hen-housed production", value = "${LayerCalc.format1(week.henHousedPct)}%")
                    SummaryRow(label = "Feed per bird per day", value = "${LayerCalc.format0(week.feedPerBirdG)} g")
                    SummaryRow(
                        label = "FCR",
                        caption = week.avgEggWeightG?.let { "kg feed per kg eggs • eggs ${LayerCalc.format1(it)} g" } ?: "Weigh eggs to see FCR",
                        value = week.fcr?.let { LayerCalc.format2(it) } ?: "—"
                    )
                    SummaryRow(
                        label = "Feed per crate",
                        caption = "kg of feed for 30 eggs",
                        value = week.feedPerCrateKg?.let { "${LayerCalc.format2(it)} kg" } ?: "—"
                    )
                    SummaryRow(label = "Bad eggs", caption = "cracked, dirty, soft", value = "${LayerCalc.format1(week.badEggPct)}%")
                }

                if (week.bodyWeightG != null || week.uniformityPct != null) {
                    WeekGroup(title = "Weigh-in", icon = ClutchIcons.Weight) {
                        SummaryRow(label = "Body weight", value = week.bodyWeightG?.let { "${LayerCalc.format2(it / 1000.0)} kg" } ?: "—")
                        SummaryRow(label = "Uniformity", value = week.uniformityPct?.let { "${LayerCalc.format0(it)}%" } ?: "—")
                    }
                }

                NotesBlock(note = note, onEditNotes = onEditNotes, ageWeek = week.ageWeek)
            }
        }
    }
}

@Composable
private fun WeekGroup(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NotesBlock(note: WeeklyNoteEntity?, ageWeek: Int, onEditNotes: () -> Unit) {
    val hasNotes = note != null && (note.notes.isNotBlank() || note.actions.isNotBlank())
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasNotes) {
            NoteLine(icon = ClutchIcons.Notes, title = "What happened", body = note?.notes.orEmpty().ifBlank { "—" })
            NoteLine(icon = ClutchIcons.Actions, title = "Actions taken", body = note?.actions.orEmpty().ifBlank { "—" })
        }
        OutlinedButton(
            onClick = onEditNotes,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("edit_week_notes_$ageWeek")
        ) {
            Icon(
                imageVector = if (hasNotes) ClutchIcons.Edit else ClutchIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (hasNotes) "Edit notes and actions" else "Add notes and actions", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NoteLine(icon: ImageVector, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(14.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyNoteSheet(
    ageWeek: Int,
    existing: WeeklyNoteEntity?,
    onDismiss: () -> Unit,
    onSave: (notes: String, actions: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var notesText by rememberSaveable(ageWeek) { mutableStateOf(existing?.notes.orEmpty()) }
    var actionsText by rememberSaveable(ageWeek) { mutableStateOf(existing?.actions.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("weekly_note_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Week $ageWeek notes",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LedgerTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = "What happened this week",
                helperText = "E.g. vaccinated, changed feed, very hot days, laying dropped",
                errorText = null,
                keyboardType = KeyboardType.Text,
                singleLine = false,
                testTag = "input_week_notes"
            )
            LedgerTextField(
                value = actionsText,
                onValueChange = { actionsText = it },
                label = "What we did or will do",
                helperText = "Corrective actions, e.g. flushed drinkers, called the vet",
                errorText = null,
                keyboardType = KeyboardType.Text,
                singleLine = false,
                testTag = "input_week_actions"
            )
            ClutchPrimaryButton(
                text = "Save notes",
                icon = ClutchIcons.Save,
                onClick = { onSave(notesText, actionsText) },
                testTag = "save_week_notes"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
