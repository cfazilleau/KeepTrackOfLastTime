package dev.cfaz.timeclicker.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft "raised" look: a tinted shadow below-right and a light glow above-left.
 * [pressed] (0..1) fades it out, pair with [pressedIn] so the element sinks in.
 * Must come before the element's background in the modifier chain.
 */
fun Modifier.raised(
    shape: Shape,
    shadow: Color,
    highlight: Color,
    distance: Dp = 9.dp,
    blur: Dp = 22.dp,
    pressed: () -> Float = { 0f },
): Modifier = this
    .dropShadow(shape) {
        radius = blur.toPx()
        color = shadow
        offset = Offset(distance.toPx(), distance.toPx())
        alpha = 1f - pressed()
    }
    .dropShadow(shape) {
        radius = (blur * 0.9f).toPx()
        color = highlight
        offset = Offset(-(distance * 0.9f).toPx(), -(distance * 0.9f).toPx())
        alpha = 1f - pressed()
    }

/** Inset shadows, as if pushed into the surface. Must come after the element's background. */
fun Modifier.pressedIn(
    shape: Shape,
    shadow: Color,
    highlight: Color,
    distance: Dp = 6.dp,
    blur: Dp = 14.dp,
    amount: () -> Float = { 1f },
): Modifier = this
    .innerShadow(shape) {
        radius = blur.toPx()
        color = shadow
        offset = Offset(distance.toPx(), distance.toPx())
        alpha = amount()
    }
    .innerShadow(shape) {
        radius = blur.toPx()
        color = highlight
        offset = Offset(-distance.toPx(), -distance.toPx())
        alpha = amount() * 0.7f
    }
