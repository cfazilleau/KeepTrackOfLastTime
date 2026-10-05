package com.keeptrack.lasttime.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.keeptrack.lasttime.R
import com.keeptrack.lasttime.data.TileColor
import com.keeptrack.lasttime.data.TileIcon
import com.keeptrack.lasttime.data.TileSize
import com.keeptrack.lasttime.data.TileSpec
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.data.TrackerGroup
import com.keeptrack.lasttime.ui.components.NeuButton
import com.keeptrack.lasttime.ui.components.NeuTextField
import com.keeptrack.lasttime.ui.components.PillButton
import com.keeptrack.lasttime.ui.components.SegmentedControl
import com.keeptrack.lasttime.ui.theme.AppIcons
import com.keeptrack.lasttime.ui.theme.LastTimeTheme
import com.keeptrack.lasttime.ui.theme.TileColors
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

/** The tile being created ([trackerId] null) or edited in the sheet. */
data class TileDraft(
    val trackerId: Long?,
    val name: String,
    val groupId: Long?,
    val color: TileColor,
    val icon: TileIcon,
    val size: TileSize,
    val photo: String?,
) {
    fun toSpec() = TileSpec(name.trim(), groupId, color, icon, size, photo)

    companion object {
        fun of(tracker: Tracker) = TileDraft(
            tracker.id, tracker.name, tracker.groupId, tracker.color, tracker.icon, tracker.size, tracker.photo,
        )
    }
}

private enum class Background { COLOUR, PHOTO }

