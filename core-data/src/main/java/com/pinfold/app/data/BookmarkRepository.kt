package com.pinfold.app.data

import com.pinfold.app.data.db.BookmarkDao
import com.pinfold.app.data.db.TagDao
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkTag
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val bookmarks: BookmarkDao,
    private val tags: TagDao,
) {
    fun observeAll(): Flow<List<BookmarkWithTags>> = bookmarks.observeAll()
    fun observeImportant(): Flow<List<BookmarkWithTags>> = bookmarks.observeImportant()
    fun observeArchived(): Flow<List<BookmarkWithTags>> = bookmarks.observeArchived()
    fun search(query: String): Flow<List<BookmarkWithTags>> = bookmarks.search(sanitizeFts(query))

    suspend fun findByNormalizedUrl(url: String) = bookmarks.findByNormalizedUrl(url)
    suspend fun findById(id: Long) = bookmarks.findById(id)

    suspend fun save(bookmark: Bookmark, tagNames: List<String>): Long {
        val id = if (bookmark.id != 0L) {
            bookmarks.update(bookmark)
            bookmark.id
        } else {
            bookmarks.insert(bookmark)
        }
        if (id > 0) setTags(id, tagNames)
        return id
    }

    suspend fun setTags(bookmarkId: Long, names: List<String>) {
        tags.unlinkAll(bookmarkId)
        names.map { it.trim() }.filter { it.isNotEmpty() }.forEach { name ->
            val existing = tags.findByName(name)
            val id = existing?.id ?: tags.insert(Tag(name = name))
            if (id > 0) tags.link(BookmarkTag(bookmarkId, id))
        }
    }

    suspend fun delete(id: Long) = bookmarks.delete(id)

    private fun sanitizeFts(query: String): String {
        val words = query.trim().lowercase().split(Regex("\\s+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }
            .filter { it.isNotEmpty() }
        return words.joinToString(" ") { "$it*" }
    }
}
