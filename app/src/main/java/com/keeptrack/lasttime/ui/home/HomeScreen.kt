package com.keeptrack.lasttime.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeptrack.lasttime.R
import com.keeptrack.lasttime.data.Tracker
import com.keeptrack.lasttime.ui.time.rememberCurrentTime
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val now = rememberCurrentTime()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Ids survive rotation; the tracker itself is looked up from the live list.
    var addDialogOpen by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        // collectLatest: a newer reset replaces the snackbar of an older one.
        viewModel.resets.collectLatest { reset ->
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.snackbar_reset, reset.trackerName),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoReset(reset)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addDialogOpen = true }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.action_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val list = trackers
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(Modifier.padding(innerPadding))
            else -> TrackerGrid(
                trackers = list,
                now = now,
                innerPadding = innerPadding,
                onClick = viewModel::markDone,
                onRename = { renamingId = it.id },
                onDelete = { deletingId = it.id },
            )
        }
    }

    if (addDialogOpen) {
        NameDialog(
            title = stringResource(R.string.dialog_add_title),
            initialName = "",
            onConfirm = { viewModel.add(it); addDialogOpen = false },
            onDismiss = { addDialogOpen = false },
        )
    }
    trackers?.find { it.id == renamingId }?.let { tracker ->
        NameDialog(
            title = stringResource(R.string.dialog_rename_title),
            initialName = tracker.name,
            onConfirm = { viewModel.rename(tracker, it); renamingId = null },
            onDismiss = { renamingId = null },
        )
    }
    trackers?.find { it.id == deletingId }?.let { tracker ->
        DeleteDialog(
            name = tracker.name,
            onConfirm = { viewModel.delete(tracker); deletingId = null },
            onDismiss = { deletingId = null },
        )
    }
}

@Composable
private fun TrackerGrid(
    trackers: List<Tracker>,
    now: Instant,
    innerPadding: PaddingValues,
    onClick: (Tracker) -> Unit,
    onRename: (Tracker) -> Unit,
    onDelete: (Tracker) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            // Leave room so the FAB never covers the last row.
            bottom = innerPadding.calculateBottomPadding() + 88.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(trackers, key = { it.id }) { tracker ->
            TrackerCard(
                tracker = tracker,
                now = now,
                onClick = { onClick(tracker) },
                onRename = { onRename(tracker) },
                onDelete = { onDelete(tracker) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
