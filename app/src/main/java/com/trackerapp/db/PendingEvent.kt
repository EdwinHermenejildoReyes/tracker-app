package com.trackerapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_events")
data class PendingEvent(
    @PrimaryKey val eventId: String,
    val latitude: Double,
    val longitude: Double,
    val deviceId: String,
    val eventType: String,
    val createdAt: Long = System.currentTimeMillis(),
)
