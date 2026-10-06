package com.keeptrack.timeclicker.ui.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * The current time, refreshed exactly when the elapsed text since [since] changes
 * (every second for the first hour, then every minute, then hourly), while the screen is visible.
 */
@Composable
fun rememberNow(since: Instant, smallest: TimeUnit = TimeUnit.SECOND): Instant {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(lifecycle, since, smallest) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = Instant.now()
                val next = RelativeTime.nextChange(since, now, smallest)
                delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
            }
        }
    }
    return now
}
