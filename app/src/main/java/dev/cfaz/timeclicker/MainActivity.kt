package dev.cfaz.timeclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cfaz.timeclicker.ui.groups.GroupsScreen
import dev.cfaz.timeclicker.ui.home.HomeScreen
import dev.cfaz.timeclicker.ui.icons.IconPaletteScreen
import dev.cfaz.timeclicker.ui.settings.SettingsScreen
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme

private enum class Screen { HOME, GROUPS, SETTINGS, ICON_PALETTE }

private const val ScreenMillis = 300
// Material's "shared axis": the old screen fades out quickly, then the new one fades in as both slide.
private const val ScreenFadeOutMillis = 90

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
                var goingBack by rememberSaveable { mutableStateOf(false) }
                // Keeps each screen's saved UI state (scroll position, selected group...) while another is shown.
                val screenStates = rememberSaveableStateHolder()
                fun open(screen: Screen) {
                    goingBack = false
                    stack = stack + screen
                }
                fun back() {
                    goingBack = true
                    // A closed screen starts afresh next time; its state goes once it has animated out.
                    screenStates.removeState(stack.last().name)
                    stack = stack.dropLast(1)
                }

                BackHandler(enabled = stack.size > 1, onBack = ::back)
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                AnimatedContent(
                    targetState = stack.last(),
                    transitionSpec = { screenTransition(forward = !goingBack, rtl = rtl) },
                    // Between the old screen fading out and the new one fading in, show the app's ground,
                    // not the (white) window behind it.
                    modifier = Modifier.fillMaxSize().background(TimeClickerTheme.palette.ground),
                    label = "screen",
                ) { screen ->
                    screenStates.SaveableStateProvider(screen.name) {
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                onManageGroups = { open(Screen.GROUPS) },
                                onOpenSettings = { open(Screen.SETTINGS) },
                            )
                            Screen.GROUPS -> GroupsScreen(onBack = ::back)
                            Screen.SETTINGS -> SettingsScreen(
                                onBack = ::back,
                                onManageGroups = { open(Screen.GROUPS) },
                                onOpenIconPalette = { open(Screen.ICON_PALETTE) },
                            )
                            Screen.ICON_PALETTE -> IconPaletteScreen(onBack = ::back)
                        }
                    }
                }
            }
        }
    }
}

/** Opening a screen slides it in from the end side; going back slides the other way. Mirrored right-to-left. */
private fun screenTransition(forward: Boolean, rtl: Boolean): ContentTransform {
    val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
    val slide = tween<IntOffset>(ScreenMillis, easing = FastOutSlowInEasing)
    val enter = slideInHorizontally(slide) { width -> sign * width / 5 } +
        fadeIn(tween(ScreenMillis - ScreenFadeOutMillis, delayMillis = ScreenFadeOutMillis, easing = LinearOutSlowInEasing))
    val exit = slideOutHorizontally(slide) { width -> -sign * width / 5 } +
        fadeOut(tween(ScreenFadeOutMillis, easing = FastOutSlowInEasing))
    return enter togetherWith exit
}
