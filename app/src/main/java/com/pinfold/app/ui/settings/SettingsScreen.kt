package com.pinfold.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsState()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Appearance & motion", style = MaterialTheme.typography.headlineMedium)
        SettingRow("Reduce transparency", settings.reduceTransparency) { viewModel.setReduceTransparency(it) }
        SettingRow("Reduce motion", settings.reduceMotion) { viewModel.setReduceMotion(it) }
        SettingRow("Glass preset: ${settings.glassPreset}", settings.glassPreset != "LIQUID") {
            viewModel.setGlassPreset(if (it) "LIQUID" else "FROSTED")
        }
        SettingRow("Motion preset: ${settings.motionPreset}", settings.motionPreset == "JELLY") {
            viewModel.setMotionPreset(if (it) "JELLY" else "NORMAL")
        }
        SettingRow("Haptics: ${settings.haptics}", settings.haptics != "OFF") {
            viewModel.setHaptics(if (it) "LIGHT" else "OFF")
        }

        Divider()
        Text("Privacy & network", style = MaterialTheme.typography.headlineSmall)
        SettingRow("Offline mode", settings.offlineMode) { viewModel.setOffline(it) }
        SettingRow("Third-party proxies (opt-in)", settings.proxiesEnabled) { viewModel.setProxies(it) }
        OutlinedTextField(
            value = settings.previewUserAgent,
            onValueChange = viewModel::setUserAgent,
            label = { Text("Preview User-Agent") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Pinfold contacts only the host of a link you save (and optionally an image host). " +
                "No account, analytics or cloud. Private and login-walled posts may never produce a preview.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.fillMaxWidth(0.75f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
