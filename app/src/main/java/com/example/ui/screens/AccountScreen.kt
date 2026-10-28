package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.UnownedPen
import com.example.domain.GhanaPhone
import com.example.ui.ClutchViewModel
import com.example.ui.daysText
import com.example.ui.pensText
import com.example.ui.components.ClutchCard
import com.example.ui.components.ClutchPrimaryButton
import com.example.ui.components.ClutchSecondaryButton
import com.example.ui.components.IconBadge
import com.example.ui.components.InfoNote
import com.example.ui.components.LedgerTextField
import com.example.ui.components.ScreenColumn
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SummaryRow
import com.example.ui.theme.ClutchIcons
import kotlinx.coroutines.launch

private enum class AuthMode(val label: String) { CREATE("Create account"), SIGN_IN("Sign in") }

@Composable
fun AccountScreen(
    viewModel: ClutchViewModel,
    startWithSignIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sync by viewModel.syncStatus.collectAsState()

    ScreenColumn(modifier = modifier, testTag = "account_screen") {
        if (sync.signedIn) {
            SignedInView(viewModel = viewModel)
        } else {
            SignedOutView(viewModel = viewModel, startWithSignIn = startWithSignIn, sessionMessage = sync.error)
        }
    }
}

@Composable
private fun SignedInView(viewModel: ClutchViewModel) {
    val sync by viewModel.syncStatus.collectAsState()
    val unclaimed by viewModel.unclaimedPens.collectAsState()
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    ScreenTitle(title = "Backup", subtitle = "Your records are saved to your account")

    // Asked once, right after signing in: never mix someone else's pens into this account silently
    if (unclaimed.isNotEmpty()) {
        UnclaimedPensCard(
            pens = unclaimed,
            onAdd = { viewModel.addPensToAccount(unclaimed) },
            onKeepSeparate = { viewModel.keepPensSeparate(unclaimed) }
        )
    }

    ClutchCard(modifier = Modifier.testTag("account_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon = ClutchIcons.Person, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(sync.userName.orEmpty(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    sync.userPhone?.let { GhanaPhone.display(it) }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = if (sync.error != null) ClutchIcons.Warning else ClutchIcons.Good,
                contentDescription = null,
                tint = if (sync.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = sync.error ?: lastBackupText(sync.lastSyncedAt, sync.isSyncing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("backup_status")
            )
        }
        ClutchPrimaryButton(
            text = if (sync.isSyncing) "Backing up…" else "Back up now",
            icon = ClutchIcons.Backup,
            loading = sync.isSyncing,
            onClick = { viewModel.backUpNow() },
            testTag = "back_up_now"
        )
    }

    InfoNote(
        title = "Lost or new phone?",
        text = "Install Clutch, choose \"I already use Clutch\" and sign in with this phone number. All your pens and records come back."
    )

    ClutchSecondaryButton(
        text = "Sign out",
        icon = ClutchIcons.SignOut,
        onClick = { confirmSignOut = true },
        testTag = "sign_out"
    )

    // Destructive and rare: kept small and last, never next to the main action
    TextButton(
        onClick = { confirmDelete = true },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delete_account")
    ) {
        Text("Delete my account", color = MaterialTheme.colorScheme.error)
    }

    if (confirmDelete) {
        DeleteAccountDialog(viewModel = viewModel, onDismiss = { confirmDelete = false })
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            icon = { Icon(ClutchIcons.SignOut, contentDescription = null, modifier = Modifier.size(22.dp)) },
            title = { Text("Sign out?") },
            text = { Text("Your pens will be hidden on this phone until you sign in again. They stay safe in your account. Anything not backed up yet goes up when you sign in again on this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") } }
        )
    }
}

