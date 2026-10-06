package com.keeptrack.timeclicker.ui.home

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.Reminder
import com.keeptrack.timeclicker.data.ReminderUnit
import com.keeptrack.timeclicker.reminder.Reminders
import com.keeptrack.timeclicker.ui.components.NeuButton
import com.keeptrack.timeclicker.ui.components.SegmentedControl
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme

/**
 * The edit sheet's reminder: an "Add a reminder" button, or after how long without being done (N hours, days or
 * weeks) it comes. Notifications are asked for when a reminder is added (Android 13+).
 */
@Composable
internal fun ReminderSection(reminder: Reminder?, onChange: (Reminder?) -> Unit) {
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
                    onChange(Reminder.DEFAULT)
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

/** "3 days", "1 week". */
fun reminderDelay(context: Context, reminder: Reminder): String =
    context.resources.getQuantityString(reminder.unit.plural(), reminder.every, reminder.every)

@PluralsRes
private fun ReminderUnit.plural(): Int = when (this) {
    ReminderUnit.HOURS -> R.plurals.elapsed_hours
    ReminderUnit.DAYS -> R.plurals.elapsed_days
    ReminderUnit.WEEKS -> R.plurals.reminder_weeks
}
