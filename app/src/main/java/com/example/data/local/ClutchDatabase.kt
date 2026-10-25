package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

class SyncStateConverters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState =
        runCatching { SyncState.valueOf(value) }.getOrDefault(SyncState.PENDING)
}

@Dao
interface ClutchDao {

    @Query("SELECT * FROM flocks WHERE deleted = 0 ORDER BY archived ASC, placedDate DESC, name ASC")
    fun observeAllFlocks(): Flow<List<FlockEntity>>

    @Query("SELECT * FROM flocks WHERE deleted = 0")
    suspend fun getAllFlocksOnce(): List<FlockEntity>

    @Query("SELECT * FROM flocks WHERE id = :flockId AND deleted = 0 LIMIT 1")
    suspend fun getFlockById(flockId: String): FlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlock(flock: FlockEntity)

    @Query("UPDATE flocks SET deleted = 1, updatedAt = :timestamp, syncState = 'PENDING' WHERE id = :flockId")
    suspend fun softDeleteFlock(flockId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE flocks SET archived = :archived, updatedAt = :timestamp, syncState = 'PENDING' WHERE id = :flockId")
    suspend fun setFlockArchived(flockId: String, archived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM flocks WHERE isExample = 1")
    suspend fun hardDeleteExampleFlocks()

    @Query("SELECT * FROM daily_records WHERE flockId = :flockId AND deleted = 0 ORDER BY date ASC")
    fun observeRecordsForFlock(flockId: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE deleted = 0 ORDER BY date ASC")
    fun observeAllActiveRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE flockId = :flockId AND deleted = 0 ORDER BY date ASC")
    suspend fun getRecordsForFlockOnce(flockId: String): List<DailyRecordEntity>

    @Query("SELECT COUNT(*) FROM daily_records WHERE flockId = :flockId AND deleted = 0")
    suspend fun countRecordsForFlock(flockId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: DailyRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(records: List<DailyRecordEntity>)

    @Query("UPDATE daily_records SET deleted = 1, updatedAt = :timestamp, syncState = 'PENDING' WHERE id = :recordId")
    suspend fun softDeleteRecord(recordId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_records WHERE flockId IN (SELECT id FROM flocks WHERE isExample = 1)")
    suspend fun hardDeleteExampleRecords()

    @Query("SELECT * FROM weekly_notes WHERE deleted = 0")
    fun observeAllWeeklyNotes(): Flow<List<WeeklyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyNote(note: WeeklyNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyNotes(notes: List<WeeklyNoteEntity>)

    @Query("DELETE FROM weekly_notes WHERE flockId IN (SELECT id FROM flocks WHERE isExample = 1)")
    suspend fun hardDeleteExampleWeeklyNotes()
}

/**
 * v1 -> v2: adds body weight and uniformity to daily records, and the weekly notes table.
 * Additive only, so every farmer's existing records are kept.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_records ADD COLUMN bodyWeightG REAL")
        db.execSQL("ALTER TABLE daily_records ADD COLUMN uniformityPct REAL")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `weekly_notes` (" +
                "`id` TEXT NOT NULL, `flockId` TEXT NOT NULL, `ageWeek` INTEGER NOT NULL, " +
                "`notes` TEXT NOT NULL, `actions` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

@Database(
    entities = [FlockEntity::class, DailyRecordEntity::class, WeeklyNoteEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(SyncStateConverters::class)
abstract class ClutchDatabase : RoomDatabase() {
    abstract fun clutchDao(): ClutchDao
}
