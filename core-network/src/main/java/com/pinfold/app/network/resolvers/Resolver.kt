package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResolverContext(
    val client: OkHttpClient,
    val userAgent: String,
    val crawlerUserAgent: String = "facebookexternalhit/1.1",
    val maxBodyBytes: Long = 512L * 1024,
)

interface Resolver {
    val name: String
    suspend fun resolve(url: String, context: ResolverContext): Preview?
}

object HttpClientFactory {
    private const val MAX_REDIRECTS = 5

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .cookieJar(okhttp3.CookieJar.NO_COOKIES)
        .addInterceptor(redirectCap())
        .build()

    private fun redirectCap(): Interceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        var redirects = 0
        var prior = response.priorResponse
        while (prior != null) {
            redirects++
            prior = prior.priorResponse
        }
        if (redirects > MAX_REDIRECTS) {
            response.close()
            throw IOException("Too many redirects")
        }
        response
    }
}
