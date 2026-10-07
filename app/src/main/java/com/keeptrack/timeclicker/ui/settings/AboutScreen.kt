package com.keeptrack.timeclicker.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.ui.components.NeuIconButton
import com.keeptrack.timeclicker.ui.theme.AppIcons
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where the "Support us" button leads. */
private const val DONATE_URL = "https://ko-fi.com/cfaz"

/** A third-party project the app is built with. [licenceAsset] is the licence's full text, in the app's assets. */
private class Credit(
    val name: String,
    @StringRes val usedFor: Int,
    val licence: String,
    val licenceAsset: String,
    val url: String,
)

private const val APACHE = "licenses/apache-2.0.txt"

/** Keep in step with the dependencies in app/build.gradle.kts and the bundled assets. */
private val credits = listOf(
    Credit("Lucide", R.string.credit_icons, "ISC · MIT (Feather)", "lucide/LICENSE", "https://lucide.dev"),
    Credit(
        "Jetpack Compose & Material 3",
        R.string.credit_ui,
        "Apache 2.0",
        APACHE,
        "https://developer.android.com/compose",
    ),
    Credit(
        "AndroidX Core, Activity & Lifecycle",
        R.string.credit_androidx,
        "Apache 2.0",
        APACHE,
        "https://developer.android.com/jetpack/androidx",
    ),
    Credit("Room", R.string.credit_database, "Apache 2.0", APACHE, "https://developer.android.com/training/data-storage/room"),
    Credit("Glance", R.string.credit_widgets, "Apache 2.0", APACHE, "https://developer.android.com/develop/ui/compose/glance"),
    Credit("Coil", R.string.credit_photos, "Apache 2.0", APACHE, "https://coil-kt.github.io/coil/"),
    Credit("Reorderable", R.string.credit_reorder, "Apache 2.0", APACHE, "https://github.com/Calvin-LL/Reorderable"),
    Credit("Kotlin & kotlinx.coroutines", R.string.credit_kotlin, "Apache 2.0", APACHE, "https://kotlinlang.org"),
)

/** The app's version, a way to support it, and the open-source projects it is built with (tap one for its licence). */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = TimeClickerTheme.palette
    val context = LocalContext.current
    var shown by remember { mutableStateOf<Credit?>(null) }
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
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineSmall, color = palette.text)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 40.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Section(title = null) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = palette.text)
                    Text(
                        stringResource(R.string.settings_version, appVersion(context)),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.muted,
                    )
                    SupportUs(
                        onDonate = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, DONATE_URL.toUri()))
                            } catch (e: ActivityNotFoundException) {
                                // No browser.
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

            Section(stringResource(R.string.credits_title)) {
                Text(
                    stringResource(R.string.credits_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                )
                credits.forEach { credit ->
                    RowDivider()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { shown = credit }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        RowTexts(credit.name, stringResource(credit.usedFor))
                        Text(credit.licence, style = MaterialTheme.typography.labelSmall, color = palette.muted)
                    }
                }
            }
        }
    }
    shown?.let { LicenceDialog(it, onDismiss = { shown = null }) }
}

private fun appVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/** Why the app is free, and a way to chip in. */
@Composable
private fun SupportUs(onDonate: () -> Unit, modifier: Modifier = Modifier) {
    val palette = TimeClickerTheme.palette
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.support_text),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier
                .height(48.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .clickable(role = Role.Button, onClick = onDonate)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(AppIcons.Heart, null, tint = palette.onAccent, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_donate), style = MaterialTheme.typography.labelLarge, color = palette.onAccent)
        }
    }
}

@Composable
private fun LicenceDialog(credit: Credit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text by produceState("", credit) {
        value = withContext(Dispatchers.IO) {
            context.assets.open(credit.licenceAsset).bufferedReader().use { it.readText() }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(credit.name) },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, credit.url.toUri()))
                    } catch (e: ActivityNotFoundException) {
                        // No browser.
                    }
                },
            ) { Text(stringResource(R.string.credits_website)) }
        },
    )
}
