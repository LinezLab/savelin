package com.pinfold.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pinfold.app.data.BookmarkRepository
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.reminders.ReminderScheduler
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.PreviewFetcher
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.network.image.ImageStore
import com.pinfold.app.network.resolvers.HttpClientFactory
import com.pinfold.app.work.LinkFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.pinfold.app.data.model.FetchStatus
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import com.pinfold.app.ui.detail.invalidateMesh
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val gridState = LazyStaggeredGridState()
    private val db = PinfoldDatabase.get(application)
    private val repository = BookmarkRepository(db.bookmarkDao(), db.tagDao())
    private val settingsRepository = SettingsRepository(application)
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.pinfold.app.data.settings.AppSettings())

    private val query = MutableStateFlow("")
    private val tab = MutableStateFlow(HomeTab.HOME)
    private val settingsPageState = MutableStateFlow("HOME")
    val settingsPage: StateFlow<String> = settingsPageState.asStateFlow()
    private val searchActiveState = MutableStateFlow(false)
    private val importantOnly = MutableStateFlow(false)
    private val reminderOnly = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val saved = settingsRepository.settings.first()
            importantOnly.value = saved.rememberFilters && saved.importantFilter
            reminderOnly.value = saved.rememberFilters && saved.reminderFilter
        }
    }

    val searchActive: StateFlow<Boolean> = searchActiveState.asStateFlow()
    val importantFilter: StateFlow<Boolean> = importantOnly.asStateFlow()
    val reminderFilter: StateFlow<Boolean> = reminderOnly.asStateFlow()

    val reminderIds = db.reminderDao().observeAll()
        .combine(importantOnly) { reminders, _ -> reminders.map { it.bookmarkId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val baseItems = query.debounce(150).flatMapLatest { text ->
        if (text.isNotBlank()) repository.search(text) else repository.observeAll()
    }

    val items: StateFlow<List<BookmarkWithTags>> = combine(
        baseItems,
        importantOnly,
        reminderOnly,
        reminderIds,
    ) { list, important, reminders, ids ->
        list.filter { item ->
            (!important || item.bookmark.isImportant) &&
                (!reminders || item.bookmark.id in ids)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateItems: StateFlow<List<BookmarkWithTags>> = repository.observePrivate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTab: StateFlow<HomeTab> = tab.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()
    val networkLog = db.networkLogDao().observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openVault() { settingsPageState.value = "VAULT" }
    fun openGlassMotion() { settingsPageState.value = "GLASS_MOTION" }
    fun closeSettingsPage() { settingsPageState.value = "HOME" }

    fun setSearchActive(value: Boolean) {
        searchActiveState.value = value
        if (!value) {
            query.value = ""
            importantOnly.value = false
            reminderOnly.value = false
        }
    }

    fun toggleImportantFilter() {
        importantOnly.value = !importantOnly.value
        viewModelScope.launch { settingsRepository.set("importantFilter", importantOnly.value) }
    }

    fun toggleReminderFilter() {
        reminderOnly.value = !reminderOnly.value
        viewModelScope.launch { settingsRepository.set("reminderFilter", reminderOnly.value) }
    }

    fun selectTab(value: HomeTab) {
        tab.value = value
        if (value != HomeTab.SETTINGS) settingsPageState.value = "HOME"
    }

    fun setOffline(value: Boolean) = viewModelScope.launch { settingsRepository.setOfflineMode(value) }
    fun setGlassPreset(value: String) = viewModelScope.launch { settingsRepository.setGlassPreset(value) }
    fun setMotionPreset(value: String) = viewModelScope.launch { settingsRepository.setMotionPreset(value) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { settingsRepository.setReduceMotion(value) }
    fun setReduceTransparency(value: Boolean) = viewModelScope.launch { settingsRepository.setReduceTransparency(value) }
    fun setHaptics(value: String) = viewModelScope.launch { settingsRepository.setHaptics(value) }
    fun setAnimationSpeed(value: Float) = viewModelScope.launch { settingsRepository.setAnimationSpeed(value) }
    fun setUserAgent(value: String) = viewModelScope.launch { settingsRepository.setPreviewUserAgent(value) }
    fun setProxies(value: Boolean) = viewModelScope.launch { settingsRepository.setProxiesEnabled(value) }
    fun setProxyTemplate(value: String) = viewModelScope.launch { settingsRepository.setProxyTemplate(value) }
    fun setCardColumns(value: Int) = viewModelScope.launch { settingsRepository.setCardColumns(value) }
    fun setCardSpacing(value: String) = viewModelScope.launch { settingsRepository.setCardSpacing(value) }
    fun setCardCaptionStyle(value: String) = viewModelScope.launch { settingsRepository.setCardCaptionStyle(value) }
    fun setCardTitleLines(value: Int) = viewModelScope.launch { settingsRepository.setCardTitleLines(value) }
    fun setCardShowAuthorLine(value: Boolean) = viewModelScope.launch { settingsRepository.setCardShowAuthorLine(value) }
    fun setLimitTallImages(value: Boolean) = viewModelScope.launch { settingsRepository.setLimitTallImages(value) }
    fun setTrueBlack(value: Boolean) = viewModelScope.launch { settingsRepository.setTrueBlack(value) }
    fun setSetting(name: String, value: Any) = viewModelScope.launch { settingsRepository.set(name, value) }
    fun setFrameOverlay(value: Boolean) = viewModelScope.launch { settingsRepository.setFrameOverlay(value) }
    fun setBiometricLock(value: Boolean) = viewModelScope.launch { settingsRepository.setBiometricLock(value) }
    fun setHideDetailsOnLockScreen(value: Boolean) = viewModelScope.launch { settingsRepository.setHideDetailsOnLockScreen(value) }
    fun setEnableClipboardChip(value: Boolean) = viewModelScope.launch { settingsRepository.setEnableClipboardChip(value) }

    fun exportBackup(target: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val source = com.pinfold.app.backup.BackupManager(context, settingsRepository).export()
        context.contentResolver.openInputStream(source)?.use { input ->
            context.contentResolver.openOutputStream(target)?.use { output -> input.copyTo(output) }
        }
    }

    fun importBackup(source: Uri, onResult: (Int) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val added = com.pinfold.app.backup.BackupManager(getApplication(), settingsRepository).import(source)
        withContext(Dispatchers.Main) { onResult(added) }
    }

    fun saveLink(raw: String, isPrivate: Boolean = settings.value.saveLinksPrivateByDefault) {
        val url = extractUrl(raw) ?: return
        val request = OneTimeWorkRequestBuilder<LinkFetchWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(
                workDataOf(
                    LinkFetchWorker.KEY_URL to url,
                    LinkFetchWorker.KEY_IS_PRIVATE to isPrivate,
                ),
            )
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueue(request)
    }

    suspend fun isSaved(raw: String): Boolean = withContext(Dispatchers.IO) {
        repository.findByNormalizedUrl(UrlNormalizer.normalize(raw.trim())) != null
    }

    fun toggleImportant(item: Bookmark) = viewModelScope.launch {
        db.bookmarkDao().setImportant(item.id, !item.isImportant)
    }

    fun togglePrivate(item: Bookmark) = viewModelScope.launch {
        repository.setPrivate(item.id, !item.isPrivate)
    }

    fun delete(item: Bookmark) = viewModelScope.launch {
        ImageStore(getApplication(), HttpClientFactory.create()).remove(item.id)
        db.bookmarkDao().delete(item.id)
    }

    fun setArchived(item: Bookmark, archived: Boolean) = viewModelScope.launch {
        db.bookmarkDao().setArchived(item.id, archived)
    }

    fun setNotes(item: Bookmark, notes: String) = viewModelScope.launch {
        db.bookmarkDao().update(db.bookmarkDao().findById(item.id)?.copy(notes = notes, updatedAt = System.currentTimeMillis()) ?: return@launch)
    }

    fun editBookmark(
        item: Bookmark,
        title: String,
        description: String,
        notes: String,
        tagNames: List<String>,
    ) = viewModelScope.launch {
        repository.save(
            item.copy(
                title = title,
                description = description,
                notes = notes,
                updatedAt = System.currentTimeMillis(),
            ),
            tagNames,
        )
    }

    fun removeImage(item: Bookmark) = viewModelScope.launch {
        com.pinfold.app.network.image.ImageStore(getApplication(), HttpClientFactory.create()).remove(item.id)
        db.bookmarkDao().update(db.bookmarkDao().findById(item.id)?.copy(imagePath = null, blurhash = null) ?: return@launch)
    }

    fun refetch(item: Bookmark) {
        if (!settings.value.refetchMissing) return
        val request = OneTimeWorkRequestBuilder<LinkFetchWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(LinkFetchWorker.KEY_URL to item.url))
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueue(request)
    }

    suspend fun randomWithImage(): BookmarkWithTags? = withContext(Dispatchers.IO) {
        repository.observeAll().first()
            .mapNotNull { item ->
                val path = item.bookmark.imagePath
                if (path.isNullOrBlank()) null else item.takeIf { java.io.File(path).isFile }
            }
            .randomOrNull()
    }

    fun refreshNow(item: Bookmark, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val config = settings.value
        val refreshed = withContext(Dispatchers.IO) {
            runCatching {
                if (config.offlineMode) return@runCatching false
                val normalized = UrlNormalizer.normalize(item.url)
                val client = HttpClientFactory.create()
                val fetcher = PreviewFetcher(client)
                val proxyTemplate = if (config.proxiesEnabled) config.proxyTemplate else ""
                val preview = fetcher.fetch(normalized, config.previewUserAgent, proxyTemplate)
                val store = ImageStore(getApplication(), client)
                val stored = preview.imageUrls.firstNotNullOfOrNull { candidate ->
                    runCatching { store.store(item.id, candidate) }.getOrNull()
                }
                val current = db.bookmarkDao().findById(item.id) ?: return@runCatching false
                val cleaned = MetadataCleaner.clean(
                    title = preview.title,
                    description = preview.description,
                    author = preview.author,
                    siteName = preview.siteName.ifBlank { PlatformDetector.domain(normalized) },
                    platform = preview.platform.ifBlank { PlatformDetector.platform(normalized) },
                    fallbackTitle = PlatformDetector.siteName(normalized),
                )
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
                        remoteImageUrl = preview.imageUrls.firstOrNull().orEmpty(),
                        imagePath = stored?.path ?: current.imagePath,
                        imageWidth = stored?.width ?: current.imageWidth,
                        imageHeight = stored?.height ?: current.imageHeight,
                        blurhash = stored?.blurhash ?: current.blurhash,
                        fetchStatus = if (stored != null) com.pinfold.app.data.model.FetchStatus.OK else com.pinfold.app.data.model.FetchStatus.PARTIAL,
                        fetchError = if (stored == null) "No preview image" else "",
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                true
            }.getOrDefault(false)
        }
        withContext(Dispatchers.Main) { onDone(refreshed) }
    }

    fun attachImage(item: Bookmark, uri: Uri, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val context = getApplication<Application>()
        val attached = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching false
                val stored = ImageStore(context, HttpClientFactory.create()).storeBytes(item.id, bytes)
                    ?: return@runCatching false
                val current = db.bookmarkDao().findById(item.id) ?: return@runCatching false
                db.bookmarkDao().update(
                    current.copy(
                        imagePath = stored.path,
                        imageWidth = stored.width,
                        imageHeight = stored.height,
                        blurhash = stored.blurhash,
                        customImage = true,
                        fetchStatus = com.pinfold.app.data.model.FetchStatus.OK,
                        fetchError = "",
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                true
            }.getOrDefault(false)
        }
        withContext(Dispatchers.Main) {
            if (attached) com.pinfold.app.ui.detail.invalidateMesh(item.id)
            onResult(attached)
        }
    }

    fun clearReminder(item: Bookmark) = viewModelScope.launch {
        val existing = db.reminderDao().observeAll().first().firstOrNull { it.bookmarkId == item.id }
        if (existing != null) {
            db.reminderDao().delete(existing.id)
            ReminderScheduler.cancel(getApplication(), existing.id)
        }
    }

    fun clearReminderById(id: Long) = viewModelScope.launch {
        db.reminderDao().delete(id)
        ReminderScheduler.cancel(getApplication(), id)
    }

    fun setReminder(item: Bookmark, triggerAt: Long) = viewModelScope.launch {
        val reminder = com.pinfold.app.data.model.Reminder(bookmarkId = item.id, triggerAt = triggerAt)
        val id = db.reminderDao().insert(reminder)
        if (id > 0) {
            ReminderScheduler.schedule(getApplication(), reminder.copy(id = id))
        }
    }

    fun recordOpen(item: Bookmark) = viewModelScope.launch {
        db.bookmarkDao().recordOpen(item.id)
    }

    private fun extractUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return Regex("https?://\\S+").find(trimmed)?.value
    }

    fun refreshMissingImages(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }) =
        viewModelScope.launch(Dispatchers.IO) {
            val config = settings.value
            if (!config.refetchMissing || config.offlineMode) return@launch
            val missing = db.bookmarkDao().missingPreviews()
                .filter { it.imagePath.isNullOrBlank() || !File(it.imagePath).isFile }
            DiagnosticsLog.log(LogLevel.INFO, "BulkRefresh", "start total=" + missing.size)
            withTimeoutOrNull(5_000) {
                val client = HttpClientFactory.create()
                val store = ImageStore(getApplication(), client)
                val fetcher = PreviewFetcher(client)
                val proxyTemplate = if (config.proxiesEnabled) config.proxyTemplate else ""
                var done = 0
                for (item in missing) {
                    runCatching {
                        val normalized = UrlNormalizer.normalize(item.url)
                        val preview = fetcher.fetch(normalized, config.previewUserAgent, proxyTemplate)
                        val stored = preview.imageUrls.firstNotNullOfOrNull { candidate ->
                            runCatching { store.store(item.id, candidate) }.getOrNull()
                        }
                        if (stored != null) {
                            val current = db.bookmarkDao().findById(item.id) ?: return@runCatching false
                            db.bookmarkDao().update(
                                current.copy(
                                    imagePath = stored.path,
                                    imageWidth = stored.width,
                                    imageHeight = stored.height,
                                    blurhash = stored.blurhash,
                                    remoteImageUrl = preview.imageUrls.firstOrNull().orEmpty(),
                                    fetchStatus = FetchStatus.OK,
                                    fetchError = "",
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                            invalidateMesh(item.id)
                            true
                        } else {
                            false
                        }
                    }.getOrElse { error ->
                        DiagnosticsLog.issue("BulkRefresh", "preview", error::class.simpleName ?: "error")
                        false
                    }
                    done++
                    onProgress(done, missing.size)
                }
            }
            DiagnosticsLog.log(LogLevel.INFO, "BulkRefresh", "finish")
        }

    fun normalized(raw: String): String = UrlNormalizer.normalize(raw)
}

enum class HomeTab { HOME, COLLECTIONS, REMINDERS, IMPORTANT, SETTINGS }
