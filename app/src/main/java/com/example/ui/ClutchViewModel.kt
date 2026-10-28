package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPrefs
import com.example.data.local.DailyRecordEntity
import com.example.data.local.FlockEntity
import com.example.data.local.WeeklyNoteEntity
import com.example.data.repository.ClutchRepository
import com.example.domain.LayerCalc
import com.example.data.local.UnownedPen
import com.example.data.sync.SignInSummary
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncStatus
import com.example.domain.WeeklyCalc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class TrendWindow(val label: String, val days: Int?) {
    DAYS_30("Last 30 days", 30),
    DAYS_90("Last 3 months", 90),
    WHOLE_FLOCK("All time", null)
}

data class FlockCardModel(
    val flock: FlockEntity,
    val ageWeeksToday: Int,
    val hensAlive: Int,
    val lastHenDayPct: Double?,
    val lastHenDayStatus: LayerCalc.TargetState?,
    val lastRecordedDate: String?,
    val recordCount: Int
)

data class RecordWithMetrics(
    val entity: DailyRecordEntity,
    val input: LayerCalc.DailyRecordInput,
    val metrics: LayerCalc.DayMetrics
)

data class ActiveFlockDashboard(
    val flock: FlockEntity,
    val flockInput: LayerCalc.FlockInput,
    val ageWeeksToday: Int,
    val latestRecord: RecordWithMetrics?,
    val isTodayRecorded: Boolean,
    val defaultStartLiveHensForDate: (String) -> Int,
    val existingRecordForDate: (String) -> DailyRecordEntity?,
    val allRecordsAsc: List<RecordWithMetrics>,
    val warnings: List<LayerCalc.WarningItem>,
    val last7Days: LayerCalc.Last7DaysSummary,
    val weeklyMortality: List<LayerCalc.WeeklyMortalityPoint>,
    val totalEggsToDate: Int,
    val weeklySummaries: List<WeeklyCalc.WeekSummary>,
    val weeklyNotes: Map<Int, WeeklyNoteEntity>,
    val latestWeighIn: DailyRecordEntity?
)

data class ClutchUiState(
    val isLoading: Boolean = true,
    val todayDate: String = currentIsoDate(),
    val flocks: List<FlockCardModel> = emptyList(),
    val selectedFlockId: String? = null,
    val activeDashboard: ActiveFlockDashboard? = null,
    val hasExampleData: Boolean = false,
    val selectedRecordDate: String = currentIsoDate(),
    val trendWindow: TrendWindow = TrendWindow.DAYS_30,
    val errorMessage: String? = null
)

fun currentIsoDate(): String {
    val cal = Calendar.getInstance()
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int = 18 * 60,
    val promptDismissed: Boolean = false
)

data class EndOfLayPrices(
    val eggPricePerCrate: Double? = null,
    val feedPricePerBag: Double? = null,
    val spentHenPrice: Double? = null,
    val otherCostPerBirdPerMonth: Double? = null
)

