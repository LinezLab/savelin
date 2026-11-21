package com.pinfold.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.ui.App
import com.pinfold.app.ui.glass.theme.SavelinTheme

class MainActivity : FragmentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

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
        val shared = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkState = remember { mutableStateOf(systemDark) }
            val repository = remember { SettingsRepository(applicationContext) }
            LaunchedEffect(Unit) {
                repository.settings.collect { settings ->
                    darkState.value = when (settings.themeMode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> systemDark
                    }
                }
            }
            SavelinTheme(dark = darkState.value) {
                App(sharedText = shared)
            }
        }
    }
}
