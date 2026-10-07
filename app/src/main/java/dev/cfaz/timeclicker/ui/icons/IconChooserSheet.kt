package dev.cfaz.timeclicker.ui.icons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.TileIcon
import dev.cfaz.timeclicker.ui.components.SheetContentScroll
import dev.cfaz.timeclicker.ui.theme.TileColors
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.rememberIconCatalog
import kotlinx.coroutines.launch

/**
 * Every Lucide icon, the palette first, with search: tapping one chooses it for the tile and closes the sheet.
 * The chosen icon is highlighted in the tile's [accent] colours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconChooserSheet(selected: TileIcon, accent: TileColors, onSelect: (TileIcon) -> Unit, onDismiss: () -> Unit) {
    val palette = TimeClickerTheme.palette
    val catalog = rememberIconCatalog()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val contentScroll = remember(gridState) {
        SheetContentScroll { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }
    var query by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = palette.sheet) {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.icon_chooser_title),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            IconSearchField(query, onQueryChange = { query = it })
            if (catalog != null) {
                IconGrid(
                    catalog,
                    query,
                    TimeClickerTheme.settings.iconPalette,
                    isHighlighted = { it == selected },
                    onClick = { icon ->
                        onSelect(icon)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    editsPalette = false,
                    highlight = accent.background,
                    onHighlight = accent.content,
                    bottomPadding = 28.dp,
                    state = gridState,
                    modifier = Modifier.nestedScroll(contentScroll),
                )
            }
        }
    }
}
