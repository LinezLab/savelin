package com.pinfold.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.ui.App
import com.pinfold.app.ui.glass.theme.SavelinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var activeTheme: String? = null

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
            else -> isSystemInDarkTheme()
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

        val shared = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        setContent {
            SavelinTheme(dark = dark) {
                App(sharedText = shared)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeTheme?.let { outState.putString(KEY_THEME, it) }
    }

    private companion object {
        const val KEY_THEME = "activeTheme"
    }
}
