package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_records",
    indices = [Index(value = ["flockId", "date"])]
)
data class DailyRecordEntity(
    @PrimaryKey
    val id: String, // flockId + "_" + date
    val flockId: String,
    val date: String, // ISO YYYY-MM-DD
    val liveHens: Int,
    val eggs: Int,
    val cracked: Int = 0,
    val dirty: Int = 0,
    val softShell: Int = 0,
    val deaths: Int = 0,
    val culls: Int = 0,
    val feedKg: Double = 0.0,
    val waterL: Double = 0.0,
    val avgEggWeightG: Double? = null,
    val maxTempC: Double? = null,
    val notes: String = "",
    val enteredBy: String = "local_farmer",
    val enteredByName: String = "Farmer",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    companion object {
        fun buildId(flockId: String, date: String): String = "${flockId}_${date}"
    }
}
