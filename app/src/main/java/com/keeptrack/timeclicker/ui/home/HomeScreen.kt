package com.keeptrack.timeclicker.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.keeptrack.timeclicker.ui.components.TapSound
import com.keeptrack.timeclicker.ui.components.rememberPressAmount
import com.keeptrack.timeclicker.ui.theme.raised
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.ui.components.NeuButton
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.time.RelativeTime
import com.keeptrack.timeclicker.ui.time.TimeUnit
import com.keeptrack.timeclicker.ui.time.format
import com.keeptrack.timeclicker.ui.time.rememberNow
import com.keeptrack.timeclicker.widget.TileWidgets
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue

private val TileHeight = 156.dp
private val TileSpacing = 16.dp

/** Springy motion shared by page switches and sections moving in the list. */
private val PageSpring = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
private val SectionSpring = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))

@Composable
fun HomeScreen(
    onManageGroups: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = TimeClickerTheme.palette
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The draft open in the edit sheet, and the saved tile it edits (null for a new one).
    var editing by remember { mutableStateOf<Pair<TileDraft, Tracker?>?>(null) }

    val undoAfterTap by rememberUpdatedState(TimeClickerTheme.settings.undoAfterTap)
    val clickSound = TimeClickerTheme.settings.clickSound
    // Loaded ahead of the first tap, which would otherwise be silent.
    LaunchedEffect(clickSound) { if (clickSound) TapSound.preload(context) }
    LaunchedEffect(viewModel) {
        // collectLatest: a newer reset replaces the snackbar of an older one.
        viewModel.resets.collectLatest { reset ->
            if (!undoAfterTap) return@collectLatest
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
            TitleBar(onOpenSettings)
            state?.let { s ->
                GroupPager(
                    state = s,
                    onSelect = viewModel::select,
                    onManageGroups = onManageGroups,
                    photoFile = viewModel::photoFile,
                    onClick = { tracker ->
                        if (clickSound) TapSound.play(context)
                        viewModel.markDone(tracker)
                    },
                    onLongClick = { editing = TileDraft.of(it) to it },
                )
            }
        }

        AddButton(
            onClick = { scope.launch { editing = viewModel.newDraft() to null } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
        )

        // Above the add button.
        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp + AddButtonSize + 12.dp),
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

    editing?.let { (draft, saved) ->
        val canPin = remember { TileWidgets.canPin(context) }
        EditTileSheet(
            initial = draft,
            saved = saved,
            groups = state?.groups.orEmpty(),
            photoFile = viewModel::photoFile,
            importPhoto = viewModel::importPhoto,
            createGroup = viewModel::createGroup,
            onSave = { viewModel.save(it); editing = null },
            onDiscard = { viewModel.discard(); editing = null },
            onDelete = { draft.trackerId?.let(viewModel::delete); editing = null },
            onAddWidget = saved?.takeIf { canPin }?.let { tile -> { scope.launch { TileWidgets.requestPin(context, tile.id) } } },
        )
    }
}

private val AddButtonSize = 64.dp

/** The main action, floating at the bottom centre: a new tile, filed in the group shown. */
@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = TimeClickerTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressAmount(interaction)
    Box(
        modifier
            .size(AddButtonSize)
            .graphicsLayer { val s = 1f - 0.06f * press(); scaleX = s; scaleY = s }
            .raised(CircleShape, palette.shadow, palette.highlight, distance = 6.dp, blur = 16.dp, pressed = press)
            .clip(CircleShape)
            .background(palette.accent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Add, stringResource(R.string.action_add_tile), tint = palette.onAccent, modifier = Modifier.size(28.dp))
    }
}

/**
 * The chips and one page per chip. Swiping left/right moves between groups; tapping a chip slides there.
 * Neighbouring pages shrink and fade a little while they slide in.
 */
