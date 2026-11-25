package com.pinfold.app.ui.settings

import com.pinfold.app.ui.background.BuiltInGradients
import com.pinfold.app.ui.background.DEFAULT_GRADIENT_ID
import com.pinfold.app.ui.background.GRADIENT_BLOB_COUNT
import com.pinfold.app.ui.background.generatePreset
import com.pinfold.app.ui.background.gradientBlobSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradientPresetTest {
    @Test
    fun blobSeedsAreDeterministicAndWithinUnitSquare() {
        val first = gradientBlobSeeds(1234L, GRADIENT_BLOB_COUNT)
        assertEquals(first, gradientBlobSeeds(1234L, GRADIENT_BLOB_COUNT))
        assertEquals(GRADIENT_BLOB_COUNT, first.size)
        assertEquals(emptyList<Pair<Float, Float>>(), gradientBlobSeeds(1234L, 0))
        assertEquals(-1, gradientBlobSeeds(1234L, -1).size)
        assertTrue(first.all { (x, y) -> x in 0f..1f && y in 0f..1f })
        assertTrue(first != gradientBlobSeeds(9876L, GRADIENT_BLOB_COUNT))
    }

    @Test
    fun builtInsContainAcidForestWithExactColors() {
        val acid = BuiltInGradients.first { it.id == DEFAULT_GRADIENT_ID }
        assertEquals("Acid Forest", acid.name)
        assertEquals(0xFF062738.toInt(), acid.baseColor)
        assertEquals(
            listOf(0xFFDDFF00.toInt(), 0xFF00484F.toInt(), 0xFF062738.toInt()),
            acid.blobColors,
        )
        assertEquals(10, BuiltInGradients.size)
        assertEquals(
            listOf(
                "SUNSET_PLUM", "EMBER_NIGHT", "DEEP_VIOLET", "NEON_ORCHID", "ELECTRIC_DUSK",
                "AQUA_BLOOM", "CANDY_LIME", "ULTRA_NIGHT", "ACID_FOREST", "DEEP_TEAL",
            ),
            BuiltInGradients.map { it.id },
        )
        assertTrue(BuiltInGradients.all { it.baseColor == it.blobColors.last() })
    }

    @Test
    fun generatePresetIsDeterministicWithNearBlackBaseAndEnoughBlobs() {
        val preset = generatePreset(42L)
        assertEquals(preset, generatePreset(42L))
        assertTrue(preset.blobColors.size >= 3)
        assertTrue("base max channel ${maxChannel(preset.baseColor)}", maxChannel(preset.baseColor) <= 48)
        assertEquals(0xFF, preset.baseColor ushr 24)
        assertTrue(preset.blobColors.any { maxChannel(it) > 80 })
        assertTrue(preset != generatePreset(43L))
    }

    @Test
    fun generatedColorsAreValidArgbForManySeeds() {
        (0L until 12L).forEach { index ->
            val preset = generatePreset(index * 7919L + 13L)
            (listOf(preset.baseColor) + preset.blobColors).forEach { color ->
                assertEquals(0xFF, color ushr 24)
                assertTrue(((color shr 16) and 0xFF) in 0..255)
                assertTrue(((color shr 8) and 0xFF) in 0..255)
                assertTrue((color and 0xFF) in 0..255)
            }
        }
    }

    private fun maxChannel(color: Int): Int =
        maxOf((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
}
