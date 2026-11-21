package com.pinfold.app.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.math.min

data class StoredImage(val path: String, val width: Int, val height: Int, val blurhash: String)

class ImageStore(private val context: Context, private val client: OkHttpClient) {
    private val dir: File = File(context.filesDir, "images").apply { mkdirs() }

    fun pathFor(id: Long): String = File(dir, "$id.webp").absolutePath

    fun existing(id: Long): StoredImage? {
        val file = File(pathFor(id))
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0) return null
        return StoredImage(file.absolutePath, options.outWidth, options.outHeight, "")
    }

    fun store(id: Long, imageUrl: String): StoredImage? {
        val bytes = download(imageUrl) ?: return null
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
        return StoredImage(final.absolutePath, scaled.width, scaled.height, Blurhash.encode(scaled))
    }

    fun remove(id: Long) {
        File(pathFor(id)).delete()
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

    private companion object {
        const val MAX_SIZE = 1080
    }
}
