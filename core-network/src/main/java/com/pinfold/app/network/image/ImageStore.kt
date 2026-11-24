package com.pinfold.app.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.math.max
import kotlin.math.min

data class StoredImage(
    val path: String,
    val width: Int,
    val height: Int,
    val blurhash: String,
    /** The tiny, strongly blurred WebP drawn through chips. Empty when generation failed. */
    val blurPath: String = "",
)

class ImageStore(private val context: Context, private val client: OkHttpClient) {
    private val dir: File = File(context.filesDir, "images").apply { mkdirs() }

    fun pathFor(id: Long): String = File(dir, "$id.webp").absolutePath

    fun existing(id: Long): StoredImage? {
        val file = File(pathFor(id))
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0) return null
        return StoredImage(file.absolutePath, options.outWidth, options.outHeight, "", blurPreviewPathFor(file.absolutePath))
    }

    fun store(id: Long, imageUrl: String): StoredImage? {
        val bytes = download(imageUrl) ?: return null
        return storeBytes(id, bytes)
    }

    fun storeBytes(id: Long, bytes: ByteArray): StoredImage? {
        val decoded = decode(bytes) ?: return null
        val scaled = scale(decoded)
        val final = File(pathFor(id))
        val temp = File(dir, "$id.part")
        temp.outputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.WEBP, 80, stream)
        }
        if (temp.exists() && temp.renameTo(final).not()) {
            final.delete()
            temp.renameTo(final)
        }
        val blur = generateBlurPreview(scaled, final.absolutePath)
        return StoredImage(final.absolutePath, scaled.width, scaled.height, Blurhash.encode(scaled), blur)
    }

    /**
     * Generates the blur preview for a picture that was attached from the gallery (no download). The
     * caller has already written the full image to [source]; [id] selects its sibling preview file.
     */
    fun createBlurPreview(id: Long, source: File): String {
        val bitmap = BitmapFactory.decodeFile(source.absolutePath) ?: return ""
        return generateBlurPreview(scale(bitmap), pathFor(id))
    }

    fun remove(id: Long) {
        File(pathFor(id)).delete()
        File(blurPreviewPathFor(pathFor(id))).delete()
    }

    private fun download(url: String): ByteArray? = runCatching {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            val bytes = body.bytes()
            if (bytes.size < 500) null else bytes
        }
    }.getOrNull()

    private fun decode(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    private fun scale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_SIZE) return bitmap
        val ratio = MAX_SIZE.toFloat() / longest
        val width = maxOf(1, (bitmap.width * ratio).roundToIntCompat())
        val height = maxOf(1, (bitmap.height * ratio).roundToIntCompat())
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun Float.roundToIntCompat(): Int = min(Int.MAX_VALUE.toFloat(), this).toInt()

    /**
     * A ~96 px long-side, strongly blurred WebP next to the stored image. A small separable box blur
     * over ARGB ints keeps this API-independent (no RenderEffect on Android 10) and deterministic.
     */
    private fun generateBlurPreview(source: Bitmap, imagePath: String): String {
        return runCatching {
            val longest = maxOf(source.width, source.height)
            val ratio = BLUR_SIZE.toFloat() / longest
            val width = maxOf(1, (source.width * ratio).toInt())
            val height = maxOf(1, (source.height * ratio).toInt())
            val small = if (width == source.width && height == source.height) source else Bitmap.createScaledBitmap(source, width, height, true)
            val pixels = IntArray(width * height)
            small.getPixels(pixels, 0, width, 0, 0, width, height)
            val blurred = boxBlur(pixels, width, height, maxOf(2, BLUR_SIZE / 12))
            val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            outBitmap.setPixels(blurred, 0, width, 0, 0, width, height)
            val out = File(blurPreviewPathFor(imagePath))
            val temp = File(out.parentFile ?: dir, out.nameWithoutExtension + ".part")
            temp.outputStream().use { outBitmap.compress(Bitmap.CompressFormat.WEBP, 70, it) }
            if (temp.exists() && temp.renameTo(out).not()) {
                out.delete()
                temp.renameTo(out)
            }
            out.absolutePath
        }.getOrDefault("")
    }

    private fun boxBlur(src: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val horizontal = IntArray(src.size)
        val output = IntArray(src.size)
        val window = radius * 2 + 1
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                val from = maxOf(0, x - radius)
                val to = minOf(width - 1, x + radius)
                for (k in from..to) {
                    val p = src[y * width + k]
                    r += Color.red(p); g += Color.green(p); b += Color.blue(p); a += Color.alpha(p)
                }
                horizontal[y * width + x] = Color.argb(a / window, r / window, g / window, b / window)
            }
        }
        for (y in 0 until height) {
            val from = maxOf(0, y - radius)
            val to = minOf(height - 1, y + radius)
            val count = to - from + 1
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                for (k in from..to) {
                    val p = horizontal[k * width + x]
                    r += Color.red(p); g += Color.green(p); b += Color.blue(p); a += Color.alpha(p)
                }
                output[y * width + x] = Color.argb(a / count, r / count, g / count, b / count)
            }
        }
        return output
    }

    companion object {
        private const val MAX_SIZE = 1080
        private const val BLUR_SIZE = 96

        /** The preview sits next to the full image: "$id.webp" -> "$id-blur.webp". */
        fun blurPreviewPathFor(imagePath: String): String {
            val file = File(imagePath)
            val parent = file.parentFile ?: File(".")
            return File(parent, file.nameWithoutExtension + "-blur.webp").absolutePath
        }
    }
}
