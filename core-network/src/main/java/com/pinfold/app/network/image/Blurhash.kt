package com.pinfold.app.network.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

object Blurhash {
    private const val BASE83 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"

    fun encode(bitmap: Bitmap, xComponents: Int = 4, yComponents: Int = 3): String {
        val width = 32
        val height = 32
        val small = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)

        val factors = Array(yComponents) { Array(xComponents) { FloatArray(3) } }
        for (y in 0 until yComponents) {
            for (x in 0 until xComponents) {
                var r = 0f
                var g = 0f
                var b = 0f
                var norm = 0f
                for (j in 0 until height) {
                    for (i in 0 until width) {
                        val basis = ((Math.PI * x * i / width).let(::cos) * (Math.PI * y * j / height).let(::cos)).toFloat()
                        val pixel = pixels[j * width + i]
                        r += basis * srgbToLinear(Color.red(pixel) / 255f)
                        g += basis * srgbToLinear(Color.green(pixel) / 255f)
                        b += basis * srgbToLinear(Color.blue(pixel) / 255f)
                        norm += basis * basis
                    }
                }
                val scale = if (norm == 0f) 0f else 1f / norm
                factors[y][x][0] = r * scale
                factors[y][x][1] = g * scale
                factors[y][x][2] = b * scale
            }
        }

        val dc = factors[0][0]
        var maximum = 0f
        for (y in 0 until yComponents) {
            for (x in 0 until xComponents) {
                if (x == 0 && y == 0) continue
                for (c in 0..2) maximum = max(maximum, kotlin.math.abs(factors[y][x][c]))
            }
        }
        val quantMax = if (maximum > 0f) maximum * (if (xComponents > 1 && yComponents > 1) 1f else 1.15f) else 1f

        val out = StringBuilder()
        val sizeFlag = (xComponents - 1) + (yComponents - 1) * 9
        out.append(toBase83(sizeFlag, 1))
        val dcValue = (linearToSrgb(dc[0]).roundToInt() shl 16) or
            (linearToSrgb(dc[1]).roundToInt() shl 8) or
            linearToSrgb(dc[2]).roundToInt()
        out.append(toBase83(dcValue, 4))
        for (y in 0 until yComponents) {
            for (x in 0 until xComponents) {
                if (x == 0 && y == 0) continue
                val factor = factors[y][x]
                out.append(toBase83(encodeAc(factor[0], quantMax), 2))
                out.append(toBase83(encodeAc(factor[1], quantMax), 2))
                out.append(toBase83(encodeAc(factor[2], quantMax), 2))
            }
        }
        return out.toString()
    }

    /**
     * Fallback for cards saved before a tiny blurred preview existed: decode the stored blurhash into
     * a small bitmap. The caller applies the same crop and tint it would apply to a preview file.
     */
    fun decode(hash: String, width: Int, height: Int, punch: Float = 1f): Bitmap? {
        if (hash.length < 6) return null
        val sizeFlag = decode83(hash.substring(0, 1))
        val countX = sizeFlag % 9 + 1
        val countY = sizeFlag / 9 + 1
        if (countX < 1 || countY < 1 || hash.length != 4 + 2 * countX * countY) return null
        if (width < 1 || height < 1) return null
        val dc = decode83(hash.substring(1, 5))
        val dcR = srgbToLinear(((dc shr 16) and 0xFF) / 255f)
        val dcG = srgbToLinear(((dc shr 8) and 0xFF) / 255f)
        val dcB = srgbToLinear((dc and 0xFF) / 255f)

        var maximum = 0f
        for (i in 5 until hash.length step 2) maximum = max(maximum, decode83(hash.substring(i, i + 2)) / 9f)
        val quantMax = if (maximum > 0f) maximum * punch else 1f

        val factors = Array(countY) { Array(countX) { FloatArray(3) } }
        var index = 5
        for (y in 0 until countY) {
            for (x in 0 until countX) {
                if (x == 0 && y == 0) continue
                if (index + 6 > hash.length) return null
                factors[y][x][0] = decodeAc(decode83(hash.substring(index, index + 2)), quantMax)
                factors[y][x][1] = decodeAc(decode83(hash.substring(index + 2, index + 4)), quantMax)
                factors[y][x][2] = decodeAc(decode83(hash.substring(index + 4, index + 6)), quantMax)
                index += 6
            }
        }

        val pixels = IntArray(width * height)
        for (j in 0 until height) {
            for (i in 0 until width) {
                var r = dcR
                var g = dcG
                var b = dcB
                for (y in 0 until countY) {
                    for (x in 0 until countX) {
                        if (x == 0 && y == 0) continue
                        val basis = (cos(Math.PI * x * i / width) * cos(Math.PI * y * j / height)).toFloat()
                        r += basis * factors[y][x][0]
                        g += basis * factors[y][x][1]
                        b += basis * factors[y][x][2]
                    }
                }
                pixels[j * width + i] = Color.rgb(
                    (linearToSrgb(r) * 255f).toInt().coerceIn(0, 255),
                    (linearToSrgb(g) * 255f).toInt().coerceIn(0, 255),
                    (linearToSrgb(b) * 255f).toInt().coerceIn(0, 255),
                )
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun decode83(value: String): Int {
        var result = 0
        for (c in value) result = result * 83 + BASE83.indexOf(c).let { if (it < 0) 0 else it }
        return result
    }

    private fun decodeAc(value: Int, maximum: Float): Float {
        val q = value / 9f
        return sign(q) * q * q * maximum
    }

    private fun encodeAc(value: Float, maximum: Float): Int {
        val quant = max(0f, min(18f, floor(sign(value) * sqrt(kotlin.math.abs(value) / maximum) * 9f + 0.5f)))
        return quant.roundToInt().coerceIn(0, 18)
    }

    private fun srgbToLinear(value: Float): Float =
        if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

    private fun linearToSrgb(value: Float): Float {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
    }

    private fun toBase83(value: Int, length: Int): String {
        var number = value.coerceAtLeast(0)
        val chars = CharArray(length)
        for (i in length - 1 downTo 0) {
            chars[i] = BASE83[number % 83]
            number /= 83
        }
        return String(chars)
    }
}
