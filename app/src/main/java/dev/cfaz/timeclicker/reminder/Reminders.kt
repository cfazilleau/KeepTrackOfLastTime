package dev.cfaz.timeclicker.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.cfaz.timeclicker.MainActivity
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.TimeClickerApplication
import dev.cfaz.timeclicker.data.Rhythm
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.TimeUnit
import dev.cfaz.timeclicker.ui.time.format
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

/**
 * Tile reminders: a notification once a tile with a [dev.cfaz.timeclicker.data.Reminder] hasn't been done for
 * its delay, or, for an automatic one, once it is late on its usual pace ([Rhythm]; never at night, and not until
 * the tile has a regular pace). One per lapse: done again, the wait starts over.
 *
 * One alarm is set, for the next tile to become due. [update] posts the reminders of tiles that became due (each
 * remembered by its due time, so it is posted once), removes those of tiles done since, and sets the alarm again.
 * It runs when the alarm fires, whenever the tiles change (see [TimeClickerApplication]), and after a restart or a
 * clock change.
 */
object Reminders {
    const val ACTION_ALARM = "dev.cfaz.timeclicker.action.REMINDER_ALARM"
    const val ACTION_DONE = "dev.cfaz.timeclicker.action.REMINDER_DONE"
    const val EXTRA_TRACKER_ID = "dev.cfaz.timeclicker.extra.TRACKER_ID"

    private const val CHANNEL_ID = "reminders"
    private const val TAG = "reminder"
    /** Per tile ("<id>"): the due time whose reminder was posted. */
    private const val PREFS = "reminders_posted"

    /** Alarms aren't exact (that needs a special permission): they may come up to this much later. */
    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    private val mutex = Mutex()

    suspend fun update(context: Context) = mutex.withLock {
        val app = context.applicationContext
        val posted = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val trackers = app.trackers().filter { it.reminder != null }
        val rhythms = trackers.filter { it.reminder!!.auto }.associate { it.id to app.rhythm(it.id) }
        // Null for an automatic reminder without a regular pace yet.
        val dueTimes = trackers.associate { tracker ->
            val reminder = tracker.reminder!!
            tracker.id to if (reminder.auto) rhythms[tracker.id]?.dueAt(tracker.lastDoneAt, zone) else reminder.dueAt(tracker.lastDoneAt)
        }
        val due = trackers.filter { dueTimes[it.id]?.let { at -> at <= now } == true }

        ensureChannel(app)
        val newlyDue = due.filter { posted.getLong(it.id.toString(), -1) != dueTimes[it.id]!!.toEpochMilli() }
        newlyDue.forEach { notify(app, it, rhythms[it.id], now) }
        posted.edit {
            // Tiles without a reminder (or deleted) are forgotten.
            clear()
            due.forEach { putLong(it.id.toString(), dueTimes[it.id]!!.toEpochMilli()) }
        }
        // Done since, reminder removed, or tile deleted: the notification goes.
        val dueIds = due.map { it.id }.toSet()
        app.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.tag == TAG && it.id.toLong() !in dueIds }
            .forEach { NotificationManagerCompat.from(app).cancel(TAG, it.id) }

        schedule(app, dueTimes.values.filterNotNull().filter { it > now }.minOrNull())
    }

    /** Removes the reminder of a tile just done from its notification. */
    fun dismiss(context: Context, trackerId: Long) = NotificationManagerCompat.from(context).cancel(TAG, trackerId.toInt())

    /** Whether reminders can be shown: notifications allowed (asked for on Android 13+). */
    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun schedule(context: Context, next: Instant?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val alarm = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // Wakes the device, within a short window: no exact-alarm permission needed.
        if (next == null) alarms.cancel(alarm) else alarms.setWindow(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), WINDOW_MILLIS, alarm)
    }

    /** [rhythm]: the tile's usual pace, for an automatic reminder. */
    private fun notify(context: Context, tracker: Tracker, rhythm: Rhythm?, now: Instant) {
        if (!canNotify(context)) return
        val resources = context.resources
        val text = if (rhythm != null) {
            resources.getString(R.string.reminder_auto_notification_text, RelativeTime.approximate(rhythm.typical).format(resources))
        } else {
            resources.getString(R.string.reminder_notification_text, RelativeTime.split(tracker.lastDoneAt, now, TimeUnit.MINUTE).format(resources))
        }
        val id = tracker.id.toInt()
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context, id,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_TRACKER_ID, tracker.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tracker.name)
            .setContentText(text)
            .setWhen(now.toEpochMilli())
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, resources.getString(R.string.reminder_mark_done), done)
            .build()
        @Suppress("MissingPermission") // checked by canNotify
        NotificationManagerCompat.from(context).notify(TAG, id, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        // Creating it again only updates its name and description (e.g. after a language change).
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private suspend fun Context.trackers(): List<Tracker> =
        (this as TimeClickerApplication).container.trackerRepository.observeTrackers().first()

    private suspend fun Context.rhythm(trackerId: Long): Rhythm? =
        (this as TimeClickerApplication).container.trackerRepository.rhythm(trackerId)
}
