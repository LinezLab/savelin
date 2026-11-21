package com.pinfold.app.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
        Text("Savelin is locked", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Savelin")
                .setNegativeButtonText("Cancel")
                .build()
            prompt?.authenticate(info)
        }) {
            Text("Unlock")
        }
    }
}
