package dev.cfaz.timeclicker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ReminderTest {

    private val done = Instant.parse("2026-10-06T08:20:00Z")

    @Test
    fun dueAfterTheDelaySinceTheLastTime() {
        assertEquals(done + Duration.ofHours(5), Reminder(5, ReminderUnit.HOURS).dueAt(done))
        assertEquals(done + Duration.ofDays(3), Reminder(3, ReminderUnit.DAYS).dueAt(done))
        assertEquals(done + Duration.ofDays(14), Reminder(2, ReminderUnit.WEEKS).dueAt(done))
    }

    @Test
    fun dueOnceTheDelayHasPassed() {
        val reminder = Reminder(2, ReminderUnit.HOURS)
        assertFalse(reminder.isDue(done, done + Duration.ofMinutes(119)))
        assertTrue(reminder.isDue(done, done + Duration.ofHours(2)))
        assertTrue(reminder.isDue(done, done + Duration.ofDays(30)))
    }

    @Test
    fun columns() {
        assertNull(Reminder.fromColumns(null, "days"))
        assertNull(Reminder.fromColumns(0, "days"))
        assertEquals(Reminder(3, ReminderUnit.WEEKS), Reminder.fromColumns(3, "weeks"))
        // Unknown unit (e.g. from a newer version): days. Too large: the maximum.
        assertEquals(Reminder(2, ReminderUnit.DAYS), Reminder.fromColumns(2, "months"))
        assertEquals(Reminder(Reminder.MAX_EVERY, ReminderUnit.HOURS), Reminder.fromColumns(500, "hours"))
    }

    @Test
    fun automaticColumns() {
        assertEquals("auto", Reminder.AUTOMATIC.unitKey)
        assertEquals("weeks", Reminder(3, ReminderUnit.WEEKS).unitKey)
        assertEquals(Reminder.AUTOMATIC, Reminder.fromColumns(Reminder.AUTOMATIC.every, Reminder.AUTOMATIC.unitKey))
        assertNull(Reminder.fromColumns(null, "auto"))
    }

    @Test
    fun nearADelay() {
        assertEquals(Reminder(10, ReminderUnit.HOURS), Reminder.near(Duration.ofHours(10).plusMinutes(20)))
        assertEquals(Reminder(30, ReminderUnit.HOURS), Reminder.near(Duration.ofHours(30)))
        assertEquals(Reminder(4, ReminderUnit.DAYS), Reminder.near(Duration.ofHours(90)))
        assertEquals(Reminder(3, ReminderUnit.WEEKS), Reminder.near(Duration.ofDays(20)))
        assertEquals(Reminder(1, ReminderUnit.HOURS), Reminder.near(Duration.ofMinutes(5)))
        assertEquals(Reminder(Reminder.MAX_EVERY, ReminderUnit.WEEKS), Reminder.near(Duration.ofDays(3650)))
    }
}
