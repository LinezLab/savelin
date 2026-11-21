package com.pinfold.app.ui.home

import android.app.Application
import android.content.Context
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
import com.pinfold.app.network.UrlNormalizer
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
    val settings = SettingsRepository(application).settings
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
        db.bookmarkDao().delete(item.id)
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
