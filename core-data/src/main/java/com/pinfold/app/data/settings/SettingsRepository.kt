package com.pinfold.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val saveLinksPrivateByDefault: Boolean = false,
    val privateVault: Boolean = false,
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
    val cardColumns: Int = 2,
    val cardSpacing: String = "TIGHT",
    val cardCaptionStyle: String = "BELOW",
    val cardTitleLines: Int = 1,
    val cardShowAuthorLine: Boolean = false,
    val limitTallImages: Boolean = false,
    val trueBlack: Boolean = false,
    val themeMode: String = "AUTO",
    val language: String = "System",
    val defaultView: String = "CARD",
    val tapAction: String = "OPEN",
    val sortOrder: String = "NEWEST",
    val trashRetentionDays: Int = 30,
    val openLinksIn: String = "NATIVE",
    val previewIdentity: String = "MOBILE",
    val previewHelpers: Boolean = false,
    val refetchMissing: Boolean = true,
    val imageQuality: String = "BALANCED",
    val notifications: Boolean = true,
    val defaultSnoozeMinutes: Int = 60,
    val reminderRepeatMinutes: Int = 0,
    val blockScreenshots: Boolean = false,
    val iconLabels: String = "ICONS_ONLY",
    val rememberFilters: Boolean = false,
    val importantFilter: Boolean = false,
    val reminderFilter: Boolean = false,
    val chipEffect: String = "LIGHT",
    val componentGlass: String = "",
    val immersiveMode: Boolean = false,
    val adaptiveLuminance: Boolean = false,
    val touchHoldEffect: Boolean = true,
    val transitionType: String = "GLASS_MORPH",
    val transitionCurve: String = "DRAWER",
    val backgroundType: String = "GENERATED",
    val backgroundStyle: String = "PETALS",
    val backgroundHue: Float = 220f,
    val backgroundLoopSeconds: Int = 40,
    val backgroundAnimate: Boolean = true,
    val backgroundDim: Float = 0.2f,
    val backgroundDetails: String = "ARTWORK",
    val detailAmbientBackground: Boolean = true,
    val backgroundShape: String = "SOFT",
    val backgroundSource: String = "PALETTE",
    val backgroundFps: Int = 0,
    val topShade: Boolean = true,
    val topShadeHeight: Int = 28,
    val topShadeStrength: Float = 0.35f,
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        val storedDetails = p[Keys.backgroundDetails]
        AppSettings(
            offlineMode = p[Keys.offlineMode] ?: false,
            tapPreference = p[Keys.tapPreference] ?: "OPEN",
            preferNativeApp = p[Keys.preferNativeApp] ?: true,
            browserMode = p[Keys.browserMode] ?: "SYSTEM",
            hideDetailsOnLockScreen = p[Keys.hideDetailsOnLockScreen] ?: true,
            biometricLock = p[Keys.biometricLock] ?: false,
            allowHttpLinks = p[Keys.allowHttpLinks] ?: false,
            enableClipboardChip = p[Keys.enableClipboardChip] ?: false,
            saveLinksPrivateByDefault = p[Keys.saveLinksPrivateByDefault] ?: false,
            privateVault = p[Keys.privateVault] ?: false,
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
            cardColumns = p[Keys.cardColumns] ?: 2,
            cardSpacing = p[Keys.cardSpacing] ?: "TIGHT",
            cardCaptionStyle = p[Keys.cardCaptionStyle] ?: "BELOW",
            cardTitleLines = p[Keys.cardTitleLines] ?: 1,
            cardShowAuthorLine = p[Keys.cardShowAuthorLine] ?: false,
            limitTallImages = p[Keys.limitTallImages] ?: false,
            trueBlack = p[Keys.trueBlack] ?: false,
            themeMode = p[Keys.themeMode] ?: "AUTO",
            language = p[Keys.language] ?: "System",
            defaultView = p[Keys.defaultView] ?: "CARD",
            tapAction = p[Keys.tapAction] ?: "OPEN",
            sortOrder = p[Keys.sortOrder] ?: "NEWEST",
            trashRetentionDays = p[Keys.trashRetentionDays] ?: 30,
            openLinksIn = p[Keys.openLinksIn] ?: "NATIVE",
            previewIdentity = p[Keys.previewIdentity] ?: "MOBILE",
            previewHelpers = p[Keys.previewHelpers] ?: false,
            refetchMissing = p[Keys.refetchMissing] ?: true,
            imageQuality = p[Keys.imageQuality] ?: "BALANCED",
            notifications = p[Keys.notifications] ?: true,
            defaultSnoozeMinutes = p[Keys.defaultSnoozeMinutes] ?: 60,
            reminderRepeatMinutes = p[Keys.reminderRepeatMinutes] ?: 0,
            blockScreenshots = p[Keys.blockScreenshots] ?: false,
            iconLabels = p[Keys.iconLabels] ?: "ICONS_ONLY",
            rememberFilters = p[Keys.rememberFilters] ?: false,
            importantFilter = p[Keys.importantFilter] ?: false,
            reminderFilter = p[Keys.reminderFilter] ?: false,
            chipEffect = p[Keys.chipEffect] ?: "LIGHT",
            componentGlass = p[Keys.componentGlass] ?: "",
            immersiveMode = p[Keys.immersiveMode] ?: false,
            adaptiveLuminance = p[Keys.adaptiveLuminance] ?: false,
            touchHoldEffect = p[Keys.touchHoldEffect] ?: true,
            transitionType = p[Keys.transitionType] ?: "GLASS_MORPH",
            transitionCurve = p[Keys.transitionCurve] ?: "DRAWER",
            backgroundType = p[Keys.backgroundType] ?: "GENERATED",
            backgroundStyle = p[Keys.backgroundStyle] ?: "PETALS",
            backgroundHue = p[Keys.backgroundHue] ?: 220f,
            backgroundLoopSeconds = p[Keys.backgroundLoopSeconds] ?: 40,
            backgroundAnimate = p[Keys.backgroundAnimate] ?: true,
            backgroundDim = p[Keys.backgroundDim] ?: 0.2f,
            backgroundDetails = when (storedDetails) {
                null, "PICTURE" -> "ARTWORK"
                "SOLID" -> "THEME"
                else -> storedDetails ?: "ARTWORK"
            },
            detailAmbientBackground = p[Keys.detailAmbientBackground] ?: true,
            backgroundShape = p[Keys.backgroundShape] ?: "SOFT",
            backgroundSource = p[Keys.backgroundSource] ?: "PALETTE",
            backgroundFps = p[Keys.backgroundFps] ?: 0,
            topShade = p[Keys.topShade] ?: true,
            topShadeHeight = p[Keys.topShadeHeight] ?: 28,
            topShadeStrength = p[Keys.topShadeStrength] ?: 0.35f,
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
    suspend fun setCardColumns(value: Int) = put(Keys.cardColumns, value)
    suspend fun setCardSpacing(value: String) = put(Keys.cardSpacing, value)
    suspend fun setCardCaptionStyle(value: String) = put(Keys.cardCaptionStyle, value)
    suspend fun setCardTitleLines(value: Int) = put(Keys.cardTitleLines, value)
    suspend fun setCardShowAuthorLine(value: Boolean) = put(Keys.cardShowAuthorLine, value)
    suspend fun setLimitTallImages(value: Boolean) = put(Keys.limitTallImages, value)
    suspend fun setTrueBlack(value: Boolean) = put(Keys.trueBlack, value)
    suspend fun setAllowHttpLinks(value: Boolean) = put(Keys.allowHttpLinks, value)
    suspend fun setHideDetailsOnLockScreen(value: Boolean) = put(Keys.hideDetailsOnLockScreen, value)
    suspend fun setBiometricLock(value: Boolean) = put(Keys.biometricLock, value)
    suspend fun setEnableClipboardChip(value: Boolean) = put(Keys.enableClipboardChip, value)

    suspend fun exportAll(): Map<String, Any?> = context.settingsStore.data.first().asMap().mapKeys { it.key.name }

    suspend fun importAll(values: Map<String, Any?>) {
        context.settingsStore.edit { prefs ->
            values.forEach { (name, value) ->
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(name)] = value
                    is String -> prefs[stringPreferencesKey(name)] = value
                    is Int -> prefs[intPreferencesKey(name)] = value
                    is Long -> prefs[longPreferencesKey(name)] = value
                    is Float -> prefs[floatPreferencesKey(name)] = value
                    else -> Unit
                }
            }
        }
    }

    suspend fun set(name: String, value: Any) {
        when (value) {
            is Boolean -> put(booleanPreferencesKey(name), value)
            is String -> put(stringPreferencesKey(name), value)
            is Int -> put(intPreferencesKey(name), value)
            is Long -> put(longPreferencesKey(name), value)
            is Float -> put(floatPreferencesKey(name), value)
        }
    }

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<String>, value: String) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Float>, value: Float) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Int>, value: Int) =
        context.settingsStore.edit { it[key] = value }

    private suspend fun put(key: Preferences.Key<Long>, value: Long) =
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
        val saveLinksPrivateByDefault = booleanPreferencesKey("saveLinksPrivateByDefault")
        val privateVault = booleanPreferencesKey("privateVault")
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
        val cardColumns = intPreferencesKey("card_columns")
        val cardSpacing = stringPreferencesKey("card_spacing")
        val cardCaptionStyle = stringPreferencesKey("card_caption_style")
        val cardTitleLines = intPreferencesKey("card_title_lines")
        val cardShowAuthorLine = booleanPreferencesKey("card_show_author")
        val limitTallImages = booleanPreferencesKey("limit_tall_images")
        val trueBlack = booleanPreferencesKey("trueBlack")
        val themeMode = stringPreferencesKey("themeMode")
        val language = stringPreferencesKey("language")
        val defaultView = stringPreferencesKey("defaultView")
        val tapAction = stringPreferencesKey("tapAction")
        val sortOrder = stringPreferencesKey("sortOrder")
        val trashRetentionDays = intPreferencesKey("trashRetentionDays")
        val openLinksIn = stringPreferencesKey("openLinksIn")
        val previewIdentity = stringPreferencesKey("previewIdentity")
        val previewHelpers = booleanPreferencesKey("previewHelpers")
        val refetchMissing = booleanPreferencesKey("refetchMissing")
        val imageQuality = stringPreferencesKey("imageQuality")
        val notifications = booleanPreferencesKey("notifications")
        val defaultSnoozeMinutes = intPreferencesKey("defaultSnoozeMinutes")
        val reminderRepeatMinutes = intPreferencesKey("reminderRepeatMinutes")
        val blockScreenshots = booleanPreferencesKey("blockScreenshots")
        val iconLabels = stringPreferencesKey("iconLabels")
        val rememberFilters = booleanPreferencesKey("rememberFilters")
        val importantFilter = booleanPreferencesKey("importantFilter")
        val reminderFilter = booleanPreferencesKey("reminderFilter")
        val chipEffect = stringPreferencesKey("chipEffect")
        val componentGlass = stringPreferencesKey("componentGlass")
        val immersiveMode = booleanPreferencesKey("immersiveMode")
        val adaptiveLuminance = booleanPreferencesKey("adaptiveLuminance")
        val touchHoldEffect = booleanPreferencesKey("touchHoldEffect")
        val transitionType = stringPreferencesKey("transitionType")
        val transitionCurve = stringPreferencesKey("transitionCurve")
        val backgroundType = stringPreferencesKey("backgroundType")
        val backgroundStyle = stringPreferencesKey("backgroundStyle")
        val backgroundHue = floatPreferencesKey("backgroundHue")
        val backgroundLoopSeconds = intPreferencesKey("backgroundLoopSeconds")
        val backgroundAnimate = booleanPreferencesKey("backgroundAnimate")
        val backgroundDim = floatPreferencesKey("backgroundDim")
        val backgroundDetails = stringPreferencesKey("backgroundDetails")
        val detailAmbientBackground = booleanPreferencesKey("detailAmbientBackground")
        val backgroundShape = stringPreferencesKey("backgroundShape")
        val backgroundSource = stringPreferencesKey("backgroundSource")
        val backgroundFps = intPreferencesKey("backgroundFps")
        val topShade = booleanPreferencesKey("topShade")
        val topShadeHeight = intPreferencesKey("topShadeHeight")
        val topShadeStrength = floatPreferencesKey("topShadeStrength")
    }
}
