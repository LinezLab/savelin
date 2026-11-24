package com.pinfold.app.ui.detail

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.toBitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

internal const val MESH_GRID = 6
internal const val MESH_SAMPLE = 128
internal const val MESH_TEX = 32
internal const val MESH_VIBRANCE = 1.12f
internal const val MESH_FLOOR = 0.045f
internal const val MESH_BLUR_RADIUS = 1
internal const val MESH_BLUR_PASSES = 3
internal const val MESH_FADE_MS = 900
internal const val MESH_CACHE_ENTRIES = 64
internal const val MESH_DECODE_SIZE = 120

internal val MeshFloorColor: Color = Color(0xFF, 0x12, 0x12)

internal fun fallbackHeroColor(seed: Long): Color {
    var hash = 17L
    for (ch in "savelin-hero-$seed") hash = hash * 31L + ch.code
    val hue = Math.floorMod(hash, 360L).toFloat()
    return Color.hsv(hue, 0.12f, 0.16f)
}

internal fun meshDescriptor(): String = "${MESH_GRID}x${MESH_GRID}/$MESH_TEX"

private val meshCache = LinkedHashMap<Long, ImageBitmap>()

@Synchronized
internal fun cachedMesh(id: Long): ImageBitmap? = meshCache[id]

@Synchronized
internal fun cacheMesh(id: Long, mesh: ImageBitmap) {
    meshCache[id] = mesh
    while (meshCache.size > MESH_CACHE_ENTRIES) {
        meshCache.remove(meshCache.keys.first())
    }
}

internal fun packOpaque(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

internal fun argbChannel(argb: Int, shift: Int): Int = (argb ushr shift) and 0xFF

internal fun meshCellIndex(x: Int, y: Int, width: Int, height: Int): Int {
    val col = (x.toLong() * MESH_GRID / width).toInt().coerceIn(0, MESH_GRID - 1)
    val flippedY = height - 1 - y
    val row = (flippedY.toLong() * MESH_GRID / height).toInt().coerceIn(0, MESH_GRID - 1)
    return row * MESH_GRID + col
}

internal fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val high = maxOf(rf, gf, bf)
    val low = minOf(rf, gf, bf)
    val l = (high + low) / 2f
    if (high == low) return floatArrayOf(0f, 0f, l)
    val d = high - low
    val s = if (l > 0.5f) d / (2f - high - low) else d / (high + low)
    val h = when {
        high == rf -> ((gf - bf) / d) % 6f
        high == gf -> (bf - rf) / d + 2f
        else -> (rf - gf) / d + 4f
    } * 60f
    return floatArrayOf(
        if (h < 0f) h + 360f else h,
        s.coerceIn(0f, 1f),
        l.coerceIn(0f, 1f),
    )
}

internal fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
    val c = (1f - abs(2f * l - 1f)) * s.coerceIn(0f, 1f)
    val hp = (((h % 360f) + 360f) % 360f) / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val rgb = when (hp.toInt().coerceIn(0, 5)) {
        0 -> floatArrayOf(c, x, 0f)
        1 -> floatArrayOf(x, c, 0f)
        2 -> floatArrayOf(0f, c, x)
        3 -> floatArrayOf(0f, x, c)
        4 -> floatArrayOf(x, 0f, c)
        else -> floatArrayOf(c, 0f, x)
    }
    val m = l - c / 2f
    return intArrayOf(
        ((rgb[0] + m) * 255f).toInt().coerceIn(0, 255),
        ((rgb[1] + m) * 255f).toInt().coerceIn(0, 255),
        ((rgb[2] + m) * 255f).toInt().coerceIn(0, 255),
    )
}

internal fun liftMeshColor(r: Int, g: Int, b: Int): IntArray {
    val hsl = rgbToHsl(r, g, b)
    return hslToRgb(hsl[0], minOf(1f, hsl[1] * MESH_VIBRANCE), maxOf(hsl[2], MESH_FLOOR))
}

internal fun twistMeshRows(cells: IntArray, seed: Int, cols: Int = MESH_GRID, rows: Int = MESH_GRID): IntArray {
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val result = cells.copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (col in 0 until cols) {
            val sourceCol = if (mirror) cols - 1 - col else col
            result[base + col] = cells[base + ((sourceCol + shift) % cols)]
        }
    }
    return result
}

internal fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun lerpChannel(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).toInt().coerceIn(0, 255)

internal fun interpolateChannels(a: Int, b: Int, c: Int, d: Int, wx: Float, wy: Float): Int {
    val r = lerpChannel(
        lerpChannel(argbChannel(a, 16), argbChannel(b, 16), wx),
        lerpChannel(argbChannel(c, 16), argbChannel(d, 16), wx),
        wy,
    )
    val g = lerpChannel(
        lerpChannel(argbChannel(a, 8), argbChannel(b, 8), wx),
        lerpChannel(argbChannel(c, 8), argbChannel(d, 8), wx),
        wy,
    )
    val bl = lerpChannel(
        lerpChannel(argbChannel(a, 0), argbChannel(b, 0), wx),
        lerpChannel(argbChannel(c, 0), argbChannel(d, 0), wx),
        wy,
    )
    return packOpaque(r, g, bl)
}

