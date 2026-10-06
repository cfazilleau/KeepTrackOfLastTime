package com.keeptrack.timeclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keeptrack.timeclicker.ui.groups.GroupsScreen
import com.keeptrack.timeclicker.ui.home.HomeScreen
import com.keeptrack.timeclicker.ui.settings.SettingsScreen
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme

private enum class Screen { HOME, GROUPS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsRepository = (application as TimeClickerApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            TimeClickerTheme(settings) {
                // A few screens only; move to Navigation Compose if more are added.
                var stack by rememberSaveable { mutableStateOf(listOf(Screen.HOME)) }
                fun open(screen: Screen) { stack = stack + screen }
                fun back() { stack = stack.dropLast(1) }

                BackHandler(enabled = stack.size > 1, onBack = ::back)
                when (stack.last()) {
                    Screen.HOME -> HomeScreen(
                        onManageGroups = { open(Screen.GROUPS) },
                        onOpenSettings = { open(Screen.SETTINGS) },
                    )
                    Screen.GROUPS -> GroupsScreen(onBack = ::back)
                    Screen.SETTINGS -> SettingsScreen(onBack = ::back, onManageGroups = { open(Screen.GROUPS) })
                }
            }
        }
    }
}
