package dev.cfaz.timeclicker.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cfaz.timeclicker.R
import dev.cfaz.timeclicker.data.AppSettings
import dev.cfaz.timeclicker.data.ThemeMode
import dev.cfaz.timeclicker.data.TimeDisplay
import dev.cfaz.timeclicker.ui.components.NeuIconButton
import dev.cfaz.timeclicker.ui.components.SegmentedControl
import dev.cfaz.timeclicker.ui.home.ConfirmDialog
import dev.cfaz.timeclicker.ui.theme.AppIcons
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.raised
import dev.cfaz.timeclicker.ui.theme.supportsDynamicColor
import java.time.LocalDate

/** The app's website, shown under the support text. */
private const val WEBSITE_HOST = "timeclicker.cfaz.dev"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageGroups: () -> Unit,
    onOpenIconPalette: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val palette = TimeClickerTheme.palette
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                BackupMessage.Exported -> resources.getString(R.string.export_done)
                BackupMessage.ExportFailed -> resources.getString(R.string.export_failed)
                is BackupMessage.Imported -> resources.getString(
                    R.string.import_done,
                    resources.getQuantityString(R.plurals.tiles_count, message.tiles, message.tiles),
                )
                BackupMessage.ImportFailed -> resources.getString(R.string.import_failed)
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) = viewModel.update(transform)

    Box(Modifier.fillMaxSize().background(palette.ground)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NeuIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack, size = 44.dp, shape = RoundedCornerShape(15.dp))
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = palette.text)
            }
            if (busy) {
                LinearProgressIndicator(
                    color = palette.accent,
                    trackColor = palette.field,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(CircleShape),
                )
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
                Section(stringResource(R.string.settings_section_appearance)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, color = palette.text)
                        SegmentedControl(
                            options = ThemeMode.entries,
                            selected = settings.theme,
                            label = {
                                stringResource(
                                    when (it) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }
                                )
                            },
                            onSelect = { mode -> update { it.copy(theme = mode) } },
                        )
                    }
                    // Material You colours exist from Android 12.
                    if (supportsDynamicColor) {
                        RowDivider()
                        SwitchRow(
                            title = stringResource(R.string.settings_system_colors),
                            hint = stringResource(R.string.settings_system_colors_hint),
                            checked = settings.dynamicColors,
                            onCheckedChange = { on -> update { it.copy(dynamicColors = on) } },
                        )
                    }
                }

                Section(stringResource(R.string.settings_section_tiles)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RowTexts(stringResource(R.string.settings_time_display), stringResource(R.string.settings_time_display_hint))
                        SegmentedControl(
                            options = TimeDisplay.entries,
                            selected = settings.timeDisplay,
                            label = {
                                stringResource(if (it == TimeDisplay.RELATIVE) R.string.time_display_relative else R.string.time_display_absolute)
                            },
                            onSelect = { display -> update { it.copy(timeDisplay = display) } },
                        )
                    }
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_haptics),
                        hint = stringResource(R.string.settings_haptics_hint),
                        checked = settings.haptics,
                        onCheckedChange = { on -> update { it.copy(haptics = on) } },
                    )
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_click_sound),
                        hint = stringResource(R.string.settings_click_sound_hint),
                        checked = settings.clickSound,
                        onCheckedChange = { on -> update { it.copy(clickSound = on) } },
                    )
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_counter),
                        hint = stringResource(R.string.settings_counter_hint),
                        checked = settings.showCounter,
                        onCheckedChange = { on -> update { it.copy(showCounter = on) } },
                    )
                    RowDivider()
                    ActionRow(
                        icon = AppIcons.Palette,
                        title = stringResource(R.string.settings_icon_palette),
                        hint = stringResource(R.string.settings_icon_palette_hint),
                        onClick = onOpenIconPalette,
                    )
                }

                Section(stringResource(R.string.settings_section_general)) {
                    ActionRow(
                        icon = AppIcons.Groups,
                        title = stringResource(R.string.action_manage_groups),
                        hint = stringResource(R.string.settings_groups_hint),
                        onClick = onManageGroups,
                    )
                    // The per-app language setting exists from Android 13.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        RowDivider()
                        ActionRow(
                            icon = AppIcons.Language,
                            title = stringResource(R.string.settings_language),
                            hint = stringResource(R.string.settings_language_hint),
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri())
                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    // Some devices hide it; nothing else to offer.
                                }
                            },
                        )
                    }
                }

                Section(stringResource(R.string.settings_section_data)) {
                    ActionRow(
                        icon = AppIcons.Export,
                        title = stringResource(R.string.settings_export),
                        hint = stringResource(R.string.settings_export_hint),
                        enabled = !busy,
                        onClick = { exportLauncher.launch("time-clicker-${LocalDate.now()}.zip") },
                    )
                    RowDivider()
                    ActionRow(
                        icon = AppIcons.Import,
                        title = stringResource(R.string.settings_import),
                        hint = stringResource(R.string.settings_import_hint),
                        enabled = !busy,
                        onClick = { confirmImport = true },
                    )
                }

                SupportSection(
                    onOpenWebsite = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://$WEBSITE_HOST".toUri()))
                        } catch (e: ActivityNotFoundException) {
                            // No browser.
                        }
                    },
                )

                CreditsSection()

                Text(
                    stringResource(R.string.settings_version, appVersion(context)),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(18.dp),
                containerColor = palette.toastBackground,
                contentColor = palette.toastContent,
                actionColor = palette.toastAction,
            )
        }
    }

    if (confirmImport) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_import_title),
            body = stringResource(R.string.dialog_import_body),
            confirmLabel = stringResource(R.string.settings_import),
            onConfirm = {
                confirmImport = false
                // Some file managers label .zip files differently; the content is checked on import anyway.
                importLauncher.launch(arrayOf(BACKUP_MIME, "application/x-zip-compressed", "application/octet-stream"))
            },
            onDismiss = { confirmImport = false },
        )
    }
}

