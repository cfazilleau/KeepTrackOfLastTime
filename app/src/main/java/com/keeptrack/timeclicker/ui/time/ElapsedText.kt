package com.keeptrack.timeclicker.ui.time

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import com.keeptrack.timeclicker.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@PluralsRes
private fun TimeUnit.plural(): Int = when (this) {
    TimeUnit.YEAR -> R.plurals.elapsed_years
    TimeUnit.DAY -> R.plurals.elapsed_days
    TimeUnit.HOUR -> R.plurals.elapsed_hours
    TimeUnit.MINUTE -> R.plurals.elapsed_minutes
    TimeUnit.SECOND -> R.plurals.elapsed_seconds
}

/** "2 minutes", localized. */
fun TimePart.format(resources: Resources): String =
    resources.getQuantityString(unit.plural(), count.toInt(), count.toInt())

/** "2 minutes 45 seconds", localized; empty when nothing has elapsed yet. */
fun Elapsed.format(resources: Resources): String {
    val major = major?.format(resources) ?: return ""
    val minor = minor?.format(resources) ?: return major
    return resources.getString(R.string.elapsed_two_units, major, minor)
}

/**
 * The localized "… ago" around a time, split where the time goes: ("", " ago") in English,
 * ("il y a ", "") in French, ("", "前") in Japanese. Lets the time itself be styled on its own.
 */
data class AgoAffixes(val prefix: String, val suffix: String)

fun agoAffixes(resources: Resources): AgoAffixes {
    val marker = "\u0000"
    val template = resources.getString(R.string.elapsed_ago, marker)
    val at = template.indexOf(marker)
    return AgoAffixes(template.substring(0, at), template.substring(at + marker.length))
}

/**
 * When something was done, as two lines for a tile: [headline] the day ("6 Oct", with the year
 * when not this year), [detail] the time ("14:32" or "2:32 PM", following the system setting).
 */
data class AbsoluteTime(val headline: String, val detail: String)

fun absoluteTime(context: Context, at: Instant, now: Instant = Instant.now()): AbsoluteTime {
    val zone = ZoneId.systemDefault()
    val date = at.atZone(zone)
    val locale = context.resources.configuration.locales[0]
    val dateSkeleton = if (date.year == now.atZone(zone).year) "dMMM" else "dMMMy"
    val timeSkeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    fun format(skeleton: String) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
    return AbsoluteTime(format(dateSkeleton), format(timeSkeleton))
}
