package com.keeptrack.timeclicker

import android.app.Application
import com.keeptrack.timeclicker.data.IconCatalog
import com.keeptrack.timeclicker.reminder.Reminders
import com.keeptrack.timeclicker.widget.TileWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TimeClickerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Read ahead, so tiles have their icons by the time the home screen shows them.
        // A failure here only means no icons: tiles still work.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { IconCatalog.load(this@TimeClickerApplication) }
        }

        // Home-screen widgets follow every change to the tiles (done, undone, edited, deleted).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.trackerRepository.observeTrackers()
                .distinctUntilChanged()
                .drop(1) // the tiles as they are at start-up: nothing changed yet
                .collectLatest { TileWidgets.refresh(this@TimeClickerApplication) }
        }
        // ...and the settings they follow (press counter, system colours, time display).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.settingsRepository.settings
                .map { Triple(it.showCounter, it.dynamicColors, it.timeDisplay) }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { TileWidgets.refresh(this@TimeClickerApplication) }
        }
        // Reminders follow the tiles' last times and reminder settings; checked at start-up too.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.trackerRepository.observeTrackers()
                .map { trackers -> trackers.map { Triple(it.id, it.lastDoneAt, it.reminder) } }
                .distinctUntilChanged()
                .collectLatest { Reminders.update(this@TimeClickerApplication) }
        }
    }
}