private const val BACKUP_MIME = "application/zip"

private fun appVersion(context: android.content.Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/** A titled group of rows on a raised card. */
@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    val palette = TimeClickerTheme.palette
    val shape = RoundedCornerShape(24.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = palette.muted,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .raised(shape, palette.shadow, palette.highlight, distance = 6.dp, blur = 16.dp)
                .clip(shape)
                .background(palette.ground),
        ) { content() }
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = TimeClickerTheme.palette.field)
}

@Composable
internal fun RowTexts(title: String, hint: String?, modifier: Modifier = Modifier) {
    val palette = TimeClickerTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = TimeClickerTheme.palette
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowTexts(title, hint, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null, // the whole row toggles
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onAccent,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.muted,
                uncheckedTrackColor = palette.field,
                uncheckedBorderColor = palette.muted,
            ),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, hint: String?, onClick: () -> Unit, enabled: Boolean = true) {
    val palette = TimeClickerTheme.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(palette.field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = palette.text, modifier = Modifier.size(20.dp))
        }
        RowTexts(title, hint, Modifier.weight(1f))
        Icon(AppIcons.Chevron, null, tint = palette.muted, modifier = Modifier.size(18.dp))
    }
}

/** Why the app is free, and a link to its website. */
@Composable
private fun SupportSection(onOpenWebsite: () -> Unit) {
    val palette = TimeClickerTheme.palette
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.support_text),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            Modifier
                .height(48.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .clickable(role = Role.Button, onClick = onOpenWebsite)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(AppIcons.ExternalLink, null, tint = palette.onAccent, modifier = Modifier.size(18.dp))
            Text(WEBSITE_HOST, style = MaterialTheme.typography.labelLarge, color = palette.onAccent)
        }
        Spacer(Modifier.height(4.dp))
    }
}
