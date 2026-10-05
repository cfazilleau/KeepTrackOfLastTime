package com.keeptrack.lasttime.ui.time

import java.time.Duration
import java.time.Instant

/** Formats elapsed time as e.g. "3 days and 5 hours ago", using the two most significant units. */
object RelativeTime {

    private enum class Unit(val minutes: Long, val singular: String, val plural: String) {
        YEAR(365L * 24 * 60, "year", "years"),
        MONTH(30L * 24 * 60, "month", "months"),
        DAY(24L * 60, "day", "days"),
        HOUR(60L, "hour", "hours"),
        MINUTE(1L, "minute", "minutes"),
    }

    fun format(from: Instant, to: Instant): String {
        val totalMinutes = Duration.between(from, to).toMinutes()
        if (totalMinutes < 1) return "just now"

        val units = Unit.entries
        val majorIndex = units.indexOfFirst { totalMinutes >= it.minutes }
        val major = units[majorIndex]
        val majorCount = totalMinutes / major.minutes

        val minor = units.getOrNull(majorIndex + 1)
        // Cap so approximate months/years never read as "1 year and 12 months".
        val minorCount = minor?.let {
            ((totalMinutes % major.minutes) / it.minutes).coerceAtMost(major.minutes / it.minutes - 1)
        } ?: 0

        val text = if (minor != null && minorCount > 0) {
            "${count(majorCount, major)} and ${count(minorCount, minor)}"
        } else {
            count(majorCount, major)
        }
        return "$text ago"
    }

    private fun count(n: Long, unit: Unit) = "$n ${if (n == 1L) unit.singular else unit.plural}"
}
