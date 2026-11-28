package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.pinfold.app.ui.components.iosHorizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinfold.app.diagnostics.CrashReporter
import com.pinfold.app.reminders.repeatMinutesFromSetting
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.PullCord
import com.pinfold.app.ui.detail.rememberTextShimmerBrush
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassPill
import com.pinfold.app.ui.glass.GlassSlider
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeTab
import com.pinfold.app.ui.home.HomeViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var importResult by remember { mutableStateOf<Int?>(null) }
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
                if (settings.pullCordEnabled) {
                    PullCord(
                        mode = settings.themeMode,
                        onModeChange = { viewModel.setSetting("themeMode", it) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        across = pullCordAcross(settings.pullCordPosition),
                        reduceMotion = settings.reduceMotion,
                    )
                    Text(
                        "Pull the cord or use:",
                        style = MaterialTheme.typography.labelSmall,
                        color = clear.onGlassSoft,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().iosHorizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickTogglePill("Dark", settings.themeMode == "DARK") {
                            viewModel.setSetting("themeMode", if (settings.themeMode == "DARK") "LIGHT" else "DARK")
                        }
                        QuickTogglePill("Notify", settings.notifications) {
                            viewModel.setSetting("notifications", !settings.notifications)
                        }
                        QuickTogglePill("Sound", settings.haptics != "OFF") {
                            viewModel.setHaptics(if (settings.haptics != "OFF") "OFF" else "LIGHT")
                        }
                        QuickTogglePill("Preview", settings.refetchMissing) {
                            viewModel.setSetting("refetchMissing", !settings.refetchMissing)
                        }
                    }
                }
                SettingsRow(AppIcons.Appearance, Candy.Lemon, "Pull cord theme switch", trailing = {
                    GlassSwitch(settings.pullCordEnabled) { viewModel.setSetting("pullCordEnabled", it) }
                })
                SettingsChoiceRow(AppIcons.Appearance, Candy.Sky, "Pull cord position", settings.pullCordPosition, listOf("LEFT", "CENTER", "RIGHT")) { viewModel.setSetting("pullCordPosition", it) }
                SettingsChoiceRow(AppIcons.Appearance, Candy.Grape, "Theme", settings.themeMode, listOf("AUTO", "LIGHT", "DARK")) { viewModel.setSetting("themeMode", it) }
                SettingsRow(AppIcons.Appearance, Candy.Lemon, "Shimmer effects", trailing = {
                    GlassSwitch(settings.shimmerEnabled) { viewModel.setSetting("shimmerEnabled", it) }
                })
                if (settings.shimmerEnabled) {
                    SettingsChoiceRow(
                        AppIcons.Appearance,
                        Candy.Mint,
                        "Shimmer colour",
                        settings.shimmerColor,
                        listOf("LEMON", "GREY", "WHITE", "BLACK", "DARK_WHITE_GREY", "CUSTOM"),
                    ) { viewModel.setSetting("shimmerColor", it) }
                    if (settings.shimmerColor == "CUSTOM") {
                        ShimmerColourCustomizer(
                            argb = settings.shimmerCustomColor,
                            reduceMotion = settings.reduceMotion,
                            onChange = { viewModel.setSetting("shimmerCustomColor", it) },
                        )
                    }
                    SettingsChoiceRow(AppIcons.Appearance, Candy.Sky, "Shimmer speed", settings.shimmerSpeed, listOf("SLOW", "NORMAL", "FAST")) { viewModel.setSetting("shimmerSpeed", it) }
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
                SettingsRow(AppIcons.Hide, Candy.Grape, "Private vault",
                    subtitle = "Show private items in Home",
                    trailing = { GlassSwitch(settings.privateVault) { viewModel.setSetting("privateVault", it) } })
                SettingsRow(AppIcons.Hide, Candy.Grape, "Long press Settings to open vault",
                    subtitle = "Off: long press opens Settings",
                    trailing = { GlassSwitch(settings.longPressSettingsVault) { viewModel.setSetting("longPressSettingsVault", it) } })
                SettingsRow(AppIcons.Warning, Candy.Tomato, "Block screenshots", trailing = {
                    GlassSwitch(settings.blockScreenshots) { viewModel.setSetting("blockScreenshots", it) }
                })
                SettingsRow(AppIcons.Paste, Candy.Mint, "Clipboard link suggestion", trailing = {
                    GlassSwitch(settings.enableClipboardChip) { viewModel.setEnableClipboardChip(it) }
                })
                SettingsRow(AppIcons.Hide, Candy.Grape, "Save links privately by default", trailing = {
                    GlassSwitch(settings.saveLinksPrivateByDefault) {
                        viewModel.setSetting("saveLinksPrivateByDefault", it)
                    }
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
}

@Composable
private fun QuickTogglePill(label: String, active: Boolean, onClick: () -> Unit) {
    GlassPill(
        onClick = onClick,
        height = 36.dp,
        horizontalPadding = 12.dp,
        label = "$label settings toggle",
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = clear.ink,
            modifier = Modifier.alpha(if (active) 1f else 0.45f),
        )
    }
}

@Composable
private fun ShimmerColourCustomizer(
    argb: Long,
    reduceMotion: Boolean,
    onChange: (Long) -> Unit,
) {
    val red = (argb shr 16).toInt() and 0xFF
    val green = (argb shr 8).toInt() and 0xFF
    val blue = argb.toInt() and 0xFF
    fun packed(r: Int, g: Int, b: Int): Long =
        ((argb and 0xFF000000L) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()) and 0xFFFFFFFFL

    Text(
        "Custom colour",
        style = MaterialTheme.typography.labelMedium,
        color = clear.onGlassSoft,
        modifier = Modifier.padding(top = 10.dp),
    )
    ColorChannelSlider("Red", red, Candy.Tangerine) { onChange(packed(it, green, blue)) }
    ColorChannelSlider("Green", green, Candy.Mint) { onChange(packed(red, it, blue)) }
    ColorChannelSlider("Blue", blue, Candy.Sky) { onChange(packed(red, green, it)) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(Capsule).background(Color(argb.toInt())),
        )
        ShimmerColourPreview(argb, reduceMotion)
    }
}

@Composable
private fun ColorChannelSlider(label: String, value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text("$label: $value", style = MaterialTheme.typography.bodyMedium, color = clear.onGlass)
        GlassSlider(
            value = value.toFloat(),
            onChange = { onChange(it.roundToInt()) },
            range = 0f..255f,
            accent = accent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ShimmerColourPreview(argb: Long, reduceMotion: Boolean) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val layout = remember(measurer, style) { measurer.measure("Preview", style = style) }
    val brush = rememberTextShimmerBrush(
        clear.onGlass,
        layout.size.width.toFloat(),
        reduceMotion,
        enabled = true,
        color = "CUSTOM",
        customArgb = argb,
    )
    val density = LocalDensity.current
    val width = with(density) { layout.size.width.toDp() }
    val height = with(density) { layout.size.height.toDp() }
    Canvas(
        Modifier.size(width, height),
    ) {
        drawText(layout, brush = brush)
    }
}
