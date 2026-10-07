package dev.cfaz.timeclicker.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.PluralsRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.Reminder
import dev.cfaz.timeclicker.data.ReminderUnit
import dev.cfaz.timeclicker.data.Rhythm
import dev.cfaz.timeclicker.reminder.Reminders
import dev.cfaz.timeclicker.ui.components.NeuButton
import dev.cfaz.timeclicker.ui.components.SegmentedControl
import dev.cfaz.timeclicker.ui.theme.AppIcons
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.time.RelativeTime
import dev.cfaz.timeclicker.ui.time.format

/**
 * The edit sheet's reminder: an "Add a reminder" button, or when it comes: automatically, when the tile is late on
 * its usual pace ([rhythm]), or after how long without being done (N hours, days or weeks). A new reminder is
 * automatic when the tile already has a pace. Notifications are asked for when a reminder is added (Android 13+).
 */
@Composable
internal fun ReminderSection(reminder: Reminder?, rhythm: Rhythm?, onChange: (Reminder?) -> Unit) {
    val palette = TimeClickerTheme.palette
    val context = LocalContext.current
    // Checked again on return from Android settings, where they may have been turned on.
    var canNotify by remember { mutableStateOf(Reminders.canNotify(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = Reminders.canNotify(context) }
    var askedPermission by remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedPermission = true
        canNotify = Reminders.canNotify(context)
    }
    fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedPermission) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Refused already (Android won't ask twice), or turned off: only the settings can turn them on.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }

    if (reminder == null) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.field)
                .clickable(role = Role.Button) {
                    onChange(if (rhythm != null) Reminder.AUTOMATIC else Reminder.DEFAULT)
                    if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) allowNotifications()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(AppIcons.Bell, null, tint = palette.text, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_add), style = MaterialTheme.typography.titleMedium, color = palette.text)
                Text(stringResource(R.string.reminder_add_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.label_reminder))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onChange(null) }) {
                    Text(stringResource(R.string.reminder_remove), style = MaterialTheme.typography.titleSmall, color = palette.muted)
                }
            }
        }
        SegmentedControl(
            options = listOf(true, false),
            selected = reminder.auto,
            label = { auto -> stringResource(if (auto) R.string.reminder_mode_auto else R.string.reminder_mode_custom) },
            onSelect = { auto ->
                onChange(
                    when {
                        auto -> reminder.copy(auto = true)
                        // A custom reminder starts from when the automatic one would come.
                        reminder == Reminder.AUTOMATIC && rhythm != null -> Reminder.near(rhythm.typical + rhythm.slack)
                        else -> reminder.copy(auto = false)
                    }
                )
            },
        )
        if (reminder.auto) {
            val resources = LocalResources.current
            Text(
                if (rhythm != null) {
                    stringResource(R.string.reminder_auto_hint, RelativeTime.approximate(rhythm.typical).format(resources))
                } else {
                    stringResource(R.string.reminder_auto_learning)
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        } else {
            CustomReminder(reminder, onChange)
        }
        if (!canNotify) {
            Text(
                stringResource(R.string.reminder_notifications_off),
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = ::allowNotifications),
            )
        }
    }
}

/** After how long without being done the reminder comes: N hours, days or weeks. */
@Composable
private fun CustomReminder(reminder: Reminder, onChange: (Reminder?) -> Unit) {
    val palette = TimeClickerTheme.palette
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.reminder_after),
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            StepButton(AppIcons.Remove, stringResource(R.string.reminder_less), enabled = reminder.every > 1) {
                onChange(reminder.copy(every = reminder.every - 1))
            }
            Text(
                reminder.every.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(52.dp),
            )
            StepButton(AppIcons.Add, stringResource(R.string.reminder_more), enabled = reminder.every < Reminder.MAX_EVERY) {
                onChange(reminder.copy(every = reminder.every + 1))
            }
        }
        SegmentedControl(
            options = ReminderUnit.entries,
            selected = reminder.unit,
            label = {
                stringResource(
                    when (it) {
                        ReminderUnit.HOURS -> R.string.reminder_unit_hours
                        ReminderUnit.DAYS -> R.string.reminder_unit_days
                        ReminderUnit.WEEKS -> R.string.reminder_unit_weeks
                    }
                )
            },
            onSelect = { onChange(reminder.copy(unit = it)) },
        )
        Text(
            stringResource(R.string.reminder_hint, reminderDelay(context, reminder)),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = TimeClickerTheme.palette
    NeuButton(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(12.dp),
        background = palette.sheet,
        distance = 4.dp,
        blur = 10.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(icon, description, tint = if (enabled) palette.text else palette.muted, modifier = Modifier.size(18.dp))
    }
}

/** "3 days", "1 week": a custom reminder's delay. */
fun reminderDelay(context: Context, reminder: Reminder): String =
    context.resources.getQuantityString(reminder.unit.plural(), reminder.every, reminder.every)

@PluralsRes
private fun ReminderUnit.plural(): Int = when (this) {
    ReminderUnit.HOURS -> R.plurals.elapsed_hours
    ReminderUnit.DAYS -> R.plurals.elapsed_days
    ReminderUnit.WEEKS -> R.plurals.reminder_weeks
}
