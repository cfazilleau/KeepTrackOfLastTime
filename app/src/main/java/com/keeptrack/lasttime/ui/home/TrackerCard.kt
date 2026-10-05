package com.keeptrack.lasttime.ui.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.keeptrack.lasttime.R
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.ui.theme.LastTimeTheme
import com.keeptrack.lasttime.ui.time.RelativeTime
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val absoluteFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())

@Composable
fun TrackerCard(
    tracker: Tracker,
    now: Instant,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(modifier) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            // Clickable sits inside the card so the ripple is clipped to the card shape.
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .combinedClickable(
                        onClickLabel = stringResource(R.string.card_hint),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onClick()
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                    )
                    .padding(16.dp),
            ) {
                Text(
                    text = tracker.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = RelativeTime.format(tracker.lastDoneAt, now),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = absoluteFormatter.format(tracker.lastDoneAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = { menuOpen = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

@Preview
@Composable
private fun TrackerCardPreview() {
    val now = Instant.now()
    LastTimeTheme {
        TrackerCard(
            tracker = Tracker(1, "Watered the plants", now - Duration.ofHours(77)),
            now = now,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}
