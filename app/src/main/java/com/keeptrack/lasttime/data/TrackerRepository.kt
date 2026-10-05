package com.keeptrack.lasttime.data

import com.keeptrack.lasttime.data.local.TrackerDao
import com.keeptrack.lasttime.data.local.TrackerEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

class TrackerRepository(
    private val dao: TrackerDao,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun observeTrackers(): Flow<List<Tracker>> = dao.observeTrackers().map { rows ->
        rows.map { row ->
            Tracker(
                id = row.tracker.id,
                name = row.tracker.name,
                lastDoneAt = Instant.ofEpochMilli(row.lastDoneAt ?: row.tracker.createdAt),
            )
        }
    }

    suspend fun addTracker(name: String): Long = dao.createTracker(name, clock.millis())

    /** Records that the tracker was done now. Returns the event id, usable with [undoMarkDone]. */
    suspend fun markDone(trackerId: Long): Long =
        dao.insertEvent(TrackerEventEntity(trackerId = trackerId, doneAt = clock.millis()))

    suspend fun undoMarkDone(eventId: Long) = dao.deleteEvent(eventId)

    suspend fun rename(trackerId: Long, name: String) = dao.rename(trackerId, name)

    suspend fun delete(trackerId: Long) = dao.deleteTracker(trackerId)
}
