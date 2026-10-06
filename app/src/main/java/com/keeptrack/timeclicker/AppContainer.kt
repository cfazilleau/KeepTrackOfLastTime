package com.keeptrack.timeclicker

import android.content.Context
import com.keeptrack.timeclicker.data.BackupRepository
import com.keeptrack.timeclicker.data.PhotoStore
import com.keeptrack.timeclicker.data.SettingsRepository
import com.keeptrack.timeclicker.data.TilePresses
import com.keeptrack.timeclicker.data.TrackerRepository
import com.keeptrack.timeclicker.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container; swap for Hilt/Koin if the object graph grows. */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    private val photoStore = PhotoStore(context)

    val trackerRepository = TrackerRepository(database.trackerDao(), photoStore)

    // App-wide, so an undo window keeps running when the screen that started it goes away.
    val tilePresses = TilePresses(trackerRepository, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    val settingsRepository = SettingsRepository(context)

    val backupRepository = BackupRepository(context, database.trackerDao(), photoStore, settingsRepository)
}
