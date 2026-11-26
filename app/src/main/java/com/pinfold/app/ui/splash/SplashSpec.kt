package com.pinfold.app.ui.splash

internal const val SPLASH_DURATION_MS: Long = 5000L
internal const val SPLASH_DURATION_REDUCED_MS: Long = 0L
internal const val SPLASH_MAX_WAIT_MS: Long = 6000L
internal const val SPLASH_LOGO_WIDTH_FRACTION: Float = 0.8f
private const val LOGO_REF_SP = 100f

internal fun splashDurationMs(reduceMotion: Boolean): Long =
    if (reduceMotion) SPLASH_DURATION_REDUCED_MS else SPLASH_DURATION_MS

internal fun splashLogoFontSizeSp(
    screenWidthPx: Float,
    measuredWidthAtRefPx: Float,
    refSp: Float = LOGO_REF_SP,
    fraction: Float = SPLASH_LOGO_WIDTH_FRACTION,
): Float = refSp * (screenWidthPx * fraction.coerceIn(0f, 1f)) / measuredWidthAtRefPx.coerceAtLeast(1f)

internal fun usableImagePath(path: String?): String? = path?.takeIf { it.isNotBlank() }
