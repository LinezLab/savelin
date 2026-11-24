package com.pinfold.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Transaction
    @Query("SELECT * FROM bookmarks WHERE isArchived = 0 AND isPrivate = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkWithTags>>

    @Transaction
    @Query("SELECT * FROM bookmarks WHERE isPrivate = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun observePrivate(): Flow<List<BookmarkWithTags>>

    @Transaction
    @Query("SELECT * FROM bookmarks WHERE isImportant = 1 AND isArchived = 0 AND isPrivate = 0 ORDER BY createdAt DESC")
    fun observeImportant(): Flow<List<BookmarkWithTags>>

    @Transaction
    @Query("SELECT * FROM bookmarks WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<BookmarkWithTags>>

    @Transaction
    @Query("""
        SELECT b.* FROM bookmarks_fts
        JOIN bookmarks b ON b.rowid = bookmarks_fts.rowid
        WHERE bookmarks_fts MATCH :query AND b.isArchived = 0 AND b.isPrivate = 0
        ORDER BY b.createdAt DESC
    """)
    fun search(query: String): Flow<List<BookmarkWithTags>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE normalizedUrl = :url LIMIT 1")
    suspend fun findByNormalizedUrl(url: String): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun findById(id: Long): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE imagePath IS NULL AND fetchStatus != :status")
    suspend fun missingPreviews(status: String = com.pinfold.app.data.model.FetchStatus.OK): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: Bookmark): Long

    @Update
    suspend fun update(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE bookmarks SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET imagePath = :path, imageWidth = :width, imageHeight = :height, blurhash = :blurhash, remoteImageUrl = :remoteUrl, fetchStatus = :status, fetchError = '', updatedAt = :now WHERE id = :id")
    suspend fun updateImage(id: Long, path: String, width: Int, height: Int, blurhash: String?, remoteUrl: String?, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET fetchStatus = :status, fetchError = :error, updatedAt = :now WHERE id = :id")
    suspend fun updateFetchStatus(id: Long, status: String, error: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET lastOpenedAt = :now, openCount = openCount + 1 WHERE id = :id")
    suspend fun recordOpen(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isImportant = :value, updatedAt = :now WHERE id = :id")
    suspend fun setImportant(id: Long, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isArchived = :value, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, value: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isPrivate = :value, updatedAt = :now WHERE id = :id")
    suspend fun setPrivate(id: Long, value: Boolean, now: Long = System.currentTimeMillis())
}
