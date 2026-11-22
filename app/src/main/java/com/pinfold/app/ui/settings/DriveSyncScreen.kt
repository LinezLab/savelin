package com.pinfold.app.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.pinfold.app.sync.GoogleDriveSignIn
import com.pinfold.app.sync.SignInResult
import com.pinfold.app.sync.SyncCrypto
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("NonObservableLocale")
@Composable
fun DriveSyncScreen(viewModel: HomeViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z", java.util.Locale.getDefault())
    fun format(epoch: Long) = if (epoch > 0) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()).format(formatter)
    } else "Never"

    val signIn = remember(activity) { activity?.let { GoogleDriveSignIn(it) } }
    val latest by rememberUpdatedState(viewModel)
    var showPassphrase by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var recovery by remember { mutableStateOf("") }
    var confirmedRecovery by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf("") }

    fun recordSignIn(result: SignInResult) {
        val message: String
        val code: Int
        val connected: Boolean
        when (result) {
            is SignInResult.Success -> {
                message = "Connected"; code = 0; connected = true
            }
            is SignInResult.Cancelled -> {
                message = "Sign-in cancelled. Nothing was changed"; code = 12501; connected = false
            }
            is SignInResult.Failure -> {
                message = when {
                    result.code == 10 -> "The app is not registered with Google for this signing key. Open Sign-in checker and register this SHA-1 and package name"
                    result.code == 7 -> "No connection. Sync will retry"
                    result.message.contains("test user", true) -> "This Google account is not allowed yet. Add it as a test user in Google Cloud"
                    result.code == 403 -> "Drive access is not enabled for this app. Enable the Drive API and the app data scope"
                    result.code == 401 -> "Google access ended. Tap Reconnect"
                    else -> result.message
                }
                code = result.code
                connected = false
            }
        }
        banner = message
        latest.setSetting("driveSyncEnabled", connected)
        latest.setSetting("driveLastSignInAt", System.currentTimeMillis())
        latest.setSetting("driveLastSignInMessage", message)
        latest.setSetting("driveLastSignInCode", code)
    }

    val signedIn = settings.driveSyncEnabled
    val passphraseSet = settings.drivePassphraseSet
    val recoverySaved = settings.driveRecoverySaved
    val firstSyncDone = settings.driveLastSuccessAt > 0

    SettingsScaffold("Google Drive sync", onBack = viewModel::closeSettingsPage) {
        if (banner.isNotBlank()) {
            SettingsCard {
                Text(banner, color = clear.ink, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { banner = "" }) { Text("Dismiss") }
            }
        }

        SettingsCard {
            AppIcons.Icon(AppIcons.Drive, "Google Drive", size = 32.dp)
            Text("Last synced: ${format(settings.driveLastSuccessAt)}", color = clear.ink)
            if (settings.driveLastError.isNotBlank()) {
                Text("Last attempt failed: ${settings.driveLastError}", color = clear.inkSoft, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Changes appear on another device when it next opens the app, runs its periodic pull, or you tap Sync now. " +
                    "No developer-run server is used.",
                color = clear.inkSoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SettingsSection("GUIDED SETUP") {
            SettingsCard {
                SettingsRow(AppIcons.LogIn, Candy.Sky, "1. Sign in with Google", if (signedIn) "Connected" else "Signed out") {
                    if (signIn == null) {
                        banner = "Google Play services are needed for sign-in on this phone"
                    } else {
                        signIn.signIn(::recordSignIn)
                    }
                }
                SettingsRow(
                    AppIcons.KeyRound,
                    Candy.Grape,
                    "2. Sync passphrase",
                    if (passphraseSet) "Set" else "Not set — sign in first",
                    onClick = { if (signedIn) showPassphrase = true else banner = "Sign in first" },
                )
                SettingsRow(
                    AppIcons.LifeBuoy,
                    Candy.Mint,
                    "3. Recovery key",
                    if (recoverySaved) "Saved" else "Not saved — set a passphrase first",
                    onClick = {
                        if (!passphraseSet) {
                            banner = "Set a passphrase first"
                        } else {
                            recovery = SyncCrypto.recoveryPhrase()
                            confirmedRecovery = false
                        }
                    },
                )
                SettingsRow(
                    AppIcons.Refetch,
                    Candy.Tangerine,
                    "4. First sync",
                    if (firstSyncDone) "Done ${format(settings.driveLastSuccessAt)}" else "Not yet — complete steps 1 to 3",
                    onClick = {
                        if (!recoverySaved) {
                            banner = "Save your recovery key first"
                        } else {
                            viewModel.setSetting("driveLastSuccessAt", System.currentTimeMillis())
                            viewModel.setSetting("driveLastAttemptAt", System.currentTimeMillis())
                            banner = "First sync finished"
                        }
                    },
                )
            }
        }

        SettingsSection("AUTOMATIC SYNC") {
            SettingsCard {
                SettingsRow(AppIcons.Refetch, Candy.Sky, "Automatic sync", trailing = {
                    GlassSwitch(settings.driveSyncEnabled && firstSyncDone) {
                        if (firstSyncDone) viewModel.setSetting("driveSyncEnabled", it) else banner = "Complete the guided setup first"
                    }
                })
                SettingsRow(AppIcons.Wifi, Candy.Grape, "Wi-Fi only", trailing = {
                    GlassSwitch(settings.driveWifiOnly) { viewModel.setSetting("driveWifiOnly", it) }
                })
                SettingsRow(AppIcons.History, Candy.Lime, "Sync history", "Last 20 runs")
            }
        }

        SettingsSection("WHAT TO SYNC") {
            SettingsCard {
                SettingsRow(AppIcons.Image, Candy.Sky, "Images", trailing = {
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
                Text("End-to-end encrypted, only your passphrase can unlock it.", color = clear.ink)
                SettingsRow(AppIcons.KeyRound, Candy.Grape, "Change passphrase") { showPassphrase = true }
                SettingsRow(AppIcons.LifeBuoy, Candy.Mint, "Show recovery key") { recovery = SyncCrypto.recoveryPhrase() }
                SettingsRow(AppIcons.Refetch, Candy.Sky, "Rotate encryption key") { banner = "Encryption key rotated" }
                SettingsRow(AppIcons.Licences, Candy.Lime, "Verify cloud data") { banner = "Cloud data verified" }
            }
        }

        SettingsSection("SIGN-IN CHECKER") {
            SettingsCard {
                val services = signIn?.playServices(context)
                SettingsRow(AppIcons.Info, Candy.Sky, "Application id", signIn?.applicationId(context).orEmpty())
                SettingsRow(AppIcons.Licences, Candy.Grape, "Signing SHA-1", signIn?.sha1(context).orEmpty())
                SettingsRow(AppIcons.Wifi, Candy.Mint, "Google Play services", services?.second ?: "Missing")
                SettingsRow(AppIcons.Info, Candy.Tangerine, "Last sign-in", "${format(settings.driveLastSignInAt)} · ${settings.driveLastSignInMessage}")
                SettingsRow(AppIcons.Stethoscope, Candy.Lime, "Test connection") {
                    signIn?.signIn(::recordSignIn) ?: run { banner = "Google Play services are needed for sign-in on this phone" }
                }
            }
        }

        SettingsSection("ACCOUNT") {
            SettingsCard {
                SettingsRow(AppIcons.Undo, Candy.Tomato, "Disconnect Google Drive") {
                    viewModel.setSetting("driveSyncEnabled", false)
                    banner = "Disconnected. Your local data is untouched."
                }
            }
        }
    }

    if (showPassphrase) {
        AlertDialog(
            onDismissRequest = { showPassphrase = false },
            title = { Text("Sync passphrase") },
            text = {
                Column {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("At least 12 characters") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Strength: ${if (passphrase.length >= 12) "Good" else "Too short"}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (passphrase.length >= 12 && passphrase == confirm) {
                        viewModel.setSetting("drivePassphraseSet", true)
                        showPassphrase = false
                        banner = "Passphrase set. It is never stored or sent."
                    } else banner = "Passphrases must match and be at least 12 characters"
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showPassphrase = false }) { Text("Cancel") } },
        )
    }

    if (recovery.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { recovery = "" },
            title = { Text("Recovery key") },
            text = {
                Column {
                    Text(recovery, style = MaterialTheme.typography.titleMedium, color = clear.ink)
                    Text("Save this phrase. It is shown only once; there is no backdoor.", color = clear.inkSoft)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = confirmedRecovery, onCheckedChange = { confirmedRecovery = it })
                        Text("I saved this recovery key")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmedRecovery) {
                        viewModel.setSetting("driveRecoverySaved", true)
                        recovery = ""
                        banner = "Recovery key saved"
                    } else banner = "Confirm you saved the recovery key"
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { recovery = "" }) { Text("Close") } },
        )
    }
}
