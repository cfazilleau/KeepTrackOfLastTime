package com.keeptrack.timeclicker.ui.home

import com.keeptrack.timeclicker.data.TileSize.SMALL
import com.keeptrack.timeclicker.data.TileSize.TALL
import com.keeptrack.timeclicker.data.TileSize.WIDE
import org.junit.Assert.assertEquals
import org.junit.Test

class BentoTest {

    @Test
    fun smallTilesFillRowsLeftToRight() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(1, 0, 1, 1), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(SMALL, SMALL, SMALL)),
        )
    }

    @Test
    fun tallTileLeavesRoomForTwoSmallOnesBesideIt() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 2), BentoCell(1, 0, 1, 1), BentoCell(1, 1, 1, 1), BentoCell(0, 2, 2, 1)),
            Bento.pack(listOf(TALL, SMALL, SMALL, WIDE)),
        )
    }

    @Test
    fun laterSmallTileFillsHoleBeforeAWideOne() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(0, 1, 2, 1), BentoCell(1, 0, 1, 1)),
            Bento.pack(listOf(SMALL, WIDE, SMALL)),
        )
    }

    @Test
    fun tallTileOnTheRightWhenLeftIsTaken() {
        assertEquals(
            listOf(BentoCell(0, 0, 1, 1), BentoCell(1, 0, 1, 2), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(SMALL, TALL, SMALL)),
        )
    }

    @Test
    fun emptyListPacksToNothing() {
        assertEquals(emptyList<BentoCell>(), Bento.pack(emptyList()))
    }

    @Test
    fun fourColumnsFitSmallTilesBesideAWideAndATallOne() {
        assertEquals(
            listOf(BentoCell(0, 0, 2, 1), BentoCell(2, 0, 1, 2), BentoCell(3, 0, 1, 1), BentoCell(0, 1, 1, 1)),
            Bento.pack(listOf(WIDE, TALL, SMALL, SMALL), columnCount = 4),
        )
    }

    @Test
    fun phonesGetTwoColumnsEvenWhenNarrow() {
        // Widths in dp at density 1: tiles at least 150 wide, 16 apart.
        assertEquals(2, Bento.columnCount(width = 280, minCellWidth = 150, gap = 16))
        assertEquals(2, Bento.columnCount(width = 372, minCellWidth = 150, gap = 16))
    }

    @Test
    fun widerScreensGetAsManyColumnsAsFit() {
        assertEquals(3, Bento.columnCount(width = 560, minCellWidth = 150, gap = 16))
        assertEquals(4, Bento.columnCount(width = 760, minCellWidth = 150, gap = 16))
        // Exactly four tiles of 150 and three gaps.
        assertEquals(4, Bento.columnCount(width = 648, minCellWidth = 150, gap = 16))
        assertEquals(7, Bento.columnCount(width = 1240, minCellWidth = 150, gap = 16))
    }
}
