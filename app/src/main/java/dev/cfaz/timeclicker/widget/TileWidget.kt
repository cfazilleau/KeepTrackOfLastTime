package dev.cfaz.timeclicker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.appwidget.AndroidRemoteViews
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
import dev.cfaz.timeclicker.MainActivity
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.IconCatalog
import dev.cfaz.timeclicker.data.PendingUndo
import dev.cfaz.timeclicker.data.TileColor
import dev.cfaz.timeclicker.data.TimeDisplay
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.data.UNDO_WINDOW_MS
import dev.cfaz.timeclicker.ui.theme.IconPaths
import dev.cfaz.timeclicker.ui.theme.TimeClickerPalette
import dev.cfaz.timeclicker.ui.theme.TileColors
import dev.cfaz.timeclicker.ui.theme.timeClickerPalette
import dev.cfaz.timeclicker.ui.theme.supportsDynamicColor
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.TimeUnit
import dev.cfaz.timeclicker.ui.time.absoluteTime
import dev.cfaz.timeclicker.ui.time.agoAffixes
import dev.cfaz.timeclicker.ui.time.format
import dev.cfaz.timeclicker.ui.time.formatAsSecond
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.Instant
import kotlin.math.roundToInt

/** A tile on the home screen. Tap = done now, tap again within the undo window = undo; the icon opens the app. */
class TileWidget : GlanceAppWidget() {

