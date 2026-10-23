package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
}

@Database(
    entities = [FlockEntity::class, DailyRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SyncStateConverters::class)
abstract class ClutchDatabase : RoomDatabase() {
    abstract fun clutchDao(): ClutchDao
}
