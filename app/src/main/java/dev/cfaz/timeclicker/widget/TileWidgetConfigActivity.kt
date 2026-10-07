package dev.cfaz.timeclicker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.TimeClickerApplication
import dev.cfaz.timeclicker.ui.home.BentoColumns
import dev.cfaz.timeclicker.ui.home.TileCard
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.launch

/** Picks the tile a widget shows: when the widget is added, and when it is reconfigured. */
class TileWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Backing out cancels adding the widget.
        setResult(RESULT_CANCELED, result)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = trackerRepository
        val settingsRepository = (application as TimeClickerApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsState()
            TimeClickerTheme(settings) {
                val palette = TimeClickerTheme.palette
                val trackers by remember { repository.observeTrackers() }.collectAsState(initial = null)
                val scope = rememberCoroutineScope()
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(palette.ground)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                ) {
                    Text(
                        stringResource(R.string.widget_config_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
                    )
                    val list = trackers
                    if (list != null && list.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.widget_config_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.muted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else if (list != null) {
                        LazyVerticalGrid(
                            columns = BentoColumns,
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 22.dp,
                                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(list, key = { it.id }) { tracker ->
                                TileCard(
                                    tracker = tracker,
                                    photoFile = repository::photoFile,
                                    onClickLabel = stringResource(R.string.widget_config_pick),
                                    onClick = {
                                        scope.launch {
                                            TileWidgets.bind(this@TileWidgetConfigActivity, appWidgetId, tracker.id)
                                            setResult(RESULT_OK, result)
                                            finish()
                                        }
                                    },
                                    onLongClick = null,
                                    modifier = Modifier.height(156.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
