package dev.cfaz.timeclicker.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/** The widest that rows of settings and text get: on a tablet they keep a phone's measure instead of stretching across. */
private val ContentMaxWidth = 640.dp
private val MinGutter = 20.dp

/**
 * Side padding for a screen [width] wide: [MinGutter] on a phone; on a tablet, enough to keep the content
 * [ContentMaxWidth] wide and centred.
 */
private fun gutter(width: Dp): Dp = max(MinGutter, (width - ContentMaxWidth) / 2)

/**
 * A screen's column, whose [content] pads its sides by the [gutter] for the column's width.
 * Padding inside the scrolling lists, rather than a narrower column, keeps the margins scrollable.
 */
@Composable
fun GutteredColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.(gutter: Dp) -> Unit) {
    BoxWithConstraints(modifier) {
        val gutter = gutter(maxWidth)
        Column(Modifier.fillMaxSize()) { content(gutter) }
    }
}
