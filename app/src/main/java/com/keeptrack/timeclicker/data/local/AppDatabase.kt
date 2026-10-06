package com.keeptrack.timeclicker.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keeptrack.timeclicker.data.TileIcon

/**
 * Local SQLite database stored in the app's private storage.
 *
 * When changing the schema: bump [version], keep the exported JSON in app/schemas,
 * add an [AutoMigration] (or a manual Migration) and extend MigrationTest.
 */
@Database(
    entities = [GroupEntity::class, TrackerEntity::class, TrackerEventEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.Migration1To2::class),
        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migration2To3::class),
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

    /** v3 adds the press counter. It counts the presses after creation, so not the event created with the tile. */
    class Migration2To3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE trackers SET count_since = created_at")
        }
    }

    companion object {
        /** Named after the app's first name; kept so existing data is found. */
        const val NAME = "last_time.db"

        /**
         * v4 stores tile icons by their Lucide name: the app's own names become the Lucide icon that replaced them.
         * Same schema, so a plain migration.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cases = TileIcon.legacyKeys.entries.joinToString(" ") { (old, new) -> "WHEN '$old' THEN '$new'" }
                db.execSQL("UPDATE trackers SET icon = CASE icon $cases ELSE icon END")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).addMigrations(MIGRATION_3_4).build()
    }
}
