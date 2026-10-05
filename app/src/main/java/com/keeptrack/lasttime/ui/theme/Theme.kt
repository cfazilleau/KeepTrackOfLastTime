package com.keeptrack.lasttime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.keeptrack.lasttime.data.TileColor

/** Background, text and shadow colours of one tile style. */
@Immutable
data class TileColors(
    val background: Color,
    val content: Color,
    /** Tint of the soft drop shadow below-right of the tile. */
    val shadow: Color,
)

/** The app's own design tokens (the neumorphic "bento" look); Material colours are derived from them. */
@Immutable
data class LastTimePalette(
    val isDark: Boolean,
    val ground: Color,
    val sheet: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    /** Neutral shadow for raised elements on the ground (buttons, chips, rows). */
    val shadow: Color,
    /** Light source glow above-left of raised elements. */
    val highlight: Color,
    val danger: Color,
    val toastBackground: Color,
    val toastContent: Color,
    val toastAction: Color,
    val tiles: Map<TileColor, TileColors>,
    val photoTile: TileColors,
) {
    fun tile(color: TileColor): TileColors = tiles.getValue(color)
}

val LightPalette = LastTimePalette(
    isDark = false,
    ground = Color(0xFFECEEF3),
    sheet = Color(0xFFF4F5F8),
    field = Color(0xFFE3E6EC),
    text = Color(0xFF1C1F26),
    muted = Color(0xFF555B69),
    shadow = Color(0x99A0A8BE),
    highlight = Color(0xF2FFFFFF),
    danger = Color(0xFFB42318),
    toastBackground = Color(0xFF1F2229),
    toastContent = Color.White,
    toastAction = Color(0xFF9FE3BC),
    tiles = mapOf(
        TileColor.SAGE to TileColors(Color(0xFFCBE7D3), Color(0xFF1D4631), Color(0x6B588C6C)),
        TileColor.LAVENDER to TileColors(Color(0xFFDCD5F8), Color(0xFF352A76), Color(0x6B7868BE)),
        TileColor.PEACH to TileColors(Color(0xFFFFD8C0), Color(0xFF6E310C), Color(0x6BC88058)),
        TileColor.SKY to TileColors(Color(0xFFCDE2F7), Color(0xFF1A416A), Color(0x6B5C84B4)),
        TileColor.BUTTER to TileColors(Color(0xFFF6E6AC), Color(0xFF574409), Color(0x6BB29840)),
        TileColor.ROSE to TileColors(Color(0xFFF7CDD7), Color(0xFF71203B), Color(0x6BC46882)),
    ),
    photoTile = TileColors(Color(0xFF3A4150), Color.White, Color(0x6B3C465C)),
)

val DarkPalette = LastTimePalette(
    isDark = true,
    ground = Color(0xFF1B1D22),
    sheet = Color(0xFF23262D),
    field = Color(0xFF2C3038),
    text = Color(0xFFF1F2F5),
    muted = Color(0xFFA2A8B4),
    shadow = Color(0x99000000),
    highlight = Color(0x0DFFFFFF),
    danger = Color(0xFFFF8A80),
    toastBackground = Color(0xFFF1F2F5),
    toastContent = Color(0xFF1C1F26),
    toastAction = Color(0xFF1D6B45),
    tiles = mapOf(
        TileColor.SAGE to TileColors(Color(0xFF274536), Color(0xFFCDEFD9), Color(0x99000000)),
        TileColor.LAVENDER to TileColors(Color(0xFF352F5E), Color(0xFFE2DCFF), Color(0x99000000)),
        TileColor.PEACH to TileColors(Color(0xFF4D3022), Color(0xFFFFDCC8), Color(0x99000000)),
        TileColor.SKY to TileColors(Color(0xFF213B57), Color(0xFFD3E6FA), Color(0x99000000)),
        TileColor.BUTTER to TileColors(Color(0xFF463A19), Color(0xFFF7E8B4), Color(0x99000000)),
        TileColor.ROSE to TileColors(Color(0xFF4C2533), Color(0xFFFAD4DE), Color(0x99000000)),
    ),
    photoTile = TileColors(Color(0xFF2A2F3A), Color.White, Color(0x99000000)),
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

object LastTimeTheme {
    val palette: LastTimePalette
        @Composable get() = LocalPalette.current
}

// Single place to swap in a bundled typeface later.
private val AppFont = FontFamily.Default

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.03).em),
        headlineSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.02).em),
        titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, letterSpacing = (-0.02).em),
        titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
        bodyLarge = base.bodyLarge.copy(fontFamily = AppFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = AppFont),
        bodySmall = base.bodySmall.copy(fontFamily = AppFont),
        labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    )
}

@Composable
fun LastTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = palette.text, onPrimary = palette.ground,
            background = palette.ground, onBackground = palette.text,
            surface = palette.sheet, onSurface = palette.text,
            surfaceContainerLow = palette.sheet, surfaceContainerHigh = palette.sheet,
            surfaceContainerHighest = palette.field, onSurfaceVariant = palette.muted,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.text, onPrimary = palette.ground,
            background = palette.ground, onBackground = palette.text,
            surface = palette.sheet, onSurface = palette.text,
            surfaceContainerLow = palette.sheet, surfaceContainerHigh = palette.sheet,
            surfaceContainerHighest = palette.field, onSurfaceVariant = palette.muted,
            error = palette.danger,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
