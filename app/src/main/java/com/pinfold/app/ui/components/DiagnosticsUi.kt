package com.pinfold.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pinfold.app.diagnostics.CrashReporter
import com.pinfold.app.ui.glass.GlassOverlay
import com.pinfold.app.ui.glass.GlassToast
import kotlinx.coroutines.delay

fun Context.copyDiagnosticsText(text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText("Savelin diagnostics", text))
}

/** A short glass toast that removes itself, so a copy action never leaves a panel on screen. */
@Composable
fun DiagnosticsToast(message: String, onDone: () -> Unit) {
    GlassOverlay {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 96.dp), contentAlignment = Alignment.BottomCenter) {
            GlassToast(message, null, {})
        }
    }
    LaunchedEffect(message) {
        delay(3200)
        onDone()
    }
}

/** On the launch after a crash: copy the redacted report and say so in a glass toast. */
@Composable
fun CrashReportNotice() {
    val context = LocalContext.current
    val pending = CrashReporter.pendingLog
    if (pending.isNullOrBlank()) return
    var toast by remember { mutableStateOf(false) }
    LaunchedEffect(pending) {
        context.copyDiagnosticsText(pending)
        CrashReporter.clearPending()
        toast = true
    }
    if (toast) DiagnosticsToast("Crash log copied to clipboard (redacted)") { toast = false }
}
