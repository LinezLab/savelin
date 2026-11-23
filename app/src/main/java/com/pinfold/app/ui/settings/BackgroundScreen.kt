package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.data.settings.AppSettings
import com.pinfold.app.ui.background.GeneratedBackground
import com.pinfold.app.ui.background.effectiveBackgroundFps
import com.pinfold.app.ui.background.rememberAppInForeground
import com.pinfold.app.ui.background.rememberPowerSaveMode
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun BackgroundScreen(viewModel: HomeViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { GlassSettings.setCustomWallpaper(context, uri) }
    }
    val wallpaper = GlassSettings.customWallpaper

    SettingsScaffold("Background", onBack = viewModel::closeSettingsPage) {
        SettingsCard {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp))) {
                BackgroundPreview(settings, wallpaper)
            }
            SettingsRow(AppIcons.Refetch, Candy.Mint, "Shuffle") { viewModel.setSetting("backgroundHue", (0..360).random().toFloat()) }
            SettingsRow(AppIcons.Undo, Candy.Tomato, "Reset") {
                viewModel.setSetting("backgroundHue", 220f)
                viewModel.setSetting("backgroundStyle", "PETALS")
                viewModel.setSetting("backgroundShape", "SOFT")
                viewModel.setSetting("backgroundSource", "PALETTE")
            }
        }

        SettingsSection("TYPE") {
            SettingsCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("GENERATED" to "Generated", "SOLID" to "Solid colour", "PICTURE" to "Picture").forEach { (value, label) ->
                        ChoiceTile(label, selected = settings.backgroundType == value, onClick = { viewModel.setSetting("backgroundType", value) },
                            preview = {
                                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(
                                    if (value == "SOLID") Color.hsv(settings.backgroundHue, 0.55f, 0.92f) else if (value == "PICTURE") Candy.Grape else Candy.Sky,
                                ))
                            })
                    }
                }
            }
        }

        SettingsSection("STYLE") {
            SettingsCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("PETALS" to "Petals", "AURORA" to "Aurora", "MESH" to "Mesh", "WAVES" to "Waves").forEach { (value, label) ->
                        ChoiceTile(label, selected = settings.backgroundStyle == value, onClick = { viewModel.setSetting("backgroundStyle", value) },
                            preview = {
                                GeneratedBackground(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    loopMs = settings.backgroundLoopSeconds * 1000,
                                    animate = false,
                                    style = value,
                                    hue = settings.backgroundHue,
                                    dim = settings.backgroundDim,
                                    shape = settings.backgroundShape,
                                    source = settings.backgroundSource,
                                    fps = settings.backgroundFps,
                                )
                            })
                    }
                }
                FineSlider("Base hue", settings.backgroundHue, 0f..360f) { viewModel.setSetting("backgroundHue", it) }
            }
        }

        SettingsSection("COLOURS AND SHAPE") {
            SettingsCard {
                SettingsChoiceRow(
                    icon = AppIcons.Appearance,
                    tileColor = Candy.Grape,
                    title = "Colour source",
                    subtitle = settings.backgroundSource,
                    options = listOf("PALETTE", "WALLPAPER"),
                    onSelect = { viewModel.setSetting("backgroundSource", it) },
                )
                SettingsChoiceRow(
                    icon = AppIcons.CustomImage,
                    tileColor = Candy.Sky,
                    title = "Shape",
                    subtitle = settings.backgroundShape,
                    options = listOf("SOFT", "SHARP", "ORGANIC"),
                    onSelect = { viewModel.setSetting("backgroundShape", it) },
                )
                if (settings.backgroundType == "PICTURE") {
                    SettingsRow(AppIcons.CustomImage, Candy.Mint, "Picture source",
                        if (wallpaper != null) "Your picture is set" else "No picture chosen — pick one") {
                        pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
        }

        SettingsSection("MOTION AND READABILITY") {
            SettingsCard {
                SettingsRow(AppIcons.Motion, Candy.Bubblegum, "Animate background", trailing = {
                    GlassSwitch(settings.backgroundAnimate) { viewModel.setSetting("backgroundAnimate", it) }
                })
                FineSlider("Loop length (s)", settings.backgroundLoopSeconds.toFloat(), 8f..120f) {
                    viewModel.setSetting("backgroundLoopSeconds", it.toInt())
                }
                SettingsChoiceRow(
                    icon = AppIcons.Speed,
                    tileColor = Candy.Lemon,
                    title = "Frame rate",
                    subtitle = if (settings.backgroundFps <= 0) "AUTO (30)" else settings.backgroundFps.toString(),
                    options = listOf("AUTO", "15", "30", "60"),
                    onSelect = { label -> viewModel.setSetting("backgroundFps", if (label == "AUTO") 0 else label.toInt()) },
                )
                FineSlider("Dim background", settings.backgroundDim, 0f..0.6f) { viewModel.setSetting("backgroundDim", it) }
                SettingsChoiceRow(
                    icon = AppIcons.Appearance,
                    tileColor = Candy.Grape,
                    title = "Details background",
                    subtitle = settings.backgroundDetails,
                    options = listOf("ARTWORK", "THEME", "GENERATED"),
                    onSelect = { viewModel.setSetting("backgroundDetails", it) },
                )
            }
        }
    }
}

@Composable
private fun BackgroundPreview(settings: AppSettings, wallpaper: androidx.compose.ui.graphics.ImageBitmap?) {
    when (settings.backgroundType) {
        "SOLID" -> Box(Modifier.fillMaxSize().background(Color.hsv(settings.backgroundHue, 0.55f, 0.92f)))
        "PICTURE" -> if (wallpaper != null) {
            Image(wallpaper, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            GeneratedPreview(settings)
        }
        else -> GeneratedPreview(settings)
    }
}

@Composable
private fun GeneratedPreview(settings: AppSettings) {
    val windowVisible = rememberAppInForeground()
    val powerSave = rememberPowerSaveMode()
    val previewAnimate = settings.backgroundAnimate && !settings.reduceMotion
    val previewPaused = !windowVisible || powerSave
    val previewFps = minOf(effectiveBackgroundFps(settings.backgroundFps), 15)
    LaunchedEffect(previewAnimate, previewPaused, previewFps) {
        DiagnosticsLog.uiAction("Background", "preview animate=" + previewAnimate + " paused=" + previewPaused + " fps=" + previewFps)
    }
    GeneratedBackground(
        Modifier.fillMaxSize(),
        loopMs = settings.backgroundLoopSeconds * 1000,
        animate = previewAnimate,
        paused = previewPaused,
        style = settings.backgroundStyle,
        hue = settings.backgroundHue,
        dim = settings.backgroundDim,
        shape = settings.backgroundShape,
        source = settings.backgroundSource,
        fps = previewFps,
    )
}
