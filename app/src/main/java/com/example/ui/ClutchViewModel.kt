package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailyRecordEntity
import com.example.data.local.FlockEntity
import com.example.data.repository.ClutchRepository
import com.example.domain.LayerCalc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class TrendWindow(val label: String, val days: Int?) {
    DAYS_30("30 days", 30),
    DAYS_90("90 days", 90),
    WHOLE_FLOCK("Whole flock", null)
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
    val totalEggsToDate: Int
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

class ClutchViewModel(
    private val repository: ClutchRepository,
    val todayDate: String = currentIsoDate()
) : ViewModel() {

    private val _selectedFlockId = MutableStateFlow<String?>(null)
    private val _selectedRecordDate = MutableStateFlow(todayDate)
    private val _trendWindow = MutableStateFlow(TrendWindow.DAYS_30)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<ClutchUiState> = combine(
        repository.allFlocks,
        repository.allActiveRecords,
        _selectedFlockId,
        _selectedRecordDate,
        _trendWindow
    ) { flocks, allRecords, selFlockId, recDate, window ->
        val recordsByFlock = allRecords.groupBy { it.flockId }
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
                    notes = it.notes
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
                totalEggsToDate = totalEggs
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

    fun loadExampleData() {
        viewModelScope.launch {
            runCatching {
                val flockId = repository.insertExampleFlockAndRecords(todayDate)
                _selectedFlockId.value = flockId
                _selectedRecordDate.value = todayDate
                _snackbarEvents.tryEmit("Loaded 12 weeks of example data for Pen A")
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "Could not load example data."
            }
        }
    }

    fun removeExampleData() {
        viewModelScope.launch {
            runCatching {
                repository.removeExampleData()
                _selectedFlockId.value = null
                _snackbarEvents.tryEmit("Example data removed")
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "Could not remove example data."
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
            repository.saveFlock(entity)
            _selectedFlockId.value = id
            _snackbarEvents.tryEmit("Saved flock ${entity.name}")
            onSaved(id)
        }
    }

    fun archiveFlock(flock: FlockEntity, archive: Boolean) {
        viewModelScope.launch {
            repository.archiveFlock(flock.id, archive)
            val msg = if (archive) "Archived ${flock.name}" else "Restored ${flock.name}"
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun deleteFlock(flock: FlockEntity) {
        viewModelScope.launch {
            val count = repository.countRecordsForFlock(flock.id)
            if (count > 0) {
                repository.archiveFlock(flock.id, true)
                _snackbarEvents.tryEmit("${flock.name} has $count records and was archived instead")
            } else {
                repository.deleteFlock(flock.id)
                _snackbarEvents.tryEmit("Deleted ${flock.name}")
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
        notes: String,
        onSaved: () -> Unit
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
                notes = notes.trim()
            )
            repository.saveDailyRecord(record)
            _snackbarEvents.tryEmit("Saved for ${flock.name}")
            onSaved()
        }
    }

    fun deleteDailyRecord(
        flock: FlockEntity,
        date: String,
        onDeleted: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteDailyRecord(flock.id, date)
            _snackbarEvents.tryEmit("Deleted record for ${flock.name} ($date)")
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
    private val repository: ClutchRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ClutchViewModel(repository) as T
    }
}
