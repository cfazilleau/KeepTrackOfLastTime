package dev.cfaz.timeclicker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class RhythmTest {

    private val start = Instant.parse("2026-10-01T09:00:00Z")
    private val utc: ZoneId = ZoneOffset.UTC

    /** Presses separated by [gaps], from [start]. */
    private fun presses(vararg gaps: Duration): List<Instant> =
        gaps.runningFold(start) { at, gap -> at + gap }

    private fun hours(h: Long) = Duration.ofHours(h)

    @Test
    fun notEnoughPresses() {
        assertNull(Rhythm.of(emptyList()))
        assertNull(Rhythm.of(presses(hours(8), hours(8), hours(8))))
    }

    @Test
    fun regularPressesGiveTheirGap() {
        val rhythm = requireNotNull(Rhythm.of(presses(hours(8), hours(8), hours(8), hours(8))))
        assertEquals(hours(8), rhythm.typical)
        assertEquals(hours(2), rhythm.slack)
    }

    @Test
    fun orderDoesNotMatter() {
        val times = presses(hours(8), hours(7), hours(9), hours(8))
        assertEquals(Rhythm.of(times), Rhythm.of(times.reversed()))
    }

    @Test
    fun oneOddGapIsIgnored() {
        val rhythm = requireNotNull(Rhythm.of(presses(hours(24), hours(24), hours(72), hours(24), hours(24))))
        assertEquals(hours(24), rhythm.typical)
    }

    @Test
    fun irregularPressesGiveNoRhythm() {
        assertNull(Rhythm.of(presses(hours(1), hours(30), hours(5), hours(70), hours(12))))
    }

    @Test
    fun aVaryingHabitGetsMoreSlack() {
        val rhythm = requireNotNull(Rhythm.of(presses(hours(20), hours(28), hours(24), hours(18), hours(30))))
        assertEquals(hours(24), rhythm.typical)
        assertEquals(hours(12), rhythm.slack) // 3 × the median deviation of 4 hours
    }

    @Test
    fun doubleTapsDoNotCount() {
        val double = Duration.ofSeconds(5)
        assertNull(Rhythm.of(presses(hours(8), double, hours(8), double, hours(8))))
        val rhythm = requireNotNull(Rhythm.of(presses(hours(8), double, hours(8), hours(8), hours(8))))
        assertEquals(hours(8), rhythm.typical)
    }

    @Test
    fun onlyRecentPressesCount() {
        // An old weekly habit, now daily.
        val old = List(10) { Duration.ofDays(7) }
        val recent = List(Rhythm.EVENTS_USED - 1) { Duration.ofDays(1) }
        val rhythm = requireNotNull(Rhythm.of(presses(*(old + recent).toTypedArray())))
        assertEquals(Duration.ofDays(1), rhythm.typical)
    }

    @Test
    fun dueAfterTheUsualGapAndItsSlack() {
        val rhythm = Rhythm(hours(8), hours(2))
        assertEquals(Instant.parse("2026-10-01T19:00:00Z"), rhythm.dueAt(start, utc))
    }

    @Test
    fun dueAtNightWaitsForTheMorning() {
        val rhythm = Rhythm(hours(8), hours(2))
        // 14:00 + 10 hours = midnight
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), rhythm.dueAt(Instant.parse("2026-10-01T14:00:00Z"), utc))
        // 21:00 + 10 hours = 07:00
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), rhythm.dueAt(Instant.parse("2026-10-01T21:00:00Z"), utc))
        // 12:00 + 10 hours = 22:00, the start of the night
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), rhythm.dueAt(Instant.parse("2026-10-01T12:00:00Z"), utc))
    }

    @Test
    fun nightFollowsTheTimeZone() {
        val rhythm = Rhythm(hours(8), hours(2))
        val paris = ZoneId.of("Europe/Paris") // UTC+2 in October
        // 10:00 UTC + 10 hours = 22:00 Paris time: waits until 08:00 Paris time.
        assertEquals(Instant.parse("2026-10-02T06:00:00Z"), rhythm.dueAt(Instant.parse("2026-10-01T10:00:00Z"), paris))
    }
}
