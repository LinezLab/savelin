package com.pinfold.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.app.Activity
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.FetchStatus
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.network.LinkExtractor
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.work.LinkFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun handle(source: Intent) {
        val text = source.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: source.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: source.getStringExtra(Intent.EXTRA_TITLE)
        val fromClip = source.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).text?.toString() }.joinToString(" ")
        }
        val links = LinkExtractor.extract(listOfNotNull(text, fromClip).joinToString(" "))
        val message = runBlocking(Dispatchers.IO) {
            val settings = SettingsRepository(applicationContext).settings.first()
            if (links.isEmpty()) {
                DiagnosticsLog.issue("Share", "extract", "no web link found")
                "No web link found"
            } else if (settings.offlineMode) {
                save(links, source)
                "Link saved. Preview will load when you are online"
            } else {
                save(links, source)
                if (links.size == 1) "Link saved successfully" else "${links.size} links saved successfully"
            }
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private suspend fun save(links: List<String>, source: Intent) = withContext(Dispatchers.IO) {
        val db = PinfoldDatabase.get(applicationContext)
        val subject = source.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        var duplicates = 0
        links.forEach { raw ->
            val normalized = UrlNormalizer.normalize(raw)
            if (db.bookmarkDao().findByNormalizedUrl(normalized) != null) {
                duplicates++
                return@forEach
            }
            val title = subject.ifBlank { PlatformDetector.domain(normalized).ifBlank { normalized } }
            val id = db.bookmarkDao().insert(
                Bookmark(
                    url = normalized,
                    originalUrl = raw,
                    normalizedUrl = normalized,
                    title = title,
                    displayTitle = title,
                    siteName = PlatformDetector.domain(normalized),
                    platform = PlatformDetector.platform(normalized),
                    fetchStatus = FetchStatus.PENDING,
                ),
            )
            if (id > 0) {
                val request = OneTimeWorkRequestBuilder<LinkFetchWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInputData(workDataOf(LinkFetchWorker.KEY_URL to normalized))
                    .build()
                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork("share-$id", ExistingWorkPolicy.KEEP, request)
            }
        }
        if (duplicates > 0 && links.size == duplicates) {
            // Toast message is decided by the caller; duplicates are a no-op for the library.
        }
    }
}
