package com.example.data.sync

import android.os.Build
import com.example.data.local.AppPrefs
import com.example.data.local.ClutchDao
import com.example.data.local.DailyRecordEntity
import com.example.data.local.FlockEntity
import com.example.data.local.SyncState
import com.example.data.local.WeeklyNoteEntity
import com.example.data.remote.ApiException
import com.example.data.remote.AuthResult
import com.example.data.remote.ClutchApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class SyncStatus(
    val signedIn: Boolean = false,
    val userName: String? = null,
    val userPhone: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val error: String? = null
)

/**
 * Cloud backup & sync for one farmer's records.
 *
 * - Push: every row still PENDING on this phone (sample data excluded).
 * - Pull: every row the server stored after our cursor, page by page.
 * - Conflicts: last write wins by updatedAt; a newer unsent edit on this phone is never overwritten.
 */
class SyncManager(
    private val dao: ClutchDao,
    private val prefs: AppPrefs,
    private val api: ClutchApi,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var pendingJob: Job? = null

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private fun readStatus() = SyncStatus(
        signedIn = prefs.authToken != null,
        userName = prefs.userName,
        userPhone = prefs.userPhone,
        lastSyncedAt = prefs.lastSyncedAt
    )

    private fun deviceName() = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80)

    suspend fun register(phone: String, name: String, password: String) =
        signIn(api.register(phone, name, password, deviceName()))

    suspend fun login(phone: String, password: String) =
        signIn(api.login(phone, password, deviceName()))

    private suspend fun signIn(auth: AuthResult) {
        prefs.authToken = auth.token
        prefs.userName = auth.user.name
        prefs.userPhone = auth.user.phone
        prefs.syncCursor = 0L
        // Anything recorded before signing in on this phone must be uploaded too
        dao.markAllFlocksPending()
        dao.markAllRecordsPending()
        dao.markAllWeeklyNotesPending()
        _status.value = readStatus()
        syncNow()
    }

    /** Deletes the account and its backup on the server. Records on this phone stay. */
    suspend fun deleteAccount(password: String) {
        val token = prefs.authToken ?: return
        api.deleteAccount(token, password)
        prefs.clearAccount()
        _status.value = readStatus()
    }

    suspend fun signOut() {
        prefs.authToken?.let { token -> runCatching { api.logout(token) } }
        prefs.clearAccount()
        _status.value = readStatus()
    }

    /** Debounced background sync after a change; silently does nothing when signed out. */
    fun syncSoon(delayMs: Long = 2_000) {
        if (prefs.authToken == null) return
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(delayMs)
            runCatching { syncNow() }
        }
    }

    /** Pushes and pulls until up to date. Throws [ApiException] so a screen can show the reason. */
    suspend fun syncNow() {
        val token = prefs.authToken ?: return
        mutex.withLock {
            _status.update { it.copy(isSyncing = true, error = null) }
            try {
                var since = prefs.syncCursor
                var firstRound = true
                while (true) {
                    val flocks = if (firstRound) dao.pendingFlocks() else emptyList()
                    val records = if (firstRound) dao.pendingRecords() else emptyList()
                    val notes = if (firstRound) dao.pendingWeeklyNotes() else emptyList()

                    val res = api.sync(
                        token = token,
                        since = since,
                        changes = mapOf(
                            KEY_FLOCKS to flocks.map { it.toJson() },
                            KEY_RECORDS to records.map { it.toJson() },
                            KEY_NOTES to notes.map { it.toJson() }
                        )
                    )

                    val acceptedFlocks = res.accepted[KEY_FLOCKS].orEmpty().toSet()
                    flocks.filter { it.id in acceptedFlocks }.forEach { dao.markFlockSynced(it.id, it.updatedAt) }
                    val acceptedRecords = res.accepted[KEY_RECORDS].orEmpty().toSet()
                    records.filter { it.id in acceptedRecords }.forEach { dao.markRecordSynced(it.id, it.updatedAt) }
                    val acceptedNotes = res.accepted[KEY_NOTES].orEmpty().toSet()
                    notes.filter { it.id in acceptedNotes }.forEach { dao.markWeeklyNoteSynced(it.id, it.updatedAt) }

                    res.changes[KEY_FLOCKS].orEmpty().forEach { applyFlock(it.toFlock()) }
                    res.changes[KEY_RECORDS].orEmpty().forEach { applyRecord(it.toRecord()) }
                    res.changes[KEY_NOTES].orEmpty().forEach { applyNote(it.toNote()) }

                    since = res.serverTime
                    prefs.syncCursor = since
                    firstRound = false
                    if (!res.more) break
                }
                val now = System.currentTimeMillis()
                prefs.lastSyncedAt = now
                _status.update { it.copy(isSyncing = false, lastSyncedAt = now, error = null) }
            } catch (e: ApiException) {
                if (e.status == 401) {
                    // Token revoked or expired: sign out locally, keep the records
                    prefs.clearAccount()
                    _status.value = readStatus().copy(error = "Please sign in again to keep backing up.")
                } else {
                    _status.update { it.copy(isSyncing = false, error = e.message) }
                }
                throw e
            } catch (e: Exception) {
                _status.update { it.copy(isSyncing = false, error = "Backup failed. Your records are safe on this phone.") }
                throw ApiException("sync_failed", "Backup failed. Your records are safe on this phone.")
            }
        }
    }

    // --- Last write wins; never overwrite a newer edit that has not been uploaded yet ---

    private suspend fun applyFlock(incoming: FlockEntity) {
        val local = dao.anyFlock(incoming.id)
        if (local != null && local.syncState == SyncState.PENDING && local.updatedAt > incoming.updatedAt) return
        dao.upsertFlock(incoming.copy(isExample = false, syncState = SyncState.SYNCED))
    }

    private suspend fun applyRecord(incoming: DailyRecordEntity) {
        val local = dao.anyRecord(incoming.id)
        if (local != null && local.syncState == SyncState.PENDING && local.updatedAt > incoming.updatedAt) return
        dao.upsertRecord(incoming.copy(syncState = SyncState.SYNCED))
    }

    private suspend fun applyNote(incoming: WeeklyNoteEntity) {
        val local = dao.anyWeeklyNote(incoming.id)
        if (local != null && local.syncState == SyncState.PENDING && local.updatedAt > incoming.updatedAt) return
        dao.upsertWeeklyNote(incoming.copy(syncState = SyncState.SYNCED))
    }

    companion object {
        const val KEY_FLOCKS = "flocks"
        const val KEY_RECORDS = "dailyRecords"
        const val KEY_NOTES = "weeklyNotes"
    }
}

