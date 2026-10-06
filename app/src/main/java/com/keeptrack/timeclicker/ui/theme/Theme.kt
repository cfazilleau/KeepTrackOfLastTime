package com.keeptrack.timeclicker.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.keeptrack.timeclicker.data.AppSettings
import com.keeptrack.timeclicker.data.ThemeMode
import com.keeptrack.timeclicker.data.TileColor

/** Background, text and shadow colours of one tile style. */
@Immutable
data class TileColors(
    val background: Color,
    val content: Color,
    /** Tint of the soft drop shadow below-right of the tile. */
    val shadow: Color,
)

/**
 * The app's own design tokens (the neumorphic "bento" look). On Android 12+ they are derived from
 * the system's Material You colours (see [timeClickerPalette]); before that they are fixed.
 */
@Immutable
data class TimeClickerPalette(
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
    /** Filled selected chips and the primary button: near-black, or the system's primary colour. */
    val accent: Color,
    val onAccent: Color,
    /** The selected option in a segmented control, lifted off the field. */
    val segment: Color,
    val toastBackground: Color,
    val toastContent: Color,
    val toastAction: Color,
    val tiles: Map<TileColor, TileColors>,
    val photoTile: TileColors,
) {
    fun tile(color: TileColor): TileColors = tiles.getValue(color)
}

val LightPalette = TimeClickerPalette(
    isDark = false,
    ground = Color(0xFFECEEF3),
    sheet = Color(0xFFF4F5F8),
    field = Color(0xFFE3E6EC),
    text = Color(0xFF1C1F26),
    muted = Color(0xFF555B69),
    shadow = Color(0x99A0A8BE),
    highlight = Color(0xF2FFFFFF),
    danger = Color(0xFFB42318),
    accent = Color(0xFF1C1F26),
    onAccent = Color(0xFFECEEF3),
    segment = Color.White,
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
    ) + schemeTiles(lightColorScheme(), isDark = false),
    photoTile = TileColors(Color(0xFF3A4150), Color.White, Color(0x6B3C465C)),
)

val DarkPalette = TimeClickerPalette(
    isDark = true,
    ground = Color(0xFF1B1D22),
    sheet = Color(0xFF23262D),
    field = Color(0xFF2C3038),
    text = Color(0xFFF1F2F5),
    muted = Color(0xFFA2A8B4),
    shadow = Color(0x99000000),
    highlight = Color(0x0DFFFFFF),
    danger = Color(0xFFFF8A80),
    accent = Color(0xFFF1F2F5),
    onAccent = Color(0xFF1B1D22),
    segment = Color(0xFF3A3F49),
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
    ) + schemeTiles(darkColorScheme(), isDark = true),
    photoTile = TileColors(Color(0xFF2A2F3A), Color.White, Color(0x99000000)),
)

/** The Material You tile colours: the scheme's primary, secondary and tertiary containers. */
private fun schemeTiles(scheme: ColorScheme, isDark: Boolean): Map<TileColor, TileColors> {
    fun tile(container: Color, content: Color, tint: Color) =
        TileColors(container, content, if (isDark) Color(0x99000000) else tint.copy(alpha = 0.42f))
    return mapOf(
        TileColor.PRIMARY to tile(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary),
        TileColor.SECONDARY to tile(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary),
        TileColor.TERTIARY to tile(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary),
    )
}

/** The neumorphic palette re-tinted with a dynamic (Material You) [scheme]. */
private fun dynamicPalette(scheme: ColorScheme, base: TimeClickerPalette): TimeClickerPalette {
    val dark = base.isDark
    val fixedTiles = base.tiles.filterKeys { it.ordinal < TileColor.PRIMARY.ordinal }.mapValues { (_, t) ->
        TileColors(harmonize(t.background, scheme.primary), harmonize(t.content, scheme.primary), harmonize(t.shadow, scheme.primary))
    }
    return base.copy(
        ground = if (dark) scheme.surfaceContainerLow else scheme.surfaceContainer,
        sheet = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLow,
        field = scheme.surfaceContainerHighest,
        text = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        shadow = if (dark) base.shadow else scheme.outline.copy(alpha = 0.45f),
        danger = scheme.error,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        segment = if (dark) scheme.surfaceBright else scheme.surfaceContainerLowest,
        toastBackground = scheme.inverseSurface,
        toastContent = scheme.inverseOnSurface,
        toastAction = scheme.inversePrimary,
        tiles = fixedTiles + schemeTiles(scheme, dark),
    )
}

