package com.keeptrack.timeclicker.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.data.TrackerGroup
import com.keeptrack.timeclicker.ui.components.GutteredColumn
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.components.NeuTextField
import com.keeptrack.timeclicker.ui.components.PillButton
import com.keeptrack.timeclicker.ui.home.ConfirmDialog
import com.keeptrack.timeclicker.ui.home.NameDialog
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.theme.pressedIn
import com.keeptrack.timeclicker.ui.theme.raised
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory),
) {
    val palette = TimeClickerTheme.palette
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    // Local copy so dragging is instant; saved to the database when the drag ends.
    var ordered by remember(rows) { mutableStateOf(rows.orEmpty()) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = ordered.indexOfFirst { it.group.id == from.key }
        val toIndex = ordered.indexOfFirst { it.group.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            ordered = ordered.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    var newName by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<TrackerGroup?>(null) }
    var deleting by remember { mutableStateOf<TrackerGroup?>(null) }
    fun addGroup() {
        if (newName.isNotBlank()) {
            viewModel.add(newName)
            newName = ""
        }
    }

    GutteredColumn(
        Modifier
            .fillMaxSize()
            .background(palette.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) { gutter ->
        Row(
            Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NeuIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack, size = 44.dp, shape = RoundedCornerShape(15.dp))
            Text(stringResource(R.string.groups_title), style = MaterialTheme.typography.headlineSmall, color = palette.text)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "intro") {
                Text(
                    stringResource(if (ordered.isEmpty()) R.string.groups_empty else R.string.groups_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
            }
            items(ordered, key = { it.group.id }) { row ->
                ReorderableItem(reorderState, key = row.group.id) { isDragging ->
                    GroupRow(
                        row = row,
                        isDragging = isDragging,
                        photoFile = viewModel::photoFile,
                        dragHandle = Modifier.draggableHandle(
                            onDragStopped = { viewModel.reorder(ordered.map { it.group.id }) },
                        ),
                        onRename = { renaming = row.group },
                        onDelete = { deleting = row.group },
                    )
                }
            }
            item(key = "new") {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.group_new), style = MaterialTheme.typography.labelMedium, color = palette.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = stringResource(R.string.group_new),
                            placeholder = stringResource(R.string.groups_new_placeholder),
                            onDone = ::addGroup,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(stringResource(R.string.action_add), onClick = ::addGroup, enabled = newName.isNotBlank())
                    }
                }
            }
            item(key = "note") {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.field)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(AppIcons.Info, null, tint = palette.muted, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.groups_note), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
            }
        }
    }

    renaming?.let { group ->
        NameDialog(
            title = stringResource(R.string.dialog_rename_group_title),
            initialName = group.name,
            onConfirm = { viewModel.rename(group, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { group ->
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_group_title, group.name),
            body = stringResource(R.string.dialog_delete_group_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(group); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun GroupRow(
    row: GroupRowUi,
    isDragging: Boolean,
    photoFile: (String) -> File,
    dragHandle: Modifier,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    val shape = RoundedCornerShape(22.dp)
    var menuOpen by remember { mutableStateOf(false) }
    val dragLabel = stringResource(R.string.group_drag, row.group.name)

    Row(
        Modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.rotate(-1.2f) else Modifier)
            .raised(shape, palette.shadow, palette.highlight, distance = 7.dp, blur = 16.dp, pressed = { if (isDragging) 1f else 0f })
            .clip(shape)
            .background(if (isDragging) palette.field else palette.ground)
            .pressedIn(shape, palette.shadow, palette.highlight, distance = 5.dp, blur = 12.dp, amount = { if (isDragging) 1f else 0f })
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            dragHandle
                .size(44.dp)
                .semantics { contentDescription = dragLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.DragHandle, null, tint = if (isDragging) palette.text else palette.muted, modifier = Modifier.size(18.dp))
        }
        TileThumbnails(row.tiles, photoFile)
        Column(Modifier.weight(1f)) {
            Text(row.group.name, style = MaterialTheme.typography.titleMedium, color = palette.text)
            Text(
                pluralStringResource(R.plurals.tiles_count, row.tiles.size, row.tiles.size),
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(AppIcons.More, stringResource(R.string.group_options, row.group.name), tint = palette.text)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.action_rename)) }, onClick = { menuOpen = false; onRename() })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = palette.danger) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

/** Up to three overlapping mini tiles showing the group's colours and photos. */
@Composable
private fun TileThumbnails(tiles: List<Tracker>, photoFile: (String) -> File) {
    val palette = TimeClickerTheme.palette
    val shape = RoundedCornerShape(9.dp)
    Box(Modifier.size(width = 58.dp, height = 28.dp)) {
        tiles.take(3).forEachIndexed { index, tile ->
            val thumbModifier = Modifier
                .offset(x = (index * 16).dp)
                .size(28.dp)
                .border(2.dp, palette.ground, shape)
                .padding(2.dp)
                .clip(shape)
            if (tile.photo != null) {
                AsyncImage(
                    model = photoFile(tile.photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = thumbModifier,
                )
            } else {
                Box(thumbModifier.background(palette.tile(tile.color).background))
            }
        }
    }
}
