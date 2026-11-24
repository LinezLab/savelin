package com.pinfold.app

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.ui.App
import com.pinfold.app.sync.DriveSignInHost
import com.pinfold.app.sync.GoogleDriveSignIn
import com.pinfold.app.ui.glass.ProvideGlassHaptics
import com.pinfold.app.ui.glass.theme.SavelinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity(), DriveSignInHost {
    override val driveSignIn = GoogleDriveSignIn(this)
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var activeTheme: String? = null
    private val incomingShare = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = SettingsRepository(applicationContext)
        val initialTheme = runBlocking(Dispatchers.IO) {
            repository.settings.map { it.themeMode }.first()
        }
        activeTheme = initialTheme
        val dark = when (initialTheme) {
            "LIGHT" -> false
            "DARK" -> true
            else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        lifecycleScope.launch {
            repository.settings
                .map { it.themeMode }
                .distinctUntilChanged()
                .collect { mode ->
                    val previous = activeTheme
                    activeTheme = mode
                    if (previous != null && previous != mode) {
                        recreate()
                    }
                }
        }

        incomingShare.value = sharedText(intent)
        setContent {
            SavelinTheme(dark = dark) {
                ProvideGlassHaptics {
                    App(sharedText = incomingShare.collectAsState().value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        driveSignIn.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEND) incomingShare.value = sharedText(intent)
    }

    override fun onResume() {
        super.onResume()
        driveSignIn.onResume(intent)
    }

    private fun sharedText(source: Intent?): String? = source?.takeIf { it.action == Intent.ACTION_SEND }?.let {
        it.getStringExtra(Intent.EXTRA_TEXT)
            ?: it.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: it.getStringExtra(Intent.EXTRA_SUBJECT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeTheme?.let { outState.putString(KEY_THEME, it) }
    }

    private companion object {
        const val KEY_THEME = "activeTheme"
    }
}
