package com.keeptrack.timeclicker.ui.home

import com.keeptrack.timeclicker.data.TileSize.SMALL
import com.keeptrack.timeclicker.data.TileSize.TALL
import com.keeptrack.timeclicker.data.TileSize.WIDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun draggedTileStaysWhileTheFingerIsOnIt() {
        assertNull(Bento.dropIndex(listOf(SMALL, SMALL, SMALL), from = 0, x = 0.9f, y = 0.5f))
    }

    @Test
    fun draggedTileMovesToWhereTheFingerIs() {
        // Bottom right of a 2x2 grid of small tiles is the last place.
        assertEquals(3, Bento.dropIndex(listOf(SMALL, SMALL, SMALL, SMALL), from = 0, x = 1.5f, y = 1.5f))
        // And back to the top left.
        assertEquals(0, Bento.dropIndex(listOf(SMALL, SMALL, SMALL, SMALL), from = 3, x = 0.2f, y = 0.2f))
    }

    @Test
    fun wideTileMovesUpToTheTop() {
        assertEquals(0, Bento.dropIndex(listOf(SMALL, SMALL, WIDE), from = 2, x = 1.5f, y = 0.5f))
    }

    @Test
    fun noMoveWhenNoOrderPutsTheTileUnderTheFinger() {
        // A small tile after the wide one would fill the hole above it, never land beside the finger below.
        assertNull(Bento.dropIndex(listOf(SMALL, SMALL, WIDE), from = 0, x = 0.5f, y = 2.5f))
    }
}
