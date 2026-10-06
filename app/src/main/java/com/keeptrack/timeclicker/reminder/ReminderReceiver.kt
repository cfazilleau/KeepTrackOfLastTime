package com.keeptrack.timeclicker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keeptrack.timeclicker.widget.TileWidgets
import com.keeptrack.timeclicker.widget.doAsync
import com.keeptrack.timeclicker.widget.trackerRepository

/**
 * The reminder alarm, a reminder's "Mark as done" button, and restarts or clock changes (which drop or shift alarms).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Reminders.ACTION_DONE -> {
                val trackerId = intent.getLongExtra(Reminders.EXTRA_TRACKER_ID, -1).takeIf { it >= 0 } ?: return
                Reminders.dismiss(context, trackerId)
                doAsync {
                    context.trackerRepository.markDone(trackerId)
                    Reminders.update(context)
                    TileWidgets.refresh(context)
                }
            }
            in UpdateActions -> doAsync { Reminders.update(context) }
        }
    }

    private companion object {
        val UpdateActions = setOf(
            Reminders.ACTION_ALARM,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
