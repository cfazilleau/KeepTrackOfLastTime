package com.keeptrack.lasttime.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keeptrack.lasttime.LastTimeApplication
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.data.TrackerGroup
import com.keeptrack.lasttime.data.TrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class GroupRowUi(val group: TrackerGroup, val tiles: List<Tracker>)

class GroupsViewModel(private val repository: TrackerRepository) : ViewModel() {

    /** `null` while the first database read is in flight. */
    val rows: StateFlow<List<GroupRowUi>?> =
        combine(repository.observeGroups(), repository.observeTrackers()) { groups, trackers ->
            val byGroup = trackers.groupBy { it.groupId }
            groups.map { GroupRowUi(it, byGroup[it.id].orEmpty()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String) {
        viewModelScope.launch { repository.createGroup(name.trim()) }
    }

    fun rename(group: TrackerGroup, name: String) {
        viewModelScope.launch { repository.renameGroup(group.id, name.trim()) }
    }

    fun delete(group: TrackerGroup) {
        viewModelScope.launch { repository.deleteGroup(group.id) }
    }

    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderGroups(orderedIds) }
    }

    fun photoFile(name: String): File = repository.photoFile(name)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastTimeApplication
                GroupsViewModel(app.container.trackerRepository)
            }
        }
    }
}
