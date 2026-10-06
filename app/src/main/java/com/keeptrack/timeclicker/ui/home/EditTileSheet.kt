package com.keeptrack.timeclicker.ui.home

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.TileColor
import com.keeptrack.timeclicker.data.TileIcon
import com.keeptrack.timeclicker.data.TileSize
import com.keeptrack.timeclicker.data.TileSpec
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.data.TrackerGroup
import com.keeptrack.timeclicker.ui.components.NeuButton
import com.keeptrack.timeclicker.ui.components.NeuTextField
import com.keeptrack.timeclicker.ui.components.PillButton
import com.keeptrack.timeclicker.ui.components.SegmentedControl
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.theme.TileColors
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
    /** Restart the press counter at 0 when saved. */
    val resetCount: Boolean = false,
) {
    fun toSpec() = TileSpec(name.trim(), groupId, color, icon, size, photo)

    companion object {
        fun of(tracker: Tracker) = TileDraft(
            tracker.id, tracker.name, tracker.groupId, tracker.color, tracker.icon, tracker.size, tracker.photo,
        )
    }
}

/**
 * Keeps an upward fling that reaches the end of the sheet's content from reaching the sheet.
 * The sheet is already at its top anchor and would "settle" there with that velocity,
 * overshooting and springing back on every fling: the sheet bounced while scrolling down.
 * Pulling down at the top still reaches the sheet, to drag it closed.
 */
private val KeepUpwardFlingInContent = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0) available.copy(x = 0f) else Velocity.Zero
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
    /** The saved tile being edited, null for a new one. */
    saved: Tracker?,
    groups: List<TrackerGroup>,
    photoFile: (String) -> File,
    importPhoto: suspend (Uri) -> String?,
    createGroup: suspend (String) -> Long,
    onSave: (TileDraft) -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
    onAddWidget: (() -> Unit)?,
) {
    val palette = TimeClickerTheme.palette
    val isNew = initial.trackerId == null
    val openedAt = remember { Instant.now() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draft by remember { mutableStateOf(initial) }
    // Remembered so switching to Colour and back to Photo restores the picked photo.
    var keptPhoto by remember { mutableStateOf(initial.photo) }
    var photoError by remember { mutableStateOf(false) }
    var newGroupDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmResetCount by remember { mutableStateOf(false) }

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
                .nestedScroll(KeepUpwardFlingInContent)
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
                        lastDoneAt = saved?.lastDoneAt ?: openedAt,
                        groupId = draft.groupId,
                        color = draft.color,
                        icon = draft.icon,
                        size = TileSize.SMALL,
                        photo = draft.photo,
                        pressCount = if (draft.resetCount) 0 else saved?.pressCount ?: 0,
                    ),
                    photoFile = photoFile,
                    onClick = {},
                    onLongClick = null,
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

            if (saved != null) {
                val count = if (draft.resetCount) 0 else saved.pressCount
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.label_counter))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pluralStringResource(R.plurals.press_count, count, count),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.text,
                            modifier = Modifier.weight(1f),
                        )
                        if (count > 0) {
                            SheetButton(stringResource(R.string.counter_reset), onClick = { confirmResetCount = true })
                        }
                    }
                }
            }

            if (saved != null && onAddWidget != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.field)
                        .clickable(role = Role.Button, onClick = onAddWidget)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Widget, null, tint = palette.text, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.widget_add), style = MaterialTheme.typography.titleMedium, color = palette.text)
                        Text(stringResource(R.string.widget_add_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
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
    if (confirmResetCount) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_reset_counter_title),
            body = stringResource(R.string.dialog_reset_counter_body),
            confirmLabel = stringResource(R.string.counter_reset),
            onConfirm = {
                confirmResetCount = false
                draft = draft.copy(resetCount = true)
            },
            onDismiss = { confirmResetCount = false },
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
    Text(text, style = MaterialTheme.typography.labelMedium, color = TimeClickerTheme.palette.muted)
}

