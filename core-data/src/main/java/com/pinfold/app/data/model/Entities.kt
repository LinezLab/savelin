package com.pinfold.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

object FetchStatus {
    const val PENDING = "PENDING"
    const val OK = "OK"
    const val PARTIAL = "PARTIAL"
    const val FAILED = "FAILED"
}

object TapAction {
    const val OPEN = "OPEN"
    const val DETAILS = "DETAILS"
    const val ASK = "ASK"
}

@Serializable
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["normalizedUrl"], unique = true), Index("collectionId"), Index("createdAt")],
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val title: String = "",
    val description: String = "",
    val siteName: String = "",
    val platform: String = "",
    val author: String = "",
    val caption: String = "",
    val displayTitle: String = "",
    val authorHandle: String = "",
    val likesText: String = "",
    val commentsText: String = "",
    val postedAt: String = "",
    val hashtags: String = "",
    @ColumnInfo(name = "remoteImageUrl") val remoteImageUrl: String? = null,
    val imagePath: String? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val blurhash: String? = null,
    val customImage: Boolean = false,
    val notes: String = "",
    val isImportant: Boolean = false,
    val isArchived: Boolean = false,
    val isPrivate: Boolean = false,
    val collectionId: Long? = null,
    val tapOverride: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
    val openCount: Int = 0,
    val fetchStatus: String = FetchStatus.PENDING,
    val fetchError: String = "",
)

@Fts4(contentEntity = Bookmark::class)
@Entity(tableName = "bookmarks_fts")
data class BookmarkFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int,
    val title: String?,
    val description: String?,
    val notes: String?,
    val url: String?,
    val platform: String?,
)

@Serializable
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Serializable
@Entity(
    tableName = "bookmark_tags",
    primaryKeys = ["bookmarkId", "tagId"],
    indices = [Index("tagId")],
)
data class BookmarkTag(
    val bookmarkId: Long,
    val tagId: Long,
)

@Serializable
@Entity(tableName = "collections")
data class Collection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "",
    val colour: String = "",
    val tapOverride: String? = null,
)

object ReminderStatus {
    const val PENDING = "PENDING"
    const val FIRED = "FIRED"
    const val SNOOZED = "SNOOZED"
    const val DONE = "DONE"
}

@Serializable
@Entity(tableName = "reminders", indices = [Index("bookmarkId"), Index("triggerAt")])
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookmarkId: Long,
    val triggerAt: Long,
    val repeatRule: String = "",
    val status: String = ReminderStatus.PENDING,
)

@Entity(tableName = "network_log")
data class NetworkLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long = System.currentTimeMillis(),
    val host: String,
    val purpose: String,
    val status: String,
)
