package com.pinfold.app.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pinfold.app.network.resolvers.FaviconHostPolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Stores icons under the saved page's own host, downloaded directly from that site. There is no
 * global favicon service, no key and no third-party host in this path — only bytes from pages you
 * saved. Instagram icons live on an approved CDN but are still keyed by instagram.com so the UI
 * finds them with the same normalized host.
 */
class FaviconStore(
    private val context: Context,
    private val client: OkHttpClient,
    private val onHost: (host: String, purpose: String) -> Unit = { _, _ -> },
) {
    private val dir: File = File(context.filesDir, "favicons").apply { mkdirs() }

    fun pathFor(host: String): String = FaviconPaths.pathFor(context, host)

    fun store(pageUrl: String, candidates: List<String>): String {
        val pageHost = FaviconHostPolicy.normalizeHost(pageUrl)
        if (pageHost.isBlank()) return ""
        for (candidate in candidates) {
            if (!FaviconHostPolicy.allowed(pageUrl, candidate)) continue
            onHost(FaviconHostPolicy.normalizeHost(candidate), "favicon")
            val bytes = download(candidate) ?: continue
            val saved = persist(pageHost, bytes) ?: continue
            return saved
        }
        return ""
    }

    fun remove(host: String) {
        File(pathFor(host)).delete()
    }

    private fun download(url: String): ByteArray? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (bytes.isEmpty() || bytes.size > 256 * 1024) null else bytes
        }
    }.getOrNull()

    private fun persist(host: String, bytes: ByteArray): String? {
        val decodable = IconBytes.decodeCandidate(bytes)
        val decoded = BitmapFactory.decodeByteArray(decodable, 0, decodable.size) ?: return null
        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= 0) return null
        val ratio = SIZE.toFloat() / longest
        val width = maxOf(1, (decoded.width * ratio).toInt())
        val height = maxOf(1, (decoded.height * ratio).toInt())
        val scaled = if (width == decoded.width && height == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, width, height, true)
        }
        val out = File(pathFor(host))
        val temp = File(dir, "${out.nameWithoutExtension}.part")
        temp.outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP, 80, it) }
        if (temp.exists() && !temp.renameTo(out)) {
            out.delete()
            temp.renameTo(out)
        }
        return out.absolutePath.takeIf { File(it).exists() }
    }

    private companion object {
        const val SIZE = 64
    }
}

/** Shared path helper so the UI can show an icon that was stored under a host's own name. */
object FaviconPaths {
    fun pathFor(context: Context, host: String): String =
        File(File(context.filesDir, "favicons"), "${sanitize(host)}.webp").absolutePath

    private fun sanitize(host: String): String =
        host.replace(Regex("""[^A-Za-z0-9.-]"""), "_").take(120)
}
