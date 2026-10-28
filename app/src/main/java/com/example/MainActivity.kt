package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppPrefs
import com.example.data.local.ClutchDatabase
import com.example.data.remote.ClutchApi
import com.example.data.repository.RoomClutchRepository
import com.example.data.sync.SyncManager
import com.example.notifications.ReminderScheduler
import com.example.ui.ClutchApp
import com.example.ui.ClutchViewModel
import com.example.ui.ClutchViewModelFactory
import com.example.ui.theme.ClutchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private val database by lazy { ClutchDatabase.get(applicationContext) }
    private val prefs by lazy { AppPrefs(applicationContext) }
    private val repository by lazy { RoomClutchRepository(database.clutchDao(), syncManager.owner) }
    private val syncManager by lazy {
        SyncManager(
            dao = database.clutchDao(),
            prefs = prefs,
            api = ClutchApi { prefs.serverUrlOverride ?: BuildConfig.API_BASE_URL },
            // Outlives the screen so a backup started on save can finish
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    /** Set when the daily reminder notification is tapped: open straight to "Record a day". */
    private val openRecordRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        ReminderScheduler.reschedule(applicationContext)

        setContent {
            ClutchTheme {
                val viewModel: ClutchViewModel = viewModel(
                    factory = ClutchViewModelFactory(
                        repository = repository,
                        syncManager = syncManager,
                        prefs = prefs,
                        onReminderChanged = { ReminderScheduler.reschedule(applicationContext) }
                    )
                )
                ClutchApp(
                    viewModel = viewModel,
                    openRecordRequested = openRecordRequest.value,
                    onOpenRecordHandled = { openRecordRequest.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_RECORD, false) == true) {
            openRecordRequest.value = true
            intent.removeExtra(EXTRA_OPEN_RECORD)
        }
    }

    companion object {
        const val EXTRA_OPEN_RECORD = "open_record"
    }
}
