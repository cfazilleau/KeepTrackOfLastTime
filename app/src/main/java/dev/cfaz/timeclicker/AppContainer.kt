package dev.cfaz.timeclicker

import android.content.Context
<<<<<<< HEAD:app/src/main/java/dev/cfaz/timeclicker/AppContainer.kt
import dev.cfaz.timeclicker.data.BackupRepository
import dev.cfaz.timeclicker.data.PhotoStore
import dev.cfaz.timeclicker.data.SettingsRepository
import dev.cfaz.timeclicker.data.TrackerRepository
import dev.cfaz.timeclicker.data.local.AppDatabase
=======
import dev.cfaz.timeclicker.data.BackupRepository
import dev.cfaz.timeclicker.data.PhotoStore
import dev.cfaz.timeclicker.data.SettingsRepository
import dev.cfaz.timeclicker.data.TilePresses
import dev.cfaz.timeclicker.data.TrackerRepository
import dev.cfaz.timeclicker.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
>>>>>>> origin/main:app/src/main/java/dev/cfaz/timeclicker/AppContainer.kt

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
