package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FlockEntity
import com.example.domain.LayerCalc
import com.example.ui.ClutchUiState
import com.example.ui.FlockCardModel
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.StatusChip

val BREED_SUGGESTIONS = listOf(
    "Lohmann Brown",
    "ISA Brown",
    "Hy-Line Brown",
    "Bovans Brown",
    "Novogen Brown",
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

    if (showAddSheetInitial && !showFormSheet) {
        editingFlock = null
        showFormSheet = true
        onAddSheetConsumed()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("flocks_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your flocks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Select Open to view a flock's daily ledger, or use the menu to edit or archive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.flocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No flocks added yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add your first laying house or pen below to begin tracking production.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.flocks, key = { it.flock.id }) { cardModel ->
                    FlockItemCard(
                        model = cardModel,
                        isSelected = cardModel.flock.id == uiState.selectedFlockId,
                        onOpen = { onOpenFlock(cardModel.flock.id) },
                        onEdit = {
                            editingFlock = cardModel.flock
                            showFormSheet = true
                        },
                        onToggleArchive = {
                            onArchiveFlock(cardModel.flock, !cardModel.flock.archived)
                        },
                        onDelete = {
                            onDeleteFlock(cardModel.flock)
                        }
                    )
                }
            }

            // One "Add flock" button
            item {
                ClutchPrimaryButton(
                    text = "Add flock",
                    icon = Icons.Default.Add,
                    onClick = {
                        editingFlock = null
                        showFormSheet = true
                    },
                    testTag = "add_flock_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
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
}

@Composable
private fun FlockItemCard(
    model: FlockCardModel,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val flock = model.flock
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flock_card_${flock.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = flock.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (flock.archived) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Archived",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.width(12.dp)
                                    )
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${flock.breed} • Placed ${flock.placedDate}",
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
                            text = { Text("Edit flock") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            modifier = Modifier.testTag("menu_edit_flock_${flock.id}")
                        )
                        DropdownMenuItem(
                            text = { Text(if (flock.archived) "Restore flock" else "Archive flock") },
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
                        // A flock with records can only be archived
                        if (model.recordCount == 0) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete empty flock",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
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
                FlockStatColumn(
                    label = "Age",
                    value = "${model.ageWeeksToday} wks"
                )
                FlockStatColumn(
                    label = "Hens alive",
                    value = "${model.hensAlive} / ${flock.hensHoused}"
                )
                FlockStatColumn(
                    label = "Last hen-day",
                    value = model.lastHenDayPct?.let { "${LayerCalc.format1(it)}%" } ?: "No records"
                )
            }

            model.lastHenDayStatus?.let { status ->
                StatusChip(state = status)
            }

            // One primary action "Open" on the card
            ClutchSecondaryButton(
                text = "Open",
                onClick = onOpen,
                testTag = "open_flock_button_${flock.id}"
            )
        }
    }
}

@Composable
private fun FlockStatColumn(
    label: String,
    value: String
) {
    Column {
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
        mutableStateOf(initialFlock?.hensHoused?.toString() ?: "500")
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

    val nameError = if (nameText.isBlank()) "Flock or pen name is required (e.g. Pen A)." else null
    val breedError = if (breedText.isBlank()) "Breed is required." else null
    val hensError = if (hensVal <= 0) "Hens housed must be over 0." else null
    val ageError = if (ageVal < 0 || ageVal > 120) "Enter age in weeks (0–120)." else null

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialFlock == null) "Add flock" else "Edit ${initialFlock.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Flock or pen name *") },
                isError = saveAttempted && nameError != null,
                supportingText = {
                    Text(if (saveAttempted && nameError != null) nameError else "Example: Pen A, House 1, North Shed")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_flock_name")
            )

            OutlinedTextField(
                value = breedText,
                onValueChange = { breedText = it },
                label = { Text("Breed *") },
                isError = saveAttempted && breedError != null,
                supportingText = {
                    Text(if (saveAttempted && breedError != null) breedError else "Tap a common breed below or type your own")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_flock_breed")
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BREED_SUGGESTIONS.forEach { suggestion ->
                    AssistChip(
                        onClick = { breedText = suggestion },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("breed_chip_${suggestion.replace(" ", "_").lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = hensHousedText,
                    onValueChange = { hensHousedText = it },
                    label = { Text("Hens housed *") },
                    isError = saveAttempted && hensError != null,
                    supportingText = {
                        Text(if (saveAttempted && hensError != null) hensError else "Starting count")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_hens_housed")
                )

                OutlinedTextField(
                    value = ageWeeksText,
                    onValueChange = { ageWeeksText = it },
                    label = { Text("Age placed (wks) *") },
                    isError = saveAttempted && ageError != null,
                    supportingText = {
                        Text(if (saveAttempted && ageError != null) ageError else "Usually 16–18 wks")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_age_weeks_placed")
                )
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_placed_date_button")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Date placed: $placedDateText")
            }

            ClutchPrimaryButton(
                text = if (initialFlock == null) "Save flock" else "Update flock",
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