@Composable
private fun GroupPager(
    state: HomeUiState,
    onSelect: (GroupFilter) -> Unit,
    onManageGroups: () -> Unit,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
) {
    val pages = state.pages
    val pagerState = rememberPagerState(initialPage = state.selectedPage) { pages.size }
    val scope = rememberCoroutineScope()

    // The settled page is the selected group (new tiles are filed in it).
    val currentPages by rememberUpdatedState(pages)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentPages.getOrNull(page)?.let { onSelect(it.chip.filter) }
        }
    }
    // Follow the selection when it moves without a swipe: its group was deleted, or groups were reordered.
    LaunchedEffect(state.selectedPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.selectedPage) {
            pagerState.animateScrollToPage(state.selectedPage, animationSpec = PageSpring)
        }
    }

    Column {
        FilterChips(
            pages = pages,
            selected = pagerState.currentPage,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index, animationSpec = PageSpring) } },
            onManageGroups = onManageGroups,
        )
        HorizontalPager(
            state = pagerState,
            // Lazy layout keys must be saveable in a Bundle: a string, not the GroupFilter itself.
            key = { pages[it].chip.filter.key },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val page = pages[index]
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val distance = pageDistance(pagerState, index).coerceIn(0f, 1f)
                        val scale = lerp(1f, 0.92f, distance)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(1f, 0.35f, distance)
                    },
            ) {
                if (state.trackerCount == 0) {
                    EmptyState(Modifier.fillMaxSize())
                } else {
                    PageList(page, photoFile, onClick, onLongClick)
                }
            }
        }
    }
}

private fun pageDistance(state: PagerState, page: Int): Float =
    ((state.currentPage - page) + state.currentPageOffsetFraction).absoluteValue

@Composable
private fun PageList(
    page: PageUi,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
) {
    val listState = rememberLazyListState()
    // Lazy lists keep the first visible item in place when items are inserted above it;
    // when already at the top, stay there so a newly created section is visible.
    // SideEffect runs before the list is measured, so it never shows a frame scrolled away from the top.
    val sectionKeys = page.sections.map { it.key }
    val seenKeys = remember { arrayOfNulls<List<String>>(1) }
    SideEffect {
        if (seenKeys[0] != sectionKeys) {
            seenKeys[0] = sectionKeys
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                listState.requestScrollToItem(0)
            }
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
        items(page.sections, key = { it.key }) { section ->
            Section(
                section = section,
                photoFile = photoFile,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier.animateItem(placementSpec = SectionSpring),
            )
        }
    }
}

/** The app's name, and the settings button. */
@Composable
private fun TitleBar(onOpenSettings: () -> Unit) {
    val palette = TimeClickerTheme.palette
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = palette.text,
            modifier = Modifier.weight(1f),
        )
        NeuIconButton(
            icon = AppIcons.Settings,
            contentDescription = stringResource(R.string.action_settings),
            onClick = onOpenSettings,
            size = 48.dp,
            shape = CircleShape,
        )
    }
}

/** The group chips, scrolling sideways, then the groups button. */
@Composable
private fun FilterChips(
    pages: List<PageUi>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onManageGroups: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    // Padding inside the scroll area so the chips' shadows aren't clipped.
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pages.forEachIndexed { index, page ->
            val chip = page.chip
            val isSelected = index == selected
            val label = chip.label ?: stringResource(
                if (chip.filter == GroupFilter.Ungrouped) R.string.chip_other else R.string.chip_all
            )
            // Swiping to a group scrolls its chip into view.
            val bringIntoView = remember { BringIntoViewRequester() }
            LaunchedEffect(isSelected) { if (isSelected) bringIntoView.bringIntoView() }

            val content: @Composable () -> Unit = {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = if (isSelected) palette.onAccent else palette.text
                    Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                    Text(chip.count.toString(), style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
                }
            }
            if (isSelected) {
                Box(
                    Modifier
                        .bringIntoViewRequester(bringIntoView)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .selectable(selected = true, role = Role.Tab, onClick = {}),
                    contentAlignment = Alignment.Center,
                ) { content() }
            } else {
                NeuButton(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    distance = 5.dp,
                    blur = 12.dp,
                    modifier = Modifier.bringIntoViewRequester(bringIntoView).height(40.dp),
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
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TimeClickerTheme.palette
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
                    val now = rememberNow(oldest.lastDoneAt, smallest = TimeUnit.MINUTE)
                    RelativeTime.split(oldest.lastDoneAt, now, smallest = TimeUnit.MINUTE).major?.let { major ->
                        Text(
                            stringResource(R.string.section_oldest, major.format(LocalResources.current)),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.muted,
                        )
                    }
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
    val palette = TimeClickerTheme.palette
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
