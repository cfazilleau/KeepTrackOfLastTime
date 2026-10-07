package com.keeptrack.timeclicker.ui.home

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.keeptrack.timeclicker.data.TileSize
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Where a tile sits in the bento grid, in cells. */
data class BentoCell(val column: Int, val row: Int, val columns: Int, val rows: Int) {
    /** Whether the point, in cells (fractional), is on this tile. */
    fun contains(x: Float, y: Float): Boolean = x >= column && x < column + columns && y >= row && y < row + rows
}

object Bento {
    /**
     * Places tiles in order on a grid of [columnCount] columns, each at the first free spot
     * (top to bottom, left to right). Later small tiles fill holes left by wide/tall ones.
     */
    fun pack(sizes: List<TileSize>, columnCount: Int = 2): List<BentoCell> {
        val taken = mutableListOf<BooleanArray>()
        fun isFree(row: Int, column: Int) = row >= taken.size || !taken[row][column]
        fun take(cell: BentoCell) {
            while (taken.size < cell.row + cell.rows) taken.add(BooleanArray(columnCount))
            for (r in cell.row until cell.row + cell.rows) {
                for (c in cell.column until cell.column + cell.columns) taken[r][c] = true
            }
        }
        fun place(columns: Int, rows: Int): BentoCell {
            var row = 0
            while (true) {
                for (column in 0..columnCount - columns) {
                    val fits = (row until row + rows).all { r -> (column until column + columns).all { c -> isFree(r, c) } }
                    if (fits) return BentoCell(column, row, columns, rows).also(::take)
                }
                row++
            }
        }
        return sizes.map { place(it.columns.coerceAtMost(columnCount), it.rows) }
    }

    /**
     * Where to move the tile at [from] so it lands under the point ([x], [y], in cells), or null to leave it:
     * it is already there, or no place in the order puts it there. Of the places that do, the nearest one wins.
     *
     * Only accepting a move that puts the tile under the finger keeps a dragged tile from flipping back and forth
     * between two places as the others repack around it.
     */
    fun dropIndex(sizes: List<TileSize>, from: Int, x: Float, y: Float, columnCount: Int = 2): Int? {
        if (pack(sizes, columnCount)[from].contains(x, y)) return null
        return sizes.indices
            .filter { it != from }
            .sortedBy { abs(it - from) }
            .firstOrNull { to -> pack(sizes.moved(from, to), columnCount)[to].contains(x, y) }
    }
}

internal fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }

/** Tiles glide (with a little overshoot) to a new spot or size instead of jumping there. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val ElasticBounds = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Rect.VisibilityThreshold)
}

/** How a held tile grows, to show it is lifted off the grid. */
private const val HeldScale = 1.05f
private val LiftSpring = spring<Float>(stiffness = Spring.StiffnessMedium)

/** A tile being dragged to a new place. */
private class BentoDrag<T> {
    /** The order shown from the moment a tile is picked up until the saved order catches up; null otherwise. */
    var order by mutableStateOf<List<T>?>(null)
    /** The tile held, until it has settled into its place. */
    var key by mutableStateOf<Any?>(null)
    /** The finger, in the grid. */
    var finger by mutableStateOf(Offset.Zero)
    /** Where the finger holds the tile, from its top-left corner. */
    var grab = Offset.Zero
    /** Once dropped, the tile glides from where it was let go into its place. */
    var dropping by mutableStateOf(false)
    val settle = Animatable(Offset.Zero, Offset.VectorConverter)
    /** 1 while the tile is lifted off the grid, 0 once it has settled. */
    val lift = Animatable(0f)
    var settleJob: Job? = null
}

/** The grid's cell size in pixels, from its last layout. */
private class BentoMetrics {
    var cellWidth = 0
    var rowHeight = 0
    var gap = 0

    fun origin(cell: BentoCell) = Offset((cell.column * (cellWidth + gap)).toFloat(), (cell.row * (rowHeight + gap)).toFloat())
    fun columnAt(x: Float) = if (cellWidth == 0) 0f else x / (cellWidth + gap)
    fun rowAt(y: Float) = if (rowHeight == 0) 0f else y / (rowHeight + gap)
}

