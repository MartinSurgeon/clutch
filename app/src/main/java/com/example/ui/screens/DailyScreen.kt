package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.RecordWithMetrics
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchDimens
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.components.SummaryRow
import com.example.ui.theme.ClutchIcons

@Composable
fun DailyScreen(
    uiState: ClutchUiState,
    onRecordDay: (String) -> Unit,
    onAddFirstFlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    if (dashboard == null) {
        ScreenColumn(modifier = modifier, testTag = "daily_screen") {
            ScreenTitle(title = "Daily records")
            EmptyStateCard(
                icon = ClutchIcons.Pens,
                title = "Add a pen first",
                body = "Tell Clutch about your birds once. Then record eggs, feed and deaths every day.",
                primaryLabel = "Add a pen",
                primaryIcon = ClutchIcons.Add,
                onPrimary = onAddFirstFlock,
                primaryTestTag = "record_goto_flocks_button"
            )
        }
        return
    }

    if (dashboard.allRecordsAsc.isEmpty()) {
        ScreenColumn(modifier = modifier, testTag = "daily_screen") {
            ScreenTitle(title = "Daily records", subtitle = dashboard.flock.name)
            EmptyStateCard(
                icon = ClutchIcons.Daily,
                title = "No days recorded yet",
                body = "Record live hens, eggs, deaths and feed each day. Hen-day, hen-housed, FCR and egg quality are worked out for you.",
                primaryLabel = "Record today's count",
                primaryIcon = ClutchIcons.Record,
                onPrimary = { onRecordDay(uiState.todayDate) },
                primaryTestTag = "daily_record_today_empty"
            )
        }
        return
    }

    var detailRecord by remember { mutableStateOf<RecordWithMetrics?>(null) }

    val groupedByWeek = remember(dashboard.allRecordsAsc) {
        dashboard.allRecordsAsc
            .sortedByDescending { LayerCalc.parseEpochDay(it.entity.date) }
            .groupBy { it.metrics.ageWeeks }
            .entries
            .sortedByDescending { it.key }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_screen"),
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
                    title = "Daily records",
                    subtitle = "${dashboard.allRecordsAsc.size} days for ${dashboard.flock.name}. Tap a day to see all the sums."
                )
            }

            item {
                if (dashboard.isTodayRecorded) {
                    ClutchSecondaryButton(
                        text = "Change today's record",
                        icon = ClutchIcons.Edit,
                        onClick = { onRecordDay(uiState.todayDate) },
                        testTag = "daily_change_today"
                    )
                } else {
                    ClutchPrimaryButton(
                        text = "Record today's count",
                        icon = ClutchIcons.Record,
                        onClick = { onRecordDay(uiState.todayDate) },
                        testTag = "daily_record_today"
                    )
                }
            }

            groupedByWeek.forEach { (ageWeek, records) ->
                item(key = "week_$ageWeek") {
                    WeekOfDaysCard(
                        ageWeek = ageWeek,
                        records = records,
                        onOpenDay = { detailRecord = it }
                    )
                }
            }
        }
    }

    detailRecord?.let { rec ->
        DayDetailSheet(
            record = rec,
            hensHoused = dashboard.flock.hensHoused,
            onDismiss = { detailRecord = null },
            onEdit = {
                detailRecord = null
                onRecordDay(rec.entity.date)
            }
        )
    }
}

@Composable
private fun WeekOfDaysCard(
    ageWeek: Int,
    records: List<RecordWithMetrics>,
    onOpenDay: (RecordWithMetrics) -> Unit
) {
    val weekEggs = records.sumOf { it.entity.eggs }
    ClutchCard(modifier = Modifier.testTag("week_group_$ageWeek")) {
        SectionHeader(
            title = "Week $ageWeek of age",
            subtitle = "${records.size} ${if (records.size == 1) "day" else "days"} • ${LayerCalc.formatCratesHelper(weekEggs)}"
        )
        Column {
            records.forEachIndexed { idx, rec ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DayRow(record = rec, onClick = { onOpenDay(rec) })
            }
        }
    }
}

