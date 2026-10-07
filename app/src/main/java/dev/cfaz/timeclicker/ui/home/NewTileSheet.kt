package dev.cfaz.timeclicker.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.ui.components.NeuTextField
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.launch

/**
 * Quick creation of a tile: just its name, focused on open, created with Enter. The colour and the rest
 * are picked for it; a long press on the tile opens [EditTileSheet] to change them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTileSheet(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = TimeClickerTheme.palette
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var created by remember { mutableStateOf(false) }

    fun create() {
        // Once only, though Enter may be pressed again while the sheet slides away.
        if (name.isBlank() || created) return
        created = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { onCreate(name) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = palette.sheet) {
        // Kept above the keyboard.
        Box(Modifier.fillMaxWidth().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            NeuTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.sheet_new_title),
                placeholder = stringResource(R.string.name_placeholder),
                onDone = ::create,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        // In the sheet's content, so the field is attached when focused.
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}
