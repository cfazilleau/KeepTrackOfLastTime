package com.keeptrack.timeclicker.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.keeptrack.timeclicker.R
import com.keeptrack.timeclicker.ui.theme.TimeClickerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/** The open-source projects the app is built with; tapping one shows its licence. */
@Composable
internal fun CreditsSection() {
    var shown by remember { mutableStateOf<Credit?>(null) }
    Section(stringResource(R.string.settings_credits)) {
        Text(
            stringResource(R.string.credits_intro),
            style = MaterialTheme.typography.bodySmall,
            color = TimeClickerTheme.palette.muted,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        )
        credits.forEachIndexed { index, credit ->
            if (index > 0) RowDivider()
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { shown = credit }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RowTexts(credit.name, stringResource(credit.usedFor))
                Text(credit.licence, style = MaterialTheme.typography.labelSmall, color = TimeClickerTheme.palette.muted)
            }
        }
    }
    shown?.let { LicenceDialog(it, onDismiss = { shown = null }) }
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
