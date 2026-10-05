package com.keeptrack.lasttime.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.keeptrack.lasttime.R
import com.keeptrack.lasttime.data.TileSize
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.ui.components.rememberPressAmount
import com.keeptrack.lasttime.ui.theme.AppIcons
import com.keeptrack.lasttime.ui.theme.LastTimeTheme
import com.keeptrack.lasttime.ui.theme.pressedIn
import com.keeptrack.lasttime.ui.theme.raised
import com.keeptrack.lasttime.ui.time.Elapsed
import com.keeptrack.lasttime.ui.time.RelativeTime
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
 * With [enabled] false it is a static preview.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TileCard(
    tracker: Tracker,
    now: Instant,
    photoFile: (String) -> File,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LastTimeTheme.palette
    val hasPhoto = tracker.photo != null
    val colors = if (hasPhoto) palette.photoTile else palette.tile(tracker.color)
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressAmount(interaction)
    val haptics = LocalHapticFeedback.current
    val elapsed = RelativeTime.split(tracker.lastDoneAt, now)
    val justDone = Duration.between(tracker.lastDoneAt, now) < Duration.ofMinutes(1)

    // Small translucent surfaces on top of the tile (icon chip, date chip, reset bubble).
    val glass = when {
        hasPhoto -> Color.White.copy(alpha = 0.2f)
        palette.isDark -> Color.White.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val glassBorder = if (hasPhoto) Color.White.copy(alpha = 0.35f) else Color.Transparent
    val textShadow = if (hasPhoto) PhotoTextShadow else null

    val clickLabel = stringResource(R.string.tile_click_label)
    val longClickLabel = stringResource(R.string.tile_long_click_label)
    val description = "${tracker.name}, ${elapsed.text}"

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
                        onLongClickLabel = longClickLabel,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onClick()
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(glass)
                        .border(1.dp, glassBorder, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.tile(tracker.icon), null, tint = colors.content, modifier = Modifier.size(20.dp))
                }
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (justDone) colors.content else glass)
                        .border(1.dp, if (justDone) Color.Transparent else glassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (justDone) AppIcons.Check else AppIcons.Refresh,
                        null,
                        tint = if (justDone) colors.background else colors.content,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            TileTexts(tracker, elapsed, colors.content, textShadow)
            if (tracker.size == TileSize.TALL) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatDate(tracker.lastDoneAt),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.content,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(glass)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun TileTexts(tracker: Tracker, elapsed: Elapsed, color: Color, shadow: TextShadow?) {
    val headlineSize = when (tracker.size) {
        TileSize.TALL -> 44.sp
        TileSize.WIDE -> 32.sp
        TileSize.SMALL -> 26.sp
    }
    Text(
        text = tracker.name,
        style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp, shadow = shadow),
        color = color,
        maxLines = if (tracker.size == TileSize.TALL) 3 else 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = elapsed.major.replaceFirstChar { it.uppercase() },
        style = TextStyle(
            fontSize = headlineSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.03).em,
            lineHeight = headlineSize * 1.08f,
            shadow = shadow,
        ),
        color = color,
        maxLines = 1,
        // Long values ("Just now", "11 months") shrink to fit instead of being cut off.
        autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = headlineSize),
        modifier = Modifier.padding(top = 4.dp),
    )
    if (elapsed.rest.isNotEmpty()) {
        Text(
            text = elapsed.rest,
            style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, shadow = shadow),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val dateFormatter: DateTimeFormatter by lazy {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMMjmm")
    DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
}

private fun formatDate(instant: Instant): String = dateFormatter.format(instant)
