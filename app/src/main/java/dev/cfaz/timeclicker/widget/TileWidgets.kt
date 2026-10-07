package dev.cfaz.timeclicker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.cfaz.timeclicker.TimeClickerApplication
import dev.cfaz.timeclicker.data.AppSettings
import dev.cfaz.timeclicker.data.TilePresses
import dev.cfaz.timeclicker.data.TrackerRepository
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Home-screen widgets: one tile each. A widget stores the id of its tile in its Glance state.
 *
 * Widgets show elapsed time to the minute. They are refreshed when tiles change (see
 * [TimeClickerApplication]) and by an alarm set for the next minute/hour/day any widget's text changes.
 * The alarm doesn't wake the device: it fires when the screen is next on.
 */
object TileWidgets {
    /** Glance state: the tile shown by the widget. */
    val TRACKER_ID = longPreferencesKey("tracker_id")

    /** Glance state: written on each refresh so a running widget session recomposes with the new time. */
    val REFRESHED_AT = longPreferencesKey("refreshed_at")

    val TrackerIdParam = ActionParameters.Key<Long>("tracker_id")

    const val ACTION_TICK = "dev.cfaz.timeclicker.action.WIDGET_TICK"

    private const val EXTRA_TRACKER_ID = "dev.cfaz.timeclicker.extra.TRACKER_ID"

    private val mutex = Mutex()

    private val placedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a widget requested with [requestPin] has been placed and bound. Not replayed. */
    val placed: SharedFlow<Unit> = placedEvents.asSharedFlow()

    /** Redraws every widget with the current time and tile data, then plans the next refresh. */
    suspend fun refresh(context: Context) = mutex.withLock {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(TileWidget::class.java)
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            updateAppWidgetState(context, id) { it[REFRESHED_AT] = now }
            TileWidget().update(context, id)
        }
        scheduleNextTick(context)
    }

    /** Shows the tile [trackerId] on the widget [appWidgetId]. */
    suspend fun bind(context: Context, appWidgetId: Int, trackerId: Long) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(context, glanceId) { it[TRACKER_ID] = trackerId }
        TileWidget().update(context, glanceId)
        scheduleNextTick(context)
    }

    /** Sets (or cancels) the alarm for the next time a widget's elapsed text changes. */
    suspend fun scheduleNextTick(context: Context) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(TileWidget::class.java)
        val shown = ids.mapNotNull { getAppWidgetState(context, PreferencesGlanceStateDefinition, it)[TRACKER_ID] }.toSet()
        val now = Instant.now()
        val next = context.trackerRepository.observeTrackers().first()
            .filter { it.id in shown }
            .minOfOrNull { RelativeTime.nextChange(it.lastDoneAt, now, TimeUnit.MINUTE) }

        val alarms = context.getSystemService(AlarmManager::class.java)
        val tick = PendingIntent.getBroadcast(
            context, 0, Intent(context, WidgetTickReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // Inexact and non-waking: no special permission, and no battery use while the screen is off.
        if (next == null) alarms.cancel(tick) else alarms.set(AlarmManager.RTC, next.toEpochMilli(), tick)
    }

    /** Whether the launcher can add a widget on request ("Add to home screen"). */
    fun canPin(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Asks the launcher to add a widget for [trackerId]; [WidgetPinnedReceiver] binds it once placed. */
    suspend fun requestPin(context: Context, trackerId: Long): Boolean {
        val callback = PendingIntent.getBroadcast(
            context,
            trackerId.toInt(),
            Intent(context, WidgetPinnedReceiver::class.java).putExtra(EXTRA_TRACKER_ID, trackerId),
            // Mutable: the launcher adds the new widget's id to it.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
            receiver = TileWidgetReceiver::class.java,
            successCallback = callback,
        )
    }

    internal fun notifyPlaced() {
        placedEvents.tryEmit(Unit)
    }

    internal fun pinnedTrackerId(intent: Intent): Long? =
        intent.getLongExtra(EXTRA_TRACKER_ID, -1).takeIf { it >= 0 }
}

internal val Context.trackerRepository: TrackerRepository
    get() = (applicationContext as TimeClickerApplication).container.trackerRepository

internal val Context.tilePresses: TilePresses
    get() = (applicationContext as TimeClickerApplication).container.tilePresses

internal val Context.appSettings: AppSettings
    get() = (applicationContext as TimeClickerApplication).container.settingsRepository.settings.value

/** Runs [block] off the main thread, keeping the receiver alive until it is done. */
internal fun BroadcastReceiver.doAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
