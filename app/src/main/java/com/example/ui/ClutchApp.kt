package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.EggYolkLogo
import com.example.ui.components.ExampleDataBanner
import com.example.ui.screens.FlocksScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.TrendsScreen
import java.io.File

const val ROUTE_TODAY = "today"
const val ROUTE_RECORD = "record"
const val ROUTE_TRENDS = "trends"
const val ROUTE_FLOCKS = "flocks"

enum class ClutchTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY(ROUTE_TODAY, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    RECORD(ROUTE_RECORD, "Record", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    TRENDS(ROUTE_TRENDS, "Trends", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
    FLOCKS(ROUTE_FLOCKS, "Flocks", Icons.Filled.Layers, Icons.Outlined.Layers)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClutchApp(
    viewModel: ClutchViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_TODAY
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var openAddFlockOnFlocksScreen by rememberSaveable { mutableStateOf(false) }
    var topOverflowExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("clutch_snackbar_host")
            )
        },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EggYolkLogo(size = 30.dp)
                            Column {
                                Text(
                                    text = "Clutch",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                uiState.activeDashboard?.flock?.let { activeFlock ->
                                    Text(
                                        text = "${activeFlock.name} • ${activeFlock.breed}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == ROUTE_FLOCKS || uiState.flocks.isNotEmpty()) {
                            IconButton(
                                onClick = { topOverflowExpanded = true },
                                modifier = Modifier.testTag("top_overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            DropdownMenu(
                                expanded = topOverflowExpanded,
                                onDismissRequest = { topOverflowExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export CSV") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        topOverflowExpanded = false
                                        viewModel.exportCsv { csvContent ->
                                            shareCsvViaIntent(context, csvContent)
                                        }
                                    },
                                    modifier = Modifier.testTag("menu_export_csv_item")
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (uiState.hasExampleData) {
                    ExampleDataBanner(
                        onRemoveExample = { viewModel.removeExampleData() }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                ClutchTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(tab.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_TODAY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(ROUTE_TODAY) {
                TodayScreen(
                    uiState = uiState,
                    onAddFirstFlock = {
                        openAddFlockOnFlocksScreen = true
                        navigateToTab(ROUTE_FLOCKS)
                    },
                    onTryExampleData = {
                        viewModel.loadExampleData()
                    },
                    onRecordToday = {
                        viewModel.selectRecordDate(uiState.todayDate)
                        navigateToTab(ROUTE_RECORD)
                    },
                    onEditToday = {
                        viewModel.selectRecordDate(uiState.todayDate)
                        navigateToTab(ROUTE_RECORD)
                    }
                )
            }

            composable(ROUTE_RECORD) {
                RecordScreen(
                    uiState = uiState,
                    onDateChange = { newDate ->
                        viewModel.selectRecordDate(newDate)
                    },
                    onSaveRecord = { flock, date, liveHens, deaths, culls, eggs, cracked, dirty, softShell, feedKg, waterL, avgEggWeightG, maxTempC, notes ->
                        viewModel.saveDailyRecord(
                            flock = flock,
                            date = date,
                            liveHens = liveHens,
                            deaths = deaths,
                            culls = culls,
                            eggs = eggs,
                            cracked = cracked,
                            dirty = dirty,
                            softShell = softShell,
                            feedKg = feedKg,
                            waterL = waterL,
                            avgEggWeightG = avgEggWeightG,
                            maxTempC = maxTempC,
                            notes = notes,
                            onSaved = {
                                navigateToTab(ROUTE_TODAY)
                            }
                        )
                    },
                    onDeleteRecord = { flock, date ->
                        viewModel.deleteDailyRecord(
                            flock = flock,
                            date = date,
                            onDeleted = {
                                navigateToTab(ROUTE_TODAY)
                            }
                        )
                    },
                    onNavigateToFlocks = {
                        navigateToTab(ROUTE_FLOCKS)
                    }
                )
            }

            composable(ROUTE_TRENDS) {
                TrendsScreen(
                    uiState = uiState,
                    onSelectWindow = { window ->
                        viewModel.setTrendWindow(window)
                    },
                    onTapDayToEdit = { date ->
                        viewModel.selectRecordDate(date)
                        navigateToTab(ROUTE_RECORD)
                    },
                    onNavigateToRecord = {
                        viewModel.selectRecordDate(uiState.todayDate)
                        navigateToTab(ROUTE_RECORD)
                    }
                )
            }

            composable(ROUTE_FLOCKS) {
                FlocksScreen(
                    uiState = uiState,
                    showAddSheetInitial = openAddFlockOnFlocksScreen,
                    onAddSheetConsumed = { openAddFlockOnFlocksScreen = false },
                    onOpenFlock = { flockId ->
                        viewModel.selectFlock(flockId)
                        navigateToTab(ROUTE_TODAY)
                    },
                    onSaveFlock = { existingId, name, breed, hensHoused, ageWeeks, placedDate ->
                        viewModel.saveFlock(
                            existingId = existingId,
                            name = name,
                            breed = breed,
                            hensHoused = hensHoused,
                            ageWeeksAtPlacement = ageWeeks,
                            placedDate = placedDate,
                            onSaved = {
                                navigateToTab(ROUTE_TODAY)
                            }
                        )
                    },
                    onArchiveFlock = { flock, archive ->
                        viewModel.archiveFlock(flock, archive)
                    },
                    onDeleteFlock = { flock ->
                        viewModel.deleteFlock(flock)
                    }
                )
            }
        }
    }
}

private fun shareCsvViaIntent(context: Context, csvContent: String) {
    runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "clutch_flock_records.csv")
        csvFile.writeText(csvContent)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Clutch Layer Flock Records CSV")
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Export Clutch CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
