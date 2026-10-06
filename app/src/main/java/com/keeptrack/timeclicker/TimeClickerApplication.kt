package com.keeptrack.timeclicker

import android.app.Application
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
    }
}
