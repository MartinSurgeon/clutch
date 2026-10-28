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

    /** Pens of one account ([owner] = its phone number), or pens in no account when [owner] is null. */
    @Query("SELECT * FROM flocks WHERE deleted = 0 AND ownerId IS :owner ORDER BY archived ASC, placedDate DESC, name ASC")
    fun observeFlocks(owner: String?): Flow<List<FlockEntity>>

    @Query("SELECT * FROM flocks WHERE deleted = 0 AND ownerId IS :owner")
    suspend fun getFlocksOnce(owner: String?): List<FlockEntity>

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

    // --- Cloud sync: only the signed-in account's pens are ever uploaded (sample data never leaves the phone) ---

    @Query("SELECT * FROM flocks WHERE syncState = 'PENDING' AND isExample = 0 AND ownerId = :owner")
    suspend fun pendingFlocks(owner: String): List<FlockEntity>

    @Query(
        "SELECT * FROM daily_records WHERE syncState = 'PENDING' " +
            "AND flockId IN (SELECT id FROM flocks WHERE ownerId = :owner AND isExample = 0)"
    )
    suspend fun pendingRecords(owner: String): List<DailyRecordEntity>

    @Query(
        "SELECT * FROM weekly_notes WHERE syncState = 'PENDING' " +
            "AND flockId IN (SELECT id FROM flocks WHERE ownerId = :owner AND isExample = 0)"
    )
    suspend fun pendingWeeklyNotes(owner: String): List<WeeklyNoteEntity>

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

    /** On sign-in every row of this account is sent again, in case the server copy is gone. */
    @Query("UPDATE flocks SET syncState = 'PENDING' WHERE ownerId = :owner AND isExample = 0")
    suspend fun markOwnedFlocksPending(owner: String)

    @Query("UPDATE daily_records SET syncState = 'PENDING' WHERE flockId IN (SELECT id FROM flocks WHERE ownerId = :owner AND isExample = 0)")
    suspend fun markOwnedRecordsPending(owner: String)

    @Query("UPDATE weekly_notes SET syncState = 'PENDING' WHERE flockId IN (SELECT id FROM flocks WHERE ownerId = :owner AND isExample = 0)")
    suspend fun markOwnedWeeklyNotesPending(owner: String)

    // --- Which account each pen belongs to ---

    /** New account: the pens this farmer made before signing up become theirs. */
    @Query("UPDATE flocks SET ownerId = :owner WHERE ownerId IS NULL AND isExample = 0")
    suspend fun claimAllUnownedFlocks(owner: String)

    /** "Add to my account": only pens that are in no account yet can be taken. */
    @Query("UPDATE flocks SET ownerId = :owner, syncState = 'PENDING' WHERE ownerId IS NULL AND isExample = 0 AND id IN (:ids)")
    suspend fun claimFlocks(ids: List<String>, owner: String)

    @Query("UPDATE daily_records SET syncState = 'PENDING' WHERE flockId IN (:flockIds)")
    suspend fun markRecordsPendingFor(flockIds: List<String>)

    @Query("UPDATE weekly_notes SET syncState = 'PENDING' WHERE flockId IN (:flockIds)")
    suspend fun markWeeklyNotesPendingFor(flockIds: List<String>)

    @Query("UPDATE flocks SET ownerId = :owner WHERE id = :id")
    suspend fun setFlockOwner(id: String, owner: String)

    /** Account deleted or signed out by the server: the pens stay on the phone, in no account. */
    @Query("UPDATE flocks SET ownerId = NULL WHERE ownerId = :owner")
    suspend fun releaseFlocks(owner: String)

    @Query(
        "SELECT f.id AS id, f.name AS name, " +
            "(SELECT COUNT(*) FROM daily_records r WHERE r.flockId = f.id AND r.deleted = 0) AS days " +
            "FROM flocks f WHERE f.ownerId IS NULL AND f.isExample = 0 AND f.deleted = 0 ORDER BY f.name"
    )
    fun observeUnownedPens(): Flow<List<UnownedPen>>

    /** Pens hidden while signed out because they belong to an account. */
    @Query("SELECT COUNT(*) FROM flocks WHERE ownerId IS NOT NULL AND deleted = 0 AND isExample = 0")
    fun observeAccountPenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flocks WHERE ownerId = :owner AND deleted = 0")
    suspend fun countOwnedFlocks(owner: String): Int

    @Query("SELECT COUNT(*) FROM daily_records WHERE deleted = 0 AND flockId IN (SELECT id FROM flocks WHERE ownerId = :owner AND deleted = 0)")
    suspend fun countOwnedRecords(owner: String): Int

    /** For the daily reminder: active pens of [owner] with no record for [date]. */
    @Query(
        "SELECT name FROM flocks WHERE deleted = 0 AND archived = 0 AND isExample = 0 AND ownerId IS :owner " +
            "AND id NOT IN (SELECT flockId FROM daily_records WHERE date = :date AND deleted = 0)"
    )
    suspend fun penNamesMissingRecord(date: String, owner: String?): List<String>
}

/** A pen on this phone that is in no account, with how many days it has records for. */
data class UnownedPen(val id: String, val name: String, val days: Int)

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

/**
 * v2 -> v3: each pen remembers which account it belongs to, so signing in on a phone never mixes
 * two farmers' records. Pens already backed up belong to whoever is signed in now; the rest are
 * settled by a full download from the server on the next sync.
 */
fun migration2To3(prefs: AppPrefs) = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE flocks ADD COLUMN ownerId TEXT")
        val owner = prefs.currentOwner ?: return
        db.execSQL("UPDATE flocks SET ownerId = ? WHERE isExample = 0 AND syncState = 'SYNCED'", arrayOf(owner))
        prefs.syncCursor = 0L
        prefs.accountChecked = false
    }
}

@Database(
    entities = [FlockEntity::class, DailyRecordEntity::class, WeeklyNoteEntity::class],
    version = 3,
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
                    .addMigrations(MIGRATION_1_2, migration2To3(AppPrefs(context)))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
