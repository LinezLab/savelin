package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import com.pinfold.app.ui.glass.theme.Candy
import org.junit.Assert.assertEquals
import org.junit.Test

class ShimmerTextTest {
    @Test
    fun shimmerDurationIs2200MsAndBandIs55Percent() {
        assertEquals(2200, TEXT_SHIMMER_DURATION_MS)
        assertEquals(0.55f, TEXT_SHIMMER_BAND_FRACTION, 0.0001f)
    }

    @Test
    fun shimmerColorsDimEdgesAndCenterALemonBand() {
        val base = Color.White
        val colors = textShimmerColors(base)
        assertEquals(5, colors.size)
        assertEquals(base.copy(alpha = 0.9f), colors[0])
        assertEquals(base, colors[1])
        assertEquals(Candy.Lemon.copy(alpha = 1f), colors[2])
        assertEquals(base, colors[3])
        assertEquals(base.copy(alpha = 0.9f), colors[4])
    }

    @Test
    fun shimmerSpanSweepsFromBeforeTextToPastText() {
        assertEquals(-55f, textShimmerSpan(0f, 100f).first, 0.0001f)
        assertEquals(0f, textShimmerSpan(0f, 100f).second, 0.0001f)
        val middle = textShimmerSpan(0.5f, 100f)
        assertEquals(22.5f, middle.first, 0.0001f)
        assertEquals(77.5f, middle.second, 0.0001f)
        val end = textShimmerSpan(1f, 100f)
        assertEquals(100f, end.first, 0.0001f)
        assertEquals(155f, end.second, 0.0001f)
    }

    @Test
    fun shimmerSpanClampsOutOfRangeFractionsAndZeroWidth() {
        assertEquals(textShimmerSpan(0f, 100f), textShimmerSpan(-0.5f, 100f))
        assertEquals(textShimmerSpan(1f, 100f), textShimmerSpan(1.5f, 100f))
        assertEquals(-1f, textShimmerSpan(0f, 0f).first, 0.0001f)
        assertEquals(0.5f, textShimmerSpan(0.5f, 0f).second, 0.0001f)
    }

    @Test
    fun shimmerDurationMapsSpeedToTweenMillis() {
        assertEquals(3600, shimmerDurationMs("SLOW"))
        assertEquals(2200, shimmerDurationMs("NORMAL"))
        assertEquals(1200, shimmerDurationMs("FAST"))
        assertEquals(TEXT_SHIMMER_DURATION_MS, shimmerDurationMs(""))
        assertEquals(TEXT_SHIMMER_DURATION_MS, shimmerDurationMs("UNKNOWN"))
    }

    @Test
    fun shimmerBandColorsMapPresetNames() {
        assertEquals(listOf(Candy.Lemon), shimmerBandColors("LEMON"))
        assertEquals(listOf(Color(0xFF9E9E9E)), shimmerBandColors("GREY"))
        assertEquals(listOf(Color.White), shimmerBandColors("WHITE"))
        assertEquals(listOf(Color.Black), shimmerBandColors("BLACK"))
        assertEquals(
            listOf(Color(0xFF1A1A1A), Color.White, Color(0xFF9E9E9E)),
            shimmerBandColors("DARK_WHITE_GREY"),
        )
    }

    @Test
    fun removedAndUnknownPresetsFallBackToLemon() {
        assertEquals(listOf(Candy.Lemon), shimmerBandColors("MINT"))
        assertEquals(listOf(Candy.Lemon), shimmerBandColors("SKY"))
        assertEquals(listOf(Candy.Lemon), shimmerBandColors(""))
        assertEquals(listOf(Candy.Lemon), shimmerBandColors("UNKNOWN"))
    }

    @Test
    fun customPresetUsesTheSuppliedArgbAndDefaultsToLemon() {
        assertEquals(listOf(Color(0xFF123456)), shimmerBandColors("CUSTOM", 0xFF123456))
        assertEquals(listOf(Candy.Lemon), shimmerBandColors("CUSTOM"))
    }

    @Test
    fun legacyBandColorReturnsFirstBandColor() {
        assertEquals(Candy.Lemon, shimmerBandColor("LEMON"))
        assertEquals(Color(0xFF9E9E9E), shimmerBandColor("GREY"))
        assertEquals(Color(0xFF1A1A1A), shimmerBandColor("DARK_WHITE_GREY"))
        assertEquals(Candy.Lemon, shimmerBandColor("MINT"))
    }

    @Test
    fun textShimmerColorsUsesSelectedBandAndKeepsFiveStops() {
        val base = Color.White
        val colors = textShimmerColors(base, shimmerBandColors("GREY"))
        assertEquals(5, colors.size)
        assertEquals(base.copy(alpha = 0.9f), colors[0])
        assertEquals(base, colors[1])
        assertEquals(Color(0xFF9E9E9E).copy(alpha = 1f), colors[2])
        assertEquals(base, colors[3])
        assertEquals(base.copy(alpha = 0.9f), colors[4])
    }

    @Test
    fun darkWhiteGreySweepBuildsSevenStopsAcrossBandWindow() {
        val base = Color.White
        val colors = textShimmerColors(base, shimmerBandColors("DARK_WHITE_GREY"))
        assertEquals(7, colors.size)
        assertEquals(base.copy(alpha = 0.9f), colors[0])
        assertEquals(base, colors[1])
        assertEquals(Color(0xFF1A1A1A).copy(alpha = 1f), colors[2])
        assertEquals(Color.White.copy(alpha = 1f), colors[3])
        assertEquals(Color(0xFF9E9E9E).copy(alpha = 1f), colors[4])
        assertEquals(base, colors[5])
        assertEquals(base.copy(alpha = 0.9f), colors[6])
    }

    @Test
    fun emptyBandListFallsBackToLemon() {
        assertEquals(textShimmerColors(Color.White), textShimmerColors(Color.White, emptyList()))
    }

    @Test
    fun disabledShimmerReturnsSolidBaseColors() {
        val base = Color.White
        assertEquals(listOf(base, base), shimmerBrushColors(base, false, "LEMON"))
        assertEquals(listOf(base, base), shimmerBrushColors(base, false, "CUSTOM", 0xFF123456))
        assertEquals(5, shimmerBrushColors(base, true, "GREY").size)
        assertEquals(Color(0xFF9E9E9E), shimmerBrushColors(base, true, "GREY")[2])
        assertEquals(7, shimmerBrushColors(base, true, "DARK_WHITE_GREY").size)
        assertEquals(Color(0xFF123456), shimmerBrushColors(base, true, "CUSTOM", 0xFF123456)[2])
    }
}
