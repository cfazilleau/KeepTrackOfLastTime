package com.keeptrack.timeclicker.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.TimeClickerApplication
import com.keeptrack.timeclicker.data.CatalogIcon
import com.keeptrack.timeclicker.data.IconCatalog
import com.keeptrack.timeclicker.data.SettingsRepository
import com.keeptrack.timeclicker.data.TileIcon
import com.keeptrack.timeclicker.ui.components.GutteredColumn
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.components.NeuTextField
import com.keeptrack.timeclicker.ui.home.ConfirmDialog
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.theme.rememberIconCatalog
import java.text.Collator
import java.text.Normalizer
import kotlinx.coroutines.launch

class IconPaletteViewModel(private val settings: SettingsRepository) : ViewModel() {

    /** Adds the icon at the end of the palette, or removes it. */
    fun toggle(icon: TileIcon) = settings.update { s ->
        s.copy(iconPalette = if (icon in s.iconPalette) s.iconPalette - icon else s.iconPalette + icon)
    }

    fun reset() = settings.update { it.copy(iconPalette = TileIcon.defaultPalette) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TimeClickerApplication
                IconPaletteViewModel(app.container.settingsRepository)
            }
        }
    }
}

/** A category of the catalog, with its translated name. */
private class CategoryUi(val id: String, val name: String, val icons: List<CatalogIcon>)

/**
 * Every Lucide icon, by category, to choose the icons offered when editing a tile.
 * The palette comes first; tapping any icon adds it to the palette or removes it. Search looks at the
 * icon names, Lucide's (English) tags, the search words in the app's language and the translated category names.
 */
@Composable
fun IconPaletteScreen(
    onBack: () -> Unit,
    viewModel: IconPaletteViewModel = viewModel(factory = IconPaletteViewModel.Factory),
) {
    val palette = TimeClickerTheme.palette
    val iconPalette = TimeClickerTheme.settings.iconPalette
    val catalog = rememberIconCatalog()
    var query by rememberSaveable { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }

    GutteredColumn(
        Modifier
            .fillMaxSize()
            .background(palette.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) { gutter ->
        Row(
            Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NeuIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack, size = 44.dp, shape = RoundedCornerShape(15.dp))
            Text(
                stringResource(R.string.settings_icon_palette),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            NeuIconButton(
                AppIcons.Refresh,
                stringResource(R.string.icon_palette_reset),
                onClick = { confirmReset = true },
                size = 44.dp,
                shape = CircleShape,
            )
        }
        IconSearchField(query, onQueryChange = { query = it }, gutter = gutter)

        if (catalog != null) {
            val selected = remember(iconPalette) { iconPalette.toSet() }
            IconGrid(
                catalog,
                query,
                iconPalette,
                firstTitle = stringResource(R.string.icon_palette_yours),
                isHighlighted = { it in selected },
                onClick = viewModel::toggle,
                editsPalette = true,
                highlight = palette.accent,
                onHighlight = palette.onAccent,
                bottomPadding = 40.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                gutter = gutter,
            )
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_reset_palette_title),
            body = stringResource(R.string.dialog_reset_palette_body),
            confirmLabel = stringResource(R.string.icon_palette_reset),
            onConfirm = {
                confirmReset = false
                viewModel.reset()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Search field for [IconGrid]; [gutter] pads its sides. */
@Composable
internal fun IconSearchField(query: String, onQueryChange: (String) -> Unit, gutter: Dp = 20.dp) {
    val palette = TimeClickerTheme.palette
    val clearButton: @Composable () -> Unit = {
        IconButton(onClick = { onQueryChange("") }) {
            Icon(AppIcons.Close, stringResource(R.string.icon_palette_clear_search), tint = palette.muted, modifier = Modifier.size(20.dp))
        }
    }
    NeuTextField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.icon_palette_search),
        placeholder = stringResource(R.string.icon_palette_search),
        leadingIcon = AppIcons.Search,
        capitalization = KeyboardCapitalization.None,
        trailing = clearButton.takeIf { query.isNotEmpty() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp),
    )
}

/**
 * The [first] icons under [firstTitle] (the palette, or the recently used icons), then every category; with a
 * search, the matching icons instead.
 * [editsPalette]: tapping an icon toggles it in the palette (with hints about it), rather than choosing it.
 * [gutter] pads the grid's sides.
 */
@Composable
internal fun IconGrid(
    catalog: IconCatalog,
    query: String,
    first: List<TileIcon>,
    firstTitle: String,
    isHighlighted: (TileIcon) -> Boolean,
    onClick: (TileIcon) -> Unit,
    editsPalette: Boolean,
    highlight: Color,
    onHighlight: Color,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    gutter: Dp = 20.dp,
    state: LazyGridState = rememberLazyGridState(),
) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    // Lucide's tags are English: search also looks at the words in the app's language, where there are some.
    val localTags by produceState(emptyMap<String, List<String>>(), locale.language) {
        value = IconCatalog.localTags(context.applicationContext, locale.language)
    }
    val categories = remember(catalog, resources, locale) {
        val collator = Collator.getInstance(locale)
        catalog.categories.mapNotNull { (id, icons) ->
            categoryNames[id]?.let { CategoryUi(id, resources.getString(it), icons) }
        }.sortedWith(compareBy(collator) { it.name })
    }
    // Every icon's searchable words, lower-cased and without accents ("étoile" is found with "etoile").
    val searchWords = remember(catalog, categories, localTags) {
        val categoryLabels = categories.associate { it.id to it.name }
        catalog.icons.associateWith { icon ->
            (listOf(icon.icon.key, iconLabel(resources, icon.icon)) + icon.tags + localTags[icon.icon.key].orEmpty() +
                icon.categories.mapNotNull(categoryLabels::get)).flatMap { it.searchWords() }.toSet()
        }
    }
    // Each word typed must start one of the icon's words: "vélo" finds "vélo" but not "développer".
    val words = query.searchWords()
    val results = remember(searchWords, words) {
        if (words.isEmpty()) emptyList() else catalog.icons.filter { icon ->
            val iconWords = searchWords.getValue(icon)
            words.all { word -> iconWords.any { it.startsWith(word) } }
        }
    }
    val chosen = remember(catalog, first) { first.mapNotNull { catalog[it] } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 12.dp,
            bottom = bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val cell: @Composable (CatalogIcon) -> Unit = { icon ->
            IconCell(
                catalog, icon.icon, iconLabel(resources, icon.icon), isHighlighted(icon.icon), editsPalette, highlight, onHighlight,
            ) { onClick(icon.icon) }
        }
        if (words.isNotEmpty()) {
            header("results", resources.getString(R.string.icon_palette_results), results.size)
            if (results.isEmpty()) note("no-results", resources.getString(R.string.icon_palette_no_results, query.trim()))
            items(results, key = { "r:${it.icon.key}" }, contentType = { "icon" }) { cell(it) }
        } else {
            if (editsPalette || chosen.isNotEmpty()) header("first", firstTitle, chosen.size)
            if (editsPalette) {
                note("hint", resources.getString(if (chosen.isEmpty()) R.string.icon_palette_empty else R.string.icon_palette_hint))
            }
            // As in the edit sheet, where "No icon" comes before the palette; it can't be removed.
            if (editsPalette) {
                item(key = "none", contentType = "icon") { NoIconCell(iconLabel(resources, TileIcon.NONE)) }
            }
            items(chosen, key = { "p:${it.icon.key}" }, contentType = { "icon" }) { cell(it) }
            categories.forEach { category ->
                header("h:${category.id}", category.name, category.icons.size)
                items(category.icons, key = { "c:${category.id}:${it.icon.key}" }, contentType = { "icon" }) { cell(it) }
            }
        }
    }
}

/** A section title across the whole grid, with how many icons it has. */
private fun LazyGridScope.header(key: String, title: String, count: Int) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        val palette = TimeClickerTheme.palette
        Row(
            Modifier.padding(top = 18.dp, bottom = 2.dp, start = 4.dp).semantics { heading() },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = palette.muted)
        }
    }
}