    // Re-rendered for each exact size, so the headline can be sized to fit.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.trackerRepository
        val trackers = repository.observeTrackers()
        val undoable = context.tilePresses.undoable
        // Loaded up front so the first frame already shows the tile (and its photo), without a flash.
        val initial = trackers.first()
        val shownId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TileWidgets.TRACKER_ID]
        val initialPhoto = initial.firstOrNull { it.id == shownId }?.photo?.let { it to loadPhoto(context, it) }
        val icons = IconCatalog.load(context)
        TileWidgets.scheduleNextTick(context)

        provideContent {
            val all by remember { trackers }.collectAsState(initial)
            val state = currentState<Preferences>()
            val trackerId = state[TileWidgets.TRACKER_ID]
            // Passed down so the tile recomposes on every refresh: with the same tile and photo,
            // it would otherwise be skipped and keep showing the time of its last change.
            val now = remember(state[TileWidgets.REFRESHED_AT], all) { Instant.now() }
            val tracker = all.firstOrNull { it.id == trackerId }
            val undoing by remember { undoable }.collectAsState()
            val photoName = tracker?.photo
            val photo by produceState(initialPhoto?.takeIf { it.first == photoName }?.second, photoName) {
                value = photoName?.let { loadPhoto(context, it) }
            }
            GlanceTheme {
                when {
                    tracker != null -> TileContent(tracker, photo?.takeIf { photoName != null }, now, icons, undoing[tracker.id])
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

/** Background and content colours of a tile in both themes. System-colour tiles use the live Material You colours. */
private class WidgetColors(val background: ColorProvider, val content: ColorProvider, val glass: ColorProvider)

@Composable
private fun widgetColors(tracker: Tracker, hasPhoto: Boolean): WidgetColors {
    val context = LocalContext.current
    val dynamicColors = context.appSettings.dynamicColors
    val (light, dark) = remember(dynamicColors) {
        timeClickerPalette(context, dark = false, dynamicColors) to timeClickerPalette(context, dark = true, dynamicColors)
    }
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
        dynamicColors && supportsDynamicColor && tracker.color == TileColor.PRIMARY ->
            WidgetColors(theme.primaryContainer, theme.onPrimaryContainer, glass)
        dynamicColors && supportsDynamicColor && tracker.color == TileColor.SECONDARY ->
            WidgetColors(theme.secondaryContainer, theme.onSecondaryContainer, glass)
        dynamicColors && supportsDynamicColor && tracker.color == TileColor.TERTIARY ->
            WidgetColors(theme.tertiaryContainer, theme.onTertiaryContainer, glass)
        else -> {
            val l: TileColors = light.tile(tracker.color)
            val d: TileColors = dark.tile(tracker.color)
            WidgetColors(color(l.background, d.background), color(l.content, d.content), glass)
        }
    }
}

@Composable
private fun TileContent(tracker: Tracker, photo: Bitmap?, now: Instant, icons: IconCatalog, pendingUndo: PendingUndo?) {
    val context = LocalContext.current
    val size = LocalSize.current
    val colors = widgetColors(tracker, hasPhoto = photo != null)
    val elapsed = RelativeTime.split(tracker.lastDoneAt, now, TimeUnit.MINUTE)
    // "3 days" + "5 hours ago", or "il y a 3 jours" + "et 5 heures": "… ago" wraps the whole time.
    val ago = agoAffixes(context.resources)
    val absolute = if (context.appSettings.timeDisplay == TimeDisplay.ABSOLUTE) absoluteTime(context, tracker.lastDoneAt, now) else null
    // Widgets refresh once a minute, so they can't count the seconds: under a minute, say so.
    val underAMinute = absolute == null && elapsed.major == null
    val headline = absolute?.headline
        ?: elapsed.major?.format(context.resources)?.let { ago.prefix + it }?.replaceFirstChar { it.uppercase() }
        ?: context.getString(R.string.elapsed_under_a_minute)
    val undo = pendingUndo != null
    val subline = when {
        undo -> context.getString(R.string.tile_undo_hint)
        absolute != null -> absolute.detail
        elapsed.major == null -> ""
        else -> (elapsed.minor?.formatAsSecond(context.resources).orEmpty() + ago.suffix).trim()
    }
    // Down to one cell (1x1, or a single row): just the name and the time, centred.
    val compact = size.width < 100.dp || size.height < 120.dp
    // Otherwise every size shows the same content; small widgets just get tighter padding.
    val padding = if (compact) 6.dp else if (size.height < 120.dp || size.width < 120.dp) 10.dp else 14.dp

    // Approximate auto-size: about 0.6em per character of a bold headline.
    // "Less than a minute ago" is long, so it may wrap onto a second line.
    val available = size.width.value - padding.value * 2
    val maxHeadline = if (compact) 22f else if (size.width > 200.dp && size.height > 160.dp) 40f else 30f
    val headlineLines = if (underAMinute) 2 else 1
    val minHeadline = if (compact) 10f else if (underAMinute) 13f else 16f
    // Wrapped text fills its lines less evenly, hence the margin.
    val fill = if (underAMinute) headlineLines * 0.85f else 1f
    val headlineSize = (available * fill / (headline.length * 0.6f)).coerceIn(minHeadline, maxHeadline)

    Box(
        GlanceModifier
            .fillMaxSize()
            .widgetShape()
            .background(colors.background)
            .clickable(
                actionRunCallback<MarkDoneAction>(actionParametersOf(TileWidgets.TrackerIdParam to tracker.id)),
                rippleOverride = R.drawable.widget_tile_pressed,
            ),
    ) {
        if (photo != null) {
            Image(ImageProvider(photo), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Image(ImageProvider(R.drawable.widget_photo_scrim), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
        if (compact) Column(
            GlanceModifier.fillMaxSize().padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                tracker.name,
                style = TextStyle(color = colors.content, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
            )
            // No room for the undo hint: the ring alone says the next tap undoes.
            if (pendingUndo != null) {
                Spacer(GlanceModifier.height(4.dp))
                UndoBubble(pendingUndo, colors)
            } else {
                Text(
                    headline,
                    style = TextStyle(color = colors.content, fontSize = headlineSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLines = headlineLines,
                )
            }
        } else Column(GlanceModifier.fillMaxSize().padding(padding)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Without an icon there is no "open app" button; the whole widget still marks the tile done.
                icons.path(tracker.icon)?.let { path ->
                    Box(
                        GlanceModifier
                            .size(34.dp)
                            .cornerRadius(12.dp)
                            .background(colors.glass)
                            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            ImageProvider(iconBitmap(context, path, IconPaths.TILE_STROKE, 18)),
                            contentDescription = context.getString(R.string.widget_open_app),
                            colorFilter = ColorFilter.tint(colors.content),
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }
                }
                // Only while a tap can be undone, as on the app's tiles.
                if (pendingUndo != null) {
                    Spacer(GlanceModifier.defaultWeight())
                    UndoBubble(pendingUndo, colors)
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
                maxLines = headlineLines,
            )
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subline,
                    style = TextStyle(color = colors.content, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (context.appSettings.showCounter) {
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
}

private val UndoBubbleSize = 30

/**
 * The undo arrow, ringed by the time left to undo. From Android 12 the ring is an animation the launcher plays,
 * so it drains smoothly as in the app; before that it can't be tinted to the tile, so it is redrawn once a second.
 */
@Composable
private fun UndoBubble(pendingUndo: PendingUndo, colors: WidgetColors) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SmoothUndoBubble(colors) else SteppedUndoBubble(pendingUndo, colors)
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun SmoothUndoBubble(colors: WidgetColors) {
    val context = LocalContext.current
    val ring = RemoteViews(context.packageName, R.layout.widget_undo_ring).apply {
        val (day, night) = listOf(false, true).map { night -> ColorStateList.valueOf(colors.content.getColor(context.inNightMode(night)).toArgb()) }
        setColorStateList(R.id.undo_ring, "setIndeterminateTintList", day, night)
    }
    Box(
        GlanceModifier.size(UndoBubbleSize.dp).cornerRadius((UndoBubbleSize / 2).dp).background(colors.glass),
        contentAlignment = Alignment.Center,
    ) {
        AndroidRemoteViews(ring, GlanceModifier.size(UndoBubbleSize.dp))
        Image(
            ImageProvider(remember { undoBitmap(context, left = 0f) }),
            contentDescription = context.getString(R.string.action_undo),
            colorFilter = ColorFilter.tint(colors.content),
            modifier = GlanceModifier.size(UndoBubbleSize.dp),
        )
    }
}

private fun Context.inNightMode(night: Boolean): Context {
    val config = Configuration(resources.configuration)
    val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
    return createConfigurationContext(config)
}

@Composable
private fun SteppedUndoBubble(pendingUndo: PendingUndo, colors: WidgetColors) {
    val context = LocalContext.current
    val secondsLeft by produceState(secondsLeft(pendingUndo), pendingUndo) {
        while (value > 0) {
            // Wakes on each whole second left, so the ring steps evenly.
            delay((pendingUndo.until - SystemClock.elapsedRealtime()) % 1000 + 1)
            value = secondsLeft(pendingUndo)
        }
    }
    val windowSeconds = (UNDO_WINDOW_MS / 1000).toInt()
    Box(
        GlanceModifier.size(UndoBubbleSize.dp).cornerRadius((UndoBubbleSize / 2).dp).background(colors.glass),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(remember(secondsLeft) { undoBitmap(context, secondsLeft.toFloat() / windowSeconds) }),
            contentDescription = context.getString(R.string.action_undo),
            colorFilter = ColorFilter.tint(colors.content),
            modifier = GlanceModifier.size(UndoBubbleSize.dp),
        )
    }
}

/** Whole seconds left in the undo window, rounded up: 10 right after the tap, 0 once it closes. */
private fun secondsLeft(pendingUndo: PendingUndo): Int =
    ((pendingUndo.until - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999).toInt() / 1000

/** The undo bubble's content, in white to be tinted: the arrow, and a ring for the [left] share of the window. */
private fun undoBitmap(context: Context, left: Float): Bitmap {
    val density = context.resources.displayMetrics.density
    val px = (UndoBubbleSize * density).roundToInt()
    return createBitmap(px, px).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (left > 0f) {
            paint.strokeWidth = 2 * density
            val inset = paint.strokeWidth / 2
            canvas.drawArc(RectF(inset, inset, px - inset, px - inset), -90f, 360f * left, false, paint)
        }
        // The 16dp arrow, centred, from its 24x24 grid.
        val icon = 16 * density
        canvas.translate((px - icon) / 2, (px - icon) / 2)
        canvas.scale(icon / 24f, icon / 24f)
        paint.strokeWidth = IconPaths.UNDO_STROKE
        canvas.drawPath(PathParser.createPathFromPathData(IconPaths.UNDO), paint)
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
