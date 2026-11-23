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
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.network.PreviewFetcher
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.network.image.FaviconStore
import com.pinfold.app.network.image.ImageStore
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
        val onIssue: (String, String, String) -> Unit = { scope, what, why -> DiagnosticsLog.issue(scope, what, why) }
        val fetcher = PreviewFetcher(client, logHost, onIssue)
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
                fetchStatus = FetchStatus.PENDING,
            ),
        )
        if (id <= 0) return@withContext Result.success()

        val favicon = FaviconStore(applicationContext, client, logHost).store(preview.url, preview.faviconUrls)
        if (favicon.isBlank() && preview.faviconUrls.isNotEmpty()) {
            DiagnosticsLog.issue("Favicon", "download", "no icon decoded host=$host")
        }

        val store = ImageStore(applicationContext, client)
        val stored = preview.imageUrls.firstNotNullOfOrNull { url ->
            runCatching { store.store(id, url) }.getOrNull()
        }
        if (stored == null && preview.imageUrls.isNotEmpty()) {
            DiagnosticsLog.issue("Preview", "image", "no image decoded host=$host")
        }

        val cleaned = MetadataCleaner.clean(
            title = preview.title,
            description = preview.description,
            author = preview.author,
            siteName = preview.siteName.ifBlank { PlatformDetector.domain(normalized) },
            platform = preview.platform.ifBlank { PlatformDetector.platform(normalized) },
            fallbackTitle = PlatformDetector.siteName(normalized),
            contentTags = preview.tags,
        )
        val current = db.bookmarkDao().findById(id) ?: return@withContext Result.success()
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
                remoteImageUrl = preview.imageUrls.firstOrNull(),
                imagePath = stored?.path ?: current.imagePath,
                imageWidth = stored?.width ?: current.imageWidth,
                imageHeight = stored?.height ?: current.imageHeight,
                blurhash = stored?.blurhash ?: current.blurhash,
                fetchStatus = if (stored != null) FetchStatus.OK else FetchStatus.PARTIAL,
                fetchError = if (stored == null) "No preview image" else "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        cleaned.tags.forEach { tag ->
            val tagId = db.tagDao().findByName(tag)?.id ?: db.tagDao().insert(com.pinfold.app.data.model.Tag(name = tag))
            if (tagId > 0) db.tagDao().link(com.pinfold.app.data.model.BookmarkTag(id, tagId))
        }
        return@withContext Result.success()
    }

    companion object {
        const val KEY_URL = "url"
    }
}
