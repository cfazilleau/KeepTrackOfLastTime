package com.keeptrack.timeclicker.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.data.PendingUndo
import com.keeptrack.timeclicker.data.Tracker
import com.keeptrack.timeclicker.ui.components.NeuButton
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.components.PillButton
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.time.RelativeTime
import com.keeptrack.timeclicker.ui.time.TimeUnit
import com.keeptrack.timeclicker.ui.time.format
import com.keeptrack.timeclicker.ui.time.rememberNow
import com.keeptrack.timeclicker.widget.TileWidgets
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue

private val TileHeight = 156.dp
private val TileSpacing = 16.dp
private const val SwayDegrees = 0.8f
private const val SwayMillis = 240

/** Springy motion shared by page switches and sections moving in the list. */
private val PageSpring = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
private val SectionSpring = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset(1, 1))

@Composable
fun HomeScreen(
    onManageGroups: () -> Unit,
    onOpenIconPalette: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val undoable by viewModel.undoable.collectAsStateWithLifecycle()
    val palette = TimeClickerTheme.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The tile open in the edit sheet.
    var editing by remember { mutableStateOf<Tracker?>(null) }
    var creating by remember { mutableStateOf(false) }
    // While on, tiles are held and dragged to a new place instead of tapped.
    var reordering by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = reordering) { reordering = false }
    // Only a section with two tiles or more has anything to reorder.
    val canReorder = state?.let { s -> s.pages.getOrNull(s.selectedPage)?.sections?.any { it.trackers.size > 1 } } == true

    val clickSound = TimeClickerTheme.settings.clickSound
    // Loaded ahead of the first tap, which would otherwise be silent.
    LaunchedEffect(clickSound) { if (clickSound) TapSound.preload(context) }

    Box(Modifier.fillMaxSize().background(palette.ground)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            TitleBar(
                reordering = reordering,
                onDoneReordering = { reordering = false },
                menu = {
                    HomeMenu(
                        canReorder = canReorder,
                        onReorder = { reordering = true },
                        onManageGroups = onManageGroups,
                        onOpenIconPalette = onOpenIconPalette,
                        onOpenSettings = onOpenSettings,
                        onOpenAbout = onOpenAbout,
                    )
                },
            )
            state?.let { s ->
                GroupPager(
                    state = s,
                    undoable = undoable,
                    reordering = reordering,
                    onReorder = viewModel::reorder,
                    onSelect = viewModel::select,
                    photoFile = viewModel::photoFile,
                    onClick = { tracker ->
                        // The click is for marking done; an undo stays quiet.
                        if (clickSound && tracker.id !in undoable) TapSound.play(context)
                        viewModel.press(tracker)
                    },
                    onLongClick = { editing = it },
                )
            }
        }

        AnimatedVisibility(
            visible = !reordering,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
        ) {
            AddButton(onClick = { creating = true })
        }
    }

    if (creating) {
        NewTileSheet(
            onCreate = { viewModel.create(it); creating = false },
            onDismiss = { creating = false },
        )
    }
    editing?.let { saved ->
        val canPin = remember { TileWidgets.canPin(context) }
        EditTileSheet(
            saved = saved,
            groups = state?.groups.orEmpty(),
            photoFile = viewModel::photoFile,
            importPhoto = viewModel::importPhoto,
            createGroup = viewModel::createGroup,
            onSave = { viewModel.save(it, saved); editing = null },
            onDiscard = { viewModel.discard(); editing = null },
            onDelete = { viewModel.delete(saved.id); editing = null },
            onAddWidget = if (canPin) ({ scope.launch { TileWidgets.requestPin(context, saved.id) } }) else null,
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
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    onReorder: (List<Long>) -> Unit,
    onSelect: (GroupFilter) -> Unit,
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
        // Without groups there is only "All": nothing to choose between.
        AnimatedVisibility(visible = pages.size > 1) {
            FilterChips(
                pages = pages,
                selected = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index, animationSpec = PageSpring) } },
            )
        }
        HorizontalPager(
            state = pagerState,
            // Lazy layout keys must be saveable in a Bundle: a string, not the GroupFilter itself.
            key = { pages[it].chip.filter.key },
            beyondViewportPageCount = 1,
            // A sideways drag moves the held tile, not the page.
            userScrollEnabled = !reordering,
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
                    PageList(page, undoable, reordering, onReorder, photoFile, onClick, onLongClick)
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
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    onReorder: (List<Long>) -> Unit,
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
                undoable = undoable,
                reordering = reordering,
                onReorder = onReorder,
                photoFile = photoFile,
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier.animateItem(placementSpec = SectionSpring),
            )
        }
    }
}

