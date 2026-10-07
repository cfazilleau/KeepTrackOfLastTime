package dev.cfaz.timeclicker.data

import java.time.Duration
import java.time.Instant

/**
 * A reminder: a notification once the tile hasn't been done for [every] [unit]. Done again, the wait starts over.
 * An [auto] reminder instead comes when the tile is late on its usual pace, guessed from its history ([Rhythm]);
 * [every] and [unit] then only keep what the custom choice showed.
 */
data class Reminder(
    val every: Int,
    val unit: ReminderUnit,
    val auto: Boolean = false,
) {
    /** How the unit is stored: its key, or [AUTO_KEY]. */
    val unitKey: String get() = if (auto) AUTO_KEY else unit.key

    /** For a custom reminder: an automatic one follows the tile's [Rhythm]. */
    val delay: Duration get() = unit.duration.multipliedBy(every.toLong())

    /** When the reminder comes for a tile last done at [lastDone]. */
    fun dueAt(lastDone: Instant): Instant = lastDone + delay

    /** Whether the tile, last done at [lastDone], has gone without being done for the whole delay. */
    fun isDue(lastDone: Instant, now: Instant): Boolean = dueAt(lastDone) <= now

    companion object {
        val DEFAULT = Reminder(every = 1, unit = ReminderUnit.DAYS)
        val AUTOMATIC = DEFAULT.copy(auto = true)
        const val MAX_EVERY = 99

        /**
         * Stored as the unit, with [DEFAULT]'s every (it marks that there is a reminder). Versions before automatic
         * reminders read it as an unknown unit: a reminder after 1 day.
         */
        const val AUTO_KEY = "auto"

        /** From the database or a backup; null when [every] is unset (no reminder). */
        fun fromColumns(every: Int?, unit: String?): Reminder? {
            if (every == null || every < 1) return null
            if (unit == AUTO_KEY) return AUTOMATIC
            return Reminder(every.coerceAtMost(MAX_EVERY), ReminderUnit.fromKey(unit))
        }

        /** A custom reminder close to [delay]: in hours under 2 days, in days under 2 weeks, else in weeks. */
        fun near(delay: Duration): Reminder {
            val unit = when {
                delay < Duration.ofDays(2) -> ReminderUnit.HOURS
                delay < Duration.ofDays(14) -> ReminderUnit.DAYS
                else -> ReminderUnit.WEEKS
            }
            val every = (delay.toMillis().toDouble() / unit.duration.toMillis()).let { Math.round(it).toInt() }
            return Reminder(every.coerceIn(1, MAX_EVERY), unit)
        }
    }
}

/** Stored by [key]; unknown keys fall back to days. */
enum class ReminderUnit(val key: String, val duration: Duration) {
    HOURS("hours", Duration.ofHours(1)),
    DAYS("days", Duration.ofDays(1)),
    WEEKS("weeks", Duration.ofDays(7));

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DAYS
    }
}
