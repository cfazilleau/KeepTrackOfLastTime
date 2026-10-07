package dev.cfaz.timeclicker.ui.time

import java.time.Duration
import java.time.Instant

/** Units of elapsed time, largest first. A year is 365 days; there are no months. */
enum class TimeUnit(val seconds: Long) {
    YEAR(365L * 24 * 60 * 60),
    DAY(24L * 60 * 60),
    HOUR(60L * 60),
    MINUTE(60L),
    SECOND(1L),
}

data class TimePart(val unit: TimeUnit, val count: Long)

/**
 * Elapsed time split for display: [major] is the headline ("2 minutes"), [minor] the next unit
 * ("45 seconds"), or null when it is zero. Both are null under the smallest unit shown.
 */
data class Elapsed(val major: TimePart?, val minor: TimePart?) {
    val isEmpty: Boolean get() = major == null
}

/** Elapsed time as its two most significant units, e.g. "2 minutes 45 seconds" or "1 year 12 days". */
object RelativeTime {

    /** Splits the time from [from] to [to]; units below [smallest] are dropped. Future times count as zero. */
    fun split(from: Instant, to: Instant, smallest: TimeUnit = TimeUnit.SECOND): Elapsed {
        val total = Duration.between(from, to).seconds.coerceAtLeast(0)
        val units = TimeUnit.entries.filter { it.seconds >= smallest.seconds }
        val majorIndex = units.indexOfFirst { total >= it.seconds }
        if (majorIndex < 0) return Elapsed(null, null)

        val major = units[majorIndex]
        val minor = units.getOrNull(majorIndex + 1)
        val minorCount = minor?.let { (total % major.seconds) / it.seconds } ?: 0
        return Elapsed(
            major = TimePart(major, total / major.seconds),
            minor = if (minor != null && minorCount > 0) TimePart(minor, minorCount) else null,
        )
    }

    /** The first moment after [now] at which [split] of [from] reads differently, to refresh the display then. */
    fun nextChange(from: Instant, now: Instant, smallest: TimeUnit = TimeUnit.SECOND): Instant {
        val elapsed = Duration.between(from, now)
        if (elapsed.isNegative) return from
        // The smallest unit currently shown. Unit sizes divide each other, so stepping by it
        // also lands exactly on the switch to a larger unit.
        val units = TimeUnit.entries.filter { it.seconds >= smallest.seconds }
        val majorIndex = units.indexOfFirst { elapsed.seconds >= it.seconds }
        val step = when (majorIndex) {
            -1 -> units.last()
            else -> units.getOrNull(majorIndex + 1) ?: units[majorIndex]
        }.seconds * 1000
        val elapsedMillis = elapsed.toMillis()
        return from.plusMillis((elapsedMillis / step + 1) * step)
    }
}
