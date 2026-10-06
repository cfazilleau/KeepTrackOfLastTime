package com.keeptrack.timeclicker.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.TileSize
import com.keeptrack.timeclicker.data.TimeDisplay
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.ui.components.rememberPressAmount
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.theme.pressedIn
import com.keeptrack.timeclicker.ui.theme.raised
import com.keeptrack.timeclicker.ui.theme.rememberIconCatalog
import com.keeptrack.timeclicker.ui.theme.rememberTileIcon
import com.keeptrack.timeclicker.ui.time.Elapsed
import com.keeptrack.timeclicker.ui.time.RelativeTime
import com.keeptrack.timeclicker.ui.time.absoluteTime
import com.keeptrack.timeclicker.ui.time.agoAffixes
import com.keeptrack.timeclicker.ui.time.format
import com.keeptrack.timeclicker.ui.time.rememberNow
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

    // Small translucent surfaces on top of the tile (icon chip, reset bubble, counter).
    val glass = when {
        hasPhoto -> Color.White.copy(alpha = 0.2f)
        palette.isDark -> Color.White.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val glassBorder = if (hasPhoto) Color.White.copy(alpha = 0.35f) else Color.Transparent
    val textShadow = if (hasPhoto) PhotoTextShadow else null

    val longClickLabel = stringResource(R.string.tile_long_click_label)
    val resources = LocalResources.current
    val description = listOfNotNull(
        tracker.name,
        if (elapsed.isEmpty) stringResource(R.string.elapsed_just_now) else stringResource(R.string.elapsed_ago, elapsed.format(resources)),
        pluralStringResource(R.plurals.press_count, tracker.pressCount, tracker.pressCount).takeIf { settings.showCounter },
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
                        onClickLabel = onClickLabel,
                        onLongClickLabel = longClickLabel.takeIf { onLongClick != null },
                        onClick = {
                            if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(glass)
                        .border(1.dp, glassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Refresh, null, tint = colors.content, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            val ago = agoAffixes(resources)
            val headline = absoluteTime?.let { AnnotatedString(it.headline) } ?: relativeHeadline(elapsed, ago.prefix)
            TileTexts(tracker, headline, { textAlpha }, colors.content, textShadow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        absoluteTime != null -> absoluteTime.detail
                        elapsed.isEmpty -> ""
                        // "5 hours ago": the next unit, then the end of "… ago" (if the language puts it after).
                        else -> (elapsed.minor?.format(resources).orEmpty() + ago.suffix).trim()
                    },
                    style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, shadow = textShadow),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).graphicsLayer { alpha = textAlpha },
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
    Text(
        text = tracker.name,
        style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp, shadow = shadow),
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
