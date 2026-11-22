package com.pinfold.app.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.background.GeneratedBackground
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun BackgroundScreen(viewModel: HomeViewModel) {
    val settings by viewModel.settings.collectAsState()

    SettingsScaffold("Background", onBack = viewModel::closeSettingsPage) {
        SettingsCard {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().height(180.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)),
            ) {
                GeneratedBackground(
                    Modifier.fillMaxWidth(),
                    loopMs = settings.backgroundLoopSeconds * 1000,
                    animate = settings.backgroundAnimate,
                )
            }
            Row {
                SettingsRow(AppIcons.Refetch, Candy.Mint, "Shuffle") { viewModel.setSetting("backgroundHue", (0..360).random().toFloat()) }
                SettingsRow(AppIcons.Undo, Candy.Tomato, "Reset") { viewModel.setSetting("backgroundHue", 220f) }
            }
        }

        SettingsSection("TYPE") {
            SettingsCard {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    listOf("GENERATED" to "Generated", "SOLID" to "Solid colour", "PICTURE" to "Picture").forEach { (value, label) ->
                        ChoiceTile(label, selected = settings.backgroundType == value, onClick = { viewModel.setSetting("backgroundType", value) },
                            preview = { Text(if (settings.backgroundType == value) "✓" else "", color = clear.ink) })
                    }
                }
            }
        }

        SettingsSection("STYLE") {
            SettingsCard {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    listOf("PETALS" to "Petals", "AURORA" to "Aurora", "MESH" to "Mesh", "WAVES" to "Waves").forEach { (value, label) ->
                        ChoiceTile(label, selected = settings.backgroundStyle == value, onClick = { viewModel.setSetting("backgroundStyle", value) },
                            preview = { Text("◠", color = clear.ink) })
                    }
                }
                FineSlider("Base hue", settings.backgroundHue, 0f..360f) { viewModel.setSetting("backgroundHue", it) }
            }
        }

        SettingsSection("MOTION") {
            SettingsCard {
                SettingsRow(AppIcons.Motion, Candy.Bubblegum, "Animate background", trailing = {
                    GlassSwitch(settings.backgroundAnimate) { viewModel.setSetting("backgroundAnimate", it) }
                })
                FineSlider("Loop length (s)", settings.backgroundLoopSeconds.toFloat(), 8f..120f) {
                    viewModel.setSetting("backgroundLoopSeconds", it.toInt())
                }
                FineSlider("Dim background", settings.backgroundDim, 0f..0.6f) { viewModel.setSetting("backgroundDim", it) }
                SettingsRow(AppIcons.Appearance, Candy.Grape, "Details screen background", settings.backgroundDetails) {
                    viewModel.setSetting("backgroundDetails", if (settings.backgroundDetails == "PICTURE") "GENERATED" else "PICTURE")
                }
            }
        }
    }
}
