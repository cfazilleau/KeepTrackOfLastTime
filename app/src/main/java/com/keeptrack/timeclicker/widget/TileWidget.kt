package com.keeptrack.timeclicker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.keeptrack.timeclicker.MainActivity
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.TileColor
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.ui.theme.IconPaths
import com.keeptrack.timeclicker.ui.theme.TimeClickerPalette
import com.keeptrack.timeclicker.ui.theme.TileColors
import com.keeptrack.timeclicker.ui.theme.timeClickerPalette
import com.keeptrack.timeclicker.ui.theme.supportsDynamicColor
import com.keeptrack.timeclicker.ui.time.RelativeTime
import com.keeptrack.timeclicker.ui.time.TimeUnit
import com.keeptrack.timeclicker.ui.time.format
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** A tile on the home screen. Tap = done now; the icon opens the app. */
class TileWidget : GlanceAppWidget() {

    // Re-rendered for each exact size, so the headline can be sized to fit.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.trackerRepository
        val trackers = repository.observeTrackers()
        // Loaded up front so the first frame already shows the tile (and its photo), without a flash.
        val initial = trackers.first()
        val shownId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TileWidgets.TRACKER_ID]
        val initialPhoto = initial.firstOrNull { it.id == shownId }?.photo?.let { it to loadPhoto(context, it) }
        TileWidgets.scheduleNextTick(context)

        provideContent {
            val all by remember { trackers }.collectAsState(initial)
            val trackerId = currentState<Preferences>()[TileWidgets.TRACKER_ID]
            val tracker = all.firstOrNull { it.id == trackerId }
            val photoName = tracker?.photo
            val photo by produceState(initialPhoto?.takeIf { it.first == photoName }?.second, photoName) {
                value = photoName?.let { loadPhoto(context, it) }
            }
            GlanceTheme {
                when {
                    tracker != null -> TileContent(tracker, photo?.takeIf { photoName != null })
                    else -> ChooseTile(deleted = trackerId != null)
                }
            }
        }
    }
}

private val Corner = 24.dp

private fun GlanceModifier.widgetShape(): GlanceModifier =
    appWidgetBackground().then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
        } else {
            GlanceModifier.cornerRadius(Corner)
        }
    )

private fun color(light: Color, dark: Color) = ColorProvider(day = light, night = dark)

/** Background and content colours of a tile in both themes. Wallpaper tiles use the live system colours. */
private class WidgetColors(val background: ColorProvider, val content: ColorProvider, val glass: ColorProvider)

@Composable
private fun widgetColors(tracker: Tracker, hasPhoto: Boolean): WidgetColors {
    val context = LocalContext.current
    val (light, dark) = remember { timeClickerPalette(context, dark = false) to timeClickerPalette(context, dark = true) }
    if (hasPhoto) {
        return WidgetColors(
            background = ColorProvider(light.photoTile.background),
            content = ColorProvider(Color.White),
            glass = ColorProvider(Color.White.copy(alpha = 0.2f)),
        )
    }
    val glass = color(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.09f))
    val theme = GlanceTheme.colors
    return when {
        supportsDynamicColor && tracker.color == TileColor.PRIMARY ->
            WidgetColors(theme.primaryContainer, theme.onPrimaryContainer, glass)
        supportsDynamicColor && tracker.color == TileColor.SECONDARY ->
            WidgetColors(theme.secondaryContainer, theme.onSecondaryContainer, glass)
        supportsDynamicColor && tracker.color == TileColor.TERTIARY ->
            WidgetColors(theme.tertiaryContainer, theme.onTertiaryContainer, glass)
        else -> {
            val l: TileColors = light.tile(tracker.color)
            val d: TileColors = dark.tile(tracker.color)
            WidgetColors(color(l.background, d.background), color(l.content, d.content), glass)
        }
    }
}