@Composable
private fun DeleteAccountDialog(viewModel: ClutchViewModel, onDismiss: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(ClutchIcons.Delete, contentDescription = null, modifier = Modifier.size(22.dp)) },
        title = { Text("Delete your account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This permanently deletes your account and every record backed up to it. " +
                        "It cannot be undone. Records on this phone stay."
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(64) },
                    label = { Text("Your password") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_delete_password")
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty() && !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        viewModel.deleteAccount(password)
                            .onSuccess { onDismiss() }
                            .onFailure { error = it.message ?: "Could not delete the account. Please try again." }
                        busy = false
                    }
                },
                modifier = Modifier.testTag("confirm_delete_account")
            ) { Text(if (busy) "Deleting…" else "Delete for ever", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Keep my account") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedOutView(viewModel: ClutchViewModel, startWithSignIn: Boolean, sessionMessage: String?) {
    var mode by rememberSaveable { mutableStateOf(if (startWithSignIn) AuthMode.SIGN_IN else AuthMode.CREATE) }
    var phone by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val phoneOk = GhanaPhone.normalize(phone) != null
    val phoneError = if (!phoneOk) "Enter a Ghana number, e.g. 024 123 4567." else null
    val nameError = if (mode == AuthMode.CREATE && name.isBlank()) "Enter your name." else null
    val passwordError = if (password.length < 6) "Use at least 6 characters." else null

    ScreenTitle(
        title = "Back up your records",
        subtitle = "Free. Uses a little data only when you save."
    )

    ClutchCard {
        BenefitLine(ClutchIcons.Cloud, "Safe if your phone is lost, stolen or broken")
        BenefitLine(ClutchIcons.Phone, "Sign in on a new phone and everything comes back")
        BenefitLine(ClutchIcons.Offline, "Keep recording without data — it backs up later")
    }

    sessionMessage?.let { InfoNote(text = it, icon = ClutchIcons.Info) }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag("auth_mode")) {
        AuthMode.entries.forEachIndexed { i, m ->
            SegmentedButton(
                selected = mode == m,
                onClick = {
                    mode = m
                    serverError = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = AuthMode.entries.size),
                icon = {},
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(m.label) }
        }
    }

    ClutchCard {
        LedgerTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16) },
            label = "Phone number",
            placeholder = "024 123 4567",
            helperText = "Your Ghana mobile number",
            errorText = if (attempted) phoneError else null,
            keyboardType = KeyboardType.Phone,
            testTag = "input_phone"
        )
        if (mode == AuthMode.CREATE) {
            LedgerTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = "Your name",
                helperText = "So the owner knows who sent the records",
                errorText = if (attempted) nameError else null,
                keyboardType = KeyboardType.Text,
                testTag = "input_name"
            )
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(64) },
            label = { Text("Password") },
            singleLine = true,
            isError = attempted && passwordError != null,
            supportingText = {
                Text(if (attempted && passwordError != null) passwordError else if (mode == AuthMode.CREATE) "At least 6 characters. Don't share it." else "")
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) ClutchIcons.Hide else ClutchIcons.Show,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_password")
        )

        serverError?.let {
            InfoNote(text = it, icon = ClutchIcons.Warning)
        }

        ClutchPrimaryButton(
            text = when {
                busy -> if (mode == AuthMode.CREATE) "Creating account…" else "Signing in…"
                mode == AuthMode.CREATE -> "Create account and back up"
                else -> "Sign in and restore"
            },
            icon = ClutchIcons.Backup,
            loading = busy,
            onClick = {
                attempted = true
                serverError = null
                if (phoneError != null || nameError != null || passwordError != null) return@ClutchPrimaryButton
                busy = true
                scope.launch {
                    val result = if (mode == AuthMode.CREATE) {
                        viewModel.register(phone, name.trim(), password)
                    } else {
                        viewModel.login(phone, password)
                    }
                    busy = false
                    result.onFailure { serverError = it.message ?: "Something went wrong. Please try again." }
                }
            },
            testTag = "auth_submit"
        )
    }

    if (BuildConfig.DEBUG) {
        DebugServerField(viewModel)
    }
}

@Composable
private fun BenefitLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon = icon, size = 32.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Testers only (debug builds): point the app at a computer running the backend on the same Wi-Fi. */
@Composable
private fun DebugServerField(viewModel: ClutchViewModel) {
    var url by rememberSaveable { mutableStateOf(viewModel.serverUrlOverride ?: BuildConfig.API_BASE_URL) }
    ClutchCard {
        SummaryRow(label = "Test server (debug builds only)", value = "")
        LedgerTextField(
            value = url,
            onValueChange = { url = it.trim() },
            label = "Server address",
            helperText = "e.g. http://192.168.1.20/clutch/backend/api",
            errorText = null,
            keyboardType = KeyboardType.Uri,
            testTag = "input_server_url"
        )
        TextButton(onClick = { viewModel.serverUrlOverride = url.ifBlank { null } }) { Text("Use this server") }
    }
}

/**
 * Pens on this phone that are in no account. Shown to a farmer who has just signed in, so the
 * choice is made once, knowingly: nothing is added to an account without asking.
 */
@Composable
fun UnclaimedPensCard(
    pens: List<UnownedPen>,
    onAdd: () -> Unit,
    onKeepSeparate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val one = pens.size == 1
    val names = pens.take(3).joinToString(", ") { "${it.name} (${daysText(it.days)})" } +
        if (pens.size > 3) " and ${pens.size - 3} more" else ""
    ClutchCard(
        modifier = modifier.testTag("unclaimed_pens"),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(
                icon = ClutchIcons.Pens,
                size = 40.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (one) "1 pen on this phone is not in your account" else "${pensText(pens.size)} on this phone are not in your account",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(names, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(
            if (one) "Add it if it is yours. If it belongs to someone else, keep it separate. It stays on this phone either way."
            else "Add them if they are yours. If they belong to someone else, keep them separate. They stay on this phone either way.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onKeepSeparate, modifier = Modifier.testTag("unclaimed_keep_separate")) { Text("Keep separate") }
            ClutchPrimaryButton(
                text = "Add to my account",
                icon = ClutchIcons.Backup,
                onClick = onAdd,
                modifier = Modifier.weight(1f),
                testTag = "unclaimed_add"
            )
        }
    }
}
