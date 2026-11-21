package com.pinfold.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.FetchStatus
import com.pinfold.app.data.model.NetworkLogEntry
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.network.PreviewFetcher
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.network.image.ImageStore
import com.pinfold.app.network.resolvers.HttpClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LinkFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rawUrl = inputData.getString(KEY_URL) ?: return Result.failure()
        val db = PinfoldDatabase.get(applicationContext)
        val settings = SettingsRepository(applicationContext).settings
        // One-shot read of settings is enough here; the flow's current value is the first emission.
        val currentSettings = settings.first()
        if (currentSettings.offlineMode) return Result.success()
        val userAgent = currentSettings.previewUserAgent

        val normalized = UrlNormalizer.normalize(rawUrl)
        val client = HttpClientFactory.create()
        val fetcher = PreviewFetcher(client) { host, purpose ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.networkLogDao().insert(NetworkLogEntry(host = host, purpose = purpose, status = "OK"))
            }
        }
        val proxyTemplate = if (currentSettings.proxiesEnabled) currentSettings.proxyTemplate else ""
        val preview = runCatching { fetcher.fetch(normalized, userAgent, proxyTemplate) }.getOrElse {
            return Result.failure()
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
        if (id <= 0) return Result.success()

        val store = ImageStore(applicationContext, client)
        val stored = preview.imageUrls.firstNotNullOfOrNull { url ->
            runCatching { store.store(id, url) }.getOrNull()
        }

        val title = preview.title.ifBlank { preview.author.ifBlank { PlatformDetector.siteName(normalized) } }
        val current = db.bookmarkDao().findById(id) ?: return Result.success()
        db.bookmarkDao().update(
            current.copy(
                title = title,
                description = preview.description,
                siteName = preview.siteName.ifBlank { PlatformDetector.domain(normalized) },
                platform = preview.platform.ifBlank { PlatformDetector.platform(normalized) },
                author = preview.author,
                caption = preview.caption,
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
        return Result.success()
    }

    companion object {
        const val KEY_URL = "url"
    }
}
