package com.pinfold.app.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DriveSyncScreen(viewModel: HomeViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z", androidx.compose.ui.platform.LocalLocale.current)
    val exact = if (settings.driveLastSuccessAt > 0) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(settings.driveLastSuccessAt), ZoneId.systemDefault()).format(formatter)
    } else "Never"

    SettingsScaffold("Google Drive sync", onBack = viewModel::closeSettingsPage) {
        SettingsCard {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AppIcons.Icon(AppIcons.Drive, "Google Drive", size = 32.dp)
                androidx.compose.foundation.layout.Spacer(Modifier.fillMaxWidth(0.05f))
                Text("Google Drive sync", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = clear.ink)
            }
            Text("Last synced: $exact", color = clear.ink, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (settings.driveLastError.isNotBlank()) {
                Text("Last attempt failed: ${settings.driveLastError}", color = clear.inkSoft, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Text(
                "Changes appear on another device when it next opens the app, runs its periodic pull, or you tap Sync now. " +
                    "No server is run by the developer.",
                color = clear.inkSoft,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }

        SettingsSection("SETUP") {
            SettingsCard {
                SettingsRow(AppIcons.Drive, Candy.Sky, "Sign in to Google Drive", "Requires the OAuth client id from Google Cloud") {
                    viewModel.setSetting("driveSyncEnabled", true)
                }
                SettingsRow(AppIcons.Lock, Candy.Grape, "Sync passphrase", if (settings.drivePassphraseSet) "Set" else "Not set")
                SettingsRow(AppIcons.Info, Candy.Mint, "Recovery key", if (settings.driveRecoverySaved) "Saved" else "Not saved")
            }
        }

        SettingsSection("AUTOMATIC SYNC") {
            SettingsCard {
                SettingsRow(AppIcons.Refetch, Candy.Sky, "Automatic sync", trailing = {
                    GlassSwitch(settings.driveSyncEnabled) { viewModel.setSetting("driveSyncEnabled", it) }
                })
                SettingsRow(AppIcons.Offline, Candy.Grape, "Wi-Fi only", trailing = {
                    GlassSwitch(settings.driveWifiOnly) { viewModel.setSetting("driveWifiOnly", it) }
                })
                SettingsRow(AppIcons.Licences, Candy.Lime, "Sync history", "Last 20 runs")
            }
        }

        SettingsSection("WHAT TO SYNC") {
            SettingsCard {
                SettingsRow(AppIcons.CustomImage, Candy.Sky, "Images", trailing = {
                    GlassSwitch(settings.driveSyncImages) { viewModel.setSetting("driveSyncImages", it) }
                })
                SettingsRow(AppIcons.Appearance, Candy.Grape, "Appearance and motion", trailing = {
                    GlassSwitch(settings.driveSyncAppearance) { viewModel.setSetting("driveSyncAppearance", it) }
                })
                SettingsRow(AppIcons.Settings, Candy.Mint, "App preferences", trailing = {
                    GlassSwitch(settings.driveSyncPreferences) { viewModel.setSetting("driveSyncPreferences", it) }
                })
                SettingsRow(AppIcons.Lock, Candy.Tomato, "Private vault (off by default)", trailing = {
                    GlassSwitch(settings.driveSyncPrivate) { viewModel.setSetting("driveSyncPrivate", it) }
                })
            }
        }

        SettingsSection("SECURITY") {
            SettingsCard {
                Text(
                    "End-to-end encrypted, only your passphrase can unlock it.",
                    color = clear.ink,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                SettingsRow(AppIcons.Lock, Candy.Grape, "Change passphrase")
                SettingsRow(AppIcons.Info, Candy.Mint, "Show recovery key")
                SettingsRow(AppIcons.Refetch, Candy.Sky, "Rotate encryption key")
                SettingsRow(AppIcons.Licences, Candy.Lime, "Verify cloud data")
            }
        }

        SettingsSection("ACCOUNT") {
            SettingsCard {
                SettingsRow(AppIcons.Undo, Candy.Tomato, "Disconnect Google Drive")
            }
        }
    }
}
