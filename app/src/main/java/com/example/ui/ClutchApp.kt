package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.LoadingState
import com.example.ui.screens.FlocksScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.TrendsScreen
import java.io.File

const val ROUTE_TODAY = "today"
const val ROUTE_RECORD = "record"
const val ROUTE_TRENDS = "trends"
const val ROUTE_FLOCKS = "flocks"

/** Screens wider than this get a side rail instead of a bottom bar (tablets, foldables, landscape). */
private const val WIDE_LAYOUT_MIN_DP = 600

enum class ClutchTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY(ROUTE_TODAY, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    RECORD(ROUTE_RECORD, "Record", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    TRENDS(ROUTE_TRENDS, "Reports", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
    FLOCKS(ROUTE_FLOCKS, "Pens", Icons.Filled.Layers, Icons.Outlined.Layers)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val isWide = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_DP
    // Hide the bottom bar while typing so the Record screen's Save bar sits right above the keyboard
    val showBottomBar = !isWide && !WindowInsets.isImeVisible

    var openAddFlockOnFlocksScreen by rememberSaveable { mutableStateOf(false) }

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
                ClutchTopBar(
                    uiState = uiState,
                    currentRoute = currentRoute,
                    onSelectFlock = { viewModel.selectFlock(it) },
                    onManagePens = { navigateToTab(ROUTE_FLOCKS) },
                    onShareRecords = {
                        viewModel.exportCsv { csvContent ->
                            shareCsvViaIntent(context, csvContent)
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
            if (showBottomBar) {
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
                                    contentDescription = null
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
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWide) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ClutchTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    LoadingState()
                } else {
                    ClutchNavHost(
                        uiState = uiState,
                        viewModel = viewModel,
                        navController = navController,
                        navigateToTab = ::navigateToTab,
                        openAddFlockOnFlocksScreen = openAddFlockOnFlocksScreen,
                        onOpenAddFlock = { openAddFlockOnFlocksScreen = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClutchNavHost(
    uiState: ClutchUiState,
    viewModel: ClutchViewModel,
    navController: androidx.navigation.NavHostController,
    navigateToTab: (String) -> Unit,
    openAddFlockOnFlocksScreen: Boolean,
    onOpenAddFlock: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_TODAY,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_TODAY) {
            TodayScreen(
                uiState = uiState,
                onAddFirstFlock = {
                    onOpenAddFlock(true)
                    navigateToTab(ROUTE_FLOCKS)
                },
                onTryExampleData = {
                    viewModel.loadExampleData()
                },
                onRecordToday = {
                    viewModel.selectRecordDate(uiState.todayDate)
                    navigateToTab(ROUTE_RECORD)
                },
                onOpenReports = {
                    navigateToTab(ROUTE_TRENDS)
                }
            )
        }

        composable(ROUTE_RECORD) {
            RecordScreen(
                uiState = uiState,
                onDateChange = { newDate ->
                    viewModel.selectRecordDate(newDate)
                },
                onSaveRecord = { flock, date, liveHens, deaths, culls, eggs, cracked, dirty, softShell, feedKg, waterL, avgEggWeightG, maxTempC, notes, onFailed ->
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
                        },
                        onFailed = onFailed
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
                onAddFirstFlock = {
                    onOpenAddFlock(true)
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
                onAddSheetConsumed = { onOpenAddFlock(false) },
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

/**
 * Top bar: on Home, Record and Reports the title is a pen switcher (familiar "account switcher"
 * pattern), so changing pens never needs a trip to another tab. On Pens it is a plain title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClutchTopBar(
    uiState: ClutchUiState,
    currentRoute: String,
    onSelectFlock: (String) -> Unit,
    onManagePens: () -> Unit,
    onShareRecords: () -> Unit
) {
    var switcherExpanded by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    val activeFlock = uiState.activeDashboard?.flock
    val showSwitcher = currentRoute != ROUTE_FLOCKS && activeFlock != null

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            EggYolkLogo(
                size = 32.dp,
                modifier = Modifier.padding(start = 16.dp, end = 4.dp)
            )
        },
        title = {
            if (showSwitcher) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { switcherExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("pen_switcher"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeFlock.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeFlock.breed} • ${uiState.activeDashboard?.ageWeeksToday ?: 0} weeks old",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Change pen"
                        )
                    }
                    DropdownMenu(
                        expanded = switcherExpanded,
                        onDismissRequest = { switcherExpanded = false }
                    ) {
                        uiState.flocks.filter { !it.flock.archived }.forEach { card ->
                            val isCurrent = card.flock.id == uiState.selectedFlockId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${card.flock.name}  •  ${card.hensAlive} birds",
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = "Current pen")
                                    } else {
                                        Box(modifier = Modifier.size(24.dp))
                                    }
                                },
                                onClick = {
                                    switcherExpanded = false
                                    onSelectFlock(card.flock.id)
                                },
                                modifier = Modifier.testTag("pen_switcher_item_${card.flock.id}")
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Manage pens") },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            onClick = {
                                switcherExpanded = false
                                onManagePens()
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = if (currentRoute == ROUTE_FLOCKS) "Your pens" else "Clutch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            if (uiState.flocks.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier.testTag("top_overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share records") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onShareRecords()
                            },
                            modifier = Modifier.testTag("menu_export_csv_item")
                        )
                    }
                }
            }
        }
    )
}

private fun shareCsvViaIntent(context: Context, csvContent: String) {
    runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "clutch_farm_records.csv")
        csvFile.writeText(csvContent)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "My farm records from Clutch")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share records with…").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
