package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchPrimaryButton
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    uiState: ClutchUiState,
    onDateChange: (String) -> Unit,
    onSaveRecord: (
        flock: FlockEntity,
        date: String,
        liveHens: Int,
        deaths: Int,
        culls: Int,
        eggs: Int,
        cracked: Int,
        dirty: Int,
        softShell: Int,
        feedKg: Double,
        waterL: Double,
        avgEggWeightG: Double?,
        maxTempC: Double?,
        notes: String
    ) -> Unit,
    onDeleteRecord: (flock: FlockEntity, date: String) -> Unit,
    onNavigateToFlocks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    if (dashboard == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
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
                        text = "Add a flock before recording daily numbers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Once a flock is set up, you can log daily birds, eggs, feed, and water here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ClutchPrimaryButton(
                        text = "Go to Flocks",
                        onClick = onNavigateToFlocks,
                        testTag = "record_goto_flocks_button"
                    )
                }
            }
        }
        return
    }

    val selectedDate = uiState.selectedRecordDate
    val existingRecord = dashboard.existingRecordForDate(selectedDate)
    val defaultLiveHens = dashboard.defaultStartLiveHensForDate(selectedDate)

    var liveHensText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf((existingRecord?.liveHens ?: defaultLiveHens).toString())
    }
    var deathsText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.deaths?.toString() ?: "0")
    }
    var cullsText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.culls?.toString() ?: "0")
    }
    var eggsText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.eggs?.toString() ?: "")
    }
    var crackedText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.cracked?.toString() ?: "0")
    }
    var dirtyText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.dirty?.toString() ?: "0")
    }
    var feedKgText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.feedKg?.toString() ?: "")
    }
    var waterLText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.waterL?.toString() ?: "")
    }
    var softShellText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.softShell?.toString() ?: "0")
    }
    var eggWeightText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.avgEggWeightG?.toString() ?: "")
    }
    var maxTempText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.maxTempC?.toString() ?: "")
    }
    var notesText by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(existingRecord?.notes ?: "")
    }

    var expandOptional by rememberSaveable(dashboard.flock.id, selectedDate) {
        mutableStateOf(
            existingRecord != null && (
                existingRecord.softShell > 0 ||
                    existingRecord.avgEggWeightG != null ||
                    existingRecord.maxTempC != null ||
                    existingRecord.notes.isNotBlank()
                )
        )
    }

    // Sync state if database updates existing record for the current date
    LaunchedEffect(existingRecord, defaultLiveHens) {
        if (existingRecord != null) {
            liveHensText = existingRecord.liveHens.toString()
            deathsText = existingRecord.deaths.toString()
            cullsText = existingRecord.culls.toString()
            eggsText = existingRecord.eggs.toString()
            crackedText = existingRecord.cracked.toString()
            dirtyText = existingRecord.dirty.toString()
            feedKgText = existingRecord.feedKg.toString()
            waterLText = existingRecord.waterL.toString()
            softShellText = existingRecord.softShell.toString()
            eggWeightText = existingRecord.avgEggWeightG?.toString() ?: ""
            maxTempText = existingRecord.maxTempC?.toString() ?: ""
            notesText = existingRecord.notes
        }
    }

    var saveAttempted by rememberSaveable(dashboard.flock.id, selectedDate) { mutableStateOf(false) }
    var liveHensTouched by rememberSaveable(dashboard.flock.id, selectedDate) { mutableStateOf(false) }
    var eggsTouched by rememberSaveable(dashboard.flock.id, selectedDate) { mutableStateOf(false) }
    var deathsCullsTouched by rememberSaveable(dashboard.flock.id, selectedDate) { mutableStateOf(false) }
    var defectsTouched by rememberSaveable(dashboard.flock.id, selectedDate) { mutableStateOf(false) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Parsed numbers
    val liveHensVal = liveHensText.trim().toIntOrNull() ?: 0
    val deathsVal = deathsText.trim().toIntOrNull() ?: 0
    val cullsVal = cullsText.trim().toIntOrNull() ?: 0
    val eggsVal = eggsText.trim().toIntOrNull()
    val crackedVal = crackedText.trim().toIntOrNull() ?: 0
    val dirtyVal = dirtyText.trim().toIntOrNull() ?: 0
    val softShellVal = softShellText.trim().toIntOrNull() ?: 0
    val feedKgVal = feedKgText.trim().toDoubleOrNull() ?: 0.0
    val waterLVal = waterLText.trim().toDoubleOrNull() ?: 0.0
    val eggWeightVal = eggWeightText.trim().toDoubleOrNull()
    val maxTempVal = maxTempText.trim().toDoubleOrNull()

    // Validation rules
    val liveHensError: String? = when {
        liveHensText.isBlank() || liveHensVal <= 0 -> "Live hens must be over 0."
        else -> null
    }
    val deathsCullsError: String? = when {
        deathsVal < 0 || cullsVal < 0 -> "Deaths and culls cannot be negative."
        liveHensVal > 0 && (deathsVal + cullsVal) > liveHensVal ->
            "Deaths ($deathsVal) + culls ($cullsVal) cannot exceed live hens ($liveHensVal)."
        else -> null
    }
    val maxAllowedEggs = floor(liveHensVal * 1.05).toInt()
    val eggsError: String? = when {
        eggsText.isBlank() || eggsVal == null -> "Eggs collected is required."
        eggsVal < 0 -> "Eggs cannot be negative."
        liveHensVal > 0 && eggsVal > maxAllowedEggs ->
            "Eggs ($eggsVal) cannot exceed 105% of live hens ($maxAllowedEggs max)."
        else -> null
    }
    val totalDefects = crackedVal + dirtyVal + softShellVal
    val defectsError: String? = when {
        crackedVal < 0 || dirtyVal < 0 || softShellVal < 0 -> "Defect counts cannot be negative."
        eggsVal != null && totalDefects > eggsVal ->
            "Cracked + dirty + soft-shell ($totalDefects) cannot exceed total eggs ($eggsVal)."
        else -> null
    }

    // Live preview calculations
    val previewEggs = eggsVal ?: 0
    val previewHenDay = LayerCalc.henDayPercent(previewEggs, liveHensVal)
    val previewFeedG = LayerCalc.feedPerBirdGrams(feedKgVal, liveHensVal)
    val recentWeight = remember(selectedDate, dashboard.allRecordsAsc) {
        LayerCalc.resolveEggWeight(
            selectedDate,
            dashboard.allRecordsAsc.map { it.input }
        )?.first
    }
    val effectivePreviewWeight = eggWeightVal ?: recentWeight
    val previewFcr = LayerCalc.feedConversionRatio(feedKgVal, previewEggs, effectivePreviewWeight)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selector Card at top
            DateSelectorCard(
                flockName = dashboard.flock.name,
                selectedDate = selectedDate,
                todayDate = uiState.todayDate,
                isExistingRecord = existingRecord != null,
                onPreviousDay = { onDateChange(LayerCalc.addDays(selectedDate, -1)) },
                onNextDay = {
                    val next = LayerCalc.addDays(selectedDate, 1)
                    if (LayerCalc.parseEpochDay(next) <= LayerCalc.parseEpochDay(uiState.todayDate)) {
                        onDateChange(next)
                    }
                },
                onOpenPicker = { showDatePicker = true }
            )

            // 1. Card "Birds"
            FormSectionCard(heading = "Birds") {
                LedgerTextField(
                    value = liveHensText,
                    onValueChange = { liveHensText = it },
                    label = "Live hens at start of day *",
                    helperText = "Pre-filled from yesterday's closing count ($defaultLiveHens hens)",
                    errorText = if (saveAttempted || liveHensTouched) liveHensError else null,
                    onBlur = { liveHensTouched = true },
                    keyboardType = KeyboardType.Number,
                    testTag = "input_live_hens"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LedgerTextField(
                        value = deathsText,
                        onValueChange = { deathsText = it },
                        label = "Died today",
                        helperText = "Mortality count",
                        errorText = if (saveAttempted || deathsCullsTouched) deathsCullsError else null,
                        onBlur = { deathsCullsTouched = true },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "input_deaths"
                    )
                    LedgerTextField(
                        value = cullsText,
                        onValueChange = { cullsText = it },
                        label = "Culled or sold",
                        helperText = "Removed birds",
                        errorText = null,
                        onBlur = { deathsCullsTouched = true },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "input_culls"
                    )
                }
            }

            // 2. Card "Eggs"
            FormSectionCard(heading = "Eggs") {
                val cratesHelper = if (eggsVal != null && eggsVal >= 0) {
                    LayerCalc.formatCratesHelper(eggsVal)
                } else {
                    "Enter total eggs collected today (30 eggs per crate)"
                }

                LedgerTextField(
                    value = eggsText,
                    onValueChange = { eggsText = it },
                    label = "Eggs collected *",
                    helperText = cratesHelper,
                    errorText = if (saveAttempted || eggsTouched) eggsError else null,
                    onBlur = { eggsTouched = true },
                    keyboardType = KeyboardType.Number,
                    testTag = "input_eggs"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LedgerTextField(
                        value = crackedText,
                        onValueChange = { crackedText = it },
                        label = "Cracked",
                        helperText = "Target under 2%",
                        errorText = if (saveAttempted || defectsTouched) defectsError else null,
                        onBlur = { defectsTouched = true },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "input_cracked"
                    )
                    LedgerTextField(
                        value = dirtyText,
                        onValueChange = { dirtyText = it },
                        label = "Dirty",
                        helperText = "Target under 3%",
                        errorText = null,
                        onBlur = { defectsTouched = true },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "input_dirty"
                    )
                }
            }

            // 3. Card "Feed and water"
            FormSectionCard(heading = "Feed and water") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LedgerTextField(
                        value = feedKgText,
                        onValueChange = { feedKgText = it },
                        label = "Feed given (kg)",
                        helperText = "Target 110–125 g/bird",
                        errorText = null,
                        onBlur = {},
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        testTag = "input_feed_kg"
                    )
                    LedgerTextField(
                        value = waterLText,
                        onValueChange = { waterLText = it },
                        label = "Water used (litres)",
                        helperText = "Total drunk today",
                        errorText = null,
                        onBlur = {},
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        testTag = "input_water_l"
                    )
                }
            }

            // 4. Collapsible "More details (optional)"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable(role = Role.Button) { expandOptional = !expandOptional }
                            .testTag("more_details_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "More details (optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Soft-shell, egg weight, house temperature, notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (expandOptional) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expandOptional) "Collapse optional details" else "Expand optional details",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = expandOptional) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LedgerTextField(
                                    value = softShellText,
                                    onValueChange = { softShellText = it },
                                    label = "Soft-shell eggs",
                                    helperText = "Target under 1%",
                                    errorText = if (saveAttempted || defectsTouched) defectsError else null,
                                    onBlur = { defectsTouched = true },
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f),
                                    testTag = "input_soft_shell"
                                )
                                LedgerTextField(
                                    value = eggWeightText,
                                    onValueChange = { eggWeightText = it },
                                    label = "Avg egg weight (g)",
                                    helperText = "Weighed weekly (55–65 g)",
                                    errorText = null,
                                    onBlur = {},
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f),
                                    testTag = "input_egg_weight"
                                )
                            }

                            LedgerTextField(
                                value = maxTempText,
                                onValueChange = { maxTempText = it },
                                label = "Hottest temperature (°C)",
                                helperText = "Peak house temperature today",
                                errorText = null,
                                onBlur = {},
                                keyboardType = KeyboardType.Decimal,
                                testTag = "input_max_temp"
                            )

                            LedgerTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = "Notes",
                                helperText = "Observations on flock behavior, water, feed, or litter",
                                errorText = null,
                                onBlur = {},
                                keyboardType = KeyboardType.Text,
                                singleLine = false,
                                testTag = "input_notes"
                            )
                        }
                    }
                }
            }

            // 5. Live Preview Card of Hen-day, Feed per bird, and FCR
            LivePreviewCard(
                henDayPct = previewHenDay,
                feedPerBirdG = previewFeedG,
                fcr = previewFcr
            )

            // 6. Full-width primary "Save day" button at the bottom
            ClutchPrimaryButton(
                text = "Save day",
                icon = Icons.Default.Save,
                onClick = {
                    saveAttempted = true
                    if (liveHensError != null || deathsCullsError != null || eggsError != null || defectsError != null) {
                        if (softShellVal > 0 && defectsError != null) {
                            expandOptional = true
                        }
                        return@ClutchPrimaryButton
                    }
                    onSaveRecord(
                        dashboard.flock,
                        selectedDate,
                        liveHensVal,
                        deathsVal,
                        cullsVal,
                        eggsVal ?: 0,
                        crackedVal,
                        dirtyVal,
                        softShellVal,
                        feedKgVal,
                        waterLVal,
                        eggWeightVal,
                        maxTempVal,
                        notesText
                    )
                },
                testTag = "save_day_button"
            )

            // 7. Delete is a small text button with a confirmation dialog
            if (existingRecord != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Delete this day's record",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDeleteConfirm && existingRecord != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete record for $selectedDate?") },
            text = {
                Text("This will remove the daily numbers recorded for ${dashboard.flock.name} on $selectedDate.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteRecord(dashboard.flock, selectedDate)
                    },
                    modifier = Modifier.testTag("confirm_delete_day_button")
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = LayerCalc.parseEpochDay(selectedDate) * 86_400_000L
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86_400_000L
                            val chosen = LayerCalc.formatEpochDay(epochDay)
                            onDateChange(chosen)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateSelectorCard(
    flockName: String,
    selectedDate: String,
    todayDate: String,
    isExistingRecord: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenPicker: () -> Unit
) {
    val isToday = selectedDate == todayDate
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.testTag("date_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous day"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onOpenPicker)
                    .padding(vertical = 4.dp)
                    .testTag("date_picker_button"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isToday) "Today ($selectedDate)" else selectedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isExistingRecord) "$flockName • Editing saved record" else "$flockName • New entry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextDay,
                enabled = !isToday,
                modifier = Modifier.testTag("date_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day"
                )
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    heading: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helperText: String,
    errorText: String?,
    onBlur: () -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    testTag: String
) {
    var hadFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorText != null,
        supportingText = {
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("${testTag}_error")
                )
            } else {
                Text(text = helperText)
            }
        },
        singleLine = singleLine,
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

@Composable
private fun LivePreviewCard(
    henDayPct: Double,
    feedPerBirdG: Double,
    fcr: Double?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_preview_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PreviewMetricColumn(
                    label = "Hen-day %",
                    subtitle = "eggs / 100 hens",
                    value = "${LayerCalc.format1(henDayPct)}%",
                    modifier = Modifier.weight(1f)
                )
                PreviewMetricColumn(
                    label = "Feed / bird",
                    subtitle = "grams today",
                    value = "${LayerCalc.format0(feedPerBirdG)} g",
                    modifier = Modifier.weight(1f)
                )
                PreviewMetricColumn(
                    label = "FCR",
                    subtitle = "kg feed / kg eggs",
                    value = fcr?.let { LayerCalc.format2(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreviewMetricColumn(
    label: String,
    subtitle: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
