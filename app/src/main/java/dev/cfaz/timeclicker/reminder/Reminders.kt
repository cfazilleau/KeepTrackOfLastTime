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
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.TimeUnit
import dev.cfaz.timeclicker.ui.time.format
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Tile reminders: a notification once a tile with a [dev.cfaz.timeclicker.data.Reminder] hasn't been done for
 * its delay. One per lapse: done again, the wait starts over.
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
        val trackers = app.trackers().filter { it.reminder != null }
        val due = trackers.filter { it.reminder!!.isDue(it.lastDoneAt, now) }

        ensureChannel(app)
        val newlyDue = due.filter { posted.getLong(it.id.toString(), -1) != it.reminder!!.dueAt(it.lastDoneAt).toEpochMilli() }
        newlyDue.forEach { notify(app, it, now) }
        posted.edit {
            // Tiles without a reminder (or deleted) are forgotten.
            clear()
            due.forEach { putLong(it.id.toString(), it.reminder!!.dueAt(it.lastDoneAt).toEpochMilli()) }
        }
        // Done since, reminder removed, or tile deleted: the notification goes.
        val dueIds = due.map { it.id }.toSet()
        app.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.tag == TAG && it.id.toLong() !in dueIds }
            .forEach { NotificationManagerCompat.from(app).cancel(TAG, it.id) }

        schedule(app, trackers.map { it.reminder!!.dueAt(it.lastDoneAt) }.filter { it > now }.minOrNull())
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

    private fun notify(context: Context, tracker: Tracker, now: Instant) {
        if (!canNotify(context)) return
        val resources = context.resources
        val elapsed = RelativeTime.split(tracker.lastDoneAt, now, TimeUnit.MINUTE).format(resources)
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
            .setContentText(resources.getString(R.string.reminder_notification_text, elapsed))
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
}
