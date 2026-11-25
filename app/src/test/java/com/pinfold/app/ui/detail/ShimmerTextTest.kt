package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ShimmerTextTest {
    @Test
    fun shimmerDurationIs2200MsAndBandIs55Percent() {
        assertEquals(2200, TEXT_SHIMMER_DURATION_MS)
        assertEquals(0.55f, TEXT_SHIMMER_BAND_FRACTION, 0.0001f)
    }

    @Test
    fun shimmerColorsDimEdgesAndCenterABrightBand() {
        val base = Color.White
        val colors = textShimmerColors(base)
        assertEquals(5, colors.size)
        assertEquals(base.copy(alpha = 0.75f), colors[0])
        assertEquals(base, colors[1])
        assertEquals(Color.White.copy(alpha = 0.95f), colors[2])
        assertEquals(base, colors[3])
        assertEquals(base.copy(alpha = 0.75f), colors[4])
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
        assertEquals(1f, textShimmerSpan(0.5f, 0f).second, 0.0001f)
    }
}
