package com.example.data.repository

import com.example.data.local.ClutchDao
import com.example.data.local.DailyRecordEntity
import com.example.data.local.FlockEntity
import com.example.data.local.SyncState
import com.example.domain.LayerCalc
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

interface ClutchRepository {
    val allFlocks: Flow<List<FlockEntity>>
    val allActiveRecords: Flow<List<DailyRecordEntity>>

    fun observeRecordsForFlock(flockId: String): Flow<List<DailyRecordEntity>>
    suspend fun getRecordsForFlockOnce(flockId: String): List<DailyRecordEntity>
    suspend fun countRecordsForFlock(flockId: String): Int

    suspend fun saveFlock(flock: FlockEntity)
    suspend fun archiveFlock(flockId: String, archived: Boolean)
    suspend fun deleteFlock(flockId: String)

    suspend fun saveDailyRecord(record: DailyRecordEntity)
    suspend fun deleteDailyRecord(flockId: String, date: String)

    suspend fun insertExampleFlockAndRecords(todayDate: String): String
    suspend fun removeExampleData()
    suspend fun buildCsvExport(): String
}

class RoomClutchRepository(
    private val dao: ClutchDao
) : ClutchRepository {

    override val allFlocks: Flow<List<FlockEntity>> = dao.observeAllFlocks()
    override val allActiveRecords: Flow<List<DailyRecordEntity>> = dao.observeAllActiveRecords()

    override fun observeRecordsForFlock(flockId: String): Flow<List<DailyRecordEntity>> =
        dao.observeRecordsForFlock(flockId)

    override suspend fun getRecordsForFlockOnce(flockId: String): List<DailyRecordEntity> =
        dao.getRecordsForFlockOnce(flockId)

    override suspend fun countRecordsForFlock(flockId: String): Int =
        dao.countRecordsForFlock(flockId)

    override suspend fun saveFlock(flock: FlockEntity) {
        dao.upsertFlock(
            flock.copy(
                updatedAt = System.currentTimeMillis(),
                deleted = false,
                syncState = SyncState.PENDING
            )
        )
    }

    override suspend fun archiveFlock(flockId: String, archived: Boolean) {
        dao.setFlockArchived(flockId, archived, System.currentTimeMillis())
    }

    override suspend fun deleteFlock(flockId: String) {
        dao.softDeleteFlock(flockId, System.currentTimeMillis())
    }

    override suspend fun saveDailyRecord(record: DailyRecordEntity) {
        val canonicalId = DailyRecordEntity.buildId(record.flockId, record.date)
        dao.upsertRecord(
            record.copy(
                id = canonicalId,
                updatedAt = System.currentTimeMillis(),
                deleted = false,
                syncState = SyncState.PENDING
            )
        )
    }

    override suspend fun deleteDailyRecord(flockId: String, date: String) {
        val canonicalId = DailyRecordEntity.buildId(flockId, date)
        dao.softDeleteRecord(canonicalId, System.currentTimeMillis())
    }

    override suspend fun removeExampleData() {
        dao.hardDeleteExampleRecords()
        dao.hardDeleteExampleFlocks()
    }

    /**
     * Generates 12 weeks (84 days) of realistic data for a 500-hen Lohmann Brown flock placed at 17 weeks:
     * - Production rising smoothly from 0% at week 17-18 to ~93% by 25 weeks (weeks 25-28 in peak window);
     * - Weekly egg weights recorded every 7 days;
     * - A 2-day production drop (>3 points below previous week's average) AND a water drop (<85% of 7-day avg) at the end.
     */
    override suspend fun insertExampleFlockAndRecords(todayDate: String): String {
        removeExampleData()

        val flockId = "example_flock_pen_a"
        val totalDays = 84 // 12 weeks: day 0 is 83 days before todayDate
        val placedDate = LayerCalc.addDays(todayDate, -(totalDays - 1))
        val now = System.currentTimeMillis()

        val exampleFlock = FlockEntity(
            id = flockId,
            name = "Pen A",
            breed = "Lohmann Brown",
            hensHoused = 500,
            placedDate = placedDate,
            ageWeeksAtPlacement = 17,
            archived = false,
            isExample = true,
            updatedAt = now,
            deleted = false,
            syncState = SyncState.PENDING
        )
        dao.upsertFlock(exampleFlock)

        var currentLiveHens = 500
        val records = mutableListOf<DailyRecordEntity>()

        for (dayIndex in 0 until totalDays) {
            val date = LayerCalc.addDays(placedDate, dayIndex)
            val weekOffset = dayIndex / 7
            val ageWks = 17 + weekOffset
            val dayInWeek = dayIndex % 7

            // Occasional realistic mortality (total ~5 birds across 12 weeks, none in final 7 days except 0)
            val deaths = if (dayIndex in listOf(12, 27, 41, 58, 71)) 1 else 0
            val culls = 0

            // Target hen-day % progression from 17 weeks to 28 weeks
            val baseHenDayPct = when (ageWks) {
                17 -> 0.0
                18 -> 2.0 + dayInWeek * 1.2
                19 -> 14.0 + dayInWeek * 3.5
                20 -> 42.0 + dayInWeek * 3.8
                21 -> 69.0 + dayInWeek * 2.0
                22 -> 83.0 + dayInWeek * 0.7
                23 -> 88.0 + dayInWeek * 0.4
                24 -> 91.0 + (dayInWeek % 3) * 0.4
                25 -> 93.0 + (if (dayInWeek % 2 == 0) 0.4 else -0.2)
                26 -> 93.2 + (if (dayInWeek % 2 == 0) 0.3 else -0.3)
                27 -> 92.8 + (if (dayInWeek % 2 == 0) 0.4 else -0.2)
                else -> 92.6 + (if (dayInWeek % 2 == 0) 0.3 else -0.2)
            }

            // Last 2 days: intentional >3 point production drop (from ~92.6% down to ~86.5% yesterday and ~85.3% today)
            val finalHenDayPct = when (dayIndex) {
                totalDays - 2 -> 86.5 // ~6.1 points below previous week's ~92.6% average
                totalDays - 1 -> 85.3 // ~7.3 points below previous week's ~92.6% average
                else -> baseHenDayPct
            }

            val eggs = ((finalHenDayPct / 100.0) * currentLiveHens).roundToInt().coerceAtLeast(0)
            val cracked = if (eggs > 100) (eggs * 0.009).roundToInt() else 0
            val dirty = if (eggs > 100) (eggs * 0.012).roundToInt() else 0
            val softShell = if (eggs > 200 && dayInWeek == 4) 2 else 0

            // Feed per bird rises from ~88g at 17w to ~116g in lay
            val feedPerBirdG = when {
                ageWks <= 18 -> 88.0 + weekOffset * 4.0
                ageWks <= 21 -> 98.0 + (ageWks - 18) * 4.5
                else -> 115.0 + (dayInWeek % 3) * 1.0
            }
            val feedKg = ((feedPerBirdG * currentLiveHens) / 1000.0 * 10.0).roundToInt() / 10.0

            // Water per bird normally ~220 mL in full lay, but drops on the final day to ~172 mL (<85% of 220 mL = 187 mL)
            val waterPerBirdMl = when {
                dayIndex == totalDays - 1 -> 172.0 // ~78% of previous 7-day average -> triggers Serious water drop warning
                dayIndex == totalDays - 2 -> 208.0
                ageWks <= 19 -> 175.0 + weekOffset * 8.0
                else -> 220.0 + (dayInWeek % 3) * 2.0
            }
            val waterL = ((waterPerBirdMl * currentLiveHens) / 1000.0 * 10.0).roundToInt() / 10.0

            // Weekly egg weight recorded every 7th day (and on the last day of each week)
            val avgEggWeightG: Double? = if (ageWks >= 19 && (dayInWeek == 6 || dayIndex == totalDays - 1)) {
                when (ageWks) {
                    19 -> 48.5
                    20 -> 51.0
                    21 -> 53.5
                    22 -> 55.2
                    23 -> 56.8
                    24 -> 58.0
                    25 -> 59.2
                    26 -> 60.0
                    27 -> 60.5
                    else -> 61.0
                }
            } else null

            val maxTempC = 24.0 + (dayInWeek % 4) * 1.0
            val note = when (dayIndex) {
                totalDays - 1 -> "Nipple line pressure in back row looked slow this afternoon."
                totalDays - 2 -> "Slightly fewer eggs in bottom tier nests."
                else -> ""
            }

            records.add(
                DailyRecordEntity(
                    id = DailyRecordEntity.buildId(flockId, date),
                    flockId = flockId,
                    date = date,
                    liveHens = currentLiveHens,
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
                    notes = note,
                    enteredBy = "example_worker",
                    enteredByName = "Grace (Example)",
                    updatedAt = now,
                    deleted = false,
                    syncState = SyncState.SYNCED
                )
            )

            currentLiveHens = (currentLiveHens - deaths - culls).coerceAtLeast(1)
        }

        dao.upsertRecords(records)
        return flockId
    }

    override suspend fun buildCsvExport(): String {
        val flocks = dao.getAllFlocksOnce()
        val sb = StringBuilder()
        sb.appendLine(
            "flock_id,flock_name,breed,hens_housed,placed_date,age_weeks_at_placement,date,age_weeks,live_hens,closing_hens,eggs,hen_day_pct,hen_housed_pct,cracked,dirty,soft_shell,deaths,culls,feed_kg,feed_g_per_bird,water_l,water_ml_per_bird,avg_egg_weight_g,egg_mass_g,fcr,max_temp_c,notes,sync_state"
        )
        for (flock in flocks) {
            val flockInput = LayerCalc.FlockInput(
                id = flock.id,
                name = flock.name,
                breed = flock.breed,
                hensHoused = flock.hensHoused,
                placedDate = flock.placedDate,
                ageWeeksAtPlacement = flock.ageWeeksAtPlacement
            )
            val records = dao.getRecordsForFlockOnce(flock.id)
            val inputs = records.map {
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
            for ((idx, rec) in records.withIndex()) {
                val m = LayerCalc.calculateDayMetrics(flockInput, inputs[idx], inputs)
                val safeNotes = rec.notes.replace("\"", "\"\"")
                val safeFlockName = flock.name.replace("\"", "\"\"")
                val safeBreed = flock.breed.replace("\"", "\"\"")
                sb.appendLine(
                    "\"${flock.id}\",\"$safeFlockName\",\"$safeBreed\",${flock.hensHoused},${flock.placedDate},${flock.ageWeeksAtPlacement},${rec.date},${m.ageWeeks},${rec.liveHens},${m.closingHens},${rec.eggs},${LayerCalc.format1(m.henDayPct)},${LayerCalc.format1(m.henHousedPct)},${rec.cracked},${rec.dirty},${rec.softShell},${rec.deaths},${rec.culls},${rec.feedKg},${LayerCalc.format1(m.feedPerBirdG)},${rec.waterL},${LayerCalc.format1(m.waterPerBirdMl)},${rec.avgEggWeightG ?: ""},${m.eggMassG?.let { LayerCalc.format1(it) } ?: ""},${m.fcr?.let { LayerCalc.format2(it) } ?: ""},${rec.maxTempC ?: ""},\"$safeNotes\",${rec.syncState.name}"
                )
            }
        }
        return sb.toString()
    }
}
