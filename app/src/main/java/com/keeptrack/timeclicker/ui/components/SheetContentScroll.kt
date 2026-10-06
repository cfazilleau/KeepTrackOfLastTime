package com.keeptrack.timeclicker.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Decides which scrolls of a bottom sheet's content may move the sheet itself.
 *
 * Only a drag that starts with the content already at the top ([isAtTop]) reaches the sheet, so pulling
 * down there still drags it closed. A drag or fling that starts further down stops at the top instead:
 * otherwise its leftover would pull the sheet down, and a fast scroll back up closed it.
 * Upward flings never reach the sheet either: it is already fully open and would overshoot and
 * spring back, so the sheet bounced while scrolling down.
 */
class SheetContentScroll(private val isAtTop: () -> Boolean) : NestedScrollConnection {
    private var inGesture = false
    private var startedAtTop = true

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && !inGesture) {
            inGesture = true
            startedAtTop = isAtTop()
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (startedAtTop) Offset.Zero else available.copy(x = 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val keep = !startedAtTop || available.y < 0
        inGesture = false
        startedAtTop = true
        return if (keep) available.copy(x = 0f) else Velocity.Zero
    }
}
