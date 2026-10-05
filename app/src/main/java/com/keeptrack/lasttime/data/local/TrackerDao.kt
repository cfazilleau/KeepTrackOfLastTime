package com.keeptrack.lasttime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    @Query(
        """
        SELECT t.*, (SELECT MAX(e.done_at) FROM tracker_events e WHERE e.tracker_id = t.id) AS last_done_at
        FROM trackers t
        ORDER BY t.position, t.created_at
        """
    )
    fun observeTrackers(): Flow<List<TrackerWithLastDone>>

    @Insert
    suspend fun insertTracker(tracker: TrackerEntity): Long

    @Insert
    suspend fun insertEvent(event: TrackerEventEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM trackers")
    suspend fun nextPosition(): Int

    @Transaction
    suspend fun createTracker(name: String, now: Long): Long {
        val id = insertTracker(TrackerEntity(name = name, createdAt = now, position = nextPosition()))
        insertEvent(TrackerEventEntity(trackerId = id, doneAt = now))
        return id
    }

    @Query("UPDATE trackers SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTracker(id: Long)

    @Query("DELETE FROM tracker_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)
}
