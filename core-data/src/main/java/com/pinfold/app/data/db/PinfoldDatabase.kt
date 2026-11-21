package com.pinfold.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkFts
import com.pinfold.app.data.model.BookmarkTag
import com.pinfold.app.data.model.Collection
import com.pinfold.app.data.model.NetworkLogEntry
import com.pinfold.app.data.model.Reminder
import com.pinfold.app.data.model.Tag

@Database(
    entities = [
        Bookmark::class,
        BookmarkFts::class,
        Tag::class,
        BookmarkTag::class,
        Collection::class,
        Reminder::class,
        NetworkLogEntry::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PinfoldDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun networkLogDao(): NetworkLogDao

    companion object {
        @Volatile
        private var instance: PinfoldDatabase? = null

        fun get(context: Context): PinfoldDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PinfoldDatabase::class.java,
                "pinfold.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "displayTitle", "authorHandle", "likesText", "commentsText", "postedAt", "hashtags",
                ).forEach { column ->
                    db.execSQL("ALTER TABLE bookmarks ADD COLUMN $column TEXT NOT NULL DEFAULT ''")
                }
            }
        }
    }
}
