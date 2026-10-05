package com.keeptrack.lasttime

import android.content.Context
import com.keeptrack.lasttime.data.TrackerRepository
import com.keeptrack.lasttime.data.local.AppDatabase

/** Manual dependency container; swap for Hilt/Koin if the object graph grows. */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)

    val trackerRepository = TrackerRepository(database.trackerDao())
}
