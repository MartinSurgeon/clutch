package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ClutchLogo
import com.example.ui.components.ExampleDataBanner
import com.example.ui.components.LoadingState
import com.example.ui.screens.DailyScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlocksScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.WeeklyScreen
import com.example.ui.theme.ClutchIcons
import java.io.File

// Tabs
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_DAILY = "daily"
const val ROUTE_WEEKLY = "weekly"
const val ROUTE_GUIDE = "guide"

// Sub-screens (opened on top of a tab, with a back arrow)
const val ROUTE_RECORD = "record"
const val ROUTE_PENS = "pens"

private const val PREFS_NAME = "clutch_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

/** Screens wider than this get a side rail instead of a bottom bar (tablets, foldables, landscape). */
private const val WIDE_LAYOUT_MIN_DP = 600

enum class ClutchTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(ROUTE_DASHBOARD, "Dashboard", ClutchIcons.Dashboard),
    DAILY(ROUTE_DAILY, "Daily", ClutchIcons.Daily),
    WEEKLY(ROUTE_WEEKLY, "Weekly", ClutchIcons.Weekly),
    GUIDE(ROUTE_GUIDE, "Guide", ClutchIcons.Guide)
}

private fun isTabRoute(route: String) = ClutchTab.entries.any { it.route == route }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClutchApp(
    viewModel: ClutchViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isWide = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_DP
    val onTab = isTabRoute(currentRoute)
    // Sub-screens are focused tasks: no tab bar. While typing, hide it so the Save bar sits above the keyboard.
    val showBottomBar = !isWide && onTab && !WindowInsets.isImeVisible

    var openAddPenSheet by rememberSaveable { mutableStateOf(false) }

    // First-launch welcome screens, shown once
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var onboardingDone by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false)) }
    var openAddPenAfterOnboarding by rememberSaveable { mutableStateOf(false) }

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

    fun openRecord(date: String) {
        viewModel.selectRecordDate(date)
        navController.navigate(ROUTE_RECORD) { launchSingleTop = true }
    }

    fun openPens(addNew: Boolean = false) {
        openAddPenSheet = addNew
        navController.navigate(ROUTE_PENS) { launchSingleTop = true }
    }

    fun goBack() {
        if (!navController.popBackStack()) navigateToTab(ROUTE_DASHBOARD)
    }

    if (!uiState.isLoading && !onboardingDone && uiState.flocks.isEmpty()) {
        OnboardingScreen(
            onGetStarted = {
                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                onboardingDone = true
                openAddPenAfterOnboarding = true
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
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
                    onBack = ::goBack,
                    onSelectFlock = { viewModel.selectFlock(it) },
                    onManagePens = { openPens() },
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
                    tonalElevation = 0.dp
                ) {
                    ClutchTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
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
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
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
                        onOpenRecord = ::openRecord,
                        onOpenPens = ::openPens,
                        onBack = ::goBack,
                        onSelectWindow = { viewModel.setTrendWindow(it) },
                        openAddPenSheet = openAddPenSheet,
                        onAddPenSheetConsumed = { openAddPenSheet = false }
                    )
                    // "Get started" goes straight to adding the first pen (one less tap)
                    LaunchedEffect(openAddPenAfterOnboarding) {
                        if (openAddPenAfterOnboarding) {
                            openAddPenAfterOnboarding = false
                            openPens(addNew = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClutchNavHost(
    uiState: ClutchUiState,
    viewModel: ClutchViewModel,
    navController: NavHostController,
    onOpenRecord: (String) -> Unit,
    onOpenPens: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectWindow: (TrendWindow) -> Unit,
    openAddPenSheet: Boolean,
    onAddPenSheetConsumed: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_DASHBOARD,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_DASHBOARD) {
            DashboardScreen(
                uiState = uiState,
                onAddFirstFlock = { onOpenPens(true) },
                onRecordToday = { onOpenRecord(uiState.todayDate) },
                onSelectWindow = onSelectWindow
            )
        }

        composable(ROUTE_DAILY) {
            DailyScreen(
                uiState = uiState,
                onRecordDay = onOpenRecord,
                onAddFirstFlock = { onOpenPens(true) }
            )
        }

        composable(ROUTE_WEEKLY) {
            WeeklyScreen(
                uiState = uiState,
                onSaveNote = { ageWeek, notes, actions ->
                    uiState.activeDashboard?.flock?.let { flock ->
                        viewModel.saveWeeklyNote(flock, ageWeek, notes, actions)
                    }
                },
                onRecordToday = { onOpenRecord(uiState.todayDate) }
            )
        }

        composable(ROUTE_GUIDE) {
            GuideScreen(
                uiState = uiState,
                onSaveFlockSize = { flock, size -> viewModel.updateFlockSize(flock, size) },
                onManagePens = { onOpenPens(false) }
            )
        }

        composable(ROUTE_RECORD) {
            RecordScreen(
                uiState = uiState,
                onDateChange = { newDate ->
                    viewModel.selectRecordDate(newDate)
                },
                onSaveRecord = { flock, date, liveHens, deaths, culls, eggs, cracked, dirty, softShell, feedKg, waterL, avgEggWeightG, maxTempC, bodyWeightG, uniformityPct, notes, onFailed ->
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
                        bodyWeightG = bodyWeightG,
                        uniformityPct = uniformityPct,
                        notes = notes,
                        onSaved = onBack,
                        onFailed = onFailed
                    )
                },
                onDeleteRecord = { flock, date ->
                    viewModel.deleteDailyRecord(
                        flock = flock,
                        date = date,
                        onDeleted = onBack
                    )
                },
                onAddFirstFlock = { onOpenPens(true) }
            )
        }

        composable(ROUTE_PENS) {
            FlocksScreen(
                uiState = uiState,
                showAddSheetInitial = openAddPenSheet,
                onAddSheetConsumed = onAddPenSheetConsumed,
                onOpenFlock = { flockId ->
                    viewModel.selectFlock(flockId)
                    onBack()
                },
                onSaveFlock = { existingId, name, breed, hensHoused, ageWeeks, placedDate ->
                    viewModel.saveFlock(
                        existingId = existingId,
                        name = name,
                        breed = breed,
                        hensHoused = hensHoused,
                        ageWeeksAtPlacement = ageWeeks,
                        placedDate = placedDate,
                        onSaved = { if (existingId == null) onBack() }
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
 * Top bar. On tabs the title is a pen switcher (familiar "account switcher" pattern), so changing
 * pens never needs a trip elsewhere. On sub-screens it shows a back arrow and the screen name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClutchTopBar(
    uiState: ClutchUiState,
    currentRoute: String,
    onBack: () -> Unit,
    onSelectFlock: (String) -> Unit,
    onManagePens: () -> Unit,
    onShareRecords: () -> Unit
) {
    var switcherExpanded by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    val activeFlock = uiState.activeDashboard?.flock
    val onTab = isTabRoute(currentRoute)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            if (onTab) {
                ClutchLogo(
                    size = 32.dp,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                )
            } else {
                IconButton(onClick = onBack, modifier = Modifier.testTag("top_back_button")) {
                    Icon(
                        imageVector = ClutchIcons.Back,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        title = {
            when {
                currentRoute == ROUTE_RECORD -> TopTitle(
                    title = "Record a day",
                    subtitle = activeFlock?.name
                )
                currentRoute == ROUTE_PENS -> TopTitle(title = "Your pens")
                activeFlock != null -> Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { switcherExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("pen_switcher"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = activeFlock.name,
                                style = MaterialTheme.typography.titleLarge,
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = ClutchIcons.Dropdown,
                            contentDescription = "Change pen",
                            modifier = Modifier.size(14.dp)
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
                                        Icon(ClutchIcons.Check, contentDescription = "Current pen", modifier = Modifier.size(16.dp))
                                    } else {
                                        Box(modifier = Modifier.size(16.dp))
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
                            leadingIcon = { Icon(ClutchIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                switcherExpanded = false
                                onManagePens()
                            },
                            modifier = Modifier.testTag("manage_pens_item")
                        )
                    }
                }
                else -> TopTitle(title = "Clutch")
            }
        },
        actions = {
            if (onTab && uiState.flocks.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier.testTag("top_overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = ClutchIcons.More,
                            contentDescription = "More options",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manage pens") },
                            leadingIcon = { Icon(ClutchIcons.Pens, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                overflowExpanded = false
                                onManagePens()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share records") },
                            leadingIcon = { Icon(ClutchIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
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

@Composable
private fun TopTitle(title: String, subtitle: String? = null) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
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
