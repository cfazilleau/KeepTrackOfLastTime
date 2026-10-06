package com.keeptrack.timeclicker.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keeptrack.timeclicker.TimeClickerApplication
import com.keeptrack.timeclicker.data.SettingsRepository
import com.keeptrack.timeclicker.data.TileIcon
import com.keeptrack.timeclicker.data.TileSize
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.data.TrackerGroup
import com.keeptrack.timeclicker.data.TrackerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Which tiles the home screen shows. */
sealed interface GroupFilter {
    data object All : GroupFilter
    data class Group(val id: Long) : GroupFilter

    /** Stable and saveable, for lazy layout keys. */
    val key: String
        get() = when (this) {
            All -> "all"
            is Group -> "group-$id"
        }
}

data class FilterChipUi(val filter: GroupFilter, val label: String?, val count: Int)

sealed interface SectionTitle {
    data class Group(val name: String) : SectionTitle
    /** Tiles without a group: a plain grid without a heading, only on the All page. */
    data object None : SectionTitle
}

data class SectionUi(val key: String, val title: SectionTitle, val trackers: List<Tracker>)

/** One swipeable page of the home screen: what one chip shows. */
data class PageUi(val chip: FilterChipUi, val sections: List<SectionUi>)

data class HomeUiState(
    val trackerCount: Int,
    val groups: List<TrackerGroup>,
    /** In chip order: All, then each group. */
    val pages: List<PageUi>,
    /** The page shown; falls back to All when its group is deleted. */
    val filter: GroupFilter,
) {
    val selectedPage: Int get() = pages.indexOfFirst { it.chip.filter == filter }.coerceAtLeast(0)
}

/** Emitted after a card is reset so the UI can offer an undo. */
data class ResetDone(val trackerName: String, val eventId: Long)

class HomeViewModel(
    private val repository: TrackerRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<GroupFilter>(GroupFilter.All)

    /** `null` while the first database read is in flight. */
    val state: StateFlow<HomeUiState?> =
        combine(repository.observeTrackers(), repository.observeGroups(), filter, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val resetEvents = Channel<ResetDone>(Channel.BUFFERED)
    val resets: Flow<ResetDone> = resetEvents.receiveAsFlow()

    fun select(newFilter: GroupFilter) {
        filter.value = newFilter
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

    /** A blank tile for the "+" button, pre-filed in the group currently shown. */
    suspend fun newDraft(): TileDraft = TileDraft(
        trackerId = null,
        name = "",
        groupId = (state.value?.filter as? GroupFilter.Group)?.id,
        color = repository.nextColor(),
        icon = TileIcon.DEFAULT,
        size = TileSize.SMALL,
        photo = null,
    )

    /** Saves the sheet's [draft]; [saved] is the tile as it was before, null for a new one. */
    fun save(draft: TileDraft, saved: Tracker?) {
        // Only a newly given icon counts as used: reopening a tile to rename it doesn't reorder the history.
        if (draft.icon != saved?.icon) settings.recordIconUse(draft.icon)
        viewModelScope.launch {
            val spec = draft.toSpec()
            if (draft.trackerId == null) {
                repository.addTracker(spec)
            } else {
                repository.updateTracker(draft.trackerId, spec)
                if (draft.resetCount) repository.resetCount(draft.trackerId)
            }
        }
    }

    /** The sheet closed without saving: drop any photo picked for it. */
    fun discard() {
        viewModelScope.launch { repository.cleanUpPhotos() }
    }

    fun delete(trackerId: Long) {
        viewModelScope.launch { repository.delete(trackerId) }
    }

    suspend fun createGroup(name: String): Long = repository.createGroup(name.trim())

    suspend fun importPhoto(uri: Uri): String? = repository.importPhoto(uri)

    fun photoFile(name: String): File = repository.photoFile(name)

    private fun buildState(trackers: List<Tracker>, groups: List<TrackerGroup>, requested: GroupFilter): HomeUiState {
        val byGroup = trackers.groupBy { it.groupId }
        val ungrouped = byGroup[null].orEmpty()
        // A deleted group falls back to everything.
        val current = when (requested) {
            is GroupFilter.Group -> if (groups.any { it.id == requested.id }) requested else GroupFilter.All
            GroupFilter.All -> requested
        }

        val pages = buildList {
            // Tiles without a group only show on All, first and without a heading, so they don't read as part of a group.
            val all = buildList {
                if (ungrouped.isNotEmpty()) add(SectionUi("ungrouped", SectionTitle.None, ungrouped))
                groups.forEach { g ->
                    byGroup[g.id]?.let { add(SectionUi("group-${g.id}", SectionTitle.Group(g.name), it)) }
                }
            }
            add(PageUi(FilterChipUi(GroupFilter.All, null, trackers.size), all))
            groups.forEach { g ->
                val inGroup = byGroup[g.id].orEmpty()
                val section = SectionUi("group-${g.id}", SectionTitle.Group(g.name), inGroup)
                add(PageUi(FilterChipUi(GroupFilter.Group(g.id), g.name, inGroup.size), listOf(section)))
            }
        }
        return HomeUiState(trackers.size, groups, pages, current)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TimeClickerApplication
                HomeViewModel(app.container.trackerRepository, app.container.settingsRepository)
            }
        }
    }
}
