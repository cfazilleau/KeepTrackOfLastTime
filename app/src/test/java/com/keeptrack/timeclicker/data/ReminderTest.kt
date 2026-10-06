package com.keeptrack.timeclicker.data

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
}
