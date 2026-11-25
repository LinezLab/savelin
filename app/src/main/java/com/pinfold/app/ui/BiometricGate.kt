package com.pinfold.app.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
fun BiometricGate(onUnlock: () -> Unit, onCancel: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val latest by androidx.compose.runtime.rememberUpdatedState(onUnlock)
    val latestCancel by androidx.compose.runtime.rememberUpdatedState(onCancel)
    val prompt = androidx.compose.runtime.remember {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                latest()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    latestCancel()
                }
            }
        }
        if (activity != null) {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(context), callback)
        } else null
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        GlassPillButton(onClick = {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Sonil")
                .setNegativeButtonText("Cancel")
                .build()
            prompt?.authenticate(info)
        }, modifier = Modifier.padding(bottom = maxHeight * 0.10f)) {
            Text("Unlock", color = clear.onGlass)
        }
    }
}
