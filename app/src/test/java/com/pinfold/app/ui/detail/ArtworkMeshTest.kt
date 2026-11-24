package com.pinfold.app.ui.detail

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMeshTest {
    @Test
    fun gridMappingFlipsVertically() {
        assertEquals(30, meshCellIndex(0, 0, 120, 120))
        assertEquals(35, meshCellIndex(119, 0, 120, 120))
        assertEquals(0, meshCellIndex(0, 119, 120, 120))
        assertEquals(5, meshCellIndex(119, 119, 120, 120))
        assertEquals(30, meshCellIndex(0, 0, 60, 120))
        assertEquals(5, meshCellIndex(59, 119, 60, 120))
    }

    @Test
    fun liftKeepsSaturationBoundedAndLuminanceFloored() {
        listOf(
            intArrayOf(0, 0, 0),
            intArrayOf(255, 32, 16),
            intArrayOf(12, 200, 90),
            intArrayOf(128, 128, 128),
        ).forEach { rgb ->
            val lifted = liftMeshColor(rgb[0], rgb[1], rgb[2])
            assertTrue(lifted.all { it in 0..255 })
            val hsl = rgbToHsl(lifted[0], lifted[1], lifted[2])
            assertTrue("saturation=${hsl[1]}", hsl[1] <= 1f)
            assertTrue("luminance=${hsl[2]}", hsl[2] >= MESH_FLOOR - 0.01f)
        }
    }

    @Test
    fun twistIsDeterministicAndLeavesFirstRowUntouched() {
        val cells = IntArray(MESH_GRID * MESH_GRID) { it + 1 }
        val first = twistMeshRows(cells, 1234L.hashCode())
        assertArrayEquals(first, twistMeshRows(cells, 1234L.hashCode()))
        assertArrayEquals(cells.copyOfRange(0, MESH_GRID), first.copyOfRange(0, MESH_GRID))
        var differs = false
        for (seed in 0 until 24) {
            if (!twistMeshRows(cells, seed).contentEquals(twistMeshRows(cells, seed + 97))) {
                differs = true
                break
            }
        }
        assertTrue(differs)
    }

    @Test
    fun smoothstepIsMonotonicWithFixedEndpoints() {
        assertEquals(0f, smoothstep(-1f), 0.0001f)
        assertEquals(1f, smoothstep(2f), 0.0001f)
        assertEquals(0.5f, smoothstep(0.5f), 0.0001f)
        var previous = smoothstep(0f)
        for (i in 1..100) {
            val value = smoothstep(i / 100f)
            assertTrue(value >= previous)
            previous = value
        }
    }

    @Test
    fun boxBlurKeepsAlphaOpaqueAndPassesAreDistinct() {
        val source = IntArray(MESH_TEX * MESH_TEX) { i ->
            if (i % 3 == 0) 0 else (0xFF000000.toInt() or (i shl 8))
        }
        assertEquals(1, MESH_BLUR_RADIUS)
        assertEquals(3, MESH_BLUR_PASSES)
        val once = boxBlur(source, MESH_TEX, MESH_TEX)
        val blurred = boxBlurPasses(source, MESH_TEX, MESH_TEX)
        assertEquals(source.size, blurred.size)
        assertTrue(blurred.all { (it ushr 24) == 0xFF })
        assertTrue(once.all { (it ushr 24) == 0xFF })
        assertFalse(once.contentEquals(blurred))
    }

    @Test
    fun resampleProducesOpaqueTextureOfConfiguredSize() {
        val cells = IntArray(MESH_GRID * MESH_GRID) { packOpaque(it * 7, 255 - it * 5, it * 3) }
        val tex = resampleMesh(cells)
        assertEquals(MESH_TEX * MESH_TEX, tex.size)
        assertTrue(tex.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun buildMeshPixelsRunsTheFullPipeline() {
        val pixels = IntArray(120 * 120) { i ->
            (0xFF shl 24) or ((i % 256) shl 16) or ((i / 120 * 2) shl 8) or 40
        }
        val baked = buildMeshPixels(pixels, 120, 120, 99)
        assertEquals(MESH_TEX * MESH_TEX, baked.size)
        assertTrue(baked.all { (it ushr 24) == 0xFF })
        assertEquals(baked.contentHashCode(), buildMeshPixels(pixels, 120, 120, 99).contentHashCode())
    }
}
