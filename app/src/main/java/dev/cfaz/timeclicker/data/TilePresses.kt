package dev.cfaz.timeclicker.data

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How long a tile stays in its "press again to undo" state after being marked done. */
const val UNDO_WINDOW_MS = 10_000L

/** A tile just marked done: pressing it again before [until] (a [SystemClock.elapsedRealtime]) undoes it. */
data class PendingUndo(val eventId: Long, val until: Long)

/**
 * Presses on tiles, in the app or on a widget. A press marks the tile done; pressing it again within
 * [UNDO_WINDOW_MS] undoes that instead. Shared, so a press on a widget can be undone in the app and the other way round.
 * The windows are kept in memory, and rebuilt from the stored presses' times when the app's process restarts.
 */
class TilePresses(private val repository: TrackerRepository, private val scope: CoroutineScope) {

    private val pending = MutableStateFlow<Map<Long, PendingUndo>>(emptyMap())

    /** By tracker id: the tiles a press would undo rather than mark done. */
    val undoable: StateFlow<Map<Long, PendingUndo>> = pending.asStateFlow()

    private val expiries = mutableMapOf<Long, Job>()

    // Presses are handled one at a time, so a quick second press sees the first one's event.
    private val mutex = Mutex()

    init {
        scope.launch { restore() }
    }

    /** Reopens the undo window of each tile pressed less than [UNDO_WINDOW_MS] ago, e.g. before the app was closed. */
    private suspend fun restore() = mutex.withLock {
        for ((trackerId, press) in repository.recentPresses(UNDO_WINDOW_MS)) {
            val (eventId, age) = press
            if (trackerId in pending.value) continue
            open(trackerId, eventId, UNDO_WINDOW_MS - age)
        }
    }

    // Must be called with the mutex held.
    private fun open(trackerId: Long, eventId: Long, remainingMs: Long) {
        pending.update { it + (trackerId to PendingUndo(eventId, SystemClock.elapsedRealtime() + remainingMs)) }
        expiries[trackerId] = scope.launch {
            delay(remainingMs)
            mutex.withLock {
                expiries.remove(trackerId)
                pending.update { it - trackerId }
            }
        }
    }

    /** Marks the tile done now, or undoes its last press if still in its undo window. True if it was marked done. */
    suspend fun press(trackerId: Long): Boolean = mutex.withLock {
        expiries.remove(trackerId)?.cancel()
        val undo = pending.value[trackerId]
        if (undo != null) {
            pending.update { it - trackerId }
            repository.undoMarkDone(undo.eventId)
            false
        } else {
            val eventId = repository.markDone(trackerId)
            open(trackerId, eventId, UNDO_WINDOW_MS)
            true
        }
    }

    /** Deletes the tile's last counted press, ending its undo window if it has one. False if there was none. */
    suspend fun revertLast(trackerId: Long): Boolean = mutex.withLock {
        expiries.remove(trackerId)?.cancel()
        pending.update { it - trackerId }
        repository.revertLastPress(trackerId)
    }
}