private fun LazyGridScope.note(key: String, text: String) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "note") {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = TimeClickerTheme.palette.muted,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
    }
}

/** One icon; highlighted when it is in the palette ([toggles]) or chosen. */
@Composable
private fun IconCell(
    catalog: IconCatalog,
    icon: TileIcon,
    label: String,
    highlighted: Boolean,
    toggles: Boolean,
    highlight: Color,
    onHighlight: Color,
    onClick: () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    val vector = remember(catalog, icon) { AppIcons.tile(catalog, icon) }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) highlight else palette.field)
            .then(
                if (toggles) {
                    Modifier.toggleable(value = highlighted, role = Role.Checkbox, onValueChange = { onClick() })
                } else {
                    Modifier.selectable(selected = highlighted, role = Role.RadioButton, onClick = onClick)
                }
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (vector != null) {
            Icon(vector, null, tint = if (highlighted) onHighlight else palette.text, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * "No icon", shown faded before the palette: it is always offered, so it can't be toggled.
 * Tapping it says so in a tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoIconCell(label: String) {
    val palette = TimeClickerTheme.palette
    val tooltip = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(stringResource(R.string.icon_palette_none_fixed)) } },
        state = tooltip,
    ) {
        Box(
            Modifier
                .aspectRatio(1f)
                .alpha(0.45f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.field)
                .clickable(role = Role.Button) { scope.launch { tooltip.show() } }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.NoIcon, null, tint = palette.text, modifier = Modifier.size(24.dp))
        }
    }
}

private val accents = Regex("\\p{Mn}+")
private val separators = Regex("[^\\p{L}\\p{N}]+")

/** The words of a text, in lower case, without accents or ligatures: "Lave-linge, cœur" → lave, linge, coeur. */
internal fun String.searchWords(): List<String> =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(accents, "")
        .replace("œ", "oe").replace("æ", "ae")
        .split(separators).filter { it.isNotEmpty() }
