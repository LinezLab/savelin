package com.pinfold.app.ui.detail

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import java.util.Random
import kotlin.math.max
import kotlin.math.roundToInt

internal const val MESH_PX = 120
internal const val MESH_GRID = 6
internal const val MESH_TEX = 32
internal const val MESH_SAMPLE = 128
internal const val MESH_BLUR_RADIUS = 1
internal const val MESH_BLUR_PASSES = 3
internal const val MESH_VIBRANCE = 1.12f
internal const val MESH_FLOOR = 0.045f

internal const val MESH_SCRIM_TOP_ALPHA = 0.06f
internal const val MESH_SCRIM_BOTTOM_ALPHA = 0.30f

internal data class MeshGrid(val colors: IntArray, val cols: Int, val rows: Int)

internal fun meshScrimAlphaAt(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return MESH_SCRIM_TOP_ALPHA + (MESH_SCRIM_BOTTOM_ALPHA - MESH_SCRIM_TOP_ALPHA) * t
}

internal fun argbToHsl(argb: Int, out: FloatArray) {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val high = maxOf(r, g, b)
    val low = minOf(r, g, b)
    val delta = high - low
    val lightness = (high + low) / 2f
    if (delta == 0f) {
        out[0] = 0f
        out[1] = 0f
    } else {
        val raw = when (high) {
            r -> (g - b) / delta % 6f
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        }
        out[0] = ((raw * 60f) % 360f + 360f) % 360f
        out[1] = (delta / (1f - kotlin.math.abs(2f * lightness - 1f))).coerceIn(0f, 1f)
    }
    out[2] = lightness
}

internal fun hslToArgb(hue: Float, saturation: Float, lightness: Float): Int {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val channels = when ((h / 60f).toInt()) {
        0 -> floatArrayOf(c, x, 0f)
        1 -> floatArrayOf(x, c, 0f)
        2 -> floatArrayOf(0f, c, x)
        3 -> floatArrayOf(0f, x, c)
        4 -> floatArrayOf(x, 0f, c)
        else -> floatArrayOf(c, 0f, x)
    }
    fun byte(value: Float) = ((value + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (byte(channels[0]) shl 16) or (byte(channels[1]) shl 8) or byte(channels[2])
}

internal fun liftMeshColor(argb: Int): Int {
    val hsl = FloatArray(3)
    argbToHsl(argb, hsl)
    hsl[1] = (hsl[1] * MESH_VIBRANCE).coerceAtMost(1f)
    hsl[2] = max(hsl[2], MESH_FLOOR)
    return hslToArgb(hsl[0], hsl[1], hsl[2])
}

internal fun smootherStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * x * (x * (x * 6f - 15f) + 10f)
}

internal fun lerpArgb(from: Int, to: Int, t: Float): Int {
    val weight = t.coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * weight).roundToInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/** Row zero is the artwork's last row; every other row is carried around itself, never reshuffled. */
internal fun rotateBelowSeam(grid: IntArray, cols: Int, rows: Int, seed: Int): IntArray {
    if (rows <= 1 || cols <= 1) return grid
    val random = Random(seed.toLong())
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = grid.copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val source = if (mirror) cols - 1 - x else x
            out[base + x] = grid[base + (source + shift) % cols]
        }
    }
    return out
}

/** The sleeve's own colours, averaged into a coarse grid. No quantiser and no invented palette. */
internal fun meshGridOf(pixels: IntArray, width: Int, height: Int, seed: Int): MeshGrid? {
    if (width < 1 || height < 1 || pixels.size < width * height) return null
    val cols = MESH_GRID.coerceAtMost(width)
    val rows = MESH_GRID.coerceAtMost(height)
    val rowStep = (height / MESH_SAMPLE).coerceAtLeast(1)
    val colStep = (width / MESH_SAMPLE).coerceAtLeast(1)
    val red = LongArray(cols * rows)
    val green = LongArray(cols * rows)
    val blue = LongArray(cols * rows)
    val count = IntArray(cols * rows)

    var y = 0
    while (y < height) {
        val rowBase = ((height - 1 - y) * rows / height) * cols
        var x = 0
        while (x < width) {
            val cell = rowBase + x * cols / width
            val pixel = pixels[y * width + x]
            red[cell] += ((pixel shr 16) and 0xFF).toLong()
            green[cell] += ((pixel shr 8) and 0xFF).toLong()
            blue[cell] += (pixel and 0xFF).toLong()
            count[cell]++
            x += colStep
        }
        y += rowStep
    }

    val colors = IntArray(cols * rows) { cell ->
        val n = count[cell].coerceAtLeast(1)
        val r = (red[cell] / n).toInt().coerceIn(0, 255)
        val g = (green[cell] / n).toInt().coerceIn(0, 255)
        val b = (blue[cell] / n).toInt().coerceIn(0, 255)
        liftMeshColor((0xFF shl 24) or (r shl 16) or (g shl 8) or b)
    }
    return MeshGrid(rotateBelowSeam(colors, cols, rows, seed), cols, rows)
}

