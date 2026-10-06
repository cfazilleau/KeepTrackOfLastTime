package com.keeptrack.timeclicker.ui.theme

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keeptrack.timeclicker.data.IconCatalog
import com.keeptrack.timeclicker.data.TileIcon

/** 24x24 outline icons, built from SVG path data. Tinted by `Icon(tint = …)`. */
private fun strokeIcon(name: String, pathData: String, width: Float = 2f): ImageVector =
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

/** How tile icons are drawn, in the app and on widgets: Lucide's stroke on its 24x24 grid. */
object IconPaths {
    const val TILE_STROKE = 2f
}

object AppIcons {
    val Add = strokeIcon("add", "M5 12h14M12 5v14", 2.2f)
    val Refresh = strokeIcon("refresh", "M21 12a9 9 0 1 1 -9 -9c2.52 0 4.93 1 6.74 2.74L21 8M21 3v5h-5", 2.4f)
    val Back = strokeIcon("back", "M15 18l-6 -6 6 -6", 2.2f)
    val Groups = strokeIcon("groups", "M10 5H3M12 19H3M14 3v4M16 17v4M21 12h-9M21 19h-5M21 5h-7M8 10v4M8 12H3")
    val Info = strokeIcon("info", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0zM12 16v-4M12 8h.01")
    val Widget = strokeIcon(
        "widget",
        "M12 3v17a1 1 0 0 1 -1 1H5a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v6a1 1 0 0 1 -1 1" +
            "H3M16 19h6M19 22v-6",
    )
    val Settings = strokeIcon(
        "settings",
        "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0" +
            " 0 3.831 2.34 2.34 0 0 1 -2.33 4.033 2.34 2.34 0 0 0 -3.319 1.915 2.34 2.34 0 0 1 -4.659 0 2.34 2.34 0" +
            " 0 0 -3.32 -1.915 2.34 2.34 0 0 1 -2.33 -4.033 2.34 2.34 0 0 0 0 -3.831A2.34 2.34 0 0 1 6.35 6.051" +
            "a2.34 2.34 0 0 0 3.319 -1.915M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
    )
    val Export = strokeIcon("export", "M12 3v12M17 8l-5 -5 -5 5M21 15v4a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2v-4")
    val Import = strokeIcon("import", "M12 15V3M21 15v4a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2v-4M7 10l5 5 5 -5")
    val Language = strokeIcon(
        "language",
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0zM12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0 -20" +
            "M2 12h20",
    )
    val Heart = strokeIcon(
        "heart",
        "M2 9.5a5.5 5.5 0 0 1 9.591 -3.676 .56 .56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29 -1.5 4 -3 5.5" +
            "l-5.492 5.313a2 2 0 0 1 -3 .019L5 15c-1.5 -1.5 -3 -3.2 -3 -5.5",
    )
    val Palette = strokeIcon(
        "palette",
        "M8.3 10a.7 .7 0 0 1 -.626 -1.079L11.4 3a.7 .7 0 0 1 1.198 -.043L16.3 8.9a.7 .7 0 0 1 -.572 1.1Z" +
            "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z" +
            "M14 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0z",
    )
    val Search = strokeIcon("search", "M21 21l-4.34 -4.34M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0z", 2.2f)
    val Close = strokeIcon("close", "M18 6L6 18M6 6l12 12", 2.2f)
    val Chevron = strokeIcon("chevron", "M9 18l6 -6 -6 -6", 2.2f)
    /** The "no icon" choice in the icon picker. */
    val NoIcon = strokeIcon(
        "none",
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0zM4.929 4.929 19.07 19.071",
        IconPaths.TILE_STROKE,
    )
    /** Opens every icon, from the icon picker. */
    val AllIcons = strokeIcon(
        "all-icons",
        "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1z" +
            "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1z" +
            "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z" +
            "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
        IconPaths.TILE_STROKE,
    )
    val More = strokeIcon(
        "more",
        "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0zM11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0zM11 19a1 1 0 1 0 2 0" +
            "a1 1 0 1 0 -2 0z",
    )
    val DragHandle = strokeIcon(
        "drag",
        "M8 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0zM8 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0zM8 19a1 1 0 1 0 2 0" +
            "a1 1 0 1 0 -2 0zM14 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0zM14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0" +
            "zM14 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z",
    )

    // Built on first use: a palette screen scrolling through every icon shouldn't keep them all.
    private val tileIcons = LruCache<String, ImageVector>(400)

    /** The vector for a tile icon, from its path in the [IconCatalog]. Null for [TileIcon.NONE] or an unknown icon. */
    fun tile(catalog: IconCatalog, icon: TileIcon): ImageVector? {
        if (icon.isNone) return null
        tileIcons[icon.key]?.let { return it }
        val path = catalog.path(icon) ?: return null
        return strokeIcon(icon.key, path, IconPaths.TILE_STROKE).also { tileIcons.put(icon.key, it) }
    }
}

/** The icon catalog, loaded on first use; null for the moment it takes to read. */
@Composable
fun rememberIconCatalog(): IconCatalog? {
    val catalog by IconCatalog.loaded.collectAsState()
    if (catalog == null) {
        val context = LocalContext.current
        LaunchedEffect(Unit) { IconCatalog.load(context.applicationContext) }
    }
    return catalog
}

/** A tile's icon, or null for no icon (and while the catalog loads). */
@Composable
fun rememberTileIcon(icon: TileIcon): ImageVector? {
    val catalog = rememberIconCatalog() ?: return null
    return remember(catalog, icon) { AppIcons.tile(catalog, icon) }
}
