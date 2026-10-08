package dev.cfaz.timeclicker.ui.time

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TimeDisplay
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

/**
 * The second unit on its own line, with what joins it to the first: "45 seconds" in English,
 * "et 45 secondes" in French. The joining words come from `elapsed_two_units`.
 */
fun TimePart.formatAsSecond(resources: Resources): String {
    val first = "\u0000"
    val second = "\u0001"
    val template = resources.getString(R.string.elapsed_two_units, first, second)
    val joiner = template.substring(template.indexOf(first) + first.length, template.indexOf(second)).trimStart()
    return joiner + format(resources)
}

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

/** The full date and time in one line, "14 March 2015 at 9:26" / "14 mars 2015 à 09:26". */
fun fullDateTime(context: Context, at: Instant): String {
    val date = at.atZone(ZoneId.systemDefault())
    val locale = context.resources.configuration.locales[0]
    val timeSkeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    fun format(skeleton: String) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
    return context.getString(R.string.date_at_time, format("dMMMMy"), format(timeSkeleton))
}

/** [at] as "3 days ago" (or "just now"), or as a full date and time, following [display]. */
private fun timeText(context: Context, display: TimeDisplay, at: Instant): String = when (display) {
    TimeDisplay.ABSOLUTE -> fullDateTime(context, at)
    TimeDisplay.RELATIVE -> {
        val elapsed = RelativeTime.split(at, Instant.now(), TimeUnit.MINUTE)
        // Mid-sentence: "just now", not "Just now".
        if (elapsed.isEmpty) context.getString(R.string.elapsed_just_now).replaceFirstChar { it.lowercase() }
        else context.getString(R.string.elapsed_ago, elapsed.format(context.resources))
    }
}

/** The sentence in a tile's edit sheet: when it was last pressed, or created if it never was. */
fun tileTimeInfo(context: Context, display: TimeDisplay, at: Instant, pressed: Boolean): String {
    val text = timeText(context, display, at)
    val template = when {
        pressed && display == TimeDisplay.ABSOLUTE -> R.string.time_info_pressed_absolute
        pressed -> R.string.time_info_pressed_relative
        display == TimeDisplay.ABSOLUTE -> R.string.time_info_created_absolute
        else -> R.string.time_info_created_relative
    }
    return context.getString(template, text)
}

/** The sentence in the settings: when the app was installed. */
fun installTimeInfo(context: Context, display: TimeDisplay): String? {
    val installedAt = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrNull() ?: return null
    val text = timeText(context, display, Instant.ofEpochMilli(installedAt))
    return context.getString(
        if (display == TimeDisplay.ABSOLUTE) R.string.time_info_installed_absolute else R.string.time_info_installed_relative,
        text,
    )
}
