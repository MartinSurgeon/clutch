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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchDimens
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IconBadge
import com.example.ui.components.LedgerTextField
import com.example.ui.components.NumberStepper
import com.example.ui.components.ScreenColumn
import com.example.ui.components.StatusChip
import kotlinx.coroutines.launch
import kotlin.math.floor

private const val EGGS_PER_CRATE = 30

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
        notes: String,
        onFailed: () -> Unit
    ) -> Unit,
    onDeleteRecord: (flock: FlockEntity, date: String) -> Unit,
    onAddFirstFlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.activeDashboard
    if (dashboard == null) {
        ScreenColumn(modifier = modifier, testTag = "record_screen") {
            EmptyStateCard(
                icon = Icons.Outlined.Layers,
                title = "Add a pen first",
                body = "Tell Clutch about your birds once. Then you can record eggs, feed and water every day.",
                primaryLabel = "Add a pen",
                primaryIcon = Icons.Default.Add,
                onPrimary = onAddFirstFlock,
                primaryTestTag = "record_goto_flocks_button"
            )
        }
        return
    }

    val selectedDate = uiState.selectedRecordDate
    val existingRecord = dashboard.existingRecordForDate(selectedDate)
    val defaultLiveHens = dashboard.defaultStartLiveHensForDate(selectedDate)
    val formKey = dashboard.flock.id + selectedDate

    var liveHensText by rememberSaveable(formKey) {
        mutableStateOf((existingRecord?.liveHens ?: defaultLiveHens).toString())
    }
    var deathsText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.deaths?.toString() ?: "0")
    }
    var cullsText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.culls?.toString() ?: "0")
    }
    var cratesText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.eggs?.let { (it / EGGS_PER_CRATE).toString() } ?: "")
    }
    var looseEggsText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.eggs?.let { (it % EGGS_PER_CRATE).toString() } ?: "")
    }
    var crackedText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.cracked?.toString() ?: "0")
    }
    var dirtyText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.dirty?.toString() ?: "0")
    }
    var feedKgText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.feedKg?.toString() ?: "")
    }
    var waterLText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.waterL?.toString() ?: "")
    }
    var softShellText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.softShell?.toString() ?: "0")
    }
    var eggWeightText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.avgEggWeightG?.toString() ?: "")
    }
    var maxTempText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.maxTempC?.toString() ?: "")
    }
    var notesText by rememberSaveable(formKey) {
        mutableStateOf(existingRecord?.notes ?: "")
    }

    var expandOptional by rememberSaveable(formKey) {
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
            cratesText = (existingRecord.eggs / EGGS_PER_CRATE).toString()
            looseEggsText = (existingRecord.eggs % EGGS_PER_CRATE).toString()
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

    var saveAttempted by rememberSaveable(formKey) { mutableStateOf(false) }
    var liveHensTouched by rememberSaveable(formKey) { mutableStateOf(false) }
    var eggsTouched by rememberSaveable(formKey) { mutableStateOf(false) }
    var isSaving by remember(formKey) { mutableStateOf(false) }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Parsed numbers
    val liveHensVal = liveHensText.trim().toIntOrNull() ?: 0
    val deathsVal = deathsText.trim().toIntOrNull() ?: 0
    val cullsVal = cullsText.trim().toIntOrNull() ?: 0
    val eggsVal: Int? = if (cratesText.isBlank() && looseEggsText.isBlank()) {
        null
    } else {
        val crates = cratesText.trim().toIntOrNull()
        val loose = looseEggsText.trim().toIntOrNull()
        if ((cratesText.isNotBlank() && crates == null) || (looseEggsText.isNotBlank() && loose == null)) {
            -1
        } else {
            (crates ?: 0) * EGGS_PER_CRATE + (loose ?: 0)
        }
    }
    val crackedVal = crackedText.trim().toIntOrNull() ?: 0
    val dirtyVal = dirtyText.trim().toIntOrNull() ?: 0
    val softShellVal = softShellText.trim().toIntOrNull() ?: 0
    val feedKgVal = feedKgText.trim().toDoubleOrNull() ?: 0.0
    val waterLVal = waterLText.trim().toDoubleOrNull() ?: 0.0
    val eggWeightVal = eggWeightText.trim().toDoubleOrNull()
    val maxTempVal = maxTempText.trim().toDoubleOrNull()

    // Validation rules, worded as what to do next
    val liveHensError: String? = when {
        liveHensText.isBlank() || liveHensVal <= 0 -> "Enter how many birds you had this morning."
        else -> null
    }
    val deathsCullsError: String? = when {
        liveHensVal > 0 && (deathsVal + cullsVal) > liveHensVal ->
            "Dead and removed birds ($deathsVal + $cullsVal) can't be more than the $liveHensVal birds you had."
        else -> null
    }
    val maxAllowedEggs = floor(liveHensVal * 1.05).toInt()
    val eggsError: String? = when {
        eggsVal == null -> "Enter the eggs you picked today. Put 0 if none."
        eggsVal < 0 -> "Use whole numbers only for crates and eggs."
        liveHensVal > 0 && eggsVal > maxAllowedEggs ->
            "$eggsVal eggs is more than $liveHensVal birds can lay. Please check the count."
        else -> null
    }
    val totalDefects = crackedVal + dirtyVal + softShellVal
    val defectsError: String? = when {
        eggsVal != null && eggsVal >= 0 && totalDefects > eggsVal ->
            "Bad eggs ($totalDefects) can't be more than all eggs picked ($eggsVal)."
        else -> null
    }
    val allErrors = listOfNotNull(liveHensError, deathsCullsError, eggsError, defectsError)

    // Live preview calculations
    val previewEggs = eggsVal?.coerceAtLeast(0) ?: 0
    val previewHenDay = LayerCalc.henDayPercent(previewEggs, liveHensVal)
    val previewFeedG = LayerCalc.feedPerBirdGrams(feedKgVal, liveHensVal)
    val previewStatus = remember(previewHenDay, selectedDate) {
        val age = LayerCalc.ageInWeeks(dashboard.flock.ageWeeksAtPlacement, dashboard.flock.placedDate, selectedDate)
        LayerCalc.evaluateHenDayTarget(previewHenDay, age)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("record_screen")
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ClutchDimens.ContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(ClutchDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(ClutchDimens.SectionGap)
            ) {
                DateSelector(
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

                if (saveAttempted && allErrors.isNotEmpty()) {
                    ErrorSummaryCard(errors = allErrors)
                }

                // 1. Eggs — what the farmer counts first
                FormSection(
                    icon = Icons.Outlined.Egg,
                    heading = "Eggs picked",
                    subtitle = "Count full crates, then the loose eggs left over"
                ) {
                    val showEggsError = (saveAttempted || eggsTouched) && eggsError != null
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LedgerTextField(
                            value = cratesText,
                            onValueChange = { cratesText = it.filter { c -> c.isDigit() } },
                            label = "Full crates",
                            placeholder = "0",
                            helperText = "30 eggs each",
                            errorText = null,
                            onBlur = { eggsTouched = true },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            testTag = "input_crates"
                        )
                        LedgerTextField(
                            value = looseEggsText,
                            onValueChange = { looseEggsText = it.filter { c -> c.isDigit() } },
                            label = "Loose eggs",
                            placeholder = "0",
                            helperText = "Not in a full crate",
                            errorText = null,
                            onBlur = { eggsTouched = true },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            testTag = "input_eggs"
                        )
                    }
                    TotalLine(
                        text = when {
                            showEggsError -> eggsError.orEmpty()
                            eggsVal != null && eggsVal >= 0 -> "Total: $eggsVal eggs"
                            else -> "Total will show here"
                        },
                        isError = showEggsError
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    NumberStepper(
                        label = "Cracked",
                        helperText = "Keep under 2%",
                        value = crackedText,
                        onValueChange = { crackedText = it },
                        isError = saveAttempted && defectsError != null,
                        testTag = "input_cracked"
                    )
                    NumberStepper(
                        label = "Dirty",
                        helperText = "Keep under 3%",
                        value = dirtyText,
                        onValueChange = { dirtyText = it },
                        isError = saveAttempted && defectsError != null,
                        testTag = "input_dirty"
                    )
                }

                // 2. Birds
                FormSection(
                    icon = Icons.Outlined.Pets,
                    heading = "Birds",
                    subtitle = "We filled this in from your last count"
                ) {
                    LedgerTextField(
                        value = liveHensText,
                        onValueChange = { liveHensText = it.filter { c -> c.isDigit() } },
                        label = "Birds this morning",
                        helperText = "Last count was $defaultLiveHens birds",
                        errorText = if (saveAttempted || liveHensTouched) liveHensError else null,
                        onBlur = { liveHensTouched = true },
                        keyboardType = KeyboardType.Number,
                        testTag = "input_live_hens"
                    )
                    NumberStepper(
                        label = "Died today",
                        value = deathsText,
                        onValueChange = { deathsText = it },
                        isError = deathsCullsError != null,
                        testTag = "input_deaths"
                    )
                    NumberStepper(
                        label = "Sold or removed",
                        helperText = "Culled, sold or given away",
                        value = cullsText,
                        onValueChange = { cullsText = it },
                        isError = deathsCullsError != null,
                        testTag = "input_culls"
                    )
                    if (deathsCullsError != null) {
                        TotalLine(text = deathsCullsError, isError = true)
                    }
                }

                // 3. Feed and water
                FormSection(
                    icon = Icons.Outlined.Grass,
                    heading = "Feed and water",
                    subtitle = "What you gave the birds today"
                ) {
                    LedgerTextField(
                        value = feedKgText,
                        onValueChange = { feedKgText = it },
                        label = "Feed given",
                        suffix = "kg",
                        helperText = "One full bag is 50 kg",
                        errorText = null,
                        keyboardType = KeyboardType.Decimal,
                        testTag = "input_feed_kg"
                    )
                    LedgerTextField(
                        value = waterLText,
                        onValueChange = { waterLText = it },
                        label = "Water used",
                        suffix = "litres",
                        helperText = "One Kufuor gallon holds about 25 litres",
                        errorText = null,
                        keyboardType = KeyboardType.Decimal,
                        testTag = "input_water_l"
                    )
                }

                // 4. Extra details, folded away (Tesler's Law)
                ExtraDetailsSection(
                    expanded = expandOptional,
                    onToggle = { expandOptional = !expandOptional }
                ) {
                    NumberStepper(
                        label = "Soft-shell eggs",
                        helperText = "Keep under 1%",
                        value = softShellText,
                        onValueChange = { softShellText = it },
                        isError = saveAttempted && defectsError != null,
                        testTag = "input_soft_shell"
                    )
                    LedgerTextField(
                        value = eggWeightText,
                        onValueChange = { eggWeightText = it },
                        label = "Average egg weight",
                        suffix = "g",
                        helperText = "Weigh 30 eggs, divide by 30. Once a week is enough.",
                        errorText = null,
                        keyboardType = KeyboardType.Decimal,
                        testTag = "input_egg_weight"
                    )
                    LedgerTextField(
                        value = maxTempText,
                        onValueChange = { maxTempText = it },
                        label = "Hottest it got in the pen",
                        suffix = "°C",
                        helperText = "From a thermometer in the pen",
                        errorText = null,
                        keyboardType = KeyboardType.Decimal,
                        testTag = "input_max_temp"
                    )
                    LedgerTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = "Notes",
                        helperText = "E.g. vaccinated today, new feed bags, birds panting",
                        errorText = null,
                        keyboardType = KeyboardType.Text,
                        singleLine = false,
                        testTag = "input_notes"
                    )
                }

                if (existingRecord != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .minimumInteractiveComponentSize()
                            .testTag("delete_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete this day",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Pinned save bar: the one primary action is always within thumb reach (Fitts's Law)
        SaveBar(
            henDayPct = previewHenDay,
            henDayStatus = previewStatus,
            feedPerBirdG = previewFeedG,
            hasEggs = eggsVal != null && eggsVal >= 0,
            isEditing = existingRecord != null,
            isSaving = isSaving,
            onSave = {
                saveAttempted = true
                if (allErrors.isNotEmpty()) {
                    if (softShellVal > 0 && defectsError != null) {
                        expandOptional = true
                    }
                    scope.launch { scrollState.animateScrollTo(0) }
                    return@SaveBar
                }
                isSaving = true
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
                ) { isSaving = false }
            }
        )
    }

    if (showDeleteConfirm && existingRecord != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Delete ${LayerCalc.friendlyDate(selectedDate)}?") },
            text = {
                Text("The eggs, feed and bird counts for ${dashboard.flock.name} on this day will be removed. You can't undo this.")
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
                        text = "Yes, delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Keep it")
                }
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = LayerCalc.parseEpochDay(selectedDate) * 86_400_000L
        val todayMillis = LayerCalc.parseEpochDay(uiState.todayDate) * 86_400_000L
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86_400_000L
                            onDateChange(LayerCalc.formatEpochDay(epochDay))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Choose")
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
private fun DateSelector(
    selectedDate: String,
    todayDate: String,
    isExistingRecord: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenPicker: () -> Unit
) {
    val isToday = selectedDate == todayDate
    val isYesterday = LayerCalc.addDays(selectedDate, 1) == todayDate
    val dayName = when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> LayerCalc.friendlyDate(selectedDate)
    }
    ClutchCard(contentPadding = 6.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("date_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Day before"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onOpenPicker)
                    .heightIn(min = 48.dp)
                    .padding(vertical = 6.dp)
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isToday || isYesterday) {
                        "${LayerCalc.friendlyDate(selectedDate)} • ${if (isExistingRecord) "Changing saved day" else "New day"}"
                    } else {
                        if (isExistingRecord) "Changing saved day" else "New day"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextDay,
                enabled = !isToday,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("date_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Day after"
                )
            }
        }
    }
}

@Composable
private fun ErrorSummaryCard(errors: List<String>) {
    ClutchCard(
        modifier = Modifier.testTag("record_error_summary"),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = if (errors.size == 1) "Please fix 1 thing before saving" else "Please fix ${errors.size} things before saving",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        errors.forEach { error ->
            Text(
                text = "• $error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FormSection(
    icon: ImageVector,
    heading: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    ClutchCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = icon, size = 36.dp)
            Column {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

@Composable
private fun TotalLine(text: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("eggs_total_line")
        )
    }
}

@Composable
private fun ExtraDetailsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    ClutchCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .heightIn(min = 48.dp)
                .testTag("more_details_toggle"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Extra details (you can skip)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Soft-shell eggs, egg weight, heat, notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide extra details" else "Show extra details",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SaveBar(
    henDayPct: Double,
    henDayStatus: LayerCalc.TargetState,
    feedPerBirdG: Double,
    hasEggs: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = ClutchDimens.ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = ClutchDimens.ScreenPadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_preview_card"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewValue(
                        label = "Laying",
                        value = if (hasEggs) "${LayerCalc.format1(henDayPct)}%" else "—"
                    )
                    PreviewValue(
                        label = "Feed per bird",
                        value = if (feedPerBirdG > 0) "${LayerCalc.format0(feedPerBirdG)} g" else "—"
                    )
                    if (hasEggs) {
                        StatusChip(state = henDayStatus)
                    }
                }
                ClutchPrimaryButton(
                    text = when {
                        isSaving -> "Saving…"
                        isEditing -> "Save changes"
                        else -> "Save day"
                    },
                    icon = Icons.Default.Check,
                    loading = isSaving,
                    onClick = onSave,
                    testTag = "save_day_button"
                )
            }
        }
    }
}

@Composable
private fun PreviewValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
    }
}
