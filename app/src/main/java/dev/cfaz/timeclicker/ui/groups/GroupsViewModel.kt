package dev.cfaz.timeclicker.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cfaz.timeclicker.TimeClickerApplication
import dev.cfaz.timeclicker.data.TileIcon
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.data.TrackerGroup
import dev.cfaz.timeclicker.data.TrackerRepository
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

    fun add(name: String, icon: TileIcon) {
        viewModelScope.launch { repository.createGroup(name.trim(), icon) }
    }

    fun update(group: TrackerGroup, name: String, icon: TileIcon) {
        viewModelScope.launch { repository.updateGroup(group.id, name.trim(), icon) }
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
                val app = this[APPLICATION_KEY] as TimeClickerApplication
                GroupsViewModel(app.container.trackerRepository)
            }
        }
    }
}