@Composable
private fun DayRow(record: RecordWithMetrics, onClick: () -> Unit) {
    val e = record.entity
    val m = record.metrics
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(vertical = 8.dp)
            .testTag("trend_day_row_${e.date}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LayerCalc.friendlyDate(e.date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${e.eggs} eggs • ${e.deaths} died • ${e.feedKg} kg feed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${LayerCalc.format1(m.henDayPct)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "HH ${LayerCalc.format1(m.henHousedPct)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = ClutchIcons.ChevronRight,
            contentDescription = "See all numbers for ${LayerCalc.friendlyDate(e.date)}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * Every automatic calculation for one day, grouped like the workbook's daily sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    record: RecordWithMetrics,
    hensHoused: Int,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val e = record.entity
    val m = record.metrics
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("day_detail_sheet")
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LayerCalc.friendlyDate(e.date, withYear = true),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Week ${m.ageWeeks} of age",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(state = m.henDayStatus)
            }

            DetailGroup(title = "Production") {
                SummaryRow(label = "Hen-day %", caption = "${e.eggs} eggs ÷ ${e.liveHens} live hens", value = "${LayerCalc.format1(m.henDayPct)}%")
                SummaryRow(label = "Hen-housed %", caption = "${e.eggs} eggs ÷ $hensHoused stocked", value = "${LayerCalc.format1(m.henHousedPct)}%")
                SummaryRow(label = "Eggs collected", caption = LayerCalc.formatCratesHelper(e.eggs), value = "${e.eggs}")
            }

            DetailGroup(title = "Birds") {
                SummaryRow(label = "Live hens (start of day)", value = "${e.liveHens}")
                SummaryRow(label = "Died", value = "${e.deaths}")
                SummaryRow(label = "Sold or removed", value = "${e.culls}")
                SummaryRow(label = "Live hens (end of day)", value = "${m.closingHens}")
            }

            DetailGroup(title = "Feed and efficiency") {
                SummaryRow(label = "Feed given", value = "${e.feedKg} kg")
                SummaryRow(label = "Feed per bird", value = "${LayerCalc.format0(m.feedPerBirdG)} g")
                SummaryRow(
                    label = "Average egg weight",
                    caption = m.eggWeightDateUsed?.takeIf { it != e.date }?.let { "From ${LayerCalc.friendlyDate(it)}" },
                    value = m.effectiveEggWeightG?.let { "${LayerCalc.format1(it)} g" } ?: "—"
                )
                SummaryRow(label = "Egg mass", caption = "per bird", value = m.eggMassG?.let { "${LayerCalc.format1(it)} g" } ?: "—")
                SummaryRow(label = "FCR", caption = "kg feed per kg eggs", value = m.fcr?.let { LayerCalc.format2(it) } ?: "—")
            }

            DetailGroup(title = "Egg quality") {
                SummaryRow(label = "Cracked", caption = "${e.cracked} eggs", value = "${LayerCalc.format1(m.crackedPct)}%")
                SummaryRow(label = "Dirty", caption = "${e.dirty} eggs", value = "${LayerCalc.format1(m.dirtyPct)}%")
                SummaryRow(label = "Soft-shell", caption = "${e.softShell} eggs", value = "${LayerCalc.format1(m.softShellPct)}%")
            }

            if (e.bodyWeightG != null || e.uniformityPct != null) {
                DetailGroup(title = "Weigh-in") {
                    SummaryRow(label = "Body weight", value = e.bodyWeightG?.let { "${LayerCalc.format0(it)} g" } ?: "—")
                    SummaryRow(label = "Uniformity", value = e.uniformityPct?.let { "${LayerCalc.format0(it)}%" } ?: "—")
                }
            }

            if (e.notes.isNotBlank()) {
                DetailGroup(title = "Notes") {
                    Text(text = e.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            ClutchPrimaryButton(
                text = "Change this day",
                icon = ClutchIcons.Edit,
                onClick = onEdit,
                testTag = "day_detail_edit"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
