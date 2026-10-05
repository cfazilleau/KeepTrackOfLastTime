package com.keeptrack.lasttime.ui.time

import java.time.Duration
import java.time.Instant

/**
 * Elapsed time split for display: [major] is the headline ("3 days"), [rest] the remainder
 * ("and 5 hours ago"). Under a minute, [major] is "just now" and [rest] is empty.
 */
data class Elapsed(val major: String, val rest: String) {
    val text: String get() = if (rest.isEmpty()) major else "$major $rest"
}

/** Formats elapsed time as e.g. "3 days and 5 hours ago", using the two most significant units. */
object RelativeTime {

    private enum class Unit(val minutes: Long, val singular: String, val plural: String) {
        YEAR(365L * 24 * 60, "year", "years"),
        MONTH(30L * 24 * 60, "month", "months"),
        DAY(24L * 60, "day", "days"),
        HOUR(60L, "hour", "hours"),
        MINUTE(1L, "minute", "minutes"),
    }

    fun format(from: Instant, to: Instant): String = split(from, to).text

    fun split(from: Instant, to: Instant): Elapsed {
        val totalMinutes = Duration.between(from, to).toMinutes()
        if (totalMinutes < 1) return Elapsed("just now", "")

        val units = Unit.entries
        val majorIndex = units.indexOfFirst { totalMinutes >= it.minutes }
        val major = units[majorIndex]
        val majorCount = totalMinutes / major.minutes

        val minor = units.getOrNull(majorIndex + 1)
        // Cap so approximate months/years never read as "1 year and 12 months".
        val minorCount = minor?.let {
            ((totalMinutes % major.minutes) / it.minutes).coerceAtMost(major.minutes / it.minutes - 1)
        } ?: 0

        val rest = if (minor != null && minorCount > 0) "and ${count(minorCount, minor)} ago" else "ago"
        return Elapsed(count(majorCount, major), rest)
    }

    private fun count(n: Long, unit: Unit) = "$n ${if (n == 1L) unit.singular else unit.plural}"
}
