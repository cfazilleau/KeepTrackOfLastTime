package com.keeptrack.lasttime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local SQLite database stored in the app's private storage.
 *
 * When changing the schema: bump [version], keep the exported JSON in app/schemas,
 * and add a migration (e.g. `@Database(autoMigrations = [AutoMigration(from = 1, to = 2)])`).
 */
@Database(
    entities = [TrackerEntity::class, TrackerEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "last_time.db").build()
    }
}
