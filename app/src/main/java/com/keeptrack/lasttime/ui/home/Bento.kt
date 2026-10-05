package com.keeptrack.lasttime.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.keeptrack.lasttime.data.TileSize

/** Where a tile sits in the bento grid, in cells. */
data class BentoCell(val column: Int, val row: Int, val columns: Int, val rows: Int)

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
}

/** Lays out [items] as a 2-column bento grid; each item's size comes from [sizeOf]. */
@Composable
fun <T> BentoGrid(
    items: List<T>,
    key: (T) -> Any,
    sizeOf: (T) -> TileSize,
    cellHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val sizes = items.map(sizeOf)
    val cells = remember(sizes) { Bento.pack(sizes) }
    Layout(
        content = { items.forEach { item -> key(key(item)) { content(item) } } },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val cellWidth = (constraints.maxWidth - gap) / 2
        val rowHeight = cellHeight.roundToPx()
        val rowCount = cells.maxOfOrNull { it.row + it.rows } ?: 0
        val placeables = measurables.mapIndexed { index, measurable ->
            val cell = cells[index]
            measurable.measure(
                Constraints.fixed(
                    width = cell.columns * cellWidth + (cell.columns - 1) * gap,
                    height = cell.rows * rowHeight + (cell.rows - 1) * gap,
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
