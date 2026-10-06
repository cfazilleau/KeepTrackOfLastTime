package com.keeptrack.timeclicker.data

import java.time.Duration
import java.time.Instant

/** A reminder: a notification once the tile hasn't been done for [every] [unit]. Done again, the wait starts over. */
data class Reminder(
    val every: Int,
    val unit: ReminderUnit,
) {
    val delay: Duration get() = unit.duration.multipliedBy(every.toLong())

    /** When the reminder comes for a tile last done at [lastDone]. */
    fun dueAt(lastDone: Instant): Instant = lastDone + delay

    /** Whether the tile, last done at [lastDone], has gone without being done for the whole delay. */
    fun isDue(lastDone: Instant, now: Instant): Boolean = dueAt(lastDone) <= now

    companion object {
        val DEFAULT = Reminder(every = 1, unit = ReminderUnit.DAYS)
        const val MAX_EVERY = 99

        /** From the database or a backup; null when [every] is unset (no reminder). */
        fun fromColumns(every: Int?, unit: String?): Reminder? {
            if (every == null || every < 1) return null
            return Reminder(every.coerceAtMost(MAX_EVERY), ReminderUnit.fromKey(unit))
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
