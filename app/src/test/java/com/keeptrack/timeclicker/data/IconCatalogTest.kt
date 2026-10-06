package com.keeptrack.timeclicker.data

import com.keeptrack.timeclicker.ui.icons.categoryNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IconCatalogTest {

    @Test
    fun parsesLinesAndSkipsComments() {
        val catalog = IconCatalog.parse(
            "# Lucide test\n" +
                "circle-check\tshapes,notifications\tdone,todo\tM2 12a10 10 0 1 0 20 0\n" +
                "x\t\t\tM18 6L6 18\n",
        )
        assertEquals(2, catalog.icons.size)
        val check = catalog[TileIcon("circle-check")]!!
        assertEquals(listOf("shapes", "notifications"), check.categories)
        assertEquals(listOf("done", "todo"), check.tags)
        assertEquals("M2 12a10 10 0 1 0 20 0", check.path)
        assertEquals(emptyList<String>(), catalog[TileIcon("x")]!!.categories)
        assertNull(catalog.path(TileIcon("missing")))
        assertEquals(listOf("circle-check"), catalog.categories.getValue("shapes").map { it.icon.key })
    }

    @Test
    fun parsesLocalTags() {
        val tags = IconCatalog.parseTags("# test\nstar\tétoile,favori\nx\t\n")
        assertEquals(listOf("étoile", "favori"), tags["star"])
        assertEquals(emptyList<String>(), tags["x"])
        assertNull(tags["heart"])
    }

    @Test
    fun iconKeysAreLucideNames() {
        // "check" is also a Lucide icon: stored now, it stays that icon.
        assertEquals(TileIcon("check"), TileIcon.fromKey("check"))
        assertEquals(TileIcon.NONE, TileIcon.fromKey("none"))
        assertEquals(TileIcon.DEFAULT, TileIcon.fromKey(null))
        assertEquals(TileIcon("rocket"), TileIcon.fromKey("rocket"))
    }

    @Test
    fun oldIconNamesMapToTheirLucideIcons() {
        assertEquals(TileIcon("circle-check"), TileIcon.fromLegacyKey("check"))
        assertEquals(TileIcon("paw-print"), TileIcon.fromLegacyKey("paw"))
        assertEquals(TileIcon("leaf"), TileIcon.fromLegacyKey("leaf"))
        assertEquals(TileIcon("trash"), TileIcon.fromLegacyKey("trash"))
        assertEquals(TileIcon.NONE, TileIcon.fromLegacyKey("none"))
    }

    @Test
    fun paletteRoundTripsThroughSettingsKeys() {
        val palette = listOf(TileIcon("rocket"), TileIcon("circle-check"))
        assertEquals(palette, SettingsRepository.paletteFromKeys(SettingsRepository.paletteToKeys(palette)))
        assertEquals(emptyList<TileIcon>(), SettingsRepository.paletteFromKeys(""))
        // "none" and duplicates are dropped.
        assertEquals(
            listOf(TileIcon("check"), TileIcon("circle-check")),
            SettingsRepository.paletteFromKeys("check,none,circle-check,check"),
        )
    }

    /** Guards regenerating the asset: icons tiles may use, and every category, must still be there. */
    @Test
    fun bundledCatalogHasEveryIconTheAppReliesOn() {
        val catalog = IconCatalog.parse(File("src/main/assets/lucide/icons.tsv").readText())
        assertTrue(catalog.icons.size > 1000)
        val legacy = listOf("check", "drop", "leaf", "grass", "flower", "bed", "coffee", "snow", "trash", "cart",
            "phone", "heart", "pill", "paw", "scissors", "brush", "gauge", "car").map { TileIcon.fromLegacyKey(it) }
        (TileIcon.defaultPalette + legacy + TileIcon.DEFAULT).forEach { icon ->
            assertTrue("missing ${icon.key}", catalog[icon] != null)
        }
        val missingNames = catalog.categories.keys - categoryNames.keys
        assertTrue("categories without a name: $missingNames", missingNames.isEmpty())
        catalog.icons.forEach { assertTrue("bad path for ${it.icon.key}", it.path.startsWith("M")) }
    }

    /** The hand-written search words in other languages must be for icons that still exist, each with some words. */
    @Test
    fun localTagsAreForBundledIcons() {
        val catalog = IconCatalog.parse(File("src/main/assets/lucide/icons.tsv").readText())
        val files = File("src/main/assets/lucide").listFiles { file -> file.name.startsWith("tags-") }.orEmpty()
        assertTrue(files.isNotEmpty())
        files.forEach { file ->
            val tags = IconCatalog.parseTags(file.readText())
            val unknown = tags.keys.filter { catalog[TileIcon(it)] == null }
            assertTrue("${file.name}: icons not in the catalog: $unknown", unknown.isEmpty())
            tags.forEach { (name, words) ->
                assertTrue("${file.name}: bad words for $name", words.isNotEmpty() && words.none { it.isBlank() })
            }
        }
    }
}
