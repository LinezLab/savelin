package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.diagnostics.CrashReporter
import com.pinfold.app.reminders.repeatMinutesFromSetting
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.DiagnosticsToast
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.home.HomeTab
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel, onOpenVault: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var importResult by remember { mutableStateOf<Int?>(null) }
    var vaultHint by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.importBackup(uri) { importResult = it }
    }
    val glass = GlassSettings.style
    val motion = MotionSettings.style

    SettingsScaffold(
        "Settings",
        onBack = { viewModel.selectTab(HomeTab.HOME) },
        topInsets = if (settings.immersiveMode) WindowInsets(0) else WindowInsets.statusBars.union(WindowInsets.displayCutout),
    ) {
        SettingsSection("LOOK AND FEEL") {
            SettingsCard {
                Text("Theme", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = com.pinfold.app.ui.glass.theme.clear.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("AUTO" to "Auto", "LIGHT" to "Light", "DARK" to "Dark").forEach { (value, label) ->
                        ChoiceTile(label = label, selected = settings.themeMode == value, onClick = { viewModel.setSetting("themeMode", value) },
                            preview = { com.pinfold.app.ui.components.AppIcons.Icon(if (value == "DARK") AppIcons.Dark else if (value == "LIGHT") AppIcons.Light else AppIcons.Appearance, null, tint = com.pinfold.app.ui.glass.theme.clear.ink, size = 24.dp) })
                    }
                }
                SettingsRow(AppIcons.Appearance, Candy.Grape, "Liquid glass and motion", "${glass.blur} · ${motion.label}", onClick = viewModel::openGlassMotion)
                SettingsRow(AppIcons.CustomImage, Candy.Bubblegum, "Artwork background", trailing = {
                    GlassSwitch(settings.detailAmbientBackground) { viewModel.setSetting("detailAmbientBackground", it) }
                })
                SettingsChoiceRow(AppIcons.Language, Candy.Sky, "App language", settings.language, listOf("System", "English")) { viewModel.setSetting("language", it) }
                SettingsRow(AppIcons.Haptics, Candy.Mint, "Haptic feedback", trailing = {
                    GlassSwitch(settings.haptics != "OFF") { viewModel.setHaptics(if (it) "LIGHT" else "OFF") }
                })
            }
        }

        SettingsSection("LIBRARY") {
            SettingsCard {
                SettingsChoiceRow(AppIcons.Filter, Candy.Grape, "Columns", settings.cardColumns.toString(), (1..4).map { it.toString() }) { viewModel.setCardColumns(it.toInt()) }
                SettingsChoiceRow(AppIcons.Compact, Candy.Bubblegum, "Card caption", settings.cardCaptionStyle, listOf("BELOW", "GLASS_STRIP", "IMAGE_ONLY")) { viewModel.setCardCaptionStyle(it) }
            }
        }

        SettingsSection("LINKS AND PREVIEWS") {
            SettingsCard {
                SettingsChoiceRow(AppIcons.Biometric, Candy.Grape, "Preview identity", settings.previewIdentity, listOf("MOBILE", "CRAWLER")) { viewModel.setSetting("previewIdentity", it) }
                SettingsRow(AppIcons.Warning, Candy.Tangerine, "Third-party preview helpers", trailing = {
                    GlassSwitch(settings.proxiesEnabled) { viewModel.setProxies(it) }
                })
                SettingsRow(AppIcons.Refetch, Candy.Mint, "Refetch missing previews", trailing = {
                    GlassSwitch(settings.refetchMissing) { viewModel.setSetting("refetchMissing", it) }
                })
            }
        }

        SettingsSection("REMINDERS") {
            SettingsCard {
                SettingsRow(AppIcons.Reminder, Candy.Tangerine, "Notifications", trailing = {
                    GlassSwitch(settings.notifications) { viewModel.setSetting("notifications", it) }
                })
                SettingsChoiceRow(AppIcons.Snooze, Candy.Lime, "Default snooze (min)", settings.defaultSnoozeMinutes.toString(), listOf("5", "15", "30", "60", "120")) { viewModel.setSetting("defaultSnoozeMinutes", it.toInt()) }
                SettingsChoiceRow(
                    AppIcons.Snooze,
                    Candy.Lime,
                    "Repeat unopened",
                    if (settings.reminderRepeatMinutes == 0) "OFF" else settings.reminderRepeatMinutes.toString(),
                    listOf("OFF", "5", "15", "30", "60"),
                ) { viewModel.setSetting("reminderRepeatMinutes", repeatMinutesFromSetting(it)) }
                SettingsRow(AppIcons.Hide, Candy.Grape, "Hide details on lock screen", trailing = {
                    GlassSwitch(settings.hideDetailsOnLockScreen) { viewModel.setHideDetailsOnLockScreen(it) }
                })
            }
        }

        SettingsSection("PRIVACY AND SECURITY") {
            SettingsCard {
                SettingsRow(AppIcons.Lock, Candy.Sky, "App lock", trailing = {
                    GlassSwitch(settings.biometricLock) { viewModel.setBiometricLock(it) }
                })
                SettingsRow(AppIcons.Hide, Candy.Grape, "Private vault", subtitle = "Long press to open",
                    onLongClick = onOpenVault, onClick = { vaultHint = true })
                SettingsRow(AppIcons.Warning, Candy.Tomato, "Block screenshots", trailing = {
                    GlassSwitch(settings.blockScreenshots) { viewModel.setSetting("blockScreenshots", it) }
                })
                SettingsRow(AppIcons.Paste, Candy.Mint, "Clipboard link suggestion", trailing = {
                    GlassSwitch(settings.enableClipboardChip) { viewModel.setEnableClipboardChip(it) }
                })
                SettingsRow(AppIcons.Offline, Candy.Tangerine, "Offline mode", trailing = {
                    GlassSwitch(settings.offlineMode) { viewModel.setOffline(it) }
                })
            }
        }

        SettingsSection("DATA") {
            SettingsCard {
                SettingsRow(AppIcons.Export, Candy.Sky, "Backup and export") { exportLauncher.launch("sonil-backup.zip") }
                SettingsRow(AppIcons.Import, Candy.Mint, "Import") { importLauncher.launch("application/zip") }
                importResult?.let { Text("Imported $it new bookmarks", color = com.pinfold.app.ui.glass.theme.clear.ink) }
            }
        }

        SettingsSection("ACCESSIBILITY") {
            SettingsCard {
                SettingsRow(AppIcons.Motion, Candy.Bubblegum, "Reduce motion", trailing = {
                    GlassSwitch(settings.reduceMotion) { viewModel.setReduceMotion(it) }
                })
                SettingsRow(AppIcons.Appearance, Candy.Sky, "Reduce transparency", trailing = {
                    GlassSwitch(settings.reduceTransparency) { viewModel.setReduceTransparency(it) }
                })
                SettingsChoiceRow(AppIcons.Tags, Candy.Lemon, "Icon labels", settings.iconLabels, listOf("ICONS_ONLY", "SELECTED", "ALWAYS")) { viewModel.setSetting("iconLabels", it) }
                SettingsRow(AppIcons.Expand, Candy.Tangerine, "Immersive mode", trailing = {
                    GlassSwitch(settings.immersiveMode) { viewModel.setSetting("immersiveMode", it) }
                })
            }
        }

        SettingsSection("ABOUT") {
            SettingsCard {
                SettingsRow(AppIcons.Licences, Candy.Sky, "Open source licences")
                SettingsRow(AppIcons.Lock, Candy.Mint, "Privacy promises")
                SettingsRow(AppIcons.Info, Candy.Grape, "Version", CrashReporter.appVersionLabel(context))
            }
        }
    }
    if (vaultHint) {
        DiagnosticsToast("Long press to open the private vault", onDone = { vaultHint = false })
    }
}
