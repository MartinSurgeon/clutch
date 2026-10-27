package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import android.content.Context
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
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

    // --- Cloud sync (sample data never leaves the phone) ---

    @Query("SELECT * FROM flocks WHERE syncState = 'PENDING' AND isExample = 0")
    suspend fun pendingFlocks(): List<FlockEntity>

    @Query(
        "SELECT * FROM daily_records WHERE syncState = 'PENDING' " +
            "AND flockId NOT IN (SELECT id FROM flocks WHERE isExample = 1)"
    )
    suspend fun pendingRecords(): List<DailyRecordEntity>

    @Query(
        "SELECT * FROM weekly_notes WHERE syncState = 'PENDING' " +
            "AND flockId NOT IN (SELECT id FROM flocks WHERE isExample = 1)"
    )
    suspend fun pendingWeeklyNotes(): List<WeeklyNoteEntity>

    @Query("SELECT * FROM flocks WHERE id = :id LIMIT 1")
    suspend fun anyFlock(id: String): FlockEntity?

    @Query("SELECT * FROM daily_records WHERE id = :id LIMIT 1")
    suspend fun anyRecord(id: String): DailyRecordEntity?

    @Query("SELECT * FROM weekly_notes WHERE id = :id LIMIT 1")
    suspend fun anyWeeklyNote(id: String): WeeklyNoteEntity?

    /** Marks synced only if the row was not edited again while the upload was in flight. */
    @Query("UPDATE flocks SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markFlockSynced(id: String, updatedAt: Long)

    @Query("UPDATE daily_records SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markRecordSynced(id: String, updatedAt: Long)

    @Query("UPDATE weekly_notes SET syncState = 'SYNCED' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markWeeklyNoteSynced(id: String, updatedAt: Long)

    /** Rows that exist only on this phone (e.g. made before the farmer signed in) need uploading. */
    @Query("UPDATE flocks SET syncState = 'PENDING' WHERE isExample = 0")
    suspend fun markAllFlocksPending()

    @Query("UPDATE daily_records SET syncState = 'PENDING' WHERE flockId NOT IN (SELECT id FROM flocks WHERE isExample = 1)")
    suspend fun markAllRecordsPending()

    @Query("UPDATE weekly_notes SET syncState = 'PENDING' WHERE flockId NOT IN (SELECT id FROM flocks WHERE isExample = 1)")
    suspend fun markAllWeeklyNotesPending()

    /** For the daily reminder: active pens with no record for [date]. */
    @Query(
        "SELECT name FROM flocks WHERE deleted = 0 AND archived = 0 AND isExample = 0 " +
            "AND id NOT IN (SELECT flockId FROM daily_records WHERE date = :date AND deleted = 0)"
    )
    suspend fun penNamesMissingRecord(date: String): List<String>
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

    companion object {
        @Volatile
        private var instance: ClutchDatabase? = null

        /** One database for the whole app — the screens and the reminder receiver share it. */
        fun get(context: Context): ClutchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClutchDatabase::class.java,
                    "clutch_ledger.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
