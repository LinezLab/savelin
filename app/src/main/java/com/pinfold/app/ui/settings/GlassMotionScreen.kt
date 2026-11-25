package com.pinfold.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.iosHorizontalScroll
import com.pinfold.app.ui.components.GlassDropdown
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassStyle
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.MotionStyle
import com.pinfold.app.ui.glass.glass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import kotlin.math.cos
import kotlin.math.exp

@Composable
fun GlassMotionScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var glassFine by remember { mutableStateOf(false) }
    var motionFine by remember { mutableStateOf(false) }
    val style = GlassSettings.style
    val motion = MotionSettings.style
    val materialName = GlassStyle.Named.firstOrNull { it.second == style }?.first ?: "Custom"
    val motionName = motion.label
    val materialOptions = GlassStyle.Named.map { it.first }.let { if (it.contains(materialName)) it else it + materialName }
    val motionOptions = MotionStyle.Presets.map { it.label }.let { if (it.contains(motionName)) it else it + motionName }

    SettingsScaffold("Liquid glass & motion", onBack = viewModel::closeSettingsPage) {
        SettingsSection("FILTERS") {
            SettingsCard {
                SettingsRow(AppIcons.Filter, Candy.Mint, "Remember filters", "Keep Important and Due on restart", trailing = {
                    GlassSwitch(settings.rememberFilters) { viewModel.setSetting("rememberFilters", it) }
                })
            }
        }
        SettingsCard {
            Row(Modifier.fillMaxWidth().iosHorizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassChip(onClick = {}, height = 40.dp) { Text("Press me", color = clear.onGlass) }
                GlassCircle(onClick = {}, size = 40.dp, label = "Preview", ink = clear.onGlass) {
                    AppIcons.Icon(AppIcons.Normal, null, tint = clear.onGlass)
                }
                GlassChip(onClick = {}, height = 40.dp) { Text("Outlined chip", color = clear.onGlass) }
            }
            Text(
                "These buttons move the way you set them below. Press and hold them.",
                style = MaterialTheme.typography.bodySmall,
                color = clear.onGlassSoft,
            )
            SettingsRow(icon = AppIcons.Haptics, tileColor = Candy.Mint, title = "Touch and hold effects", trailing = {
                GlassSwitch(checked = settings.touchHoldEffect, onChange = { viewModel.setSetting("touchHoldEffect", it) })
            })
        }

        SettingsSection("GLASS MATERIAL") {
            SettingsCard {
                GlassDropdown(
                    label = "Material",
                    options = materialOptions,
                    selected = materialName,
                    onSelect = { name -> GlassStyle.Named.firstOrNull { it.first == name }?.second?.let { GlassSettings.update(context, it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().iosHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassStyle.Named.forEach { (name, preset) ->
                        ChoiceTile(
                            label = name,
                            selected = style == preset,
                            onClick = { GlassSettings.update(context, preset) },
                            preview = { GlassPresetPreview(preset) },
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
                GlassDropdown(
                    label = "Button motion",
                    options = motionOptions,
                    selected = motionName,
                    onSelect = { name -> MotionStyle.Presets.firstOrNull { it.label == name }?.let { MotionSettings.update(context, it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().iosHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MotionStyle.Presets.forEach { preset ->
                        ChoiceTile(
                            label = preset.label,
                            selected = motion == preset,
                            onClick = { MotionSettings.update(context, preset) },
                            preview = { SpringCurvePreview(preset) },
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

/** A real, small glass surface over a colourful backdrop — the preset is applied, not described. */
@Composable
private fun GlassPresetPreview(preset: GlassStyle) {
    val icon = when (preset) {
        GlassStyle.Default -> AppIcons.Liquid
        GlassStyle.Frosted -> AppIcons.Frosted
        GlassStyle.Crystal -> AppIcons.Crystal
        GlassStyle.Water -> AppIcons.Water
        GlassStyle.Solid -> AppIcons.Solid
        else -> AppIcons.Appearance
    }
    Box(
        Modifier.fillMaxWidth().height(82.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Candy.Bubblegum, Candy.Sky, Candy.Mint))),
    ) {
        Box(
            Modifier.align(Alignment.Center).size(width = 84.dp, height = 44.dp)
                .glass(
                    shape = RoundedCornerShape(12.dp),
                    wash = clear.glass,
                    blur = 8.dp * preset.blur,
                    lensHeight = 8.dp * preset.refractionHeight,
                    lensAmount = 10.dp * preset.refractionAmount,
                    styleOverride = preset,
                    scaleByStyle = false,
                ),
        )
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            AppIcons.Icon(icon, null, tint = clear.onGlass, size = 14.dp)
        }
    }
}

/** The motion tile draws the actual damped curve its spring will follow. */
@Composable
private fun SpringCurvePreview(motion: MotionStyle) {
    val ink = clear.onGlass
    Canvas(Modifier.fillMaxWidth().height(82.dp)) {
        val damping = 1f + (0.5f - motion.bounce) * 1.2f
        val w = size.width
        val h = size.height
        drawLine(ink.copy(alpha = 0.2f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1.dp.toPx())
        val path = Path()
        for (i in 0..120) {
            val x = i / 120f
            val t = x * 6f
            val value = 1f - exp(-0.8f * damping * t) * cos(2.4f * t * motion.speed)
            val y = h * (0.14f + 0.72f * (1f - (value.coerceIn(-0.2f, 1.4f) / 1.4f)))
            if (i == 0) path.moveTo(x * w, y) else path.lineTo(x * w, y)
        }
        drawPath(path, ink.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx()))
    }
}
