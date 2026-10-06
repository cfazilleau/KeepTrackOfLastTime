package com.keeptrack.timeclicker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One icon of the bundled Lucide set: Lucide's category ids and English search tags, and its path data. */
class CatalogIcon(val icon: TileIcon, val categories: List<String>, val tags: List<String>, val path: String)

/**
 * Every bundled Lucide icon (https://lucide.dev, ISC licence), read from `assets/lucide/icons.tsv`,
 * which tools/lucide/generate_icons.py writes. Each path is on a 24x24 grid, drawn with a 2-unit round stroke.
 */
class IconCatalog(val icons: List<CatalogIcon>) {
    private val byKey = icons.associateBy { it.icon.key }

    operator fun get(icon: TileIcon): CatalogIcon? = byKey[icon.key]

    fun path(icon: TileIcon): String? = byKey[icon.key]?.path

    /** Lucide's category ids, each with its icons in name order. An icon can be in several categories. */
    val categories: Map<String, List<CatalogIcon>> by lazy {
        buildMap<String, MutableList<CatalogIcon>> {
            icons.forEach { icon -> icon.categories.forEach { getOrPut(it) { mutableListOf() } += icon } }
        }
    }

    companion object {
        private const val ASSET = "lucide/icons.tsv"
        private val state = MutableStateFlow<IconCatalog?>(null)
        private val mutex = Mutex()

        /** Null until [load] has finished once. */
        val loaded: StateFlow<IconCatalog?> = state.asStateFlow()

        /** Reads the catalog the first time (a few hundred KB, off the main thread); later calls return it at once. */
        suspend fun load(context: Context): IconCatalog = state.value ?: mutex.withLock {
            state.value ?: withContext(Dispatchers.IO) {
                parse(context.assets.open(ASSET).bufferedReader().use { it.readText() })
            }.also { state.value = it }
        }

        /** One icon per line: name, categories, tags (both comma-separated) and path, separated by tabs. "#" starts a comment. */
        fun parse(text: String): IconCatalog = IconCatalog(
            text.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line ->
                    val (name, categories, tags, path) = line.split('\t', limit = 4)
                    CatalogIcon(TileIcon(name), categories.splitList(), tags.splitList(), path.trim())
                }
                .toList()
        )

        private fun String.splitList(): List<String> = if (isBlank()) emptyList() else split(',')
    }
}