// --- Entity <-> JSON (field names match the server's sync_tables.php) ---

private fun JSONObject.optNullableDouble(key: String): Double? = if (isNull(key) || !has(key)) null else getDouble(key)

internal fun FlockEntity.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("breed", breed).put("hensHoused", hensHoused)
    .put("placedDate", placedDate).put("ageWeeksAtPlacement", ageWeeksAtPlacement)
    .put("archived", archived).put("updatedAt", updatedAt).put("deleted", deleted)

internal fun JSONObject.toFlock() = FlockEntity(
    id = getString("id"),
    name = getString("name"),
    breed = getString("breed"),
    hensHoused = getInt("hensHoused"),
    placedDate = getString("placedDate"),
    ageWeeksAtPlacement = getInt("ageWeeksAtPlacement"),
    archived = getBoolean("archived"),
    isExample = false,
    updatedAt = getLong("updatedAt"),
    deleted = getBoolean("deleted"),
    syncState = SyncState.SYNCED
)

internal fun DailyRecordEntity.toJson(): JSONObject = JSONObject()
    .put("id", id).put("flockId", flockId).put("date", date).put("liveHens", liveHens).put("eggs", eggs)
    .put("cracked", cracked).put("dirty", dirty).put("softShell", softShell).put("deaths", deaths).put("culls", culls)
    .put("feedKg", feedKg).put("waterL", waterL)
    .put("avgEggWeightG", avgEggWeightG ?: JSONObject.NULL)
    .put("maxTempC", maxTempC ?: JSONObject.NULL)
    .put("bodyWeightG", bodyWeightG ?: JSONObject.NULL)
    .put("uniformityPct", uniformityPct ?: JSONObject.NULL)
    .put("notes", notes).put("enteredBy", enteredBy).put("enteredByName", enteredByName)
    .put("updatedAt", updatedAt).put("deleted", deleted)

internal fun JSONObject.toRecord() = DailyRecordEntity(
    id = getString("id"),
    flockId = getString("flockId"),
    date = getString("date"),
    liveHens = getInt("liveHens"),
    eggs = getInt("eggs"),
    cracked = optInt("cracked"),
    dirty = optInt("dirty"),
    softShell = optInt("softShell"),
    deaths = optInt("deaths"),
    culls = optInt("culls"),
    feedKg = optDouble("feedKg", 0.0),
    waterL = optDouble("waterL", 0.0),
    avgEggWeightG = optNullableDouble("avgEggWeightG"),
    maxTempC = optNullableDouble("maxTempC"),
    bodyWeightG = optNullableDouble("bodyWeightG"),
    uniformityPct = optNullableDouble("uniformityPct"),
    notes = optString("notes"),
    enteredBy = optString("enteredBy"),
    enteredByName = optString("enteredByName"),
    updatedAt = getLong("updatedAt"),
    deleted = getBoolean("deleted"),
    syncState = SyncState.SYNCED
)

internal fun WeeklyNoteEntity.toJson(): JSONObject = JSONObject()
    .put("id", id).put("flockId", flockId).put("ageWeek", ageWeek).put("notes", notes).put("actions", actions)
    .put("updatedAt", updatedAt).put("deleted", deleted)

internal fun JSONObject.toNote() = WeeklyNoteEntity(
    id = getString("id"),
    flockId = getString("flockId"),
    ageWeek = getInt("ageWeek"),
    notes = optString("notes"),
    actions = optString("actions"),
    updatedAt = getLong("updatedAt"),
    deleted = getBoolean("deleted"),
    syncState = SyncState.SYNCED
)
