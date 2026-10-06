package com.keeptrack.timeclicker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.keeptrack.timeclicker.data.TileIcon

/** 24x24 outline icons, built from SVG path data. Tinted by `Icon(tint = …)`. */
private fun strokeIcon(name: String, pathData: String, width: Float = 1.9f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()

private fun fillIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

private fun dot(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

/** Raw SVG path data on a 24x24 grid, shared with the home-screen widgets (which draw them as bitmaps). */
object IconPaths {
    const val TILE_STROKE = 1.9f
    const val REFRESH = "M20 12a8 8 0 1 1-2.4-5.7M20 4v5h-5"
    const val CHECK = "M5 12.5l4.5 4.5L19 7.5"

    /** Every tile icon but [TileIcon.NONE]. */
    val tiles: Map<TileIcon, String> = mapOf(
        TileIcon.CHECK to "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM8 12.5l2.8 2.8L16 10",
        TileIcon.DROP to "M12 3c3.2 4.2 6 7.3 6 11a6 6 0 0 1-12 0c0-3.7 2.8-6.8 6-11z",
        TileIcon.LEAF to "M5 19c0-8 5-14 15-14 0 10-6 15-14 15M5 19l7-7",
        TileIcon.GRASS to "M4 20h16M7 20c0-5 1-9 3-12M11 20c0-4 .6-7 2.2-10M15 20c0-3 1-6 3-8",
        TileIcon.FLOWER to "M12 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM12 12v9M12 17c-1.8-1.8-4-2.2-6-1.2M12 18.5c1.8-1.8 4-2.2 6-1.2",
        TileIcon.BED to "M3 19v-7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v7M3 15h18M7 10V7.5A1.5 1.5 0 0 1 8.5 6h2A1.5 1.5 0 0 1 12 7.5V10",
        TileIcon.COFFEE to "M5 9h11v4a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5V9zM16 10h1.5a2.5 2.5 0 0 1 0 5H16M9 3.5v2.5M12.5 3.5v2.5",
        TileIcon.SNOW to "M12 3v18M4.2 7.5l15.6 9M4.2 16.5l15.6-9M9.5 4.5L12 7l2.5-2.5M9.5 19.5L12 17l2.5 2.5",
        TileIcon.TRASH to "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v5M14 11v5",
        TileIcon.CART to "M3 4h2l2.4 11h10.2L20 8H6.2M9 20h.01M17 20h.01",
        TileIcon.PHONE to "M6 4h3l1.8 4.5-2.3 1.4a11 11 0 0 0 5.6 5.6l1.4-2.3L20 15v3a2 2 0 0 1-2 2A15 15 0 0 1 4 6a2 2 0 0 1 2-2z",
        TileIcon.HEART to "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z",
        TileIcon.PILL to "M10.5 20.5a5 5 0 0 1-7-7l6-6a5 5 0 0 1 7 7zM7.5 10.5l6 6",
        TileIcon.PAW to "M12 20c-3 0-5-1.6-5-3.6 0-2.4 2.4-4.4 5-4.4s5 2 5 4.4c0 2-2 3.6-5 3.6z" +
            dot(6f, 9.5f, 1.8f) + dot(18f, 9.5f, 1.8f) + dot(9.5f, 5.5f, 1.8f) + dot(14.5f, 5.5f, 1.8f),
        TileIcon.SCISSORS to "M6 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM8.1 7.9L20 18M8.1 16.1L20 6",
        TileIcon.BRUSH to "M5 19l8.5-8.5M13.5 10.5l2-2M14 4h6v4.5h-6zM16 4v4.5M18 4v4.5",
        TileIcon.GAUGE to "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 13l3.5-3.5M7 16h10",
        TileIcon.CAR to "M4 16v-3l2-5h12l2 5v3M4 16h16M4 16v3h3v-3M17 16v3h3v-3M7.5 13h.01M16.5 13h.01",
    )
}

object AppIcons {
    val Add = strokeIcon("add", "M12 5v14M5 12h14", 2.2f)
    val Refresh = strokeIcon("refresh", IconPaths.REFRESH, 2.4f)
    val Check = strokeIcon("check", IconPaths.CHECK, 2.6f)
    val Back = strokeIcon("back", "M15 5l-7 7 7 7", 2.2f)
    val Groups = strokeIcon("groups", "M4 7h10M18 7h2M4 17h4M12 17h8M16 4.5v5M10 14.5v5", 2f)
    val Info = strokeIcon("info", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 11v5M12 7.5h.01", 2f)
    val Widget = strokeIcon("widget", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM16.5 13v7M13 16.5h7", 2f)
    val Settings = strokeIcon(
        "settings",
        "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06" +
            "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4" +
            "a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3" +
            "a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06" +
            "a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33" +
            "l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09" +
            "a1.65 1.65 0 0 0-1.51 1z",
        1.8f,
    )
    val Export = strokeIcon("export", "M12 15V3M7 8l5-5 5 5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4", 2f)
    val Import = strokeIcon("import", "M12 3v12M7 10l5 5 5-5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4", 2f)
    val Language = strokeIcon(
        "language",
        "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3c2.5 2.7 3.8 5.7 3.8 9s-1.3 6.3-3.8 9c-2.5-2.7-3.8-5.7-3.8-9S9.5 5.7 12 3z",
        1.9f,
    )
    val Heart = strokeIcon("heart", IconPaths.tiles.getValue(TileIcon.HEART), 2f)
    val Chevron = strokeIcon("chevron", "M9 5l7 7-7 7", 2.2f)
    /** The "no icon" choice in the icon picker. */
    val NoIcon = strokeIcon("none", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM5.6 5.6l12.8 12.8", IconPaths.TILE_STROKE)
    val More = fillIcon("more", dot(12f, 5.5f, 1.8f) + dot(12f, 12f, 1.8f) + dot(12f, 18.5f, 1.8f))
    val DragHandle = fillIcon(
        "drag",
        listOf(6f, 12f, 18f).joinToString("") { y -> dot(9f, y, 1.6f) + dot(15f, y, 1.6f) },
    )

    private val tileIcons: Map<TileIcon, ImageVector> =
        IconPaths.tiles.mapValues { (icon, path) -> strokeIcon(icon.key, path, IconPaths.TILE_STROKE) }

    /** Null for [TileIcon.NONE]. */
    fun tile(icon: TileIcon): ImageVector? = tileIcons[icon]
}
