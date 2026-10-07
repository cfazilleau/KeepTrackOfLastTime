package dev.cfaz.timeclicker

import android.content.Context
import dev.cfaz.timeclicker.data.BackupRepository
import dev.cfaz.timeclicker.data.PhotoStore
import dev.cfaz.timeclicker.data.SettingsRepository
import dev.cfaz.timeclicker.data.TrackerRepository
import dev.cfaz.timeclicker.data.local.AppDatabase

/** Manual dependency container; swap for Hilt/Koin if the object graph grows. */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    private val photoStore = PhotoStore(context)

    val trackerRepository = TrackerRepository(database.trackerDao(), photoStore)

    val settingsRepository = SettingsRepository(context)

    val backupRepository = BackupRepository(context, database.trackerDao(), photoStore, settingsRepository)
}
