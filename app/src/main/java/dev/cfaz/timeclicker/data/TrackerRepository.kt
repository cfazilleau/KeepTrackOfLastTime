package dev.cfaz.timeclicker.data

import android.net.Uri
import dev.cfaz.timeclicker.data.local.GroupEntity
import dev.cfaz.timeclicker.data.local.TrackerDao
import dev.cfaz.timeclicker.data.local.TrackerEntity
import dev.cfaz.timeclicker.data.local.TrackerEventEntity
import dev.cfaz.timeclicker.data.local.TrackerWithLastDone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Clock
import java.time.Instant

class TrackerRepository(
    private val dao: TrackerDao,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun observeTrackers(): Flow<List<Tracker>> = dao.observeTrackers().map { rows -> rows.map { it.toModel() } }

    suspend fun tracker(id: Long): Tracker? = dao.tracker(id)?.toModel()

    fun observeGroups(): Flow<List<TrackerGroup>> = dao.observeGroups().map { rows ->
        rows.map { it.toModel() }
    }

    /** Restarts the tile's press counter at 0; its history is kept. */
    suspend fun resetCount(trackerId: Long) = dao.resetCount(trackerId, clock.millis())

    suspend fun addTracker(spec: TileSpec): Long {
        val id = dao.createTracker(
            TrackerEntity(
                name = spec.name,
                createdAt = clock.millis(),
                position = 0, // assigned by the DAO
                groupId = spec.groupId,
                color = spec.color.key,
                icon = spec.icon.key,
                size = spec.size.key,
                photo = spec.photo,
                reminderEvery = spec.reminder?.every,
                reminderUnit = spec.reminder?.unitKey,
            )
        )
        cleanUpPhotos()
        return id
    }

    suspend fun updateTracker(trackerId: Long, spec: TileSpec) {
        dao.updateTracker(
            trackerId, spec.name, spec.groupId, spec.color.key, spec.icon.key, spec.size.key, spec.photo,
            spec.reminder?.every, spec.reminder?.unitKey,
        )
        cleanUpPhotos()
    }

    suspend fun delete(trackerId: Long) {
        dao.deleteTracker(trackerId)
        cleanUpPhotos()
    }

    /** Reorders some trackers among themselves, e.g. one group's; the others don't move. */
    suspend fun reorderTrackers(orderedIds: List<Long>) = dao.reorderTrackers(orderedIds)

    /** How often the tracker is usually done, from its recent history; null when it isn't regular. */
    suspend fun rhythm(trackerId: Long): Rhythm? =
        Rhythm.of(dao.recentEventTimes(trackerId, Rhythm.EVENTS_USED).map(Instant::ofEpochMilli))

    /** Records that the tracker was done now. Returns the event id, usable with [undoMarkDone]. */
    suspend fun markDone(trackerId: Long): Long =
        dao.insertEvent(TrackerEventEntity(trackerId = trackerId, doneAt = clock.millis()))

    suspend fun undoMarkDone(eventId: Long) = dao.deleteEvent(eventId)

    /**
     * Deletes the tracker's last press counted by its counter; the tile's creation, and presses before a counter
     * reset, are kept. False if there was none.
     */
    suspend fun revertLastPress(trackerId: Long): Boolean = dao.deleteLastCountedEvent(trackerId) > 0

    // --- Photos ---

    suspend fun importPhoto(uri: Uri): String? = photos.import(uri)

    fun photoFile(name: String): File = photos.file(name)

    /** Removes photo files no tile points to any more. */
    suspend fun cleanUpPhotos() = photos.retainOnly(dao.photoNames().toSet())

    // --- Groups ---

    suspend fun createGroup(name: String, icon: TileIcon = TileIcon.NONE): Long = dao.createGroup(name, icon.key)

    suspend fun updateGroup(groupId: Long, name: String, icon: TileIcon) = dao.updateGroup(groupId, name, icon.key)

    suspend fun deleteGroup(groupId: Long) = dao.deleteGroup(groupId)

    suspend fun reorderGroups(orderedIds: List<Long>) = dao.reorderGroups(orderedIds)
}

private fun GroupEntity.toModel() = TrackerGroup(id, name, TileIcon.fromKey(icon))

private fun TrackerWithLastDone.toModel() = Tracker(
    id = tracker.id,
    name = tracker.name,
    lastDoneAt = Instant.ofEpochMilli(lastDoneAt ?: tracker.createdAt),
    groupId = tracker.groupId,
    color = TileColor.fromKey(tracker.color),
    icon = TileIcon.fromKey(tracker.icon),
    size = TileSize.fromKey(tracker.size),
    photo = tracker.photo,
    pressCount = pressCount,
    reminder = Reminder.fromColumns(tracker.reminderEvery, tracker.reminderUnit),
)