/**
 * Lays out [items] as a 2-column bento grid; each item's size comes from [sizeOf].
 * When tiles are added, removed, resized or reordered, the others move to their new place.
 *
 * With [onReorder], a tile can be held and dragged elsewhere; the others make room as it moves.
 * When it is dropped somewhere new, [onReorder] gets the new order.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> BentoGrid(
    items: List<T>,
    key: (T) -> Any,
    sizeOf: (T) -> TileSize,
    cellHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    onReorder: ((List<T>) -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val drag = remember { BentoDrag<T>() }
    val metrics = remember { BentoMetrics() }
    val order = drag.order ?: items
    val sizes = order.map(sizeOf)
    val cells = remember(sizes) { Bento.pack(sizes) }

    // The gesture handlers outlive a composition; they read the latest of these.
    val latestItems by rememberUpdatedState(items)
    val latestKey by rememberUpdatedState(key)
    val latestSizeOf by rememberUpdatedState(sizeOf)
    val latestOnReorder by rememberUpdatedState(onReorder)
    val haptics = LocalHapticFeedback.current
    val hapticsOn by rememberUpdatedState(TimeClickerTheme.settings.haptics)
    val scope = rememberCoroutineScope()

    // The saved order caught up (or changed some other way): show it.
    LaunchedEffect(items) { if (drag.key == null) drag.order = null }

    fun cellOf(order: List<T>, tile: Any): BentoCell? {
        val index = order.indexOfFirst { latestKey(it) == tile }
        return if (index < 0) null else Bento.pack(order.map(latestSizeOf))[index]
    }

    fun pickUp(tile: Any, at: Offset) {
        drag.settleJob?.cancel()
        val current = drag.order ?: latestItems
        val cell = cellOf(current, tile) ?: return
        drag.order = current
        drag.key = tile
        drag.dropping = false
        drag.grab = at
        drag.finger = metrics.origin(cell) + at
        drag.settleJob = scope.launch { drag.lift.animateTo(1f, LiftSpring) }
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun moveBy(amount: Offset) {
        val current = drag.order ?: return
        drag.finger += amount
        val from = current.indexOfFirst { latestKey(it) == drag.key }
        if (from < 0) return
        val to = Bento.dropIndex(current.map(latestSizeOf), from, metrics.columnAt(drag.finger.x), metrics.rowAt(drag.finger.y))
        if (to != null) {
            drag.order = current.moved(from, to)
            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    fun drop() {
        val current = drag.order ?: return
        val tile = drag.key ?: return
        val cell = cellOf(current, tile) ?: return
        val changed = current.map(latestKey) != latestItems.map(latestKey)
        if (changed) latestOnReorder?.invoke(current)
        if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        drag.dropping = true
        drag.settleJob = scope.launch {
            launch { drag.lift.animateTo(0f, LiftSpring) }
            drag.settle.snapTo(drag.finger - drag.grab - metrics.origin(cell))
            drag.settle.animateTo(Offset.Zero, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
            drag.key = null
            drag.dropping = false
            if (drag.order?.map(latestKey) == latestItems.map(latestKey)) drag.order = null
        }
    }

    LookaheadScope {
        BentoLayout(
            cells = cells,
            cellHeight = cellHeight,
            spacing = spacing,
            metrics = metrics,
            modifier = modifier,
        ) {
            order.forEachIndexed { index, item ->
                key(key(item)) {
                    val tile = key(item)
                    val held = drag.key == tile
                    Box(
                        Modifier
                            // First in the chain, so it isn't recreated (ending the drag) when the modifiers below change.
                            .then(
                                if (onReorder != null) {
                                    Modifier.pointerInput(tile) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { at -> pickUp(tile, at) },
                                            onDrag = { change, amount -> change.consume(); moveBy(amount) },
                                            onDragEnd = ::drop,
                                            onDragCancel = ::drop,
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            // The held tile follows the finger rather than gliding to each new place.
                            .then(
                                if (held) {
                                    val cell = cells[index]
                                    Modifier.zIndex(1f).graphicsLayer {
                                        val offset = if (drag.dropping) drag.settle.value else drag.finger - drag.grab - metrics.origin(cell)
                                        translationX = offset.x
                                        translationY = offset.y
                                        val scale = 1f + (HeldScale - 1f) * drag.lift.value
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                } else {
                                    Modifier.animateBounds(this@LookaheadScope, boundsTransform = ElasticBounds)
                                }
                            )
                            .fillMaxSize(),
                        propagateMinConstraints = true,
                    ) {
                        content(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoLayout(
    cells: List<BentoCell>,
    cellHeight: Dp,
    spacing: Dp,
    metrics: BentoMetrics,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val cellWidth = (constraints.maxWidth - gap) / 2
        val rowHeight = cellHeight.roundToPx()
        metrics.cellWidth = cellWidth
        metrics.rowHeight = rowHeight
        metrics.gap = gap
        val rowCount = cells.maxOfOrNull { it.row + it.rows } ?: 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val cell = cells[index]
            // Loose, so a tile can be smaller than its cell while it grows into it.
            measurable.measure(
                Constraints(
                    maxWidth = cell.columns * cellWidth + (cell.columns - 1) * gap,
                    maxHeight = cell.rows * rowHeight + (cell.rows - 1) * gap,
                )
            )
        }
        val height = if (rowCount == 0) 0 else rowCount * rowHeight + (rowCount - 1) * gap
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val cell = cells[index]
                placeable.place(cell.column * (cellWidth + gap), cell.row * (rowHeight + gap))
            }
        }
    }
}
