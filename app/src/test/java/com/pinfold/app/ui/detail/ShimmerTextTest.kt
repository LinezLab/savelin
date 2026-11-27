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
    fun shimmerBandColorMapsPresetNames() {
        assertEquals(Candy.Lemon, shimmerBandColor("LEMON"))
        assertEquals(Candy.Mint, shimmerBandColor("MINT"))
        assertEquals(Candy.Sky, shimmerBandColor("SKY"))
        assertEquals(Candy.Lemon, shimmerBandColor(""))
        assertEquals(Candy.Lemon, shimmerBandColor("UNKNOWN"))
    }

    @Test
    fun shimmerColorsUseSelectedBandAndKeepFiveStops() {
        val base = Color.White
        val colors = textShimmerColors(base, shimmerBandColor("MINT"))
        assertEquals(5, colors.size)
        assertEquals(base.copy(alpha = 0.9f), colors[0])
        assertEquals(base, colors[1])
        assertEquals(Candy.Mint.copy(alpha = 1f), colors[2])
        assertEquals(base, colors[3])
        assertEquals(base.copy(alpha = 0.9f), colors[4])
    }

    @Test
    fun disabledShimmerReturnsSolidBaseColors() {
        val base = Color.White
        assertEquals(listOf(base, base), shimmerBrushColors(base, false, "LEMON"))
        assertEquals(listOf(base, base), shimmerBrushColors(base, false, "MINT"))
        assertEquals(5, shimmerBrushColors(base, true, "SKY").size)
        assertEquals(Candy.Sky.copy(alpha = 1f), shimmerBrushColors(base, true, "SKY")[2])
    }
}
