package dev.cfaz.timeclicker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TileColor
import dev.cfaz.timeclicker.data.TileIcon
import dev.cfaz.timeclicker.ui.icons.IconChooserSheet
import dev.cfaz.timeclicker.ui.theme.AppIcons
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.rememberTileIcon

/**
 * Creates or edits a group: its name, and optionally an icon, chosen from the icon list by the button in the field.
 * Without choosing one, the group has no icon.
 */
@Composable
fun GroupDialog(
    title: String,
    initialName: String,
    initialIcon: TileIcon,
    onConfirm: (name: String, icon: TileIcon) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(initialName.length)))
    }
    var iconKey by rememberSaveable { mutableStateOf(initialIcon.key) }
    val icon = TileIcon(iconKey)
    var choosingIcon by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val canSave = value.text.isNotBlank()
    val save = { if (canSave) onConfirm(value.text, icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    trailingIcon = {
                        val vector = rememberTileIcon(icon)
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.field)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.icon_chooser_title),
                                ) { choosingIcon = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(vector ?: AppIcons.NoIcon, null, tint = palette.text, modifier = Modifier.size(22.dp))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                if (!icon.isNone) {
                    TextButton(onClick = { iconKey = TileIcon.NONE.key }) { Text(stringResource(R.string.group_icon_remove)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = canSave) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
    if (choosingIcon) {
        IconChooserSheet(
            selected = icon,
            accent = palette.tile(TileColor.SAGE),
            onSelect = { iconKey = it.key },
            onDismiss = { choosingIcon = false },
        )
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** Asks before something destructive, e.g. deleting a tile or a group. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