/**
 * Bottom sheet to create or edit a tile. Edits are kept when the sheet is swiped away;
 * a new tile is only created with the Add button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTileSheet(
    initial: TileDraft,
    lastDoneAt: Instant?,
    now: Instant,
    groups: List<TrackerGroup>,
    photoFile: (String) -> File,
    importPhoto: suspend (Uri) -> String?,
    createGroup: suspend (String) -> Long,
    onSave: (TileDraft) -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LastTimeTheme.palette
    val isNew = initial.trackerId == null
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember { mutableStateOf(initial) }
    // Remembered so switching to Colour and back to Photo restores the picked photo.
    var keptPhoto by remember { mutableStateOf(initial.photo) }
    var photoError by remember { mutableStateOf(false) }
    var newGroupDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = draft.name.isNotBlank()
    fun close(save: Boolean) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (save && canSave) onSave(draft) else onDiscard()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = importPhoto(uri)
                photoError = name == null
                if (name != null) {
                    keptPhoto = name
                    draft = draft.copy(photo = name)
                }
            }
        }
    }
    fun launchPicker() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    ModalBottomSheet(
        onDismissRequest = { if (isNew || !canSave) onDiscard() else onSave(draft) },
        sheetState = sheetState,
        containerColor = palette.sheet,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (isNew) R.string.sheet_new_title else R.string.sheet_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                if (isNew) {
                    TextButton(onClick = { close(save = false) }) {
                        Text(stringResource(R.string.action_cancel), color = palette.muted)
                    }
                }
                PillButton(
                    text = stringResource(if (isNew) R.string.action_add else R.string.action_done),
                    enabled = canSave,
                    onClick = { close(save = true) },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val previewName = draft.name.ifBlank { stringResource(R.string.tile_preview_name) }
                TileCard(
                    tracker = Tracker(
                        id = draft.trackerId ?: -1,
                        name = previewName,
                        lastDoneAt = lastDoneAt ?: now,
                        groupId = draft.groupId,
                        color = draft.color,
                        icon = draft.icon,
                        size = TileSize.SMALL,
                        photo = draft.photo,
                    ),
                    now = now,
                    photoFile = photoFile,
                    onClick = {},
                    onLongClick = {},
                    enabled = false,
                    modifier = Modifier.size(width = 150.dp, height = 156.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.label_name))
                    NeuTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        label = stringResource(R.string.label_name),
                        placeholder = stringResource(R.string.name_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.label_group))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionPill(stringResource(R.string.group_none), draft.groupId == null) { draft = draft.copy(groupId = null) }
                    groups.forEach { group ->
                        OptionPill(group.name, draft.groupId == group.id) { draft = draft.copy(groupId = group.id) }
                    }
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(BorderStroke(1.5.dp, palette.muted), RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button) { newGroupDialog = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(AppIcons.Add, null, tint = palette.text, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.group_new), style = MaterialTheme.typography.titleSmall, color = palette.text)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.label_background))
                val background = if (draft.photo != null) Background.PHOTO else Background.COLOUR
                SegmentedControl(
                    options = Background.entries,
                    selected = background,
                    label = {
                        stringResource(if (it == Background.PHOTO) R.string.background_photo else R.string.background_colour)
                    },
                    onSelect = {
                        when {
                            it == Background.COLOUR -> draft = draft.copy(photo = null)
                            keptPhoto != null -> draft = draft.copy(photo = keptPhoto)
                            else -> launchPicker()
                        }
                    },
                )
                if (background == Background.PHOTO && draft.photo != null) {
                    PhotoRow(
                        file = photoFile(draft.photo!!),
                        onChange = ::launchPicker,
                        onRemove = { keptPhoto = null; draft = draft.copy(photo = null) },
                    )
                } else {
                    ColourSwatches(selected = draft.color, onSelect = { draft = draft.copy(color = it) })
                }
                if (photoError) {
                    Text(stringResource(R.string.photo_error), style = MaterialTheme.typography.bodySmall, color = palette.danger)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.label_icon))
                IconPicker(selected = draft.icon, accent = palette.tile(draft.color), onSelect = { draft = draft.copy(icon = it) })
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.label_size))
                SegmentedControl(
                    options = TileSize.entries,
                    selected = draft.size,
                    label = {
                        stringResource(
                            when (it) {
                                TileSize.SMALL -> R.string.size_small
                                TileSize.WIDE -> R.string.size_wide
                                TileSize.TALL -> R.string.size_tall
                            }
                        )
                    },
                    onSelect = { draft = draft.copy(size = it) },
                )
            }

            if (!isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_tile), style = MaterialTheme.typography.titleMedium, color = palette.danger)
                }
            }
        }
    }

    if (newGroupDialog) {
        NameDialog(
            title = stringResource(R.string.dialog_new_group_title),
            initialName = "",
            onConfirm = { name ->
                newGroupDialog = false
                scope.launch { draft = draft.copy(groupId = createGroup(name)) }
            },
            onDismiss = { newGroupDialog = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_title, initial.name),
            body = stringResource(R.string.dialog_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete() }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = LastTimeTheme.palette.muted)
}

@Composable
private fun OptionPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LastTimeTheme.palette
    val shape = RoundedCornerShape(999.dp)
    val content: @Composable () -> Unit = {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) palette.ground else palette.text,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
    if (selected) {
        Box(
            Modifier
                .height(40.dp)
                .clip(shape)
                .background(palette.text)
                .selectable(selected = true, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    } else {
        NeuButton(
            onClick = onClick,
            shape = shape,
            background = palette.sheet,
            distance = 4.dp,
            blur = 10.dp,
            modifier = Modifier.height(40.dp),
        ) { content() }
    }
}

@Composable
private fun PhotoRow(file: File, onChange: () -> Unit, onRemove: () -> Unit) {
    val palette = LastTimeTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NeuButton(
                    onClick = onChange,
                    shape = RoundedCornerShape(12.dp),
                    background = palette.sheet,
                    distance = 4.dp,
                    blur = 10.dp,
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(
                        stringResource(R.string.photo_change),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.photo_remove), style = MaterialTheme.typography.titleSmall, color = palette.muted)
                }
            }
            Text(stringResource(R.string.photo_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}

@Composable
private fun ColourSwatches(selected: TileColor, onSelect: (TileColor) -> Unit) {
    val palette = LastTimeTheme.palette
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TileColor.entries.forEach { color ->
            val colors = palette.tile(color)
            val isSelected = color == selected
            val name = stringResource(colorName(color))
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(2.dp, if (isSelected) colors.content else Color.Transparent, RoundedCornerShape(16.dp))
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
                    .semantics { contentDescription = name },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: TileIcon, accent: TileColors, onSelect: (TileIcon) -> Unit) {
    val palette = LastTimeTheme.palette
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 6,
    ) {
        TileIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            val description = stringResource(R.string.icon_choice, icon.key)
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) accent.background else palette.field)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(icon) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.tile(icon), null, tint = if (isSelected) accent.content else palette.text, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun colorName(color: TileColor) = when (color) {
    TileColor.SAGE -> R.string.color_sage
    TileColor.LAVENDER -> R.string.color_lavender
    TileColor.PEACH -> R.string.color_peach
    TileColor.SKY -> R.string.color_sky
    TileColor.BUTTER -> R.string.color_butter
    TileColor.ROSE -> R.string.color_rose
}
