package dev.cfaz.timeclicker.data

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * How often a tile is usually done, guessed from its history: [typical] is the median time between two
 * presses, [slack] how late it can be before it counts as forgotten.
 */
data class Rhythm(val typical: Duration, val slack: Duration) {

    /**
     * When a reminder is due if the tile was last done at [lastDone]: a little after it usually is,
     * moved to the next morning if that falls at night.
     */
    fun dueAt(lastDone: Instant, zone: ZoneId): Instant = outsideNight(lastDone + typical + slack, zone)

    companion object {
        /** Presses looked at: the recent ones, so a change of habit is picked up. */
        const val EVENTS_USED = 11

        /** Fewer gaps than this (5 presses) is not enough to call it a habit. */
        const val MIN_INTERVALS = 4

        /** Gaps this short are double taps, not separate times. */
        val MIN_INTERVAL: Duration = Duration.ofMinutes(1)

        /** Above this spread (median deviation / median gap), the presses are too irregular to guess from. */
        const val MAX_SPREAD = 0.5

        /** No reminders from [NIGHT_START] to [NIGHT_END]: they wait for the morning. */
        val NIGHT_START: LocalTime = LocalTime.of(22, 0)
        val NIGHT_END: LocalTime = LocalTime.of(8, 0)

        /** The rhythm of presses at [times] (any order), or null when there is no regular one. */
        fun of(times: List<Instant>): Rhythm? {
            val gaps = times.sorted().takeLast(EVENTS_USED)
                .zipWithNext { a, b -> Duration.between(a, b).toMillis() }
                .filter { it >= MIN_INTERVAL.toMillis() }
            if (gaps.size < MIN_INTERVALS) return null
            val typical = median(gaps)
            val deviation = median(gaps.map { abs(it - typical) })
            if (deviation > typical * MAX_SPREAD) return null
            // A quarter of the usual gap, or more for a habit that varies more.
            val slack = maxOf(typical / 4, deviation * 3)
            return Rhythm(Duration.ofMillis(typical), Duration.ofMillis(slack))
        }

        private fun median(values: List<Long>): Long {
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        }

        /** [at], or the next morning when [at] is at night. */
        private fun outsideNight(at: Instant, zone: ZoneId): Instant {
            val local = at.atZone(zone)
            val time = local.toLocalTime()
            return when {
                time >= NIGHT_START -> local.toLocalDate().plusDays(1).atTime(NIGHT_END).atZone(zone).toInstant()
                time < NIGHT_END -> local.toLocalDate().atTime(NIGHT_END).atZone(zone).toInstant()
                else -> at
            }
        }
    }
}
