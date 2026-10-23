package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "flocks")
data class FlockEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val breed: String,
    val hensHoused: Int,
    val placedDate: String, // ISO YYYY-MM-DD
    val ageWeeksAtPlacement: Int,
    val archived: Boolean = false,
    val isExample: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
)
