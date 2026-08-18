package com.trackerapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: PendingEvent)

    @Query("SELECT * FROM pending_events ORDER BY createdAt ASC")
    fun getAll(): List<PendingEvent>

    @Query("DELETE FROM pending_events WHERE eventId = :eventId")
    fun deleteById(eventId: String)
}
