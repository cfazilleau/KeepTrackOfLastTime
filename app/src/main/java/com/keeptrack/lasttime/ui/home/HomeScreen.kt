package com.keeptrack.lasttime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeptrack.lasttime.R
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.ui.components.NeuButton
import com.keeptrack.lasttime.ui.components.NeuIconButton
import com.keeptrack.lasttime.ui.theme.AppIcons
import com.keeptrack.lasttime.ui.theme.LastTimeTheme
import com.keeptrack.lasttime.ui.time.RelativeTime
import com.keeptrack.lasttime.ui.time.rememberCurrentTime
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

private val TileHeight = 156.dp
private val TileSpacing = 16.dp

@Composable
fun HomeScreen(
    onManageGroups: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberCurrentTime()
    val palette = LastTimeTheme.palette
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // The tile open in the edit sheet, and when it was last done (for the preview).
    var editing by remember { mutableStateOf<Pair<TileDraft, Instant?>?>(null) }

    LaunchedEffect(viewModel) {
        // collectLatest: a newer reset replaces the snackbar of an older one.
        viewModel.resets.collectLatest { reset ->
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.snackbar_reset, reset.trackerName),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoReset(reset)
        }
    }

    Box(Modifier.fillMaxSize().background(palette.ground)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Header(
                state = state,
                onAdd = { scope.launch { editing = viewModel.newDraft() to null } },
            )
            state?.let { s ->
                FilterChips(chips = s.chips, onSelect = viewModel::select, onManageGroups = onManageGroups)
                if (s.trackerCount == 0) {
                    EmptyState(Modifier.weight(1f))
                } else {
                    // Lazy lists keep the first visible item in place when items are inserted above it;
                    // when already at the top, stay there so a newly created section is visible.
                    val sectionKeys = s.sections.map { it.key }
                    remember(sectionKeys) {
                        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                            listState.requestScrollToItem(0)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            // Room for the tiles' glow, which the list would otherwise clip.
                            top = 22.dp,
                            bottom = 120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        items(s.sections, key = { it.key }) { section ->
                            Section(
                                section = section,
                                now = now,
                                photoFile = viewModel::photoFile,
                                onClick = viewModel::markDone,
                                onLongClick = { editing = TileDraft.of(it) to it.lastDoneAt },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(18.dp),
                containerColor = palette.toastBackground,
                contentColor = palette.toastContent,
                actionColor = palette.toastAction,
            )
        }
    }

    editing?.let { (draft, lastDoneAt) ->
        EditTileSheet(
            initial = draft,
            lastDoneAt = lastDoneAt,
            now = now,
            groups = state?.groups.orEmpty(),
            photoFile = viewModel::photoFile,
            importPhoto = viewModel::importPhoto,
            createGroup = viewModel::createGroup,
            onSave = { viewModel.save(it); editing = null },
            onDiscard = { viewModel.discard(); editing = null },
            onDelete = { draft.trackerId?.let(viewModel::delete); editing = null },
        )
    }
}

@Composable
private fun Header(state: HomeUiState?, onAdd: () -> Unit) {
    val palette = LastTimeTheme.palette
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (state != null && state.trackerCount > 0) {
                val things = pluralStringResource(R.plurals.things_count, state.trackerCount, state.trackerCount)
                val subtitle = if (state.groups.isEmpty()) {
                    stringResource(R.string.header_tracked, things)
                } else {
                    val groups = pluralStringResource(R.plurals.groups_count, state.groups.size, state.groups.size)
                    stringResource(R.string.header_grouped, things, groups)
                }
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = palette.muted)
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = palette.text)
        }
        NeuIconButton(AppIcons.Add, stringResource(R.string.action_add_tile), onAdd)
    }
}

@Composable
private fun FilterChips(chips: List<FilterChipUi>, onSelect: (GroupFilter) -> Unit, onManageGroups: () -> Unit) {
    val palette = LastTimeTheme.palette
    // Padding inside the scroll area so the chips' shadows aren't clipped.
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            val label = chip.label ?: stringResource(
                if (chip.filter == GroupFilter.Ungrouped) R.string.chip_other else R.string.chip_all
            )
            val content: @Composable () -> Unit = {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (chip.selected) palette.ground else palette.text
                    Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                    Text(chip.count.toString(), style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
                }
            }
            if (chip.selected) {
                Box(
                    Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(palette.text)
                        .selectable(selected = true, role = Role.Tab, onClick = {}),
                    contentAlignment = Alignment.Center,
                ) { content() }
            } else {
                NeuButton(
                    onClick = { onSelect(chip.filter) },
                    shape = CircleShape,
                    distance = 5.dp,
                    blur = 12.dp,
                    modifier = Modifier.height(40.dp),
                ) { content() }
            }
        }
        NeuIconButton(
            icon = AppIcons.Groups,
            contentDescription = stringResource(R.string.action_manage_groups),
            onClick = onManageGroups,
            size = 40.dp,
            shape = CircleShape,
        )
    }
}

@Composable
private fun Section(
    section: SectionUi,
    now: Instant,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LastTimeTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val title = when (val t = section.title) {
            is SectionTitle.Group -> t.name
            SectionTitle.Other -> stringResource(R.string.chip_other)
            SectionTitle.None -> null
        }
        if (title != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = palette.text, modifier = Modifier.weight(1f))
                // With a single tile the summary would just repeat it.
                section.trackers.takeIf { it.size > 1 }?.minByOrNull { it.lastDoneAt }?.let { oldest ->
                    Text(
                        stringResource(R.string.section_oldest, RelativeTime.split(oldest.lastDoneAt, now).major),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.muted,
                    )
                }
            }
        }
        if (section.trackers.isEmpty()) {
            Text(stringResource(R.string.empty_group), style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        } else {
            BentoGrid(
                items = section.trackers,
                key = { it.id },
                sizeOf = { it.size },
                cellHeight = TileHeight,
                spacing = TileSpacing,
            ) { tracker ->
                TileCard(
                    tracker = tracker,
                    now = now,
                    photoFile = photoFile,
                    onClick = { onClick(tracker) },
                    onLongClick = { onLongClick(tracker) },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val palette = LastTimeTheme.palette
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge, color = palette.text)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