/** The app's name and the [menu]; while reordering, how to reorder and a button to finish. */
@Composable
private fun TitleBar(reordering: Boolean, onDoneReordering: () -> Unit, menu: @Composable () -> Unit) {
    val palette = TimeClickerTheme.palette
    AnimatedContent(
        targetState = reordering,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.CenterStart,
        label = "titleBar",
    ) { isReordering ->
        Row(
            // As tall as the menu button either way, so the chips below don't move.
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isReordering) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reorder_tiles), style = MaterialTheme.typography.titleLarge, color = palette.text)
                    Text(stringResource(R.string.reorder_hint), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                PillButton(stringResource(R.string.action_done), onClick = onDoneReordering)
            } else {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                menu()
            }
        }
    }
}

/** The gear button and what it opens: things to do with the tiles, then the app's other screens. */
@Composable
private fun HomeMenu(
    canReorder: Boolean,
    onReorder: () -> Unit,
    onManageGroups: () -> Unit,
    onOpenIconPalette: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    var open by remember { mutableStateOf(false) }
    Box {
        NeuIconButton(
            icon = AppIcons.Settings,
            contentDescription = stringResource(R.string.action_menu),
            onClick = { open = true },
            size = 48.dp,
            shape = CircleShape,
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(0.dp, 8.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = palette.ground,
        ) {
            @Composable
            fun Item(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.titleSmall) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
                    enabled = enabled,
                    onClick = { open = false; onClick() },
                    colors = MenuDefaults.itemColors(
                        textColor = palette.text,
                        leadingIconColor = palette.text,
                        disabledTextColor = palette.muted,
                        disabledLeadingIconColor = palette.muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
            Item(AppIcons.Reorder, stringResource(R.string.reorder_tiles), onReorder, enabled = canReorder)
            Item(AppIcons.Groups, stringResource(R.string.action_manage_groups), onManageGroups)
            Item(AppIcons.Palette, stringResource(R.string.settings_icon_palette), onOpenIconPalette)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = palette.field)
            Item(AppIcons.Settings, stringResource(R.string.action_settings), onOpenSettings)
            Item(AppIcons.Info, stringResource(R.string.about_title), onOpenAbout)
        }
    }
}

/** The group chips, scrolling sideways. */
@Composable
private fun FilterChips(
    pages: List<PageUi>,
    selected: Int,
    onSelect: (Int) -> Unit,
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
            val label = chip.label ?: stringResource(R.string.chip_all)
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
    }
}

@Composable
private fun Section(
    section: SectionUi,
    undoable: Map<Long, PendingUndo>,
    reordering: Boolean,
    onReorder: (List<Long>) -> Unit,
    photoFile: (String) -> File,
    onClick: (Tracker) -> Unit,
    onLongClick: (Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TimeClickerTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val title = when (val t = section.title) {
            is SectionTitle.Group -> t.name
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
            // Tiles sway a little while they can be moved, the way home-screen icons do.
            val sway = if (reordering) {
                rememberInfiniteTransition(label = "sway").animateFloat(
                    initialValue = -SwayDegrees,
                    targetValue = SwayDegrees,
                    animationSpec = infiniteRepeatable(tween(SwayMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "swayAngle",
                )
            } else {
                null
            }
            val ids = section.trackers.map { it.id }
            val moveEarlier = stringResource(R.string.action_move_earlier)
            val moveLater = stringResource(R.string.action_move_later)
            BentoGrid(
                items = section.trackers,
                key = { it.id },
                sizeOf = { it.size },
                cellHeight = TileHeight,
                spacing = TileSpacing,
                onReorder = if (reordering) ({ trackers -> onReorder(trackers.map { it.id }) }) else null,
            ) { tracker ->
                val reorderModifier = if (reordering) {
                    val index = ids.indexOf(tracker.id)
                    // Neighbours sway in opposite directions.
                    val direction = if (index % 2 == 0) 1f else -1f
                    Modifier
                        .graphicsLayer { rotationZ = (sway?.value ?: 0f) * direction }
                        // Dragging isn't possible with a screen reader: the same moves, one place at a time.
                        .semantics {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(moveEarlier) { onReorder(ids.moved(index, index - 1)); true }
                                    .takeIf { index > 0 },
                                CustomAccessibilityAction(moveLater) { onReorder(ids.moved(index, index + 1)); true }
                                    .takeIf { index < ids.lastIndex },
                            )
                        }
                } else {
                    Modifier
                }
                TileCard(
                    tracker = tracker,
                    undoUntil = undoable[tracker.id]?.until,
                    photoFile = photoFile,
                    onClick = { onClick(tracker) },
                    onLongClick = { onLongClick(tracker) },
                    enabled = !reordering,
                    modifier = reorderModifier,
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