@Composable
private fun OptionPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = TimeClickerTheme.palette
    val shape = RoundedCornerShape(999.dp)
    val content: @Composable () -> Unit = {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) palette.onAccent else palette.text,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
    if (selected) {
        Box(
            Modifier
                .height(40.dp)
                .clip(shape)
                .background(palette.accent)
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
    val palette = TimeClickerTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SheetButton(stringResource(R.string.photo_change), onClick = onChange)
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.photo_remove), style = MaterialTheme.typography.titleSmall, color = palette.muted)
                }
            }
            Text(stringResource(R.string.photo_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
    }
}

/** A small raised button on the sheet. */
@Composable
private fun SheetButton(text: String, onClick: () -> Unit) {
    val palette = TimeClickerTheme.palette
    NeuButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        background = palette.sheet,
        distance = 4.dp,
        blur = 10.dp,
        modifier = Modifier.height(40.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = palette.text, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

/** The six pastel colours. */
@Composable
private fun ColourSwatches(selected: TileColor, onSelect: (TileColor) -> Unit) {
    val colors = TileColor.pickable
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val swatchWidth = (maxWidth - gap * (colors.size - 1)) / colors.size
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            colors.forEach { Swatch(it, it == selected, swatchWidth, onSelect) }
        }
    }
}

@Composable
private fun Swatch(color: TileColor, isSelected: Boolean, width: Dp, onSelect: (TileColor) -> Unit) {
    val colors = TimeClickerTheme.palette.tile(color)
    val name = stringResource(colorName(color))
    Box(
        Modifier
            .width(width)
            .height(48.dp)
            .border(2.dp, if (isSelected) colors.content else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
            .semantics { contentDescription = name },
    )
}

/** "No icon", then every icon; six per row, the last row aligned to the start. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: TileIcon, accent: TileColors, onSelect: (TileIcon) -> Unit) {
    val palette = TimeClickerTheme.palette
    val columns = 6
    val icons = TileIcon.entries
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = columns,
    ) {
        icons.forEach { icon ->
            val isSelected = icon == selected
            val description = stringResource(R.string.icon_choice, stringResource(iconName(icon)))
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
                val tint = if (isSelected) accent.content else palette.text
                val vector = AppIcons.tile(icon)
                if (vector != null) {
                    Icon(vector, null, tint = tint, modifier = Modifier.size(22.dp))
                } else {
                    Icon(AppIcons.NoIcon, null, tint = tint.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                }
            }
        }
        // Empty cells keep the last row's icons the same width as the others.
        repeat((columns - icons.size % columns) % columns) { Spacer(Modifier.weight(1f)) }
    }
}

private fun colorName(color: TileColor) = when (color) {
    TileColor.SAGE -> R.string.color_sage
    TileColor.LAVENDER -> R.string.color_lavender
    TileColor.PEACH -> R.string.color_peach
    TileColor.SKY -> R.string.color_sky
    TileColor.BUTTER -> R.string.color_butter
    TileColor.ROSE -> R.string.color_rose
    TileColor.PRIMARY -> R.string.color_wallpaper_primary
    TileColor.SECONDARY -> R.string.color_wallpaper_secondary
    TileColor.TERTIARY -> R.string.color_wallpaper_tertiary
}

private fun iconName(icon: TileIcon) = when (icon) {
    TileIcon.NONE -> R.string.icon_none
    TileIcon.CHECK -> R.string.icon_check
    TileIcon.DROP -> R.string.icon_drop
    TileIcon.LEAF -> R.string.icon_leaf
    TileIcon.GRASS -> R.string.icon_grass
    TileIcon.FLOWER -> R.string.icon_flower
    TileIcon.BED -> R.string.icon_bed
    TileIcon.COFFEE -> R.string.icon_coffee
    TileIcon.SNOW -> R.string.icon_snow
    TileIcon.TRASH -> R.string.icon_trash
    TileIcon.CART -> R.string.icon_cart
    TileIcon.PHONE -> R.string.icon_phone
    TileIcon.HEART -> R.string.icon_heart
    TileIcon.PILL -> R.string.icon_pill
    TileIcon.PAW -> R.string.icon_paw
    TileIcon.SCISSORS -> R.string.icon_scissors
    TileIcon.BRUSH -> R.string.icon_brush
    TileIcon.GAUGE -> R.string.icon_gauge
    TileIcon.CAR -> R.string.icon_car
}
