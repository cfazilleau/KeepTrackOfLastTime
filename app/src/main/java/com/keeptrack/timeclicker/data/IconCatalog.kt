package com.keeptrack.timeclicker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

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
        private val localTagsByLanguage = ConcurrentHashMap<String, Map<String, List<String>>>()

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

        /**
         * Search words in [language] (the locale's language code, "fr") for each icon name, read from
         * `assets/lucide/tags-<language>.tsv` the first time. Lucide's tags are only in English; these files
         * are written by hand. Empty for a language without one.
         */
        suspend fun localTags(context: Context, language: String): Map<String, List<String>> =
            localTagsByLanguage[language] ?: withContext(Dispatchers.IO) {
                try {
                    parseTags(context.assets.open("lucide/tags-$language.tsv").bufferedReader().use { it.readText() })
                } catch (e: FileNotFoundException) {
                    emptyMap()
                }
            }.also { localTagsByLanguage[language] = it }

        /** One icon per line: name and its search words (comma-separated), separated by a tab. "#" starts a comment. */
        fun parseTags(text: String): Map<String, List<String>> = text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val (name, tags) = line.split('\t', limit = 2)
                name to tags.trim().splitList()
            }

        private fun String.splitList(): List<String> = if (isBlank()) emptyList() else split(',')
    }
}
