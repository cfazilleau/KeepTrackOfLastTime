package com.keeptrack.timeclicker.ui.icons

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.components.NeuTextField
import com.keeptrack.timeclicker.ui.home.ConfirmDialog
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import com.keeptrack.timeclicker.ui.theme.rememberIconCatalog
import java.text.Collator
import java.text.Normalizer

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
 * icon names, Lucide's (English) tags and the translated category names.
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

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
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
        IconSearchField(query, onQueryChange = { query = it })

        if (catalog != null) {
            val selected = remember(iconPalette) { iconPalette.toSet() }
            IconGrid(
                catalog,
                query,
                iconPalette,
                isHighlighted = { it in selected },
                onClick = viewModel::toggle,
                editsPalette = true,
                highlight = palette.accent,
                onHighlight = palette.onAccent,
                bottomPadding = 40.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
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

/** Search field for [IconGrid]. */
@Composable
internal fun IconSearchField(query: String, onQueryChange: (String) -> Unit) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

/**
 * The palette then every category; with a search, the matching icons instead.
 * [editsPalette]: tapping an icon toggles it in the palette (with hints about it), rather than choosing it.
 */
@Composable
internal fun IconGrid(
    catalog: IconCatalog,
    query: String,
    iconPalette: List<TileIcon>,
    isHighlighted: (TileIcon) -> Boolean,
    onClick: (TileIcon) -> Unit,
    editsPalette: Boolean,
    highlight: Color,
    onHighlight: Color,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val categories = remember(catalog, resources, locale) {
        val collator = Collator.getInstance(locale)
        catalog.categories.mapNotNull { (id, icons) ->
            categoryNames[id]?.let { CategoryUi(id, resources.getString(it), icons) }
        }.sortedWith(compareBy(collator) { it.name })
    }
    // Every icon's searchable text, lower-cased and without accents ("étoile" is found with "etoile").
    val searchText = remember(catalog, categories) {
        val categoryLabels = categories.associate { it.id to it.name }
        catalog.icons.associateWith { icon ->
            (listOf(icon.icon.key.replace('-', ' '), iconLabel(resources, icon.icon)) + icon.tags +
                icon.categories.mapNotNull(categoryLabels::get)).joinToString(" ").simplified()
        }
    }
    val words = query.simplified().split(' ').filter { it.isNotBlank() }
    val results = remember(searchText, words) {
        if (words.isEmpty()) emptyList() else catalog.icons.filter { icon -> words.all { it in searchText.getValue(icon) } }
    }
    val chosen = remember(catalog, iconPalette) { iconPalette.mapNotNull { catalog[it] } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
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
            if (editsPalette || chosen.isNotEmpty()) header("yours", resources.getString(R.string.icon_palette_yours), chosen.size)
            if (editsPalette) {
                note("hint", resources.getString(if (chosen.isEmpty()) R.string.icon_palette_empty else R.string.icon_palette_hint))
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

/** Lower case, without accents, for matching search words. */
private fun String.simplified(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
