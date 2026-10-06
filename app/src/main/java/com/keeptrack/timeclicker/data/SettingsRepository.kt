package com.keeptrack.timeclicker.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** How tiles and widgets show when a tile was last done. */
enum class TimeDisplay(val key: String) {
    /** How long ago: "3 days 5 hours ago". */
    RELATIVE("relative"),

    /** When: "6 Oct" and "14:32". */
    ABSOLUTE("absolute");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: RELATIVE
    }
}

/** User preferences from the settings screen. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Android 12+: tint the app with the wallpaper's colours (Material You). */
    val wallpaperColors: Boolean = true,
    val timeDisplay: TimeDisplay = TimeDisplay.RELATIVE,
    /** Vibrate when a tile is tapped or long-pressed. */
    val haptics: Boolean = true,
    /** Show how many times each tile was pressed, on tiles and widgets. */
    val showCounter: Boolean = true,
    /** After tapping a tile, offer to undo it. */
    val undoAfterTap: Boolean = true,
)

/**
 * Settings are few and small, so they live in SharedPreferences: read synchronously at start-up
 * (no theme flash on launch), and exposed as a [StateFlow] for the UI.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<AppSettings> = state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val new = transform(state.value)
        write(new)
        state.value = new
    }

    private fun read() = AppSettings(
        theme = ThemeMode.fromKey(prefs.getString(THEME, null)),
        wallpaperColors = prefs.getBoolean(WALLPAPER_COLORS, true),
        timeDisplay = TimeDisplay.fromKey(prefs.getString(TIME_DISPLAY, null)),
        haptics = prefs.getBoolean(HAPTICS, true),
        showCounter = prefs.getBoolean(SHOW_COUNTER, true),
        undoAfterTap = prefs.getBoolean(UNDO_AFTER_TAP, true),
    )

    private fun write(settings: AppSettings) = prefs.edit {
        putString(THEME, settings.theme.key)
        putBoolean(WALLPAPER_COLORS, settings.wallpaperColors)
        putString(TIME_DISPLAY, settings.timeDisplay.key)
        putBoolean(HAPTICS, settings.haptics)
        putBoolean(SHOW_COUNTER, settings.showCounter)
        putBoolean(UNDO_AFTER_TAP, settings.undoAfterTap)
    }

    companion object {
        // Also the keys of the "settings" object in backups.
        const val THEME = "theme"
        const val WALLPAPER_COLORS = "wallpaper_colors"
        const val TIME_DISPLAY = "time_display"
        const val HAPTICS = "haptics"
        const val SHOW_COUNTER = "show_counter"
        const val UNDO_AFTER_TAP = "undo_after_tap"
    }
}
