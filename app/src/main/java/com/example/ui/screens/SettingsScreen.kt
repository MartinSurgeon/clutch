package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.BuildConfig
import com.example.domain.GhanaPhone
import com.example.notifications.ReminderReceiver
import com.example.notifications.ReminderScheduler
import com.example.ui.ClutchUiState
import com.example.ui.ClutchViewModel
import com.example.ui.ReminderSettings
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.IconBadge
import com.example.ui.components.InfoNote
import com.example.ui.components.OfflineReassurance
import com.example.ui.components.ScreenColumn
import com.example.ui.components.SectionHeader
import com.example.ui.formatMinuteOfDay
import com.example.ui.share.latestDayReport
import com.example.ui.share.latestWeekReport
import com.example.ui.share.rememberHasWhatsApp
import com.example.ui.share.rememberShareReport
import com.example.ui.theme.ClutchIcons

/**
 * Settings & backup: three short sections, each with one clear action.
 */
@Composable
fun SettingsScreen(
    uiState: ClutchUiState,
    viewModel: ClutchViewModel,
    onOpenAccount: () -> Unit,
    onOpenEndOfLay: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder by viewModel.reminder.collectAsState()
    val sync by viewModel.syncStatus.collectAsState()
    val share = rememberShareReport()
    val hasWhatsApp = rememberHasWhatsApp()
    val dashboard = uiState.activeDashboard

    ScreenColumn(modifier = modifier, testTag = "settings_screen") {
        // 1. Backup — most important: protects the farmer's records
        SectionHeader(title = "Backup")
        ClutchCard(onClick = onOpenAccount, modifier = Modifier.testTag("settings_backup_card")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(icon = if (sync.signedIn) ClutchIcons.Cloud else ClutchIcons.Backup, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sync.signedIn) "Backed up to your account" else "Not backed up yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sync.signedIn) {
                            "${sync.userPhone?.let { GhanaPhone.display(it) } ?: ""} • ${lastBackupText(sync.lastSyncedAt, sync.isSyncing)}"
                        } else {
                            "If this phone is lost or broken, your records go with it."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(ClutchIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
            }
        }

        TextButton(onClick = onExportCsv, modifier = Modifier.testTag("menu_export_csv_item")) {
            Icon(ClutchIcons.Share, contentDescription = null, modifier = Modifier.size(14.dp))
            Text("  Download a copy of all records (CSV)")
        }

        // 2. Daily reminder
        SectionHeader(title = "Daily reminder")
        ReminderCard(
            reminder = reminder,
            onChange = { enabled, minute -> viewModel.setReminder(enabled, minute) }
        )

        // 3. Reports & planning
        if (dashboard != null && dashboard.latestRecord != null) {
            SectionHeader(title = "Reports", subtitle = "Send a summary to the owner, vet or buyer")
            ClutchCard {
                latestDayReport(dashboard)?.let { report ->
                    ClutchPrimaryButton(
                        text = if (hasWhatsApp) "Send today on WhatsApp" else "Send today's report",
                        icon = if (hasWhatsApp) ClutchIcons.WhatsApp else ClutchIcons.Send,
                        onClick = { share(report, true) },
                        testTag = "settings_share_day"
                    )
                }
                latestWeekReport(dashboard)?.let { report ->
                    ClutchSecondaryButton(
                        text = "Send this week's report",
                        icon = ClutchIcons.Weekly,
                        onClick = { share(report, true) },
                        testTag = "settings_share_week"
                    )
                }
            }

            SectionHeader(title = "Planning")
            ClutchCard(onClick = onOpenEndOfLay) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconBadge(icon = ClutchIcons.Calculator, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End-of-lay planner", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "See when keeping old layers stops paying",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(ClutchIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                }
            }
        }

        Text(
            text = "Clutch ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OfflineReassurance()
    }
}

internal fun lastBackupText(lastSyncedAt: Long, syncing: Boolean): String {
    if (syncing) return "Backing up…"
    if (lastSyncedAt == 0L) return "Not backed up yet"
    val minutes = (System.currentTimeMillis() - lastSyncedAt) / 60_000
    return when {
        minutes < 1 -> "Backed up just now"
        minutes < 60 -> "Backed up $minutes min ago"
        minutes < 24 * 60 -> "Backed up ${minutes / 60} h ago"
        else -> "Backed up ${minutes / (24 * 60)} days ago"
    }
}

/**
 * Reminder on/off and time. Notification permission is asked only when the farmer turns it on,
 * with the reason right there (contextual permission request).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderSettings,
    onChange: (enabled: Boolean, minuteOfDay: Int) -> Unit
) {
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) onChange(true, reminder.minuteOfDay)
    }

    // Re-checked whenever the farmer comes back from phone settings
    var notifyOk by remember { mutableStateOf(true) }
    var onTime by remember { mutableStateOf(true) }
    LifecycleResumeEffect(reminder.enabled) {
        notifyOk = hasPermission() && ReminderScheduler.canNotify(context)
        onTime = ReminderScheduler.canRingOnTime(context)
        onPauseOrDispose { }
    }

    fun turnOn() {
        if (hasPermission()) {
            permissionDenied = false
            onChange(true, reminder.minuteOfDay)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ClutchCard(modifier = Modifier.testTag("reminder_card")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Switch) { if (reminder.enabled) onChange(false, reminder.minuteOfDay) else turnOn() }
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = if (reminder.enabled) ClutchIcons.Reminder else ClutchIcons.ReminderOff, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Remind me to record", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Pops up on your screen if today's count is not in yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = { on -> if (on) turnOn() else onChange(false, reminder.minuteOfDay) },
                modifier = Modifier.testTag("reminder_switch")
            )
        }
        if (reminder.enabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow(
                icon = ClutchIcons.Waiting,
                title = "Time",
                value = formatMinuteOfDay(reminder.minuteOfDay),
                onClick = { showPicker = true }
            )
        }
        if (permissionDenied || (reminder.enabled && !notifyOk)) {
            InfoNote(
                title = "Notifications are off for Clutch",
                text = "To get the reminder, allow notifications for Clutch in your phone settings."
            )
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }.onFailure {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) { Text("Open phone settings") }
        } else if (reminder.enabled && !onTime) {
            InfoNote(
                title = "It may come a few minutes late",
                text = "Allow \"Alarms & reminders\" for Clutch so it pops up at exactly ${formatMinuteOfDay(reminder.minuteOfDay)}."
            )
            TextButton(
                onClick = { ReminderScheduler.exactAlarmSettingsIntent(context)?.let { runCatching { context.startActivity(it) } } },
                modifier = Modifier.testTag("reminder_allow_exact")
            ) { Text("Allow exact time") }
        }
        if (reminder.enabled && notifyOk) {
            TextButton(
                onClick = { ReminderReceiver.showTest(context) },
                modifier = Modifier.testTag("reminder_test")
            ) {
                Icon(ClutchIcons.Reminder, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("  Show me a test reminder")
            }
        }
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = reminder.minuteOfDay / 60,
            initialMinute = reminder.minuteOfDay % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    onChange(true, state.hour * 60 + state.minute)
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Icon(ClutchIcons.ChevronRight, contentDescription = "Change $title", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
    }
}

/**
 * One-time Dashboard prompt after the first record: offers the reminder at the moment it makes
 * sense, with one primary and one secondary choice.
 */
@Composable
fun ReminderPromptCard(
    reminder: ReminderSettings,
    onEnable: (minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onEnable(reminder.minuteOfDay) else onDismiss()
    }
    ClutchCard(
        modifier = Modifier.testTag("reminder_prompt"),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(
                icon = ClutchIcons.Reminder,
                size = 40.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Never miss a day", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "We'll remind you at ${formatMinuteOfDay(reminder.minuteOfDay)} — only if today's count is missing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("reminder_prompt_not_now")) { Text("Not now") }
            ClutchPrimaryButton(
                text = "Remind me daily",
                icon = ClutchIcons.Reminder,
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onEnable(reminder.minuteOfDay)
                },
                modifier = Modifier.weight(1f),
                testTag = "reminder_prompt_enable"
            )
        }
    }
}
