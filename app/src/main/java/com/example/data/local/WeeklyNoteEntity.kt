package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Management notes and corrective actions for one week of a flock's age.
 */
@Entity(tableName = "weekly_notes")
data class WeeklyNoteEntity(
    @PrimaryKey
    val id: String, // flockId + "_w" + ageWeek
    val flockId: String,
    val ageWeek: Int,
    val notes: String = "",
    val actions: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    companion object {
        fun buildId(flockId: String, ageWeek: Int): String = "${flockId}_w$ageWeek"
    }
}
