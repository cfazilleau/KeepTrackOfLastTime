package com.keeptrack.lasttime.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local SQLite database stored in the app's private storage.
 *
 * When changing the schema: bump [version], keep the exported JSON in app/schemas,
 * add an [AutoMigration] (or a manual Migration) and extend MigrationTest.
 */
@Database(
    entities = [GroupEntity::class, TrackerEntity::class, TrackerEventEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.Migration1To2::class),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    /** v2 adds groups and tile styling. Existing trackers get the colours in turn, like new ones. */
    class Migration1To2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Same order as TileColor.entries.
            db.execSQL(
                """
                UPDATE trackers SET color = CASE ((SELECT COUNT(*) FROM trackers t2 WHERE t2.position < trackers.position OR (t2.position = trackers.position AND t2.id < trackers.id)) % 6)
                    WHEN 0 THEN 'sage' WHEN 1 THEN 'lavender' WHEN 2 THEN 'peach'
                    WHEN 3 THEN 'sky' WHEN 4 THEN 'butter' ELSE 'rose' END
                """
            )
        }
    }

    companion object {
        const val NAME = "last_time.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
