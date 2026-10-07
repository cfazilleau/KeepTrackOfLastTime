package dev.cfaz.timeclicker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(AppDatabase.Migration1To2(), AppDatabase.Migration2To3()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_keepsTrackersAndHistory_andAssignsColoursInTurn() {
        helper.createDatabase(DB, 1).use { db ->
            for (i in 0 until 7) {
                db.execSQL("INSERT INTO trackers (id, name, created_at, position) VALUES (${i + 1}, 'Tracker $i', 1000, $i)")
                db.execSQL("INSERT INTO tracker_events (tracker_id, done_at) VALUES (${i + 1}, ${2000 + i})")
            }
        }

        // Validates the migrated schema against the v2 entities.
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT name, color, icon, size, group_id, photo FROM trackers ORDER BY position").use { c ->
                val colours = mutableListOf<String>()
                while (c.moveToNext()) {
                    colours += c.getString(1)
                    assertEquals("check", c.getString(2))
                    assertEquals("small", c.getString(3))
                    assertNull(c.getString(4))
                    assertNull(c.getString(5))
                }
                assertEquals(listOf("sage", "lavender", "peach", "sky", "butter", "rose", "sage"), colours)
            }
            db.query("SELECT COUNT(*) FROM tracker_events").use { c ->
                c.moveToFirst()
                assertEquals(7, c.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3_countsPressesAfterCreationOnly() {
        helper.createDatabase(DB_V3, 2).use { db ->
            db.execSQL("INSERT INTO trackers (id, name, created_at, position) VALUES (1, 'Plants', 1000, 0)")
            // The event created with the tile, then three presses.
            for (doneAt in listOf(1000, 2000, 3000, 4000)) {
                db.execSQL("INSERT INTO tracker_events (tracker_id, done_at) VALUES (1, $doneAt)")
            }
        }

        helper.runMigrationsAndValidate(DB_V3, 3, true).use { db ->
            db.query(
                """
                SELECT t.count_since,
                    (SELECT COUNT(*) FROM tracker_events e WHERE e.tracker_id = t.id AND e.done_at > t.count_since)
                FROM trackers t WHERE t.id = 1
                """
            ).use { c ->
                c.moveToFirst()
                assertEquals(1000L, c.getLong(0))
                assertEquals(3, c.getInt(1))
            }
        }
    }

    @Test
    fun migrate3To4_renamesIconsToLucide() {
        helper.createDatabase(DB_V4, 3).use { db ->
            listOf("check", "paw", "leaf", "flower").forEachIndexed { i, icon ->
                db.execSQL("INSERT INTO trackers (id, name, created_at, position, icon) VALUES (${i + 1}, 'T$i', 1000, $i, '$icon')")
            }
        }

        helper.runMigrationsAndValidate(DB_V4, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT icon FROM trackers ORDER BY id").use { c ->
                val icons = mutableListOf<String>()
                while (c.moveToNext()) icons += c.getString(0)
                assertEquals(listOf("circle-check", "paw-print", "leaf", "flower-2"), icons)
            }
        }
    }

    @Test
    fun migrate4To5_addsRemindersOff() {
        helper.createDatabase(DB_V5, 4).use { db ->
            db.execSQL("INSERT INTO trackers (id, name, created_at, position) VALUES (1, 'Plants', 1000, 0)")
        }

        helper.runMigrationsAndValidate(DB_V5, 5, true).use { db ->
            db.query("SELECT name, reminder_every, reminder_unit FROM trackers WHERE id = 1").use { c ->
                c.moveToFirst()
                assertEquals("Plants", c.getString(0))
                assertNull(c.getString(1))
                assertNull(c.getString(2))
            }
        }
    }

    @Test
    fun deletingGroupKeepsItsTrackers() {
        helper.createDatabase(DB_V2, 2).use { db ->
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("INSERT INTO tracker_groups (id, name, position) VALUES (1, 'Garden', 0)")
            db.execSQL("INSERT INTO trackers (id, name, created_at, position, group_id) VALUES (1, 'Plants', 1000, 0, 1)")
            db.execSQL("DELETE FROM tracker_groups WHERE id = 1")
            db.query("SELECT group_id FROM trackers WHERE id = 1").use { c ->
                c.moveToFirst()
                assertNull(c.getString(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
        const val DB_V2 = "groups-test"
        const val DB_V3 = "counter-test"
        const val DB_V4 = "icons-test"
        const val DB_V5 = "reminders-test"
    }
}
