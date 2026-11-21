package com.pinfold.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pinfold.app.data.model.BookmarkTag
import com.pinfold.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name LIKE :prefix || '%' ORDER BY name LIMIT 20")
    suspend fun autocomplete(prefix: String): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Update
    suspend fun update(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: BookmarkTag)

    @Query("DELETE FROM bookmark_tags WHERE bookmarkId = :bookmarkId")
    suspend fun unlinkAll(bookmarkId: Long)

    @Query("DELETE FROM bookmark_tags WHERE bookmarkId = :bookmarkId AND tagId = :tagId")
    suspend fun unlink(bookmarkId: Long, tagId: Long)

    @Query("SELECT * FROM bookmark_tags WHERE bookmarkId = :bookmarkId")
    suspend fun linksFor(bookmarkId: Long): List<BookmarkTag>
}
