package dev.cfaz.timeclicker.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TileSize
import dev.cfaz.timeclicker.data.UNDO_WINDOW_MS
import dev.cfaz.timeclicker.data.TimeDisplay
import dev.cfaz.timeclicker.data.Tracker
import dev.cfaz.timeclicker.ui.components.rememberPressAmount
import dev.cfaz.timeclicker.ui.theme.AppIcons
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.pressedIn
import dev.cfaz.timeclicker.ui.theme.raised
import dev.cfaz.timeclicker.ui.theme.rememberIconCatalog
import dev.cfaz.timeclicker.ui.theme.rememberTileIcon
import dev.cfaz.timeclicker.ui.time.Elapsed
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.absoluteTime
import dev.cfaz.timeclicker.ui.time.agoAffixes
import dev.cfaz.timeclicker.ui.time.format
import dev.cfaz.timeclicker.ui.time.formatAsSecond
import dev.cfaz.timeclicker.ui.time.rememberNow
import java.io.File
import java.text.NumberFormat
import androidx.compose.ui.graphics.Shadow as TextShadow

private val TileShape = RoundedCornerShape(28.dp)
private val PhotoScrim = Brush.verticalGradient(
    0f to Color(0x1F000000),
    0.35f to Color(0x14000000),
    1f to Color(0xB3000000),
)
private val PhotoTextShadow = TextShadow(Color(0x73000000), Offset(0f, 1f), 8f)

/**
 * A bento tile. Raised off the ground, sinks in while pressed. Tap = done now, long-press = edit.
 * With [enabled] false it is a static preview. Every size shows the same content, only larger.
 *
 * The elapsed time ticks by itself. Right after a tap it is hidden; after a second it fades in.
 * Until [undoUntil] (a [SystemClock.elapsedRealtime]) the tile offers to undo that tap: the next tap undoes it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TileCard(
    tracker: Tracker,
    photoFile: (String) -> File,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String = stringResource(R.string.tile_click_label),
    undoUntil: Long? = null,
) {
    val palette = TimeClickerTheme.palette
    val hasPhoto = tracker.photo != null
    val colors = if (hasPhoto) palette.photoTile else palette.tile(tracker.color)
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressAmount(interaction)
    val settings = TimeClickerTheme.settings
    val haptics = LocalHapticFeedback.current
    val now = rememberNow(tracker.lastDoneAt)
    val elapsed = RelativeTime.split(tracker.lastDoneAt, now)
    val context = LocalContext.current
    val absolute = settings.timeDisplay == TimeDisplay.ABSOLUTE
    val absoluteTime = if (absolute) remember(tracker.lastDoneAt) { absoluteTime(context, tracker.lastDoneAt) } else null
    // Relative time: hidden at once on a reset, then fades in when the first second is shown.
    val hidden = elapsed.isEmpty && !absolute
    val textAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = if (hidden) snap() else tween(durationMillis = 700),
        label = "elapsedAlpha",
    )

    // The share of the undo window left, drained by the ring around the corner bubble.
    val undoing = enabled && undoUntil != null
    val undoLeft = remember { Animatable(0f) }
    LaunchedEffect(undoUntil) {
        if (undoUntil == null) {
            undoLeft.snapTo(0f)
        } else {
            val remaining = (undoUntil - SystemClock.elapsedRealtime()).coerceIn(0L, UNDO_WINDOW_MS)
            undoLeft.snapTo(remaining.toFloat() / UNDO_WINDOW_MS)
            undoLeft.animateTo(0f, tween(durationMillis = remaining.toInt(), easing = LinearEasing))
        }
    }

    // Small translucent surfaces on top of the tile (icon chip, reset bubble, counter).
    val glass = when {
        hasPhoto -> Color.White.copy(alpha = 0.2f)
        palette.isDark -> Color.White.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val glassBorder = if (hasPhoto) Color.White.copy(alpha = 0.35f) else Color.Transparent
    val textShadow = if (hasPhoto) PhotoTextShadow else null

    val longClickLabel = stringResource(R.string.tile_long_click_label)
    val clickLabel = if (undoing) stringResource(R.string.action_undo) else onClickLabel
    val undoHint = stringResource(R.string.tile_undo_hint)
    val resources = LocalResources.current
    val description = listOfNotNull(
        tracker.name,
        if (elapsed.isEmpty) stringResource(R.string.elapsed_just_now) else stringResource(R.string.elapsed_ago, elapsed.format(resources)),
        pluralStringResource(R.plurals.press_count, tracker.pressCount, tracker.pressCount).takeIf { settings.showCounter },
        tracker.reminder?.let {
            if (it.auto) stringResource(R.string.tile_reminder_auto_description)
            else stringResource(R.string.tile_reminder_description, reminderDelay(context, it))
        },
        undoHint.takeIf { undoing },
    ).joinToString(", ")

    Box(
        modifier
            .graphicsLayer { val s = 1f - 0.03f * press(); scaleX = s; scaleY = s }
            .raised(TileShape, colors.shadow, palette.highlight, pressed = press)
            .clip(TileShape)
            .background(colors.background)
            .then(
                if (enabled) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = clickLabel,
                        onLongClickLabel = longClickLabel.takeIf { onLongClick != null },
                        onClick = {
                            if (settings.haptics) {
                                haptics.performHapticFeedback(if (undoing) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
                            }
                            onClick()
                        },
                        onLongClick = onLongClick?.let {
                            {
                                if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                it()
                            }
                        },
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        if (tracker.photo != null) {
            AsyncImage(
                model = photoFile(tracker.photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(PhotoScrim))
        }
        // Inset shadows go above the photo so a photo tile sinks in too.
        Box(Modifier.fillMaxSize().pressedIn(TileShape, colors.shadow, palette.highlight, amount = press))

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The chip keeps its place while the icon catalog loads, so the tile doesn't shift.
                val catalog = rememberIconCatalog()
                val icon = rememberTileIcon(tracker.icon)
                if (!tracker.icon.isNone && (icon != null || catalog == null)) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(glass)
                            .border(1.dp, glassBorder, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (icon != null) Icon(icon, null, tint = colors.content, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                // Only while a tap can be undone: the undo arrow, ringed by the time left to do it.
                AnimatedVisibility(undoing, enter = fadeIn() + scaleIn(initialScale = 0.6f), exit = fadeOut() + scaleOut(targetScale = 0.6f)) {
                    Box(
                        Modifier
                            .padding(end = if (tracker.reminder != null) 6.dp else 0.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(glass)
                            .border(1.dp, glassBorder, CircleShape)
                            .drawWithContent {
                                drawContent()
                                val left = undoLeft.value
                                if (left > 0f) {
                                    val stroke = 2.dp.toPx()
                                    drawArc(
                                        color = colors.content,
                                        startAngle = -90f,
                                        sweepAngle = 360f * left,
                                        useCenter = false,
                                        topLeft = Offset(stroke / 2, stroke / 2),
                                        size = Size(size.width - stroke, size.height - stroke),
                                        style = Stroke(stroke, cap = StrokeCap.Round),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Undo, null, tint = colors.content, modifier = Modifier.size(16.dp))
                    }
                }
                // A reminder is set: the bell, in the corner.
                if (tracker.reminder != null) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(glass)
                            .border(1.dp, glassBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Bell, null, tint = colors.content, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            val ago = agoAffixes(resources)
            val headline = absoluteTime?.let { AnnotatedString(it.headline) } ?: relativeHeadline(elapsed, ago.prefix)
            TileTexts(tracker, headline, { textAlpha }, colors.content, textShadow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        undoing -> undoHint
                        absoluteTime != null -> absoluteTime.detail
                        elapsed.isEmpty -> ""
                        // "5 hours ago" or "et 5 heures": the next unit, then the end of "… ago" (if the language puts it after).
                        else -> (elapsed.minor?.formatAsSecond(resources).orEmpty() + ago.suffix).trim()
                    },
                    style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, shadow = textShadow),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // The undo hint shrinks to fit a small tile rather than lose its end.
                    autoSize = if (undoing) TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.5.sp) else null,
                    // The undo hint shows at once, while the elapsed time is still hidden.
                    modifier = Modifier.weight(1f).graphicsLayer { alpha = if (undoing) 1f else textAlpha },
                )
                // How many times the tile was pressed (since created, or since its counter was reset).
                if (settings.showCounter) Text(
                    text = NumberFormat.getIntegerInstance().format(tracker.pressCount),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp),
                    color = colors.content,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(glass)
                        .border(1.dp, glassBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/**
 * The headline unit of the elapsed time ("2 minutes"); the next unit goes on the bottom row.
 * Languages that put "ago" first ("il y a 2 minutes") get it before the headline, in smaller type.
 */
