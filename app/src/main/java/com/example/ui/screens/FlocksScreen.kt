package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.FlockCardModel
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchDimens
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IconBadge
import com.example.ui.components.LedgerTextField
import com.example.ui.components.StatusChip

/** Common layer breeds in Ghana, kept to 7 so the choice stays quick (Miller's Law). */
val BREED_SUGGESTIONS = listOf(
    "Lohmann Brown",
    "ISA Brown",
    "Hy-Line Brown",
    "Bovans Brown",
    "Shaver Brown",
    "Dekalb White",
    "Local or mixed"
)

@Composable
fun FlocksScreen(
    uiState: ClutchUiState,
    showAddSheetInitial: Boolean = false,
    onAddSheetConsumed: () -> Unit = {},
    onOpenFlock: (String) -> Unit,
    onSaveFlock: (
        existingId: String?,
        name: String,
        breed: String,
        hensHoused: Int,
        ageWeeksAtPlacement: Int,
        placedDate: String
    ) -> Unit,
    onArchiveFlock: (FlockEntity, Boolean) -> Unit,
    onDeleteFlock: (FlockEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingFlock by remember { mutableStateOf<FlockEntity?>(null) }
    var showFormSheet by rememberSaveable { mutableStateOf(false) }
    var flockToDelete by remember { mutableStateOf<FlockEntity?>(null) }
    var showOldPens by rememberSaveable { mutableStateOf(false) }

    if (showAddSheetInitial && !showFormSheet) {
        editingFlock = null
        showFormSheet = true
        onAddSheetConsumed()
    }

    val activePens = uiState.flocks.filter { !it.flock.archived }
    val oldPens = uiState.flocks.filter { it.flock.archived }

    fun openAddSheet() {
        editingFlock = null
        showFormSheet = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("flocks_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ClutchDimens.ContentMaxWidth)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = ClutchDimens.ScreenPadding,
                end = ClutchDimens.ScreenPadding,
                top = ClutchDimens.ScreenPadding,
                // Room so the last card is never hidden behind the Add button
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.flocks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Layers,
                        title = "No pens yet",
                        body = "A pen is one group of birds you keep together, like \"Pen A\" or \"Layer House 1\". Add your first one to start.",
                        primaryLabel = "Add a pen",
                        primaryIcon = Icons.Default.Add,
                        onPrimary = ::openAddSheet,
                        primaryTestTag = "add_flock_empty_button"
                    )
                }
            } else {
                item {
                    Text(
                        text = "Tap a pen to see its numbers on Home.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(activePens, key = { it.flock.id }) { cardModel ->
                    PenCard(
                        model = cardModel,
                        isSelected = cardModel.flock.id == uiState.selectedFlockId,
                        onOpen = { onOpenFlock(cardModel.flock.id) },
                        onEdit = {
                            editingFlock = cardModel.flock
                            showFormSheet = true
                        },
                        onToggleArchive = { onArchiveFlock(cardModel.flock, true) },
                        onDelete = { flockToDelete = cardModel.flock }
                    )
                }

                if (oldPens.isNotEmpty()) {
                    item(key = "old_pens_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { showOldPens = !showOldPens }
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 4.dp)
                                .testTag("old_pens_toggle"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Old pens (${oldPens.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = "Birds sold off or finished laying. Records are kept.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showOldPens) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showOldPens) "Hide old pens" else "Show old pens",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showOldPens) {
                        items(oldPens, key = { it.flock.id }) { cardModel ->
                            PenCard(
                                model = cardModel,
                                isSelected = cardModel.flock.id == uiState.selectedFlockId,
                                onOpen = { onOpenFlock(cardModel.flock.id) },
                                onEdit = {
                                    editingFlock = cardModel.flock
                                    showFormSheet = true
                                },
                                onToggleArchive = { onArchiveFlock(cardModel.flock, false) },
                                onDelete = { flockToDelete = cardModel.flock }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.flocks.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = ::openAddSheet,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add a pen", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ClutchDimens.ScreenPadding)
                    .testTag("add_flock_button")
            )
        }
    }

    if (showFormSheet) {
        FlockFormBottomSheet(
            initialFlock = editingFlock,
            todayDate = uiState.todayDate,
            onDismiss = { showFormSheet = false },
            onSave = { existingId, name, breed, hensHoused, ageWeeks, placedDate ->
                showFormSheet = false
                onSaveFlock(existingId, name, breed, hensHoused, ageWeeks, placedDate)
            }
        )
    }

    flockToDelete?.let { flock ->
        AlertDialog(
            onDismissRequest = { flockToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Remove ${flock.name}?") },
            text = { Text("This pen has no records yet, so nothing else will be lost. You can't undo this.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        flockToDelete = null
                        onDeleteFlock(flock)
                    },
                    modifier = Modifier.testTag("confirm_delete_flock_button")
                ) {
                    Text("Yes, remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { flockToDelete = null }) { Text("Keep it") }
            }
        )
    }
}

@Composable
private fun PenCard(
    model: FlockCardModel,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val flock = model.flock
    var menuExpanded by remember { mutableStateOf(false) }

    ClutchCard(
        modifier = Modifier.testTag("flock_card_${flock.id}"),
        onClick = onOpen,
        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = Icons.Outlined.Layers)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = flock.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected && !flock.archived) {
                        PenBadge(text = "On Home", highlighted = true)
                    } else if (flock.archived) {
                        PenBadge(text = "Old", highlighted = false)
                    }
                }
                Text(
                    text = "${flock.breed} • Stocked ${LayerCalc.friendlyDate(flock.placedDate, withYear = true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("flock_overflow_${flock.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options for ${flock.name}"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit pen") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        modifier = Modifier.testTag("menu_edit_flock_${flock.id}")
                    )
                    DropdownMenuItem(
                        text = { Text(if (flock.archived) "Bring back" else "Move to old pens") },
                        leadingIcon = {
                            Icon(
                                if (flock.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleArchive()
                        },
                        modifier = Modifier.testTag("menu_archive_flock_${flock.id}")
                    )
                    // A pen with records can only be moved to old pens, never deleted
                    if (model.recordCount == 0) {
                        DropdownMenuItem(
                            text = { Text("Remove pen", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            modifier = Modifier.testTag("menu_delete_flock_${flock.id}")
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PenStat(label = "Birds", value = "${model.hensAlive}", modifier = Modifier.weight(1f))
            PenStat(label = "Age", value = "${model.ageWeeksToday} wks", modifier = Modifier.weight(1f))
            PenStat(
                label = "Laying",
                value = model.lastHenDayPct?.let { "${LayerCalc.format1(it)}%" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            model.lastHenDayStatus?.let { StatusChip(state = it) }
            Text(
                text = model.lastRecordedDate?.let { "Last count: ${LayerCalc.friendlyDate(it)}" } ?: "No counts yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PenBadge(text: String, highlighted: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (highlighted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PenStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlockFormBottomSheet(
    initialFlock: FlockEntity?,
    todayDate: String,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        name: String,
        breed: String,
        hensHoused: Int,
        ageWeeksAtPlacement: Int,
        placedDate: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameText by rememberSaveable(initialFlock?.id) {
        mutableStateOf(initialFlock?.name ?: "")
    }
    var breedText by rememberSaveable(initialFlock?.id) {
        mutableStateOf(initialFlock?.breed ?: "Lohmann Brown")
    }
    var hensHousedText by rememberSaveable(initialFlock?.id) {
        mutableStateOf(initialFlock?.hensHoused?.toString() ?: "")
    }
    var ageWeeksText by rememberSaveable(initialFlock?.id) {
        mutableStateOf(initialFlock?.ageWeeksAtPlacement?.toString() ?: "17")
    }
    var placedDateText by rememberSaveable(initialFlock?.id) {
        mutableStateOf(initialFlock?.placedDate ?: todayDate)
    }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val hensVal = hensHousedText.trim().toIntOrNull() ?: 0
    val ageVal = ageWeeksText.trim().toIntOrNull() ?: -1

    val nameError = if (nameText.isBlank()) "Give the pen a name, e.g. Pen A." else null
    val breedError = if (breedText.isBlank()) "Pick a breed or type your own." else null
    val hensError = if (hensVal <= 0) "Enter how many birds you stocked." else null
    val ageError = if (ageVal < 0 || ageVal > 120) "Enter age in weeks, from 0 to 120." else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("flock_form_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialFlock == null) "Add a pen" else "Edit ${initialFlock.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FormGroupTitle("About the pen")

            LedgerTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = "Pen name",
                helperText = "E.g. Pen A, Layer House 1, Back Pen",
                errorText = if (saveAttempted) nameError else null,
                keyboardType = KeyboardType.Text,
                testTag = "input_flock_name"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Breed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    BREED_SUGGESTIONS.forEach { suggestion ->
                        FilterChip(
                            selected = breedText == suggestion,
                            onClick = { breedText = suggestion },
                            label = { Text(suggestion) },
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("breed_chip_${suggestion.replace(" ", "_").lowercase()}")
                        )
                    }
                }
                LedgerTextField(
                    value = breedText,
                    onValueChange = { breedText = it },
                    label = "Or type the breed",
                    helperText = null,
                    errorText = if (saveAttempted) breedError else null,
                    keyboardType = KeyboardType.Text,
                    testTag = "input_flock_breed"
                )
            }

            FormGroupTitle("The birds")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LedgerTextField(
                    value = hensHousedText,
                    onValueChange = { hensHousedText = it.filter { c -> c.isDigit() } },
                    label = "Birds stocked",
                    placeholder = "500",
                    helperText = "How many you started with",
                    errorText = if (saveAttempted) hensError else null,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    testTag = "input_hens_housed"
                )
                LedgerTextField(
                    value = ageWeeksText,
                    onValueChange = { ageWeeksText = it.filter { c -> c.isDigit() } },
                    label = "Age then",
                    suffix = "wks",
                    helperText = "Point of lay is 16–18",
                    errorText = if (saveAttempted) ageError else null,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    testTag = "input_age_weeks_placed"
                )
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("input_placed_date_button")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Date stocked: ${LayerCalc.friendlyDate(placedDateText, withYear = true)}")
            }

            ClutchPrimaryButton(
                text = if (initialFlock == null) "Save pen" else "Save changes",
                icon = Icons.Default.Check,
                onClick = {
                    saveAttempted = true
                    if (nameError != null || breedError != null || hensError != null || ageError != null) {
                        return@ClutchPrimaryButton
                    }
                    onSave(
                        initialFlock?.id,
                        nameText,
                        breedText,
                        hensVal,
                        ageVal,
                        placedDateText
                    )
                },
                testTag = "save_flock_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val initialMillis = LayerCalc.parseEpochDay(placedDateText) * 86_400_000L
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86_400_000L
                            placedDateText = LayerCalc.formatEpochDay(epochDay)
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
private fun FormGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() }
    )
}
