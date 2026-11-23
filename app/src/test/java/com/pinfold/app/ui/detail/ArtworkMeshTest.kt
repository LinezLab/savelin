package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMeshTest {
    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun gridSamplesTheArtworkBottomFirstAndKeepsItsProportions() {
        val size = 120
        val red = 0xFFD97123.toInt()
        val blue = 0xFF1E5ADC.toInt()
        val pixels = IntArray(size * size) { index -> if (index / size < size / 2) blue else red }
        val grid = meshGridOf(pixels, size, size, 7)
        assertNotNull(grid)
        assertEquals(MESH_GRID, grid!!.cols)
        assertEquals(MESH_GRID, grid.rows)

        val seam = Color(grid.colors[0])
        val far = Color(grid.colors[(grid.rows - 1) * grid.cols])
        assertTrue("seam should carry the artwork's bottom colour: $seam", seam.red > seam.blue)
        assertTrue("far row should carry the artwork's top colour: $far", far.blue > far.red)
        assertTrue(grid.colors.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun rotationKeepsSeamRowAndOnlyCycliclyShiftsTheRest() {
        val colors = IntArray(MESH_GRID * MESH_GRID) { it }
        val rotated = rotateBelowSeam(colors, MESH_GRID, MESH_GRID, 3)
        assertArrayEquals(colors.copyOfRange(0, MESH_GRID), rotated.copyOfRange(0, MESH_GRID))
        for (row in 1 until MESH_GRID) {
            val base = row * MESH_GRID
            assertArrayEquals(
                colors.copyOfRange(base, base + MESH_GRID).sortedArray(),
                rotated.copyOfRange(base, base + MESH_GRID).sortedArray(),
            )
        }
        assertArrayEquals(rotated, rotateBelowSeam(colors, MESH_GRID, MESH_GRID, 3))
    }

    @Test
    fun resamplingIsSmoothAndMonotonic() {
        val control = IntArray(MESH_GRID * MESH_GRID) { index ->
            val value = (index % MESH_GRID) * 51
            argb(value, value, value)
        }
        val out = resampleMesh(MeshGrid(control, MESH_GRID, MESH_GRID))
        assertEquals(MESH_TEX * MESH_TEX, out.size)
        val row = 16 * MESH_TEX
        var previous = out[row]
        for (x in 1 until MESH_TEX) {
            val current = out[row + x]
            assertTrue("sample $x should not go backwards", current >= previous)
            val redStep = ((current shr 16) and 0xFF) - ((previous shr 16) and 0xFF)
            assertTrue("sample $x jumped $previous -> $current", redStep in 0..20)
            previous = current
        }
        assertTrue(((out[row] shr 16) and 0xFF) < 32)
        assertTrue(((out[row + MESH_TEX - 1] shr 16) and 0xFF) > 220)
    }

    @Test
    fun bakedBlurRemovesCheckerboardWithoutLosingOpacity() {
        val texels = IntArray(MESH_TEX * MESH_TEX) { index ->
            val value = if (((index / MESH_TEX) + (index % MESH_TEX)) % 2 == 0) 0 else 255
            argb(value, value, value)
        }
        fun roughness(pixels: IntArray): Int {
            var total = 0
            for (y in 0 until MESH_TEX) {
                for (x in 1 until MESH_TEX) {
                    val a = Color(pixels[y * MESH_TEX + x - 1]).red
                    val b = Color(pixels[y * MESH_TEX + x]).red
                    total += kotlin.math.abs((a * 255f).toInt() - (b * 255f).toInt())
                }
            }
            return total
        }
        val before = roughness(texels)
        val after = roughness(boxBlurMesh(texels, MESH_TEX, MESH_BLUR_RADIUS, MESH_BLUR_PASSES))
        assertTrue("roughness $before -> $after", after * 2 < before)
        assertTrue(boxBlurMesh(texels, MESH_TEX, MESH_BLUR_RADIUS, MESH_BLUR_PASSES)
            .all { (it ushr 24) == 0xFF })
    }

    @Test
    fun fallbackMeshIsDeterministicOpaqueAndDifferentPerBookmark() {
        val tone = fallbackTone("YouTube", "youtube", 11L)
        val first = meshTexels(fallbackMeshGrid(tone, 5))
        val same = meshTexels(fallbackMeshGrid(fallbackTone("YouTube", "youtube", 11L), 5))
        val other = meshTexels(fallbackMeshGrid(fallbackTone("GitHub", "github", 12L), 5))
        assertArrayEquals(first, same)
        assertFalse(first.contentEquals(other))
        assertEquals(MESH_TEX * MESH_TEX, first.size)
        assertTrue(first.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun seamScrimAndDrawRegionsMatchTheAnchoredComposition() {
        assertEquals(MESH_SCRIM_TOP_ALPHA, meshScrimAlphaAt(0f), 0.0001f)
        assertEquals(MESH_SCRIM_BOTTOM_ALPHA, meshScrimAlphaAt(1f), 0.0001f)
        var previous = meshScrimAlphaAt(0f)
        for (i in 1..100) {
            val alpha = meshScrimAlphaAt(i / 100f)
            assertTrue(alpha >= previous)
            previous = alpha
        }
        assertEquals(0f, meshSeamY(-20f, 800f), 0.0001f)
        assertEquals(800f, meshSeamY(900f, 800f), 0.0001f)

        val middle = meshDrawRegions(1080f, 1920f, 600f)
        assertEquals(600, middle.heldHeight)
        assertEquals(600, middle.meshTop)
        assertEquals(1320, middle.meshHeight)
        val top = meshDrawRegions(1080f, 1920f, 0f)
        assertEquals(0, top.heldHeight)
        assertEquals(1920, top.meshHeight)
    }

    @Test
    fun texelsFromArtworkAreOpaqueAndKeepTheArtworkHue() {
        val pixels = IntArray(200 * 200) { 0xFF1E5ADC.toInt() }
        val texels = meshTexelsOf(pixels, 200, 200, 9)
        assertNotNull(texels)
        assertEquals(MESH_TEX * MESH_TEX, texels!!.size)
        assertTrue(texels.all { (it ushr 24) == 0xFF })
        val hsl = FloatArray(3)
        argbToHsl(texels[MESH_TEX * MESH_TEX / 2], hsl)
        assertTrue("hue=${hsl[0]}", hsl[0] in 195f..235f)
        assertTrue("lightness floored", hsl[2] >= MESH_FLOOR - 0.01f)
    }
}
