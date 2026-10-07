package dev.cfaz.timeclicker.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import dev.cfaz.timeclicker.ui.components.TapSound

class TileWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TileWidget()
}

/** Tapping a widget marks its tile as done now, like tapping the tile in the app (click sound included). */
class MarkDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val trackerId = parameters[TileWidgets.TrackerIdParam] ?: return
        if (context.appSettings.clickSound) TapSound.play(context)
        context.trackerRepository.markDone(trackerId)
        TileWidgets.refresh(context)
    }
}

/** The refresh alarm, and clock or language changes: redraw the widgets' elapsed time. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in Actions) return
        doAsync { TileWidgets.refresh(context) }
    }

    private companion object {
        val Actions = setOf(
            TileWidgets.ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/** A widget requested with "Add to home screen" was placed: show the tile it was requested for. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val trackerId = TileWidgets.pinnedTrackerId(intent)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || trackerId == null) return
        doAsync { TileWidgets.bind(context, appWidgetId, trackerId) }
    }
}