/** Material You colours are available from Android 12. */
val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The palette for [context]: following the system's Material You colours on Android 12+ when [dynamicColors] is on,
 * the fixed one otherwise. Also used by widgets.
 */
fun timeClickerPalette(context: Context, dark: Boolean, dynamicColors: Boolean = true): TimeClickerPalette {
    val dynamic = supportsDynamicColor && dynamicColors
    return when {
        dynamic && dark -> dynamicPalette(dynamicDarkColorScheme(context), DarkPalette)
        dynamic -> dynamicPalette(dynamicLightColorScheme(context), LightPalette)
        dark -> DarkPalette
        else -> LightPalette
    }
}

private val LocalPalette = staticCompositionLocalOf { LightPalette }
private val LocalSettings = compositionLocalOf { AppSettings() }

object TimeClickerTheme {
    val palette: TimeClickerPalette
        @Composable get() = LocalPalette.current

    /** The user's settings, for components that follow them (haptics, press counter). */
    val settings: AppSettings
        @Composable get() = LocalSettings.current
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

// The scrims enableEdgeToEdge() uses by default (3-button navigation only).
private val LightNavScrim = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkNavScrim = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)

/** [TimeClickerTheme] following the user's [settings]. */
@Composable
fun TimeClickerTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // System bar icons follow the app's theme, which may differ from the system's.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LightNavScrim, DarkNavScrim) { dark },
        )
        onDispose {}
    }
    CompositionLocalProvider(LocalSettings provides settings) {
        TimeClickerTheme(darkTheme = dark, dynamicColors = settings.dynamicColors, content = content)
    }
}

@Composable
fun TimeClickerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val target = remember(context, darkTheme, dynamicColors) { timeClickerPalette(context, darkTheme, dynamicColors) }
    val palette = animatePalette(target)
    val colors = if (supportsDynamicColor && dynamicColors) {
        val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.copy(background = palette.ground, surface = palette.sheet)
    } else if (darkTheme) {
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

private const val ThemeFadeMillis = 350

/** Fades from the current palette to [target] when it changes (theme or system colours switched). */
@Composable
private fun animatePalette(target: TimeClickerPalette): TimeClickerPalette {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        // Start from what is on screen, even if an earlier fade hasn't finished.
        from = lerp(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(ThemeFadeMillis, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    return if (t >= 1f) to else lerp(from, to, t)
}

private fun lerp(a: TimeClickerPalette, b: TimeClickerPalette, t: Float) = TimeClickerPalette(
    isDark = if (t < 0.5f) a.isDark else b.isDark,
    ground = lerp(a.ground, b.ground, t),
    sheet = lerp(a.sheet, b.sheet, t),
    field = lerp(a.field, b.field, t),
    text = lerp(a.text, b.text, t),
    muted = lerp(a.muted, b.muted, t),
    shadow = lerp(a.shadow, b.shadow, t),
    highlight = lerp(a.highlight, b.highlight, t),
    danger = lerp(a.danger, b.danger, t),
    accent = lerp(a.accent, b.accent, t),
    onAccent = lerp(a.onAccent, b.onAccent, t),
    segment = lerp(a.segment, b.segment, t),
    toastBackground = lerp(a.toastBackground, b.toastBackground, t),
    toastContent = lerp(a.toastContent, b.toastContent, t),
    toastAction = lerp(a.toastAction, b.toastAction, t),
    tiles = b.tiles.mapValues { (color, tile) -> lerp(a.tiles[color] ?: tile, tile, t) },
    photoTile = lerp(a.photoTile, b.photoTile, t),
)

private fun lerp(a: TileColors, b: TileColors, t: Float) =
    TileColors(lerp(a.background, b.background, t), lerp(a.content, b.content, t), lerp(a.shadow, b.shadow, t))
