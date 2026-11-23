package com.pinfold.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.data.settings.AppSettings
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassDropdown
import com.pinfold.app.ui.glass.ComponentGlass
import com.pinfold.app.ui.glass.ComponentGlassStyle
import com.pinfold.app.ui.glass.ComponentStyleMode
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.MotionStyle
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.TintChoice
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel

val ComponentStyleMode.label: String
    get() = when (this) {
        ComponentStyleMode.GLOBAL -> "Global"
        ComponentStyleMode.CUSTOM -> "Custom"
        ComponentStyleMode.MATCH_BUTTONS -> "Match buttons"
    }

/** Settings ▸ Liquid glass and motion ▸ Components. */
@Composable
fun ComponentsSection(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var selected by remember { mutableStateOf<GlassComponent?>(null) }
    SettingsSection("COMPONENTS") {
        SettingsCard {
            GlassComponent.entries.forEach { component ->
                val mode = ComponentGlass.mode(component)
                SettingsRow(
                    icon = AppIcons.Appearance,
                    tileColor = Candy.Sky,
                    title = component.label,
                    subtitle = "${mode.label} · ${ComponentGlass.source(component)}",
                    onClick = { selected = if (selected == component) null else component },
                    trailing = {
                        GlassChip(
                            onClick = {
                                val next = when (mode) {
                                    ComponentStyleMode.GLOBAL -> ComponentStyleMode.CUSTOM
                                    ComponentStyleMode.CUSTOM -> if (component.canMatchButtons) ComponentStyleMode.MATCH_BUTTONS else ComponentStyleMode.GLOBAL
                                    ComponentStyleMode.MATCH_BUTTONS -> ComponentStyleMode.GLOBAL
                                }
                                ComponentGlass.setMode(context, component, next)
                            },
                            height = 40.dp,
                        ) { Text(mode.label, color = clear.onGlass, style = MaterialTheme.typography.labelSmall) }
                    },
                )
            }
        }
        selected?.let { ComponentDetail(it, viewModel, settings) }
    }
}

@Composable
private fun ComponentDetail(component: GlassComponent, viewModel: HomeViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val mode = ComponentGlass.mode(component)
    val editable = mode == ComponentStyleMode.CUSTOM
    val style = ComponentGlass.effective(component)
    SettingsCard {
        Text(
            "${component.label} · ${ComponentGlass.source(component)}",
            style = MaterialTheme.typography.titleMedium,
            color = clear.onGlass,
        )
        Text(
            if (editable) "Custom values are live; Reset to Global follows the main theme again."
            else "Global groups follow the main theme; switch to Custom to edit.",
            style = MaterialTheme.typography.bodySmall,
            color = clear.onGlassSoft,
        )
        ComponentSample(component)
        Box(Modifier.alpha(if (editable) 1f else 0.4f)) {
            FineSlider("Intensity", style.intensity, 0f..3f) { update(context, component, style) { s -> s.copy(intensity = it.coerceIn(0f, 3f)) } }
            FineSlider("Blur", style.blur, 0f..4f) { update(context, component, style) { s -> s.copy(blur = it.coerceIn(0f, 4f)) } }
            FineSlider("Refraction height", style.refractionHeight, 0f..3f) { update(context, component, style) { s -> s.copy(refractionHeight = it.coerceIn(0f, 3f)) } }
            FineSlider("Refraction amount", style.refractionAmount, 0f..3f) { update(context, component, style) { s -> s.copy(refractionAmount = it.coerceIn(0f, 3f)) } }
            FineSlider("Edge thickness", style.edgeThickness, 0f..3f) { update(context, component, style) { s -> s.copy(edgeThickness = it.coerceIn(0f, 3f)) } }
            FineSlider("Tint opacity", style.tintOpacity, 0f..1f) { update(context, component, style) { s -> s.copy(tintOpacity = it.coerceIn(0f, 1f)) } }
            FineSlider("Saturation", style.saturation, 0f..3f) { update(context, component, style) { s -> s.copy(saturation = it.coerceIn(0f, 3f)) } }
            FineSlider("Rim light", style.rimLight, 0f..2f) { update(context, component, style) { s -> s.copy(rimLight = it.coerceIn(0f, 2f)) } }
            FineSlider("Specular", style.specular, 0f..2f) { update(context, component, style) { s -> s.copy(specular = it.coerceIn(0f, 2f)) } }
            FineSlider("Noise", style.noise, 0f..1f) { update(context, component, style) { s -> s.copy(noise = it.coerceIn(0f, 1f)) } }
            FineSlider("Shadow", style.shadow, 0f..2f) { update(context, component, style) { s -> s.copy(shadow = it.coerceIn(0f, 2f)) } }
            FineSlider("Corner radius", style.cornerRadius, 0f..3f) { update(context, component, style) { s -> s.copy(cornerRadius = it.coerceIn(0f, 3f)) } }
            FineSlider("Opacity", style.opacity, 0f..1f) { update(context, component, style) { s -> s.copy(opacity = it.coerceIn(0f, 1f)) } }
            GlassDropdown(
                label = "Tint",
                options = TintChoice.entries.map { it.name },
                selected = style.tint.name,
                onSelect = { name -> runCatching { TintChoice.valueOf(name) }.getOrNull()?.let { choice -> update(context, component, style) { s -> s.copy(tint = choice) } } },
            )
            GlassDropdown(
                label = "Motion style",
                options = MotionStyle.Presets.map { it.label }.let { if (it.contains(style.motionStyle)) it else it + style.motionStyle },
                selected = style.motionStyle,
                onSelect = { name -> update(context, component, style) { s -> s.copy(motionStyle = name) } },
            )
            if (component == GlassComponent.CHIPS) {
                GlassDropdown(label = "Chip effect", options = listOf("LIGHT", "FULL"), selected = settings.chipEffect, onSelect = {
                    viewModel.setSetting("chipEffect", it)
                    update(context, component, style) { s -> s.copy(chipEffect = it) }
                },)
            }
        }
        val others = GlassComponent.entries.filter { it != component }.map { it.label }
        GlassDropdown(label = "Copy style from", options = others, selected = "", onSelect = { name ->
            GlassComponent.entries.firstOrNull { it.label == name }?.let { source -> ComponentGlass.copyFrom(context, component, source) }
        },)
        SettingsRow(icon = AppIcons.Undo, tileColor = Candy.Tomato, title = "Reset to Global", onClick = { ComponentGlass.reset(context, component) })
    }
}

private fun update(
    context: android.content.Context,
    component: GlassComponent,
    base: ComponentGlassStyle,
    change: (ComponentGlassStyle) -> ComponentGlassStyle,
) {
    ComponentGlass.setCustom(context, component, change(base))
}

/** A live glass sample driven by the group's resolved style, not a description of it. */
@Composable
private fun ComponentSample(component: GlassComponent) {
    Box(
        Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Candy.Bubblegum, Candy.Sky, Candy.Mint))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 96.dp, height = 44.dp)
                .then(SavelinGlass.modifierFor(component, RoundedRectangle(12.dp))),
            contentAlignment = Alignment.Center,
        ) {
            Text(component.label, color = clear.onGlass, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