@Composable
private fun TileContent(tracker: Tracker, photo: Bitmap?) {
    val context = LocalContext.current
    val size = LocalSize.current
    val colors = widgetColors(tracker, hasPhoto = photo != null)
    val now = Instant.now() // recomposed on every refresh (see TileWidgets.REFRESHED_AT)
    val elapsed = RelativeTime.split(tracker.lastDoneAt, now, TimeUnit.MINUTE)
    val justDone = Duration.between(tracker.lastDoneAt, now) < Duration.ofMinutes(1)
    val headline = elapsed.major?.format(context.resources)?.replaceFirstChar { it.uppercase() }
        ?: context.getString(R.string.elapsed_just_now)
    // Every size shows the same content; small widgets just get tighter padding.
    val padding = if (size.height < 120.dp || size.width < 120.dp) 10.dp else 14.dp

    // Approximate auto-size: about 0.6em per character of a bold headline.
    val available = size.width.value - padding.value * 2
    val maxHeadline = if (size.width > 200.dp && size.height > 160.dp) 40f else 30f
    val headlineSize = (available / (headline.length * 0.6f)).coerceIn(16f, maxHeadline)

    Box(
        GlanceModifier
            .fillMaxSize()
            .widgetShape()
            .background(colors.background)
            .clickable(actionRunCallback<MarkDoneAction>(actionParametersOf(TileWidgets.TrackerIdParam to tracker.id))),
    ) {
        if (photo != null) {
            Image(ImageProvider(photo), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Image(ImageProvider(R.drawable.widget_photo_scrim), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
        Column(GlanceModifier.fillMaxSize().padding(padding)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    GlanceModifier
                        .size(34.dp)
                        .cornerRadius(12.dp)
                        .background(colors.glass)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        ImageProvider(iconBitmap(context, IconPaths.tiles.getValue(tracker.icon), IconPaths.TILE_STROKE, 18)),
                        contentDescription = context.getString(R.string.widget_open_app),
                        colorFilter = ColorFilter.tint(colors.content),
                        modifier = GlanceModifier.size(18.dp),
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                if (justDone) {
                    Box(
                        GlanceModifier.size(30.dp).cornerRadius(15.dp).background(colors.content),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            ImageProvider(iconBitmap(context, IconPaths.CHECK, 2.6f, 14)),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colors.background),
                            modifier = GlanceModifier.size(14.dp),
                        )
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                tracker.name,
                style = TextStyle(color = colors.content, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
            )
            Text(
                headline,
                style = TextStyle(color = colors.content, fontSize = headlineSize.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    elapsed.minor?.format(context.resources).orEmpty(),
                    style = TextStyle(color = colors.content, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    NumberFormat.getIntegerInstance().format(tracker.pressCount),
                    style = TextStyle(color = colors.content, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.cornerRadius(10.dp).background(colors.glass).padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** No tile yet (just added), or its tile was deleted: tap to pick one. */
@Composable
private fun ChooseTile(deleted: Boolean) {
    val context = LocalContext.current
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(LocalGlanceId.current)
    val intent = Intent(context, TileWidgetConfigActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    Column(
        GlanceModifier
            .fillMaxSize()
            .widgetShape()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity(intent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (deleted) {
            Text(
                context.getString(R.string.widget_tile_deleted),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            Spacer(GlanceModifier.height(4.dp))
        }
        Text(
            context.getString(R.string.widget_choose_tile),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
        )
    }
}

/** Draws a 24x24 stroke icon as a white bitmap, to be tinted. */
private fun iconBitmap(context: Context, pathData: String, strokeWidth: Float, sizeDp: Int): Bitmap {
    val px = (sizeDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    return createBitmap(px, px).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.scale(px / 24f, px / 24f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(PathParser.createPathFromPathData(pathData), paint)
    }
}

/** Decodes a tile photo small enough for a widget (software bitmap: widgets can't use hardware ones). */
private suspend fun loadPhoto(context: Context, name: String): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(context.trackerRepository.photoFile(name))
        .size(640)
        .allowHardware(false)
        .build()
    return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
}
