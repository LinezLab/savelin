package com.pinfold.app

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.reminders.ReminderScheduler
import com.pinfold.app.ui.App
import com.pinfold.app.ui.glass.ProvideGlassHaptics
import com.pinfold.app.ui.glass.theme.SavelinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var activeTheme: String? = null
    private val incomingShare = MutableStateFlow<String?>(null)
    private val clipboardShare = MutableStateFlow<Pair<String, Long>?>(null)
    private val openBookmark = MutableStateFlow<Pair<Long, Long>?>(null)
    private var openSequence = 0L
    private var lastClipUrl: String? = null
    private var clipSequence = 0L
    private lateinit var repository: SettingsRepository

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

        repository = SettingsRepository(applicationContext)
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

        lifecycleScope.launch {
            repository.settings
                .map { it.blockScreenshots }
                .distinctUntilChanged()
                .collect { block ->
                    if (block) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
        }
        lifecycleScope.launch {
            repository.settings
                .map { it.immersiveMode }
                .distinctUntilChanged()
                .collect { immersive ->
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    if (immersive) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
        }

        incomingShare.value = sharedText(intent)
        readOpenIntent(intent)
        setContent {
            SavelinTheme(dark = dark) {
                ProvideGlassHaptics {
                    App(
                        sharedText = incomingShare.collectAsState().value,
                        clipboardShare = clipboardShare.collectAsState().value,
                        openBookmark = openBookmark.collectAsState().value,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_SEND) incomingShare.value = sharedText(intent)
        readOpenIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!::repository.isInitialized) return
        lifecycleScope.launch {
            if (!repository.settings.first().enableClipboardChip) return@launch
            val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip
                ?.takeIf { it.itemCount > 0 }
            val text = clip?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString()?.trim()
            val url = text?.let { Regex("https?://\\S+").find(it)?.value } ?: return@launch
            if (url == lastClipUrl) return@launch
            lastClipUrl = url
            clipboardShare.value = url to ++clipSequence
        }
    }

    private fun readOpenIntent(source: Intent?) {
        val id = source?.getLongExtra(ReminderScheduler.EXTRA_BOOKMARK_ID, -1L) ?: -1L
        if (id >= 0) openBookmark.value = id to ++openSequence
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