internal fun resampleMesh(
    cells: IntArray,
    cols: Int = MESH_GRID,
    rows: Int = MESH_GRID,
    size: Int = MESH_TEX,
): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * rows - 0.5f
        val y0 = floor(fy).toInt()
        val wy = smoothstep(fy - y0)
        val cy0 = y0.coerceIn(0, rows - 1)
        val cy1 = (y0 + 1).coerceIn(0, rows - 1)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * cols - 0.5f
            val x0 = floor(fx).toInt()
            val wx = smoothstep(fx - x0)
            val cx0 = x0.coerceIn(0, cols - 1)
            val cx1 = (x0 + 1).coerceIn(0, cols - 1)
            out[ty * size + tx] = interpolateChannels(
                cells[cy0 * cols + cx0],
                cells[cy0 * cols + cx1],
                cells[cy1 * cols + cx0],
                cells[cy1 * cols + cx1],
                wx,
                wy,
            )
        }
    }
    return out
}

internal fun boxBlur(
    source: IntArray,
    width: Int,
    height: Int,
    radius: Int = MESH_BLUR_RADIUS,
): IntArray {
    if (radius <= 0 || width < 1 || height < 1) return source.copyOf()
    val window = radius * 2 + 1
    val horizontal = IntArray(width * height)
    val vertical = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * width
        var sr = 0
        var sg = 0
        var sb = 0
        for (k in -radius..radius) {
            val color = source[row + k.coerceIn(0, width - 1)]
            sr += argbChannel(color, 16)
            sg += argbChannel(color, 8)
            sb += argbChannel(color, 0)
        }
        for (x in 0 until width) {
            horizontal[row + x] = packOpaque(sr / window, sg / window, sb / window)
            val removed = source[row + (x - radius).coerceIn(0, width - 1)]
            val added = source[row + (x + radius + 1).coerceIn(0, width - 1)]
            sr += argbChannel(added, 16) - argbChannel(removed, 16)
            sg += argbChannel(added, 8) - argbChannel(removed, 8)
            sb += argbChannel(added, 0) - argbChannel(removed, 0)
        }
    }
    for (x in 0 until width) {
        var sr = 0
        var sg = 0
        var sb = 0
        for (k in -radius..radius) {
            val color = horizontal[k.coerceIn(0, height - 1) * width + x]
            sr += argbChannel(color, 16)
            sg += argbChannel(color, 8)
            sb += argbChannel(color, 0)
        }
        for (y in 0 until height) {
            vertical[y * width + x] = packOpaque(sr / window, sg / window, sb / window)
            val removed = horizontal[(y - radius).coerceIn(0, height - 1) * width + x]
            val added = horizontal[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            sr += argbChannel(added, 16) - argbChannel(removed, 16)
            sg += argbChannel(added, 8) - argbChannel(removed, 8)
            sb += argbChannel(added, 0) - argbChannel(removed, 0)
        }
    }
    return vertical
}

internal fun boxBlurPasses(
    source: IntArray,
    width: Int,
    height: Int,
    passes: Int = MESH_BLUR_PASSES,
): IntArray {
    var result = source
    repeat(passes) { result = boxBlur(result, width, height) }
    return result
}

internal fun buildMeshPixels(pixels: IntArray, width: Int, height: Int, seed: Int): IntArray {
    if (width < 1 || height < 1 || pixels.size < width * height) {
        return IntArray(MESH_TEX * MESH_TEX) { packOpaque(0, 0, 0) }
    }
    val cells = MESH_GRID * MESH_GRID
    val sumsR = LongArray(cells)
    val sumsG = LongArray(cells)
    val sumsB = LongArray(cells)
    val counts = IntArray(cells)
    val stride = max(1, maxOf(width, height) / MESH_SAMPLE)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val index = meshCellIndex(x, y, width, height)
            val color = pixels[y * width + x]
            sumsR[index] += argbChannel(color, 16)
            sumsG[index] += argbChannel(color, 8)
            sumsB[index] += argbChannel(color, 0)
            counts[index]++
            x += stride
        }
        y += stride
    }
    val lifted = IntArray(cells)
    for (i in 0 until cells) {
        val n = counts[i]
        val rgb = if (n == 0) {
            liftMeshColor(0, 0, 0)
        } else {
            liftMeshColor(
                (sumsR[i] / n).toInt(),
                (sumsG[i] / n).toInt(),
                (sumsB[i] / n).toInt(),
            )
        }
        lifted[i] = packOpaque(rgb[0], rgb[1], rgb[2])
    }
    return boxBlurPasses(resampleMesh(twistMeshRows(lifted, seed)), MESH_TEX, MESH_TEX)
}

internal suspend fun loadMesh(context: Context, id: Long, path: String): ImageBitmap? = withContext(Dispatchers.IO) {
    cachedMesh(id)?.let { return@withContext it }
    try {
        val request = ImageRequest.Builder(context)
            .data(File(path))
            .size(MESH_DECODE_SIZE)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        if (result !is SuccessResult) {
            artworkIssue("mesh", "reason=decodeFailed")
            return@withContext null
        }
        val bitmap = result.image.toBitmap()
        val source = bitmap.asImageBitmap()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val baked = buildMeshPixels(pixels, width, height, id.hashCode())
        val mesh = Bitmap.createBitmap(baked, MESH_TEX, MESH_TEX, Bitmap.Config.ARGB_8888).asImageBitmap()
        cacheMesh(id, mesh)
        logArtwork("Mesh built grid=${MESH_GRID}x${MESH_GRID} tex=$MESH_TEX source=IMAGE")
        mesh
    } catch (e: Exception) {
        artworkIssue("mesh", "reason=exception:${e.javaClass.simpleName}")
        null
    }
}
