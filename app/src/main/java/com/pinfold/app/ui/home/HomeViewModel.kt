package com.pinfold.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pinfold.app.data.BookmarkRepository
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.reminders.ReminderScheduler
import com.pinfold.app.network.UrlNormalizer
import com.pinfold.app.network.image.ImageStore
import com.pinfold.app.network.resolvers.HttpClientFactory
import com.pinfold.app.work.LinkFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val navVisibleState = MutableStateFlow(true)
    private val importantOnly = MutableStateFlow(false)
    private val reminderOnly = MutableStateFlow(false)

    val searchActive: StateFlow<Boolean> = searchActiveState.asStateFlow()
    val navVisible: StateFlow<Boolean> = navVisibleState.asStateFlow()

    val reminderIds = db.reminderDao().observeAll()
        .combine(importantOnly) { reminders, _ -> reminders.map { it.bookmarkId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val baseItems = query.debounce(150)
        .flatMapLatest { text ->
            if (text.isBlank()) repository.observeAll() else repository.search(text)
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

    val currentTab: StateFlow<HomeTab> = tab.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()
    val networkLog = db.networkLogDao().observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openGlassMotion() { settingsPageState.value = "GLASS_MOTION" }
    fun openBackground() { settingsPageState.value = "BACKGROUND" }
    fun closeSettingsPage() { settingsPageState.value = "HOME" }

    fun setSearchActive(value: Boolean) {
        searchActiveState.value = value
        navVisibleState.value = true
        if (!value) {
            query.value = ""
            importantOnly.value = false
            reminderOnly.value = false
        }
    }

    fun setNavVisible(value: Boolean) {
        navVisibleState.value = value || searchActiveState.value
    }

    fun toggleImportantFilter() {
        importantOnly.value = !importantOnly.value
    }

    fun toggleReminderFilter() {
        reminderOnly.value = !reminderOnly.value
    }

    fun selectTab(value: HomeTab) {
        tab.value = value
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

    fun exportBackup(target: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val source = com.pinfold.app.backup.BackupManager(context).export()
        context.contentResolver.openInputStream(source)?.use { input ->
            context.contentResolver.openOutputStream(target)?.use { output -> input.copyTo(output) }
        }
    }

    fun importBackup(source: Uri, onResult: (Int) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val added = com.pinfold.app.backup.BackupManager(getApplication()).import(source)
        withContext(Dispatchers.Main) { onResult(added) }
    }

    fun saveLink(raw: String) {
        val url = extractUrl(raw) ?: return
        val request = OneTimeWorkRequestBuilder<LinkFetchWorker>()
            .setInputData(workDataOf(LinkFetchWorker.KEY_URL to url))
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueue(request)
    }

    fun toggleImportant(item: Bookmark) = viewModelScope.launch {
        db.bookmarkDao().setImportant(item.id, !item.isImportant)
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

    fun removeImage(item: Bookmark) = viewModelScope.launch {
        com.pinfold.app.network.image.ImageStore(getApplication(), HttpClientFactory.create()).remove(item.id)
        db.bookmarkDao().update(db.bookmarkDao().findById(item.id)?.copy(imagePath = null, blurhash = null) ?: return@launch)
    }

    fun refetch(item: Bookmark) {
        val request = OneTimeWorkRequestBuilder<LinkFetchWorker>()
            .setInputData(workDataOf(LinkFetchWorker.KEY_URL to item.url))
            .build()
        WorkManager.getInstance(getApplication<Application>()).enqueue(request)
    }

    fun attachImage(item: Bookmark, uri: Uri) = viewModelScope.launch {
        val context = getApplication<Application>()
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return@launch
        val store = ImageStore(context, HttpClientFactory.create())
        val file = java.io.File(store.pathFor(item.id))
        file.outputStream().use { it.write(bytes) }
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
        db.bookmarkDao().updateImage(
            id = item.id,
            path = file.absolutePath,
            width = options.outWidth,
            height = options.outHeight,
            blurhash = null,
            remoteUrl = item.remoteImageUrl,
            status = com.pinfold.app.data.model.FetchStatus.OK,
        )
        db.bookmarkDao().update(db.bookmarkDao().findById(item.id)!!.copy(customImage = true))
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

    fun normalized(raw: String): String = UrlNormalizer.normalize(raw)
}

enum class HomeTab { HOME, COLLECTIONS, REMINDERS, IMPORTANT, SETTINGS }
