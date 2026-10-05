package com.keeptrack.lasttime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    // --- Trackers ---

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

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM trackers")
    suspend fun nextTrackerPosition(): Int

    @Query("SELECT COUNT(*) FROM trackers")
    suspend fun trackerCount(): Int

    @Transaction
    suspend fun createTracker(tracker: TrackerEntity): Long {
        val id = insertTracker(tracker.copy(position = nextTrackerPosition()))
        insertEvent(TrackerEventEntity(trackerId = id, doneAt = tracker.createdAt))
        return id
    }

    @Query(
        """
        UPDATE trackers
        SET name = :name, group_id = :groupId, color = :color, icon = :icon, size = :size, photo = :photo
        WHERE id = :id
        """
    )
    suspend fun updateTracker(
        id: Long,
        name: String,
        groupId: Long?,
        color: String,
        icon: String,
        size: String,
        photo: String?,
    )

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTracker(id: Long)

    @Query("SELECT photo FROM trackers WHERE photo IS NOT NULL")
    suspend fun photoNames(): List<String>

    // --- Events ---

    @Insert
    suspend fun insertEvent(event: TrackerEventEntity): Long

    @Query("DELETE FROM tracker_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)

    // --- Groups ---

    @Query("SELECT * FROM tracker_groups ORDER BY position, id")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tracker_groups")
    suspend fun nextGroupPosition(): Int

    @Transaction
    suspend fun createGroup(name: String): Long =
        insertGroup(GroupEntity(name = name, position = nextGroupPosition()))

    @Query("UPDATE tracker_groups SET name = :name WHERE id = :id")
    suspend fun renameGroup(id: Long, name: String)

    @Query("DELETE FROM tracker_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("UPDATE tracker_groups SET position = :position WHERE id = :id")
    suspend fun setGroupPosition(id: Long, position: Int)

    @Transaction
    suspend fun reorderGroups(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setGroupPosition(id, index) }
    }
}
