package dev.cfaz.timeclicker.ui.home

import dev.cfaz.timeclicker.data.TileSize.SMALL
import dev.cfaz.timeclicker.data.TileSize.TALL
import dev.cfaz.timeclicker.data.TileSize.WIDE
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
}
