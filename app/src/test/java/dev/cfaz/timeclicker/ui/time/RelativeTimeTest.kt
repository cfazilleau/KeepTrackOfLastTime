package dev.cfaz.timeclicker.ui.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelativeTimeTest {

    private val now = Instant.parse("2026-10-06T12:00:00Z")

    private fun ago(duration: Duration, smallest: TimeUnit = TimeUnit.SECOND) =
        RelativeTime.split(now - duration, now, smallest)

    private fun parts(vararg parts: Pair<Long, TimeUnit>) =
        Elapsed(parts.getOrNull(0)?.let { TimePart(it.second, it.first) }, parts.getOrNull(1)?.let { TimePart(it.second, it.first) })

    @Test
    fun underASecondIsEmpty() {
        assertTrue(ago(Duration.ZERO).isEmpty)
        assertTrue(ago(Duration.ofMillis(999)).isEmpty)
    }

    @Test
    fun futureTimestampsAreEmpty() {
        assertTrue(RelativeTime.split(now + Duration.ofMinutes(5), now).isEmpty)
    }

    @Test
    fun seconds() {
        assertEquals(parts(1L to TimeUnit.SECOND), ago(Duration.ofSeconds(1)))
        assertEquals(parts(37L to TimeUnit.SECOND), ago(Duration.ofSeconds(37)))
    }

    @Test
    fun minutesAndSeconds() {
        assertEquals(parts(2L to TimeUnit.MINUTE, 45L to TimeUnit.SECOND), ago(Duration.ofSeconds(165)))
        assertEquals(parts(2L to TimeUnit.MINUTE), ago(Duration.ofMinutes(2)))
    }

    @Test
    fun hoursAndMinutes_dropSeconds() {
        assertEquals(parts(1L to TimeUnit.HOUR, 5L to TimeUnit.MINUTE), ago(Duration.ofSeconds(3600 + 5 * 60 + 59)))
        // A zero next unit is left out rather than skipping to a smaller one.
        assertEquals(parts(1L to TimeUnit.HOUR), ago(Duration.ofSeconds(3600 + 30)))
    }

    @Test
    fun daysAndHours() {
        assertEquals(parts(3L to TimeUnit.DAY, 5L to TimeUnit.HOUR), ago(Duration.ofDays(3).plusHours(5).plusMinutes(59)))
    }

    @Test
    fun yearsAndDays_noMonths() {
        assertEquals(parts(64L to TimeUnit.DAY), ago(Duration.ofDays(64)))
        assertEquals(parts(1L to TimeUnit.YEAR, 61L to TimeUnit.DAY), ago(Duration.ofDays(365 + 61).plusHours(3)))
        assertEquals(parts(2L to TimeUnit.YEAR), ago(Duration.ofDays(730)))
    }

    @Test
    fun smallestUnitDropsSeconds() {
        assertTrue(ago(Duration.ofSeconds(59), TimeUnit.MINUTE).isEmpty)
        assertEquals(parts(2L to TimeUnit.MINUTE), ago(Duration.ofSeconds(165), TimeUnit.MINUTE))
        assertEquals(parts(1L to TimeUnit.HOUR, 1L to TimeUnit.MINUTE), ago(Duration.ofSeconds(3665), TimeUnit.MINUTE))
    }

    @Test
    fun nextChangeFollowsTheSmallestUnitShown() {
        val from = now
        fun next(after: Duration, smallest: TimeUnit = TimeUnit.SECOND) =
            Duration.between(from, RelativeTime.nextChange(from, from + after, smallest))

        assertEquals(Duration.ofSeconds(1), next(Duration.ofMillis(300)))
        assertEquals(Duration.ofSeconds(38), next(Duration.ofMillis(37_500)))
        assertEquals(Duration.ofMinutes(61), next(Duration.ofMinutes(60).plusSeconds(10)))
        assertEquals(Duration.ofHours(25), next(Duration.ofHours(24).plusMinutes(1)))
        assertEquals(Duration.ofMinutes(1), next(Duration.ofSeconds(20), TimeUnit.MINUTE))
        assertEquals(Duration.ofMinutes(3), next(Duration.ofSeconds(165), TimeUnit.MINUTE))
    }

    @Test
    fun approximateUsesOneUnitCountingAtLeastTwo() {
        assertEquals(TimePart(TimeUnit.HOUR, 8), RelativeTime.approximate(Duration.ofHours(8).plusMinutes(12)))
        assertEquals(TimePart(TimeUnit.HOUR, 24), RelativeTime.approximate(Duration.ofDays(1)))
        assertEquals(TimePart(TimeUnit.HOUR, 36), RelativeTime.approximate(Duration.ofHours(36)))
        assertEquals(TimePart(TimeUnit.DAY, 3), RelativeTime.approximate(Duration.ofHours(62)))
        assertEquals(TimePart(TimeUnit.MINUTE, 90), RelativeTime.approximate(Duration.ofMinutes(90)))
        assertEquals(TimePart(TimeUnit.DAY, 400), RelativeTime.approximate(Duration.ofDays(400)))
        assertEquals(TimePart(TimeUnit.YEAR, 2), RelativeTime.approximate(Duration.ofDays(730)))
    }

    @Test
    fun nextChangeOfAFutureTimeIsThatTime() {
        assertEquals(now, RelativeTime.nextChange(now, now - Duration.ofMinutes(1)))
    }
}
