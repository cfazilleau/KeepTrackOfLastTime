package com.keeptrack.timeclicker.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.keeptrack.timeclicker.R
import kotlin.math.pow
import kotlin.random.Random

/**
 * The short click played when a tile is marked as done, in the app or from a widget. One small pool
 * for the whole app.
 *
 * It plays on the media stream (like a game's sounds), so it is heard when the phone is on silent or
 * vibrate, at the media volume. Loading is asynchronous: call [preload] early. A click requested before
 * the sound is loaded (a widget tap that just started the app) plays as soon as it is.
 *
 * Each click is played at a slightly different pitch, so repeated taps sound lively rather than
 * mechanical.
 */
object TapSound {
    private var pool: SoundPool? = null
    private var soundId = 0
    private var loaded = false
    private var playWhenLoaded = false

    @Synchronized
    fun preload(context: Context) {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build().apply {
            setOnLoadCompleteListener { pool, _, status -> onLoaded(pool, status == 0) }
            soundId = load(context.applicationContext, R.raw.tile_click, 1)
        }
    }

    @Synchronized
    fun play(context: Context) {
        preload(context)
        if (loaded) pool?.play(soundId, VOLUME, VOLUME, 1, 0, randomRate()) else playWhenLoaded = true
    }

    @Synchronized
    private fun onLoaded(pool: SoundPool, success: Boolean) {
        loaded = success
        if (success && playWhenLoaded) pool.play(soundId, VOLUME, VOLUME, 1, 0, randomRate())
        playWhenLoaded = false
    }

    /** A playback rate up to [PITCH_SEMITONES] semitones above or below the original pitch. */
    private fun randomRate(): Float =
        2f.pow((Random.nextFloat() * 2f - 1f) * PITCH_SEMITONES / 12f)

    private const val VOLUME = 0.7f
    private const val PITCH_SEMITONES = 2f
}
