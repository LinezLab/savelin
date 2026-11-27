package com.pinfold.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.FetchStatus
import com.pinfold.app.data.model.NetworkLogEntry
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.network.PreviewFetcher
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.network.image.FaviconStore
import com.pinfold.app.network.image.ImageStore
import com.pinfold.app.network.image.StoredImage
import com.pinfold.app.network.resolvers.HttpClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinkFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val rawUrl = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val hasPrivateFlag = inputData.keyValueMap.containsKey(KEY_IS_PRIVATE)
        val isPrivateFlag = inputData.getBoolean(KEY_IS_PRIVATE, false)
        val db = PinfoldDatabase.get(applicationContext)
        val currentSettings = SettingsRepository(applicationContext).settings.first()
        if (currentSettings.offlineMode) return@withContext Result.success()
        val userAgent = currentSettings.previewUserAgent

        val normalized = UrlNormalizer.normalize(rawUrl)
        val client = HttpClientFactory.create()
        val host = runCatching { java.net.URI.create(normalized).host.orEmpty() }.getOrDefault("")
        val logHost: (String, String) -> Unit = { h, purpose ->
            CoroutineScope(Dispatchers.IO).launch {
                db.networkLogDao().insert(NetworkLogEntry(host = h, purpose = purpose, status = "OK"))
            }
        }
        val fetcher = PreviewFetcher(
            client = client,
            onHost = logHost,
            onBreadcrumb = { step, outcome, stepHost ->
                DiagnosticsLog.log(LogLevel.INFO, "Preview/$step", "$outcome host=$stepHost")
            },
        )
        val proxyTemplate = if (currentSettings.proxiesEnabled) currentSettings.proxyTemplate else ""

        val fetched = runCatching { fetcher.fetch(normalized, userAgent, proxyTemplate) }
        val preview = fetched.getOrNull()
        if (preview == null) {
            val why = fetched.exceptionOrNull()?.let { "${it.javaClass.simpleName} host=$host" } ?: "host=$host"
            DiagnosticsLog.issue("Preview", "fetch", why)
            return@withContext if (runAttemptCount >= 5) Result.failure() else Result.retry()
        }

        val existing = db.bookmarkDao().findByNormalizedUrl(normalized)
        val id = existing?.id ?: db.bookmarkDao().insert(
            Bookmark(
                url = preview.url.ifBlank { normalized },
                originalUrl = rawUrl,
                normalizedUrl = normalized,
                isPrivate = if (hasPrivateFlag) isPrivateFlag else false,
                fetchStatus = FetchStatus.PENDING,
            ),
        )
        if (id <= 0) return@withContext Result.success()

        runCatching {
            FaviconStore(applicationContext, client, logHost).store(preview.url, preview.faviconUrls)
        }

        val store = ImageStore(applicationContext, client)
        val tryStore: (List<String>) -> Pair<StoredImage, String>? = { candidates ->
            candidates.firstNotNullOfOrNull { candidate ->
                runCatching { store.store(id, candidate) }.getOrNull()?.let { it to candidate }
            }
        }

        var storedWithUrl = tryStore(preview.imageUrls)
        var remoteImageUrl = preview.imageUrls.firstOrNull().orEmpty()
        if (storedWithUrl == null && preview.imageUrls.isNotEmpty()) {
            DiagnosticsLog.issue("Preview", "image-primary", "no image decoded host=$host")
            val fallback = runCatching { fetcher.fetchFallback(normalized, userAgent, proxyTemplate) }.getOrNull()
            if (fallback != null) {
                storedWithUrl = tryStore(fallback.imageUrls)
                if (storedWithUrl != null) {
                    remoteImageUrl = storedWithUrl.second
                    DiagnosticsLog.log(LogLevel.INFO, "Preview/image", "fallback-ok host=$host")
                } else {
                    DiagnosticsLog.issue("Preview", "image-fallback", "no image decoded host=$host")
                }
            }
        }
        val stored = storedWithUrl?.first

        val cleaned = MetadataCleaner.clean(
            title = preview.title,
            description = preview.description,
            author = preview.author,
            siteName = preview.siteName.ifBlank { PlatformDetector.domain(normalized) },
            platform = preview.platform.ifBlank { PlatformDetector.platform(normalized) },
            fallbackTitle = PlatformDetector.siteName(normalized),
        )
        val current = db.bookmarkDao().findById(id) ?: return@withContext Result.success()
        val effectivePrivate = if (hasPrivateFlag) isPrivateFlag else current.isPrivate
        db.bookmarkDao().update(
            current.copy(
                title = cleaned.displayTitle,
                displayTitle = cleaned.displayTitle,
                authorHandle = cleaned.authorHandle,
                caption = MetadataCleaner.cleanForDisplay(cleaned.caption),
                likesText = cleaned.likesText,
                commentsText = cleaned.commentsText,
                postedAt = cleaned.postedAt,
                hashtags = cleaned.hashtags.joinToString(","),
                description = preview.description,
                siteName = preview.siteName.ifBlank { PlatformDetector.domain(normalized) },
                platform = preview.platform.ifBlank { PlatformDetector.platform(normalized) },
                author = preview.author,
                isPrivate = effectivePrivate,
                remoteImageUrl = remoteImageUrl,
                imagePath = stored?.path ?: current.imagePath,
                imageWidth = stored?.width ?: current.imageWidth,
                imageHeight = stored?.height ?: current.imageHeight,
                blurhash = stored?.blurhash ?: current.blurhash,
                fetchStatus = if (stored != null) FetchStatus.OK else FetchStatus.PARTIAL,
                fetchError = if (stored == null) "No preview image" else "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        cleaned.hashtags.forEach { tag ->
            val tagId = db.tagDao().findByName(tag)?.id ?: db.tagDao().insert(com.pinfold.app.data.model.Tag(name = tag))
            if (tagId > 0) db.tagDao().link(com.pinfold.app.data.model.BookmarkTag(id, tagId))
        }
        return@withContext Result.success()
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_IS_PRIVATE = "is_private"
    }
}