@Composable
private fun relativeHeadline(elapsed: Elapsed, agoPrefix: String): AnnotatedString {
    val major = elapsed.major?.format(LocalResources.current)
    return when {
        // Keeps its line while hidden, so the tile doesn't jump when the text fades in.
        major == null -> AnnotatedString(" ")
        agoPrefix.isBlank() -> AnnotatedString(major.replaceFirstChar { it.uppercase() })
        else -> buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 0.55.em, fontWeight = FontWeight.Bold, letterSpacing = 0.em)) {
                append(agoPrefix.replaceFirstChar { it.uppercase() })
            }
            append(major)
        }
    }
}

/** Name and the big time text: how long ago, or the day it was done. */
@Composable
private fun TileTexts(tracker: Tracker, headline: AnnotatedString, textAlpha: () -> Float, color: Color, shadow: TextShadow?) {
    val headlineSize = when (tracker.size) {
        TileSize.TALL -> 44.sp
        TileSize.WIDE -> 32.sp
        TileSize.SMALL -> 26.sp
    }
    // Grows with the tile, like the headline; two lines of it still fit a small tile under its icon.
    val nameSize = when (tracker.size) {
        TileSize.TALL -> 20.sp
        TileSize.WIDE -> 17.sp
        TileSize.SMALL -> 16.sp
    }
    Text(
        text = tracker.name,
        style = TextStyle(
            fontSize = nameSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.01).em,
            lineHeight = nameSize * 1.15f,
            shadow = shadow,
        ),
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = headline,
        style = TextStyle(
            fontSize = headlineSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.03).em,
            lineHeight = headlineSize * 1.08f,
            shadow = shadow,
        ),
        color = color,
        maxLines = 1,
        // Long values ("59 minutes", "364 days") shrink to fit instead of being cut off.
        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = headlineSize),
        modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = textAlpha() },
    )
}
