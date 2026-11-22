package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.home.HomeTab
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val networkLog by viewModel.networkLog.collectAsState()
    var importResult by remember { mutableStateOf<Int?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.importBackup(uri) { importResult = it }
    }
    val glass = GlassSettings.style
    val motion = MotionSettings.style

    SettingsScaffold("Settings", onBack = { viewModel.selectTab(HomeTab.HOME) }) {
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
                SettingsRow(AppIcons.CustomImage, Candy.Mint, "Background", "${settings.backgroundStyle} · ${settings.backgroundLoopSeconds}s loop", onClick = viewModel::openBackground)
                SettingsRow(AppIcons.Language, Candy.Sky, "App language", settings.language, onClick = { viewModel.setSetting("language", "English") })
                SettingsRow(AppIcons.Haptics, Candy.Mint, "Haptic feedback", trailing = {
                    GlassSwitch(settings.haptics != "OFF") { viewModel.setHaptics(if (it) "LIGHT" else "OFF") }
                })
            }
        }

        SettingsSection("LIBRARY") {
            SettingsCard {
                SettingsRow(AppIcons.Card, Candy.Sky, "Default view", settings.defaultView) { viewModel.setSetting("defaultView", if (settings.defaultView == "CARD") "COMPACT" else "CARD") }
                SettingsRow(AppIcons.Filter, Candy.Grape, "Columns", settings.cardColumns.toString()) {
                    viewModel.setCardColumns(if (settings.cardColumns >= 4) 1 else settings.cardColumns + 1)
                }
                SettingsRow(AppIcons.Compact, Candy.Bubblegum, "Card caption", settings.cardCaptionStyle) {
                    val values = listOf("BELOW", "GLASS_STRIP", "IMAGE_ONLY")
                    viewModel.setCardCaptionStyle(values[(values.indexOf(settings.cardCaptionStyle) + 1) % values.size])
                }
                SettingsRow(AppIcons.Select, Candy.Tangerine, "Tap on a bookmark", settings.tapAction) {
                    val values = listOf("OPEN", "DETAILS", "ASK")
                    viewModel.setSetting("tapAction", values[(values.indexOf(settings.tapAction) + 1) % values.size])
                }
                SettingsRow(AppIcons.Sort, Candy.Lime, "Sort order", settings.sortOrder) {
                    viewModel.setSetting("sortOrder", if (settings.sortOrder == "NEWEST") "OLDEST" else "NEWEST")
                }
                SettingsRow(AppIcons.Delete, Candy.Tomato, "Trash retention", "${settings.trashRetentionDays} days") {
                    viewModel.setSetting("trashRetentionDays", 30)
                }
            }
        }

        SettingsSection("LINKS AND PREVIEWS") {
            SettingsCard {
                SettingsRow(AppIcons.Open, Candy.Sky, "Open links in", settings.openLinksIn) {
                    viewModel.setSetting("openLinksIn", if (settings.openLinksIn == "NATIVE") "BROWSER" else "NATIVE")
                }
                SettingsRow(AppIcons.Biometric, Candy.Grape, "Preview identity", settings.previewIdentity) {
                    viewModel.setSetting("previewIdentity", if (settings.previewIdentity == "MOBILE") "CRAWLER" else "MOBILE")
                }
                SettingsRow(AppIcons.Warning, Candy.Tangerine, "Third-party preview helpers", trailing = {
                    GlassSwitch(settings.previewHelpers) { viewModel.setProxies(it) }
                })
                SettingsRow(AppIcons.Refetch, Candy.Mint, "Refetch missing previews", trailing = {
                    GlassSwitch(settings.refetchMissing) { viewModel.setSetting("refetchMissing", it) }
                })
                SettingsRow(AppIcons.CustomImage, Candy.Bubblegum, "Image quality", settings.imageQuality) {
                    val values = listOf("QUALITY", "BALANCED", "DATA_SAVER")
                    viewModel.setSetting("imageQuality", values[(values.indexOf(settings.imageQuality) + 1) % values.size])
                }
            }
        }

        SettingsSection("REMINDERS") {
            SettingsCard {
                SettingsRow(AppIcons.Reminder, Candy.Tangerine, "Notifications", trailing = {
                    GlassSwitch(settings.notifications) { viewModel.setSetting("notifications", it) }
                })
                SettingsRow(AppIcons.Snooze, Candy.Lime, "Default snooze", "${settings.defaultSnoozeMinutes} min") {
                    viewModel.setSetting("defaultSnoozeMinutes", 60)
                }
                SettingsRow(AppIcons.Hide, Candy.Grape, "Hide details on lock screen", trailing = {
                    GlassSwitch(settings.hideDetailsOnLockScreen) { viewModel.setSetting("hideDetailsOnLockScreen", it) }
                })
            }
        }

        SettingsSection("PRIVACY AND SECURITY") {
            SettingsCard {
                SettingsRow(AppIcons.Lock, Candy.Sky, "App lock", trailing = {
                    GlassSwitch(settings.biometricLock) { viewModel.setBiometricLock(it) }
                })
                SettingsRow(AppIcons.Hide, Candy.Grape, "Private vault", trailing = {
                    GlassSwitch(settings.privateVault) { viewModel.setSetting("privateVault", it) }
                })
                SettingsRow(AppIcons.Warning, Candy.Tomato, "Block screenshots", trailing = {
                    GlassSwitch(settings.blockScreenshots) { viewModel.setSetting("blockScreenshots", it) }
                })
                SettingsRow(AppIcons.Paste, Candy.Mint, "Clipboard link suggestion", trailing = {
                    GlassSwitch(settings.enableClipboardChip) { viewModel.setSetting("enableClipboardChip", it) }
                })
                SettingsRow(AppIcons.Offline, Candy.Tangerine, "Offline mode", trailing = {
                    GlassSwitch(settings.offlineMode) { viewModel.setOffline(it) }
                })
                SettingsRow(AppIcons.NetworkLog, Candy.Lime, "Network transparency log", "${networkLog.size} recent")
            }
        }

        SettingsSection("DATA") {
            SettingsCard {
                SettingsRow(AppIcons.Drive, Candy.Mint, "Google Drive sync", if (settings.driveSyncEnabled) "On" else "Off", onClick = viewModel::openDriveSync)
                SettingsRow(AppIcons.Export, Candy.Sky, "Backup and export") { exportLauncher.launch("savelin-backup.zip") }
                SettingsRow(AppIcons.Import, Candy.Mint, "Import") { importLauncher.launch("application/zip") }
                importResult?.let { Text("Imported $it new bookmarks", color = com.pinfold.app.ui.glass.theme.clear.ink) }
            }
        }

        SettingsSection("ACCESSIBILITY AND PERFORMANCE") {
            SettingsCard {
                SettingsRow(AppIcons.Motion, Candy.Bubblegum, "Reduce motion", trailing = {
                    GlassSwitch(settings.reduceMotion) { viewModel.setReduceMotion(it) }
                })
                SettingsRow(AppIcons.Appearance, Candy.Sky, "Reduce transparency", trailing = {
                    GlassSwitch(settings.reduceTransparency) { viewModel.setReduceTransparency(it) }
                })
                SettingsRow(AppIcons.Tags, Candy.Lemon, "Icon labels", settings.iconLabels) {
                    val values = listOf("ICONS_ONLY", "SELECTED", "ALWAYS")
                    viewModel.setSetting("iconLabels", values[(values.indexOf(settings.iconLabels) + 1) % values.size])
                }
                SettingsRow(AppIcons.Motion, Candy.Grape, "Performance mode", settings.performanceMode) {
                    val values = listOf("AUTO", "QUALITY", "BALANCED", "BATTERY")
                    viewModel.setSetting("performanceMode", values[(values.indexOf(settings.performanceMode) + 1) % values.size])
                }
                SettingsRow(AppIcons.Expand, Candy.Tangerine, "Immersive mode", trailing = {
                    GlassSwitch(settings.immersiveMode) { viewModel.setSetting("immersiveMode", it) }
                })
                SettingsRow(AppIcons.NetworkLog, Candy.Lime, "Show frame time overlay", trailing = {
                    GlassSwitch(settings.frameOverlay) { viewModel.setFrameOverlay(it) }
                })
            }
        }

        SettingsSection("ABOUT") {
            SettingsCard {
                SettingsRow(AppIcons.Licences, Candy.Sky, "Open source licences")
                SettingsRow(AppIcons.Lock, Candy.Mint, "Privacy promises")
                SettingsRow(AppIcons.Info, Candy.Grape, "Version", "1.1.0")
            }
        }
    }
}
