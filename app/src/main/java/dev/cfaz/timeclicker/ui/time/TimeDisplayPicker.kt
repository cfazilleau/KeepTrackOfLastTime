package dev.cfaz.timeclicker.ui.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TimeDisplay
import dev.cfaz.timeclicker.ui.components.SegmentedControl
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme

/** The Relative / Absolute choice, the same in the settings and a tile's edit sheet, with [info] under it. */
@Composable
fun TimeDisplayPicker(selected: TimeDisplay, onSelect: (TimeDisplay) -> Unit, info: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SegmentedControl(
            options = TimeDisplay.entries,
            selected = selected,
            label = {
                stringResource(if (it == TimeDisplay.RELATIVE) R.string.time_display_relative else R.string.time_display_absolute)
            },
            onSelect = onSelect,
        )
        if (info != null) {
            Text(info, style = MaterialTheme.typography.bodySmall, color = TimeClickerTheme.palette.muted)
        }
    }
}
