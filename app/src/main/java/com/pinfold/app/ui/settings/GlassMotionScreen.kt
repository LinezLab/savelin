package com.pinfold.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassDropdown
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassStyle
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.MotionStyle
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun GlassMotionScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var glassFine by remember { mutableStateOf(false) }
    var motionFine by remember { mutableStateOf(false) }
    val style = GlassSettings.style
    val motion = MotionSettings.style

    SettingsScaffold("Liquid glass & motion", onBack = viewModel::closeSettingsPage) {
        SettingsCard {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassChip(onClick = {}, height = 40.dp) { Text("Press me", color = clear.ink) }
                GlassCircle(onClick = {}, size = 40.dp, label = "Preview") {
                    AppIcons.Icon(AppIcons.Normal, null, tint = clear.ink)
                }
                GlassChip(onClick = {}, height = 40.dp) { Text("Outlined chip", color = clear.ink) }
            }
            Text(
                "These buttons move the way you set them below. Press and hold them.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = clear.inkSoft,
            )
            SettingsRow(icon = AppIcons.Haptics, tileColor = Candy.Mint, title = "Touch and hold effects", trailing = {
                GlassSwitch(checked = settings.touchHoldEffect, onChange = { viewModel.setSetting("touchHoldEffect", it) })
            })
        }

        SettingsSection("GLASS") {
            SettingsCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassStyle.Named.forEach { (name, preset) ->
                        ChoiceTile(
                            label = name,
                            selected = style == preset,
                            onClick = { GlassSettings.update(context, preset) },
                            preview = {
                                androidx.compose.foundation.layout.Box(
                                    Modifier.fillMaxWidth().height(64.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center,
                                ) {
                                    AppIcons.Icon(AppIcons.Appearance, null, tint = clear.ink, size = 24.dp)
                                }
                            },
                        )
                    }
                }
                ExpandableRow(title = "Fine tune glass", icon = AppIcons.Filter, expanded = glassFine, onToggle = { glassFine = !glassFine })
                if (glassFine) {
                    FineSlider("Intensity", style.tint, 0f..3f) { GlassSettings.update(context, style.copy(tint = it)) }
                    FineSlider("Blur", style.blur, 0f..4f) { GlassSettings.update(context, style.copy(blur = it)) }
                    FineSlider("Refraction", style.refractionAmount, 0f..3f) { GlassSettings.update(context, style.copy(refractionAmount = it)) }
                    FineSlider("Saturation", style.saturation, 0f..3f) { GlassSettings.update(context, style.copy(saturation = it)) }
                    FineSlider("Rim light", style.highlight, 0f..2f) { GlassSettings.update(context, style.copy(highlight = it)) }
                }
                SettingsRow(icon = AppIcons.Appearance, tileColor = Candy.Lemon, title = "Adaptive luminance",
                    subtitle = "Glass adapts to what is behind it", trailing = {
                        GlassSwitch(checked = settings.adaptiveLuminance, onChange = { viewModel.setSetting("adaptiveLuminance", it) })
                    })
            }
        }

        SettingsSection("BUTTON MOTION") {
            SettingsCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MotionStyle.Presets.forEach { preset ->
                        ChoiceTile(
                            label = preset.label,
                            selected = motion == preset,
                            onClick = { MotionSettings.update(context, preset) },
                            preview = { Text("∿", color = Candy.Bubblegum, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium) },
                        )
                    }
                }
                ExpandableRow(title = "Fine tune glass spring", icon = AppIcons.Motion, expanded = motionFine, onToggle = { motionFine = !motionFine })
                if (motionFine) {
                    FineSlider("Bounce", motion.bounce, 0f..1f) { MotionSettings.update(context, motion.copy(bounce = it)) }
                    FineSlider("Speed", motion.speed, 0.4f..2.5f) { MotionSettings.update(context, motion.copy(speed = it)) }
                    FineSlider("Press", motion.press, 0f..2f) { MotionSettings.update(context, motion.copy(press = it)) }
                    FineSlider("Wobble", motion.wobble, 0f..3f) { MotionSettings.update(context, motion.copy(wobble = it)) }
                    FineSlider("Stretch", motion.stretch, 0f..2f) { MotionSettings.update(context, motion.copy(stretch = it)) }
                    FineSlider("Bloom", motion.bloom, 0f..2f) { MotionSettings.update(context, motion.copy(bloom = it)) }
                }
            }
        }

        SettingsSection("SCREEN TRANSITIONS") {
            SettingsCard {
                GlassDropdown(
                    label = "Transition",
                    options = listOf("GLASS_MORPH", "SHARED_X", "SHARED_Y", "FADE_THROUGH", "SLIDE", "SCALE_FADE", "NONE"),
                    selected = settings.transitionType,
                    onSelect = { viewModel.setSetting("transitionType", it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassDropdown(
                    label = "Curve",
                    options = listOf("DRAWER", "STANDARD", "EMPHASIZED", "EASE_IN_OUT", "LINEAR"),
                    selected = settings.transitionCurve,
                    onSelect = { viewModel.setSetting("transitionCurve", it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FineSlider("Duration ×", settings.animationSpeed, 0.3f..3f) { viewModel.setAnimationSpeed(it) }
            }
        }
        SettingsCard {
            SettingsRow(icon = AppIcons.Undo, tileColor = Candy.Tomato, title = "Reset to defaults", onClick = {
                GlassSettings.update(context, GlassStyle.Default)
                MotionSettings.update(context, MotionStyle.Normal)
            })
        }
    }
}
