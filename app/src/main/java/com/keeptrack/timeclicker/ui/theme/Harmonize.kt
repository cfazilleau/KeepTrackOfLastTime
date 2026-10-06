package com.keeptrack.timeclicker.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Shifts [color]'s hue toward [source]'s by half their difference, at most 15°, keeping its lightness
 * and chroma: Material's "harmonize", done in OKLCH. Fixed colours then sit well with dynamic ones.
 */
fun harmonize(color: Color, source: Color): Color {
    val (l, c, h) = color.toOklch()
    val (_, sourceC, sourceH) = source.toOklch()
    if (c < 0.01 || sourceC < 0.01) return color // greys have no hue to move
    var diff = sourceH - h
    if (diff > 180) diff -= 360
    if (diff < -180) diff += 360
    val rotation = min(abs(diff) * 0.5, 15.0) * if (diff < 0) -1 else 1
    return oklchToColor(l, c, h + rotation, color.alpha)
}

private fun Color.toOklch(): Triple<Double, Double, Double> {
    fun lin(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    val r = lin(red); val g = lin(green); val b = lin(blue)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val ll = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val hue = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
    return Triple(ll, hypot(a, bb), hue)
}

private fun oklchToColor(lightness: Double, chroma: Double, hue: Double, alpha: Float): Color {
    val rad = hue * PI / 180
    val a = chroma * cos(rad)
    val b = chroma * sin(rad)
    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    fun gamma(v: Double): Float {
        val c = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
        return c.toFloat().coerceIn(0f, 1f)
    }
    return Color(
        red = gamma(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
        green = gamma(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
        blue = gamma(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s),
        alpha = alpha,
    )
}
