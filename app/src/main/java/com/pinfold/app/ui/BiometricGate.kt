package com.pinfold.app.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pinfold.app.ui.glass.GlassPillButton
import com.pinfold.app.ui.glass.theme.clear
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricGate(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val latest by androidx.compose.runtime.rememberUpdatedState(onUnlock)
    val prompt = androidx.compose.runtime.remember {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                latest()
            }
        }
        if (activity != null) {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
        } else null
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sonil is locked", style = MaterialTheme.typography.headlineMedium, color = clear.onGlass)
        GlassPillButton(onClick = {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Sonil")
                .setNegativeButtonText("Cancel")
                .build()
            prompt?.authenticate(info)
        }) {
            Text("Unlock", color = clear.onGlass)
        }
    }
}
