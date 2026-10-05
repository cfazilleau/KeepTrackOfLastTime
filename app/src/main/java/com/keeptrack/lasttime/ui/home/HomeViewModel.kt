package com.keeptrack.lasttime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keeptrack.lasttime.LastTimeApplication
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.data.TrackerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Emitted after a card is reset so the UI can offer an undo. */
data class ResetDone(val trackerName: String, val eventId: Long)

class HomeViewModel(private val repository: TrackerRepository) : ViewModel() {

    /** `null` while the first database read is in flight. */
    val trackers: StateFlow<List<Tracker>?> = repository.observeTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val resetEvents = Channel<ResetDone>(Channel.BUFFERED)
    val resets: Flow<ResetDone> = resetEvents.receiveAsFlow()

    fun add(name: String) {
        viewModelScope.launch { repository.addTracker(name.trim()) }
    }

    fun markDone(tracker: Tracker) {
        viewModelScope.launch {
            val eventId = repository.markDone(tracker.id)
            resetEvents.send(ResetDone(tracker.name, eventId))
        }
    }

    fun undoReset(reset: ResetDone) {
        viewModelScope.launch { repository.undoMarkDone(reset.eventId) }
    }

    fun rename(tracker: Tracker, name: String) {
        viewModelScope.launch { repository.rename(tracker.id, name.trim()) }
    }

    fun delete(tracker: Tracker) {
        viewModelScope.launch { repository.delete(tracker.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastTimeApplication
                HomeViewModel(app.container.trackerRepository)
            }
        }
    }
}
