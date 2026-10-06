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
import com.keeptrack.timeclicker.ui.groups.GroupsScreen
import com.keeptrack.timeclicker.ui.home.HomeScreen
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme

private enum class Screen { HOME, GROUPS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TimeClickerTheme {
                // Two screens only; move to Navigation Compose if more are added.
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeScreen(onManageGroups = { screen = Screen.GROUPS })
                    Screen.GROUPS -> {
                        BackHandler { screen = Screen.HOME }
                        GroupsScreen(onBack = { screen = Screen.HOME })
                    }
                }
            }
        }
    }
}
