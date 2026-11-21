package com.pinfold.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val offlineMode: Boolean = false,
    val tapPreference: String = "OPEN",
    val preferNativeApp: Boolean = true,
    val browserMode: String = "SYSTEM",
    val hideDetailsOnLockScreen: Boolean = true,
    val biometricLock: Boolean = false,
    val allowHttpLinks: Boolean = false,
    val enableClipboardChip: Boolean = false,
    val reduceTransparency: Boolean = false,
    val reduceMotion: Boolean = false,
    val glassPreset: String = "LIQUID",
    val motionPreset: String = "NORMAL",
    val transition: String = "GLASS_MORPH",
    val animationSpeed: Float = 1f,
    val haptics: String = "LIGHT",
    val holdDelayMs: Int = 350,
    val holdEffect: String = "MAGNIFY",
    val performance: String = "AUTO",
    val frameOverlay: Boolean = false,
    val previewUserAgent: String = "facebookexternalhit/1.1",
    val proxiesEnabled: Boolean = false,
    val proxyTemplate: String = "",
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            offlineMode = p[Keys.offlineMode] ?: false,
            tapPreference = p[Keys.tapPreference] ?: "OPEN",
            preferNativeApp = p[Keys.preferNativeApp] ?: true,
            browserMode = p[Keys.browserMode] ?: "SYSTEM",
            hideDetailsOnLockScreen = p[Keys.hideDetailsOnLockScreen] ?: true,
            biometricLock = p[Keys.biometricLock] ?: false,
            allowHttpLinks = p[Keys.allowHttpLinks] ?: false,
            enableClipboardChip = p[Keys.enableClipboardChip] ?: false,
            reduceTransparency = p[Keys.reduceTransparency] ?: false,
            reduceMotion = p[Keys.reduceMotion] ?: false,
            glassPreset = p[Keys.glassPreset] ?: "LIQUID",
            motionPreset = p[Keys.motionPreset] ?: "NORMAL",
            transition = p[Keys.transition] ?: "GLASS_MORPH",
            animationSpeed = p[Keys.animationSpeed] ?: 1f,
            haptics = p[Keys.haptics] ?: "LIGHT",
            holdDelayMs = p[Keys.holdDelayMs] ?: 350,
            holdEffect = p[Keys.holdEffect] ?: "MAGNIFY",
            performance = p[Keys.performance] ?: "AUTO",
            frameOverlay = p[Keys.frameOverlay] ?: false,
            previewUserAgent = p[Keys.previewUserAgent] ?: "facebookexternalhit/1.1",
            proxiesEnabled = p[Keys.proxiesEnabled] ?: false,
            proxyTemplate = p[Keys.proxyTemplate] ?: "",
        )
    }

    suspend fun setOfflineMode(value: Boolean) = put(Keys.offlineMode, value)
    suspend fun setTapPreference(value: String) = put(Keys.tapPreference, value)
    suspend fun setPreferNativeApp(value: Boolean) = put(Keys.preferNativeApp, value)
    suspend fun setBrowserMode(value: String) = put(Keys.browserMode, value)
    suspend fun setReduceTransparency(value: Boolean) = put(Keys.reduceTransparency, value)
    suspend fun setReduceMotion(value: Boolean) = put(Keys.reduceMotion, value)
    suspend fun setGlassPreset(value: String) = put(Keys.glassPreset, value)
    suspend fun setMotionPreset(value: String) = put(Keys.motionPreset, value)
    suspend fun setTransition(value: String) = put(Keys.transition, value)
    suspend fun setAnimationSpeed(value: Float) = put(Keys.animationSpeed, value)
    suspend fun setHaptics(value: String) = put(Keys.haptics, value)
    suspend fun setHoldDelay(value: Int) = put(Keys.holdDelayMs, value)
    suspend fun setHoldEffect(value: String) = put(Keys.holdEffect, value)
    suspend fun setPerformance(value: String) = put(Keys.performance, value)
    suspend fun setFrameOverlay(value: Boolean) = put(Keys.frameOverlay, value)
    suspend fun setPreviewUserAgent(value: String) = put(Keys.previewUserAgent, value)
    suspend fun setProxiesEnabled(value: Boolean) = put(Keys.proxiesEnabled, value)
    suspend fun setProxyTemplate(value: String) = put(Keys.proxyTemplate, value)
    suspend fun setAllowHttpLinks(value: Boolean) = put(Keys.allowHttpLinks, value)
    suspend fun setHideDetailsOnLockScreen(value: Boolean) = put(Keys.hideDetailsOnLockScreen, value)
    suspend fun setBiometricLock(value: Boolean) = put(Keys.biometricLock, value)
    suspend fun setEnableClipboardChip(value: Boolean) = put(Keys.enableClipboardChip, value)

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<String>, value: String) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Float>, value: Float) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Int>, value: Int) =
        context.settingsStore.edit { it[key] = value }

    private object Keys {
        val offlineMode = booleanPreferencesKey("offline_mode")
        val tapPreference = stringPreferencesKey("tap_preference")
        val preferNativeApp = booleanPreferencesKey("prefer_native_app")
        val browserMode = stringPreferencesKey("browser_mode")
        val hideDetailsOnLockScreen = booleanPreferencesKey("hide_details_lock")
        val biometricLock = booleanPreferencesKey("biometric_lock")
        val allowHttpLinks = booleanPreferencesKey("allow_http")
        val enableClipboardChip = booleanPreferencesKey("clipboard_chip")
        val reduceTransparency = booleanPreferencesKey("reduce_transparency")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val glassPreset = stringPreferencesKey("glass_preset")
        val motionPreset = stringPreferencesKey("motion_preset")
        val transition = stringPreferencesKey("transition")
        val animationSpeed = floatPreferencesKey("animation_speed")
        val haptics = stringPreferencesKey("haptics")
        val holdDelayMs = intPreferencesKey("hold_delay_ms")
        val holdEffect = stringPreferencesKey("hold_effect")
        val performance = stringPreferencesKey("performance")
        val frameOverlay = booleanPreferencesKey("frame_overlay")
        val previewUserAgent = stringPreferencesKey("preview_user_agent")
        val proxiesEnabled = booleanPreferencesKey("proxies_enabled")
        val proxyTemplate = stringPreferencesKey("proxy_template")
    }
}
