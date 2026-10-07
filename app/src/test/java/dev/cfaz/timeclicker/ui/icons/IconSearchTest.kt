package dev.cfaz.timeclicker.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Test

class IconSearchTest {

    @Test
    fun searchWordsAreLowerCaseWithoutAccentsOrLigatures() {
        assertEquals(listOf("lave", "linge", "coeur", "etoile"), "Lave-linge, cœur  Étoile".searchWords())
        assertEquals(listOf("goutte", "d", "eau"), "goutte d'eau".searchWords())
        assertEquals(listOf("paw", "print"), "paw-print".searchWords())
        assertEquals(emptyList<String>(), " , ".searchWords())
    }
}
