package com.pinfold.app.network.resolvers

import okhttp3.Request
import okio.Buffer
import kotlin.math.min

object HtmlFetcher {
    fun get(url: String, context: ResolverContext, userAgent: String = context.userAgent): Pair<String, String>? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "en")
            .build()
        val response = context.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            val source = body.source()
            val buffer = Buffer()
            while (!source.exhausted() && buffer.size < context.maxBodyBytes) {
                val chunk = source.readByteArray(min(4096L, context.maxBodyBytes - buffer.size))
                if (chunk.isEmpty()) break
                buffer.write(chunk)
            }
            buffer.readUtf8() to response.request.url.toString()
        }
        return response
    }
}
