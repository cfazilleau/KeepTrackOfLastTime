package com.keeptrack.lasttime.ui.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelativeTimeTest {

    private val now = Instant.parse("2026-10-06T12:00:00Z")

    private fun ago(duration: Duration) = RelativeTime.format(now - duration, now)

    @Test
    fun underAMinuteIsJustNow() {
        assertEquals("just now", ago(Duration.ofSeconds(59)))
        assertEquals("just now", ago(Duration.ZERO))
    }

    @Test
    fun futureTimestampsClampToJustNow() {
        assertEquals("just now", RelativeTime.format(now + Duration.ofMinutes(5), now))
    }

    @Test
    fun minutes() {
        assertEquals("1 minute ago", ago(Duration.ofMinutes(1)))
        assertEquals("42 minutes ago", ago(Duration.ofMinutes(42)))
    }

    @Test
    fun hoursAndMinutes() {
        assertEquals("1 hour ago", ago(Duration.ofHours(1)))
        assertEquals("2 hours and 1 minute ago", ago(Duration.ofMinutes(121)))
    }

    @Test
    fun daysAndHours() {
        assertEquals("3 days and 5 hours ago", ago(Duration.ofDays(3).plusHours(5).plusMinutes(59)))
        assertEquals("1 day ago", ago(Duration.ofDays(1).plusMinutes(30)))
    }

    @Test
    fun monthsAndYears() {
        assertEquals("2 months and 4 days ago", ago(Duration.ofDays(64)))
        assertEquals("1 year and 2 months ago", ago(Duration.ofDays(365 + 61)))
        assertEquals("1 year and 11 months ago", ago(Duration.ofDays(365 + 364)))
    }
}
