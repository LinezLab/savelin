package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.GlassBackButton
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassStyle
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.MotionStyle
import com.pinfold.app.ui.home.HomeViewModel
import kotlin.math.roundToInt

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

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassBackButton(onBack = { viewModel.selectTab(com.pinfold.app.ui.home.HomeTab.HOME) })
        SectionTitle("Liquid glass", com.pinfold.app.ui.components.AppIcons.Liquid, MaterialTheme.typography.headlineMedium)
        val style = GlassSettings.style
        com.pinfold.app.ui.components.GlassDropdown(
            label = "Glass style",
            options = GlassStyle.Named.map { it.first },
            selected = GlassStyle.Named.firstOrNull { it.second == style }?.first ?: "Liquid",
            onSelect = { name -> GlassStyle.Named.firstOrNull { it.first == name }?.second?.let { GlassSettings.update(context, it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        FineSlider("Blur", style.blur, 0f..4f) { GlassSettings.update(context, style.copy(blur = it)) }
        FineSlider("Refraction height", style.refractionHeight, 0f..3f) { GlassSettings.update(context, style.copy(refractionHeight = it)) }
        FineSlider("Refraction amount", style.refractionAmount, 0f..3f) { GlassSettings.update(context, style.copy(refractionAmount = it)) }
        FineSlider("Saturation", style.saturation, 0f..3f) { GlassSettings.update(context, style.copy(saturation = it)) }
        FineSlider("Brightness", style.brightness, -1f..1f) { GlassSettings.update(context, style.copy(brightness = it)) }
        FineSlider("Tint", style.tint, 0f..3f) { GlassSettings.update(context, style.copy(tint = it)) }
        FineSlider("Highlight", style.highlight, 0f..2f) { GlassSettings.update(context, style.copy(highlight = it)) }
        SettingRow("Depth effect", style.depthEffect) { GlassSettings.update(context, style.copy(depthEffect = it)) }
        SettingRow("Chromatic aberration", style.chromaticAberration) { GlassSettings.update(context, style.copy(chromaticAberration = it)) }
        SettingRow("Reduce transparency", settings.reduceTransparency) { viewModel.setReduceTransparency(it) }

        Divider()
        SectionTitle("Motion", com.pinfold.app.ui.components.AppIcons.Motion, MaterialTheme.typography.headlineMedium)
        val motion = MotionSettings.style
        com.pinfold.app.ui.components.GlassDropdown(
            label = "Button motion",
            options = MotionStyle.Presets.map { it.label },
            selected = motion.label,
            onSelect = { name -> MotionStyle.Presets.firstOrNull { it.label == name }?.let { MotionSettings.update(context, it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Glass spring fine tune", style = MaterialTheme.typography.titleMedium)
        FineSlider("Bounce", motion.bounce, 0f..1f) { MotionSettings.update(context, motion.copy(bounce = it)) }
        FineSlider("Speed", motion.speed, 0.4f..2.5f) { MotionSettings.update(context, motion.copy(speed = it)) }
        FineSlider("Press", motion.press, 0f..2f) { MotionSettings.update(context, motion.copy(press = it)) }
        FineSlider("Wobble", motion.wobble, 0f..3f) { MotionSettings.update(context, motion.copy(wobble = it)) }
        FineSlider("Stretch", motion.stretch, 0f..2f) { MotionSettings.update(context, motion.copy(stretch = it)) }
        FineSlider("Bloom", motion.bloom, 0f..2f) { MotionSettings.update(context, motion.copy(bloom = it)) }
        FineSlider("Animation speed", settings.animationSpeed, 0.3f..3f) { viewModel.setAnimationSpeed(it) }
        SettingRow("Reduce motion", settings.reduceMotion) { viewModel.setReduceMotion(it) }
        SettingRow("Haptics on", settings.haptics != "OFF") {
            viewModel.setHaptics(if (it) "LIGHT" else "OFF")
        }

        Divider()
        SectionTitle("Cards", com.pinfold.app.ui.components.AppIcons.Card, MaterialTheme.typography.headlineSmall)
        Text("Columns: ${settings.cardColumns}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { count ->
                Button(onClick = { viewModel.setCardColumns(count) }, modifier = Modifier.padding(horizontal = 2.dp)) {
                    Text(if (settings.cardColumns == count) "✓ $count" else "$count")
                }
            }
        }
        com.pinfold.app.ui.components.GlassDropdown(
            label = "Card spacing",
            options = listOf("TIGHT", "NORMAL", "ROOMY"),
            selected = settings.cardSpacing,
            onSelect = viewModel::setCardSpacing,
            modifier = Modifier.fillMaxWidth(),
        )
        com.pinfold.app.ui.components.GlassDropdown(
            label = "Caption style",
            options = listOf("BELOW", "GLASS_STRIP", "IMAGE_ONLY"),
            selected = settings.cardCaptionStyle,
            onSelect = viewModel::setCardCaptionStyle,
            modifier = Modifier.fillMaxWidth(),
        )

        Divider()
        SectionTitle("Privacy & network", com.pinfold.app.ui.components.AppIcons.Lock, MaterialTheme.typography.headlineSmall)
        SettingRow("Lock with biometrics", settings.biometricLock) { viewModel.setBiometricLock(it) }
        SettingRow("Offline mode", settings.offlineMode) { viewModel.setOffline(it) }
        SettingRow("Third-party proxies (opt-in)", settings.proxiesEnabled) { viewModel.setProxies(it) }
        Text("Network transparency — ${networkLog.size} recent requests", style = MaterialTheme.typography.titleMedium)
        networkLog.take(10).forEach { entry ->
            Text("${entry.host} · ${entry.purpose} · ${entry.status}", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = settings.proxyTemplate,
            onValueChange = viewModel::setProxyTemplate,
            label = { Text("Proxy URL template (use {url})") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = settings.previewUserAgent,
            onValueChange = viewModel::setUserAgent,
            label = { Text("Preview User-Agent") },
            modifier = Modifier.fillMaxWidth(),
        )

        Divider()
        SectionTitle("Backup", com.pinfold.app.ui.components.AppIcons.Export, MaterialTheme.typography.headlineSmall)
        Button(onClick = { exportLauncher.launch("savelin-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
            Text("Export backup")
        }
        Button(onClick = { importLauncher.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) {
            Text("Import backup")
        }
        importResult?.let { Text("Imported $it new bookmarks", style = MaterialTheme.typography.bodyMedium) }
        Text(
            "Savelin contacts only the host of a link you save (and optionally an image host). " +
                "No account, analytics or cloud. Private and login-walled posts may never produce a preview.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FineSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text("$label: ${((value * 100).roundToInt() / 100f)}", style = MaterialTheme.typography.bodyMedium)
    Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.fillMaxWidth(0.75f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionTitle(label: String, icon: Int, style: androidx.compose.ui.text.TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.pinfold.app.ui.components.AppIcons.Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
        Text(label, style = style)
    }
}
