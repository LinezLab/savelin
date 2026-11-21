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
