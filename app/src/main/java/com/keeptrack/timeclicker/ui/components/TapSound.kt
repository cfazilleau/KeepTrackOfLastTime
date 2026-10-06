package com.keeptrack.timeclicker.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.keeptrack.timeclicker.R

/**
 * The short click played when a tile is marked as done. One small pool for the whole app.
 *
 * It plays as a system sound (sonification), so it is silent when the phone is on silent or vibrate.
 * Loading is asynchronous: call [preload] early so the first tap is heard.
 */
object TapSound {
    private var pool: SoundPool? = null
    private var soundId = 0
    @Volatile private var loaded = false

    @Synchronized
    fun preload(context: Context) {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build().apply {
            setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
            soundId = load(context.applicationContext, R.raw.tile_click, 1)
        }
    }

    fun play(context: Context) {
        preload(context)
        if (loaded) pool?.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
    }

    private const val VOLUME = 0.7f
}