/** Deterministic, image-like field for a bookmark with no stored preview. */
internal fun fallbackMeshGrid(tone: ArtworkTone, seed: Int): MeshGrid {
    val accents = tone.accents.ifEmpty { listOf(tone.dominant) }
    val colors = IntArray(MESH_GRID * MESH_GRID) { index ->
        val accent = accents[index % accents.size]
        val shade = ((index / accents.size) % 3) * 0.09f
        liftMeshColor(lerpArgb(accent.toArgb(), tone.base.toArgb(), shade))
    }
    return MeshGrid(rotateBelowSeam(colors, MESH_GRID, MESH_GRID, seed), MESH_GRID, MESH_GRID)
}

internal fun resampleMesh(grid: MeshGrid, size: Int = MESH_TEX): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * grid.rows - 0.5f
        val y0 = kotlin.math.floor(fy).toInt().coerceIn(0, grid.rows - 1)
        val y1 = (y0 + 1).coerceAtMost(grid.rows - 1)
        val wy = smootherStep(fy - y0)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * grid.cols - 0.5f
            val x0 = kotlin.math.floor(fx).toInt().coerceIn(0, grid.cols - 1)
            val x1 = (x0 + 1).coerceAtMost(grid.cols - 1)
            val wx = smootherStep(fx - x0)
            val top = lerpArgb(grid.colors[y0 * grid.cols + x0], grid.colors[y0 * grid.cols + x1], wx)
            val bottom = lerpArgb(grid.colors[y1 * grid.cols + x0], grid.colors[y1 * grid.cols + x1], wx)
            out[ty * size + tx] = lerpArgb(top, bottom, wy)
        }
    }
    return out
}

internal fun boxBlurMesh(texels: IntArray, size: Int, radius: Int, passes: Int): IntArray {
    var source = texels.copyOf()
    var target = IntArray(source.size)
    repeat(passes) { pass ->
        val horizontal = pass % 2 == 0
        for (fixed in 0 until size) {
            fun at(offset: Int): Int = if (horizontal) {
                fixed * size + offset.coerceIn(0, size - 1)
            } else {
                offset.coerceIn(0, size - 1) * size + fixed
            }
            var r = 0
            var g = 0
            var b = 0
            val window = radius * 2 + 1
            for (offset in -radius..radius) {
                val pixel = source[at(offset)]
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
            }
            for (moving in 0 until size) {
                target[at(moving)] = (0xFF shl 24) or
                    (((r + window / 2) / window) shl 16) or
                    (((g + window / 2) / window) shl 8) or
                    ((b + window / 2) / window)
                val leaving = source[at(moving - radius)]
                val entering = source[at(moving + radius + 1)]
                r += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                g += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                b += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
        val swap = source
        source = target
        target = swap
    }
    return source
}

internal fun meshTexels(grid: MeshGrid): IntArray =
    boxBlurMesh(resampleMesh(grid), MESH_TEX, MESH_BLUR_RADIUS, MESH_BLUR_PASSES)

internal fun meshTexelsOf(pixels: IntArray, width: Int, height: Int, seed: Int): IntArray? =
    meshGridOf(pixels, width, height, seed)?.let(::meshTexels)

internal fun downsamplePixels(pixels: IntArray, width: Int, height: Int, step: Int): IntArray {
    val outWidth = max(1, width / step)
    val outHeight = max(1, height / step)
    val out = IntArray(outWidth * outHeight)
    for (y in 0 until outHeight) {
        for (x in 0 until outWidth) {
            out[y * outWidth + x] = pixels[y * step * width + x * step]
        }
    }
    return out
}

internal fun meshBitmap(texels: IntArray): ImageBitmap {
    val bitmap = Bitmap.createBitmap(texels, MESH_TEX, MESH_TEX, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}
