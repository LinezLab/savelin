package com.pinfold.app.ui.home

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PinfoldDatabase.get(application)
    private val repository = BookmarkRepository(db.bookmarkDao(), db.tagDao())
    private val settingsRepository = SettingsRepository(application)
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.pinfold.app.data.settings.AppSettings())

    private val query = MutableStateFlow("")
    private val tab = MutableStateFlow(HomeTab.HOME)

    val items: StateFlow<List<BookmarkWithTags>> = query.debounce(200)
        .flatMapLatest { text ->
            if (text.isBlank()) repository.observeAll() else repository.search(text)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTab: StateFlow<HomeTab> = tab.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()

    fun onQueryChange(value: String) {
        query.value = value
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
    fun setUserAgent(value: String) = viewModelScope.launch { settingsRepository.setPreviewUserAgent(value) }
    fun setProxies(value: Boolean) = viewModelScope.launch { settingsRepository.setProxiesEnabled(value) }

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