class ClutchViewModel(
    private val repository: ClutchRepository,
    private val syncManager: SyncManager? = null,
    private val prefs: AppPrefs? = null,
    private val onReminderChanged: () -> Unit = {},
    val todayDate: String = currentIsoDate()
) : ViewModel() {

    /** Backup status for the account screen and the "backed up" line. */
    val syncStatus: StateFlow<SyncStatus> = syncManager?.status ?: MutableStateFlow(SyncStatus())

    private val _reminder = MutableStateFlow(
        ReminderSettings(
            enabled = prefs?.reminderEnabled ?: false,
            minuteOfDay = prefs?.reminderMinuteOfDay ?: 18 * 60,
            promptDismissed = prefs?.reminderPromptDismissed ?: false
        )
    )
    val reminder: StateFlow<ReminderSettings> = _reminder.asStateFlow()

    init {
        // Catch up with the server whenever the app opens (does nothing when signed out)
        syncManager?.syncSoon(delayMs = 500)
    }

    fun setReminder(enabled: Boolean, minuteOfDay: Int = _reminder.value.minuteOfDay) {
        prefs?.reminderEnabled = enabled
        prefs?.reminderMinuteOfDay = minuteOfDay
        _reminder.value = _reminder.value.copy(enabled = enabled, minuteOfDay = minuteOfDay)
        onReminderChanged()
        if (enabled) _snackbarEvents.tryEmit("Daily reminder set for ${formatMinuteOfDay(minuteOfDay)}")
    }

    fun dismissReminderPrompt() {
        prefs?.reminderPromptDismissed = true
        _reminder.value = _reminder.value.copy(promptDismissed = true)
    }

    // --- Account & backup ---

    suspend fun register(phone: String, name: String, password: String): Result<Unit> =
        runCatching { syncManager!!.register(phone, name, password) }
            .onSuccess { _snackbarEvents.tryEmit(signedInMessage(it, newAccount = true)) }
            .map { }

    suspend fun login(phone: String, password: String): Result<Unit> =
        runCatching { syncManager!!.login(phone, password) }
            .onSuccess { _snackbarEvents.tryEmit(signedInMessage(it, newAccount = false)) }
            .map { }

    suspend fun deleteAccount(password: String): Result<Unit> =
        runCatching { syncManager!!.deleteAccount(password) }
            .onSuccess { _snackbarEvents.tryEmit("Account deleted. Your records stay on this phone.") }

    fun signOut() {
        viewModelScope.launch {
            syncManager?.signOut()
            _snackbarEvents.tryEmit("Signed out. Your pens are hidden until you sign in again.")
        }
    }

    /** Pens on this phone in no account, offered to the signed-in farmer ("Add to my account?"). */
    val unclaimedPens: StateFlow<List<UnownedPen>> = (syncManager?.unclaimedPens ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pens hidden while signed out, because they belong to an account. */
    val hiddenPenCount: StateFlow<Int> = (syncManager?.hiddenPenCount ?: flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addPensToAccount(pens: List<UnownedPen>) {
        viewModelScope.launch {
            syncManager?.claimPens(pens.map { it.id })
            _snackbarEvents.tryEmit("${pensText(pens.size)} added to your account. Backing up now.")
        }
    }

    fun keepPensSeparate(pens: List<UnownedPen>) {
        syncManager?.keepSeparate(pens.map { it.id })
        _snackbarEvents.tryEmit("Kept out of your account. They show again when no one is signed in.")
    }

    fun backUpNow() {
        viewModelScope.launch {
            runCatching { syncManager?.syncNow() }
                .onSuccess { _snackbarEvents.tryEmit("Backed up") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Backup failed. Your records are safe on this phone.") }
        }
    }

    private fun backUpSoon() {
        syncManager?.syncSoon()
    }

    var serverUrlOverride: String?
        get() = prefs?.serverUrlOverride
        set(value) {
            prefs?.serverUrlOverride = value
        }

    // --- End-of-lay prices (kept per pen on this phone) ---

    fun endOfLayPrices(flockId: String): EndOfLayPrices = EndOfLayPrices(
        eggPricePerCrate = prefs?.eolPrice(flockId, AppPrefs.EOL_EGG_PRICE),
        feedPricePerBag = prefs?.eolPrice(flockId, AppPrefs.EOL_FEED_PRICE),
        spentHenPrice = prefs?.eolPrice(flockId, AppPrefs.EOL_HEN_PRICE),
        otherCostPerBirdPerMonth = prefs?.eolPrice(flockId, AppPrefs.EOL_OTHER_COST)
    )

    fun saveEndOfLayPrices(flockId: String, p: EndOfLayPrices) {
        prefs?.setEolPrice(flockId, AppPrefs.EOL_EGG_PRICE, p.eggPricePerCrate)
        prefs?.setEolPrice(flockId, AppPrefs.EOL_FEED_PRICE, p.feedPricePerBag)
        prefs?.setEolPrice(flockId, AppPrefs.EOL_HEN_PRICE, p.spentHenPrice)
        prefs?.setEolPrice(flockId, AppPrefs.EOL_OTHER_COST, p.otherCostPerBirdPerMonth)
    }

    private val _selectedFlockId = MutableStateFlow<String?>(null)
    private val _selectedRecordDate = MutableStateFlow(todayDate)
    private val _trendWindow = MutableStateFlow(TrendWindow.DAYS_30)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<ClutchUiState> = combine(
        repository.allFlocks,
        repository.allActiveRecords,
        repository.allWeeklyNotes,
        _selectedFlockId,
        combine(_selectedRecordDate, _trendWindow) { date, window -> date to window }
    ) { flocks, allRecords, allNotes, selFlockId, (recDate, window) ->
        val recordsByFlock = allRecords.groupBy { it.flockId }
        val notesByFlock = allNotes.groupBy { it.flockId }
        val hasExample = flocks.any { it.isExample }

        val flockCards = flocks.map { flock ->
            val fRecords = (recordsByFlock[flock.id] ?: emptyList()).sortedBy {
                LayerCalc.parseEpochDay(it.date)
            }
            val latest = fRecords.lastOrNull()
            val ageToday = LayerCalc.ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, todayDate)
            val hensAlive = if (latest != null) {
                LayerCalc.closingHens(latest.liveHens, latest.deaths, latest.culls)
            } else {
                flock.hensHoused
            }
            val lastHd = latest?.let { LayerCalc.henDayPercent(it.eggs, it.liveHens) }
            val lastAge = latest?.let {
                LayerCalc.ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, it.date)
            } ?: ageToday
            val lastStatus = lastHd?.let { LayerCalc.evaluateHenDayTarget(it, lastAge) }

            FlockCardModel(
                flock = flock,
                ageWeeksToday = ageToday,
                hensAlive = hensAlive,
                lastHenDayPct = lastHd,
                lastHenDayStatus = lastStatus,
                lastRecordedDate = latest?.date,
                recordCount = fRecords.size
            )
        }

        val activeFlocks = flocks.filter { !it.archived }
        val effectiveFlock = flocks.find { it.id == selFlockId }
            ?: activeFlocks.firstOrNull()
            ?: flocks.firstOrNull()

        val dashboard = effectiveFlock?.let { flock ->
            val fInput = LayerCalc.FlockInput(
                id = flock.id,
                name = flock.name,
                breed = flock.breed,
                hensHoused = flock.hensHoused,
                placedDate = flock.placedDate,
                ageWeeksAtPlacement = flock.ageWeeksAtPlacement
            )
            val rawRecords = (recordsByFlock[flock.id] ?: emptyList()).sortedBy {
                LayerCalc.parseEpochDay(it.date)
            }
            val inputs = rawRecords.map {
                LayerCalc.DailyRecordInput(
                    date = it.date,
                    liveHens = it.liveHens,
                    eggs = it.eggs,
                    cracked = it.cracked,
                    dirty = it.dirty,
                    softShell = it.softShell,
                    deaths = it.deaths,
                    culls = it.culls,
                    feedKg = it.feedKg,
                    waterL = it.waterL,
                    avgEggWeightG = it.avgEggWeightG,
                    maxTempC = it.maxTempC,
                    notes = it.notes,
                    bodyWeightG = it.bodyWeightG,
                    uniformityPct = it.uniformityPct
                )
            }
            val recordsWithMetrics = rawRecords.mapIndexed { idx, entity ->
                val input = inputs[idx]
                val metrics = LayerCalc.calculateDayMetrics(fInput, input, inputs)
                RecordWithMetrics(entity = entity, input = input, metrics = metrics)
            }
            val latestRec = recordsWithMetrics.lastOrNull()
            val isTodayRec = rawRecords.any { it.date == todayDate }
            val warnings = LayerCalc.evaluateWarnings(fInput, inputs)
            val summaryDate = latestRec?.entity?.date ?: todayDate
            val last7 = LayerCalc.calculateLast7DaysSummary(summaryDate, inputs)
            val weeklyMort = LayerCalc.weeklyMortalityByAge(fInput, inputs)
            val totalEggs = rawRecords.sumOf { it.eggs }

            val byDateMap = rawRecords.associateBy { it.date }

            ActiveFlockDashboard(
                flock = flock,
                flockInput = fInput,
                ageWeeksToday = LayerCalc.ageInWeeks(flock.ageWeeksAtPlacement, flock.placedDate, todayDate),
                latestRecord = latestRec,
                isTodayRecorded = isTodayRec,
                defaultStartLiveHensForDate = { targetDate ->
                    val existing = byDateMap[targetDate]
                    if (existing != null) {
                        existing.liveHens
                    } else {
                        val targetEpoch = LayerCalc.parseEpochDay(targetDate)
                        val prior = rawRecords
                            .filter { LayerCalc.parseEpochDay(it.date) < targetEpoch }
                            .maxByOrNull { LayerCalc.parseEpochDay(it.date) }
                        if (prior != null) {
                            LayerCalc.closingHens(prior.liveHens, prior.deaths, prior.culls)
                        } else {
                            flock.hensHoused
                        }
                    }
                },
                existingRecordForDate = { targetDate -> byDateMap[targetDate] },
                allRecordsAsc = recordsWithMetrics,
                warnings = warnings,
                last7Days = last7,
                weeklyMortality = weeklyMort,
                totalEggsToDate = totalEggs,
                weeklySummaries = WeeklyCalc.summarizeWeeks(fInput, inputs),
                weeklyNotes = (notesByFlock[flock.id] ?: emptyList()).associateBy { it.ageWeek },
                latestWeighIn = rawRecords.lastOrNull { it.bodyWeightG != null || it.uniformityPct != null }
            )
        }

        ClutchUiState(
            isLoading = false,
            todayDate = todayDate,
            flocks = flockCards,
            selectedFlockId = effectiveFlock?.id,
            activeDashboard = dashboard,
            hasExampleData = hasExample,
            selectedRecordDate = recDate,
            trendWindow = window,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClutchUiState(todayDate = todayDate, selectedRecordDate = todayDate)
    )

    fun selectFlock(flockId: String) {
        _selectedFlockId.value = flockId
    }

    fun selectRecordDate(date: String) {
        _selectedRecordDate.value = date
    }

    fun setTrendWindow(window: TrendWindow) {
        _trendWindow.value = window
    }

    fun removeExampleData() {
        viewModelScope.launch {
            runCatching {
                repository.removeExampleData()
                _selectedFlockId.value = null
                _snackbarEvents.tryEmit("Sample data cleared")
            }.onFailure {
                _snackbarEvents.tryEmit("Sorry, the sample could not be cleared. Please try again.")
            }
        }
    }

    fun saveFlock(
        existingId: String?,
        name: String,
        breed: String,
        hensHoused: Int,
        ageWeeksAtPlacement: Int,
        placedDate: String,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = existingId ?: UUID.randomUUID().toString()
            val entity = FlockEntity(
                id = id,
                name = name.trim(),
                breed = breed.trim(),
                hensHoused = hensHoused,
                placedDate = placedDate.trim(),
                ageWeeksAtPlacement = ageWeeksAtPlacement,
                archived = false,
                isExample = false
            )
            runCatching { repository.saveFlock(entity) }
                .onSuccess {
                    _selectedFlockId.value = id
                    _snackbarEvents.tryEmit("${entity.name} saved")
                    backUpSoon()
                    onSaved(id)
                }
                .onFailure { _snackbarEvents.tryEmit("Sorry, ${entity.name} was not saved. Please try again.") }
        }
    }

    /** Change only the starting flock size (birds stocked), keeping everything else about the pen. */
    fun updateFlockSize(flock: FlockEntity, hensHoused: Int) {
        viewModelScope.launch {
            runCatching { repository.saveFlock(flock.copy(hensHoused = hensHoused)) }
                .onSuccess {
                    _snackbarEvents.tryEmit("Starting flock size for ${flock.name} is now $hensHoused birds")
                    backUpSoon()
                }
                .onFailure { _snackbarEvents.tryEmit("Sorry, the flock size was not saved. Please try again.") }
        }
    }

    fun saveWeeklyNote(
        flock: FlockEntity,
        ageWeek: Int,
        notes: String,
        actions: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val note = WeeklyNoteEntity(
                id = WeeklyNoteEntity.buildId(flock.id, ageWeek),
                flockId = flock.id,
                ageWeek = ageWeek,
                notes = notes.trim(),
                actions = actions.trim()
            )
            runCatching { repository.saveWeeklyNote(note) }
                .onSuccess {
                    _snackbarEvents.tryEmit("Notes for week $ageWeek saved")
                    backUpSoon()
                    onSaved()
                }
                .onFailure { _snackbarEvents.tryEmit("Sorry, the notes were not saved. Please try again.") }
        }
    }

    fun archiveFlock(flock: FlockEntity, archive: Boolean) {
        viewModelScope.launch {
            repository.archiveFlock(flock.id, archive)
            val msg = if (archive) "${flock.name} moved to old pens" else "${flock.name} is back in your pens"
            _snackbarEvents.tryEmit(msg)
            backUpSoon()
        }
    }

    fun deleteFlock(flock: FlockEntity) {
        viewModelScope.launch {
            val count = repository.countRecordsForFlock(flock.id)
            if (count > 0) {
                repository.archiveFlock(flock.id, true)
                _snackbarEvents.tryEmit("${flock.name} has $count days of records, so it was moved to old pens instead")
                backUpSoon()
            } else {
                repository.deleteFlock(flock.id)
                _snackbarEvents.tryEmit("${flock.name} removed")
                backUpSoon()
            }
        }
    }

    fun saveDailyRecord(
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
        bodyWeightG: Double?,
        uniformityPct: Double?,
        notes: String,
        onSaved: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val record = DailyRecordEntity(
                id = DailyRecordEntity.buildId(flock.id, date),
                flockId = flock.id,
                date = date,
                liveHens = liveHens,
                eggs = eggs,
                cracked = cracked,
                dirty = dirty,
                softShell = softShell,
                deaths = deaths,
                culls = culls,
                feedKg = feedKg,
                waterL = waterL,
                avgEggWeightG = avgEggWeightG,
                maxTempC = maxTempC,
                bodyWeightG = bodyWeightG,
                uniformityPct = uniformityPct,
                notes = notes.trim()
            )
            runCatching { repository.saveDailyRecord(record) }
                .onSuccess {
                    _snackbarEvents.tryEmit("Saved! ${flock.name} is up to date.")
                    backUpSoon()
                    onSaved()
                }
                .onFailure {
                    _snackbarEvents.tryEmit("Sorry, the day was not saved. Please try again.")
                    onFailed()
                }
        }
    }

    fun deleteDailyRecord(
        flock: FlockEntity,
        date: String,
        onDeleted: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteDailyRecord(flock.id, date)
            _snackbarEvents.tryEmit("Record for ${LayerCalc.friendlyDate(date)} deleted")
            backUpSoon()
            onDeleted()
        }
    }

    fun exportCsv(onCsvReady: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.buildCsvExport()
            onCsvReady(csv)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class ClutchViewModelFactory(
    private val repository: ClutchRepository,
    private val syncManager: SyncManager? = null,
    private val prefs: AppPrefs? = null,
    private val onReminderChanged: () -> Unit = {}
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ClutchViewModel(repository, syncManager, prefs, onReminderChanged) as T
    }
}

/** "6:00 PM" style time for a minute-of-day value. */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val h24 = (minuteOfDay / 60) % 24
    val m = minuteOfDay % 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h12:${m.toString().padStart(2, '0')} ${if (h24 < 12) "AM" else "PM"}"
}

fun pensText(count: Int): String = if (count == 1) "1 pen" else "$count pens"

fun daysText(count: Int): String = if (count == 1) "1 day" else "$count days"

internal fun signedInMessage(summary: SignInSummary, newAccount: Boolean): String = when {
    !summary.restored && newAccount -> "Account created. Backup will finish when you have internet."
    !summary.restored -> "Signed in. Your records will come down when you have internet."
    newAccount && summary.pens == 0 -> "Account created. New records will be backed up."
    newAccount -> "Account created. ${pensText(summary.pens)} backed up."
    summary.pens == 0 -> "Signed in. There are no records in this account yet."
    else -> "Signed in. ${pensText(summary.pens)} and ${summary.days} daily records restored."
}
