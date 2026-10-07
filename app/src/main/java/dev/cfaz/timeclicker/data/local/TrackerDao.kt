package dev.cfaz.timeclicker.data.local

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
        SELECT t.*,
            (SELECT MAX(e.done_at) FROM tracker_events e WHERE e.tracker_id = t.id) AS last_done_at,
            (SELECT COUNT(*) FROM tracker_events e WHERE e.tracker_id = t.id AND e.done_at > t.count_since) AS press_count
        FROM trackers t
        ORDER BY t.position, t.created_at
        """
    )
    fun observeTrackers(): Flow<List<TrackerWithLastDone>>

    @Query(
        """
        SELECT t.*,
            (SELECT MAX(e.done_at) FROM tracker_events e WHERE e.tracker_id = t.id) AS last_done_at,
            (SELECT COUNT(*) FROM tracker_events e WHERE e.tracker_id = t.id AND e.done_at > t.count_since) AS press_count
        FROM trackers t
        WHERE t.id = :id
        """
    )
    suspend fun tracker(id: Long): TrackerWithLastDone?

    @Insert
    suspend fun insertTracker(tracker: TrackerEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM trackers")
    suspend fun nextTrackerPosition(): Int

    /** Inserts the tracker and its first event; the press counter starts after that event. */
    @Transaction
    suspend fun createTracker(tracker: TrackerEntity): Long {
        val id = insertTracker(tracker.copy(position = nextTrackerPosition(), countSince = tracker.createdAt))
        insertEvent(TrackerEventEntity(trackerId = id, doneAt = tracker.createdAt))
        return id
    }

    @Query(
        """
        UPDATE trackers
        SET name = :name, group_id = :groupId, color = :color, icon = :icon, size = :size, photo = :photo,
            reminder_every = :reminderEvery, reminder_unit = :reminderUnit
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
        reminderEvery: Int?,
        reminderUnit: String?,
    )

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTracker(id: Long)

    @Query("UPDATE trackers SET count_since = :since WHERE id = :id")
    suspend fun resetCount(id: Long, since: Long)

    @Query("SELECT id FROM trackers ORDER BY position, created_at")
    suspend fun orderedTrackerIds(): List<Long>

    @Query("UPDATE trackers SET position = :position WHERE id = :id")
    suspend fun setTrackerPosition(id: Long, position: Int)

    /**
     * Puts [orderedIds] in that order, in the places they already take among all trackers:
     * the other trackers keep theirs. Every tracker is renumbered, so positions stay distinct.
     */
    @Transaction
    suspend fun reorderTrackers(orderedIds: List<Long>) {
        val all = orderedTrackerIds()
        val moved = orderedIds.filter { it in all }
        val movedSet = moved.toSet()
        val next = moved.iterator()
        all.map { id -> if (id in movedSet) next.next() else id }
            .forEachIndexed { index, id -> setTrackerPosition(id, index) }
    }

    @Query("SELECT photo FROM trackers WHERE photo IS NOT NULL")
    suspend fun photoNames(): List<String>

    // --- Events ---

    @Insert
    suspend fun insertEvent(event: TrackerEventEntity): Long

    @Query("DELETE FROM tracker_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Long)

    /** Deletes the tracker's latest event counted by its press counter. Returns how many were deleted: 0 or 1. */
    @Query(
        """
        DELETE FROM tracker_events WHERE id = (
            SELECT e.id FROM tracker_events e JOIN trackers t ON t.id = e.tracker_id
            WHERE e.tracker_id = :trackerId AND e.done_at > t.count_since
            ORDER BY e.done_at DESC, e.id DESC
            LIMIT 1
        )
        """
    )
    suspend fun deleteLastCountedEvent(trackerId: Long): Int

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

    // --- Backups ---

    @Query("SELECT * FROM tracker_groups")
    suspend fun allGroups(): List<GroupEntity>

    @Query("SELECT * FROM trackers")
    suspend fun allTrackers(): List<TrackerEntity>

    @Query("SELECT * FROM tracker_events")
    suspend fun allEvents(): List<TrackerEventEntity>

    @Insert
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert
    suspend fun insertTrackers(trackers: List<TrackerEntity>)

    @Insert
    suspend fun insertEvents(events: List<TrackerEventEntity>)

    @Query("DELETE FROM tracker_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM trackers")
    suspend fun deleteAllTrackers()

    @Query("DELETE FROM tracker_groups")
    suspend fun deleteAllGroups()

    /** Replaces everything with a backup's content, keeping its ids (widgets and events refer to them). */
    @Transaction
    suspend fun replaceAll(groups: List<GroupEntity>, trackers: List<TrackerEntity>, events: List<TrackerEventEntity>) {
        deleteAllEvents()
        deleteAllTrackers()
        deleteAllGroups()
        insertGroups(groups)
        insertTrackers(trackers)
        insertEvents(events)
    }
}
