package com.pinfold.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.home.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsState()
    val networkLog by viewModel.networkLog.collectAsState()
    var importResult by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
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
        Text("Backup", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { exportLauncher.launch("pinfold-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
            Text("Export backup")
        }
        Button(onClick = { importLauncher.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) {
            Text("Import backup")
        }
        importResult?.let { Text("Imported $it new bookmarks", style = MaterialTheme.typography.bodyMedium) }
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
