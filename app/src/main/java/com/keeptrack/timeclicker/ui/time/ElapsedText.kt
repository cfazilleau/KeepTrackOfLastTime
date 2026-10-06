package com.keeptrack.timeclicker.ui.time

import android.content.res.Resources
import androidx.annotation.PluralsRes
import com.keeptrack.timeclicker.R

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
