package com.pinfold.app.backup

import android.content.Context
import android.net.Uri
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkTag
import com.pinfold.app.data.model.Collection
import com.pinfold.app.data.model.Reminder
import com.pinfold.app.data.model.Tag
import com.pinfold.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal const val BACKUP_FORMAT = 2

@Serializable
data class BackupFile(val format: Int = BACKUP_FORMAT, val bookmarks: List<Bookmark>)

@Serializable
data class Pref(val name: String, val kind: String, val value: String)

@Serializable
data class SettingsFile(val settings: List<Pref> = emptyList())

@Serializable
data class LibraryFile(
    val tags: List<Tag> = emptyList(),
    val bookmarkTags: List<BookmarkTag> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
)

class BackupManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
) {
    private val db = PinfoldDatabase.get(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun export(): Uri {
        val bookmarks = runBlocking(Dispatchers.IO) { db.bookmarkDao().getAll() }
        val settings = runBlocking(Dispatchers.IO) { settingsRepository.exportAll() }
        val library = runBlocking(Dispatchers.IO) {
            LibraryFile(
                tags = db.tagDao().observeAll().first(),
                bookmarkTags = bookmarks.flatMap { db.tagDao().linksFor(it.id) },
                collections = db.collectionDao().observeAll().first(),
                reminders = db.reminderDao().observeAll().first(),
            )
        }
        val prefs = settings.map { (name, value) ->
            val (kind, text) = when (value) {
                is Boolean -> "boolean" to value.toString()
                is String -> "string" to value
                is Int -> "int" to value.toString()
                is Long -> "long" to value.toString()
                is Float -> "float" to value.toString()
                else -> "string" to (value?.toString() ?: "")
            }
            Pref(name, kind, text)
        }
        val file = File(context.cacheDir, "sonil-backup.zip")
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            writeJson(zip, "bookmarks.json", json.encodeToString(BackupFile(BACKUP_FORMAT, bookmarks)))
            writeJson(zip, "settings.json", json.encodeToString(SettingsFile(prefs)))
            writeJson(zip, "library.json", json.encodeToString(library))
            val imagesDir = File(context.filesDir, "images")
            imagesDir.listFiles()?.filter { it.isFile }?.forEach { image ->
                zip.putNextEntry(ZipEntry("images/" + image.name))
                image.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
    }

    fun import(source: Uri): Int {
        var backup: BackupFile? = null
        var settingsFile: SettingsFile? = null
        var libraryFile: LibraryFile? = null
        val staging = File(context.cacheDir, "backup-import")
        staging.deleteRecursively()
        staging.mkdirs()
        val input = context.contentResolver.openInputStream(source) ?: return 0
        input.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    val name = entry!!.name
                    if (!safeEntry(name)) {
                        zip.closeEntry()
                        continue
                    }
                    when (name) {
                        "bookmarks.json" -> backup = runCatching {
                            json.decodeFromString<BackupFile>(zip.bufferedReader().readText())
                        }.getOrNull()
                        "settings.json" -> settingsFile = runCatching {
                            json.decodeFromString<SettingsFile>(zip.bufferedReader().readText())
                        }.getOrNull()
                        "library.json" -> libraryFile = runCatching {
                            json.decodeFromString<LibraryFile>(zip.bufferedReader().readText())
                        }.getOrNull()
                        else -> {
                            val fileName = name.removePrefix("images/")
                            if (fileName.isNotBlank() && !fileName.contains("/") && !fileName.contains("\\")) {
                                File(staging, fileName).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        val parsed = backup ?: return 0
        if (parsed.format !in 1..BACKUP_FORMAT) return 0
        return runBlocking(Dispatchers.IO) {
            val dao = db.bookmarkDao()
            val idMap = mutableMapOf<Long, Long>()
            var added = 0
            for (bookmark in parsed.bookmarks) {
                if (dao.findByNormalizedUrl(bookmark.normalizedUrl) != null) continue
                val newId = dao.insert(bookmark.copy(id = 0, imagePath = null))
                if (newId > 0) {
                    idMap[bookmark.id] = newId
                    added++
                }
            }

            val collectionMap = mutableMapOf<Long, Long>()
            libraryFile?.collections?.forEach { collection ->
                val newId = db.collectionDao().insert(collection.copy(id = 0))
                if (newId > 0) collectionMap[collection.id] = newId
            }
            for ((oldId, newId) in idMap) {
                val original = parsed.bookmarks.firstOrNull { it.id == oldId } ?: continue
                val current = dao.findById(newId) ?: continue
                val mapped = original.collectionId?.let { collectionMap[it] }
                if (mapped != current.collectionId) dao.update(current.copy(collectionId = mapped))
            }
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            staging.listFiles()?.filter { it.isFile }?.forEach { image ->
                val name = image.name
                val rawId = name.substringBefore(".").substringBefore("-").toLongOrNull() ?: return@forEach
                val newId = idMap[rawId] ?: return@forEach
                val suffix = if (name.contains("-blur")) "-blur.webp" else ".webp"
                val target = File(imagesDir, newId.toString() + suffix)
                if (!target.exists()) {
                    if (!image.renameTo(target)) {
                        target.delete()
                        image.renameTo(target)
                    }
                }
                image.delete()
            }
            for ((oldId, newId) in idMap) {
                val original = parsed.bookmarks.firstOrNull { it.id == oldId } ?: continue
                val current = dao.findById(newId) ?: continue
                val newPath = File(imagesDir, newId.toString() + ".webp").absolutePath
                if (File(newPath).isFile) {
                    dao.updateImage(
                        newId,
                        newPath,
                        original.imageWidth,
                        original.imageHeight,
                        original.blurhash,
                        original.remoteImageUrl,
                        original.fetchStatus,
                    )
                    dao.findById(newId)?.let { updated ->
                        dao.update(updated.copy(customImage = original.customImage, fetchError = original.fetchError))
                    }
                } else {
                    dao.update(current.copy(imagePath = null, imageWidth = 0, imageHeight = 0, blurhash = null))
                }
            }

            val tagMap = mutableMapOf<Long, Long>()
            libraryFile?.tags?.forEach { tag ->
                val newId = db.tagDao().findByName(tag.name)?.id ?: db.tagDao().insert(tag.copy(id = 0))
                if (newId > 0) tagMap[tag.id] = newId
            }
            libraryFile?.bookmarkTags?.forEach { link ->
                val bookmarkId = idMap[link.bookmarkId] ?: return@forEach
                val tagId = tagMap[link.tagId] ?: return@forEach
                db.tagDao().link(BookmarkTag(bookmarkId, tagId))
            }
            libraryFile?.reminders?.forEach { reminder ->
                val bookmarkId = idMap[reminder.bookmarkId] ?: return@forEach
                db.reminderDao().insert(reminder.copy(id = 0, bookmarkId = bookmarkId))
            }
            val values = settingsFile?.settings?.mapNotNull { pref ->
                val value: Any? = when (pref.kind) {
                    "boolean" -> pref.value.toBooleanStrictOrNull()
                    "int" -> pref.value.toIntOrNull()
                    "long" -> pref.value.toLongOrNull()
                    "float" -> pref.value.toFloatOrNull()
                    else -> pref.value
                }
                value?.let { pref.name to it }
            }?.toMap()
            if (!values.isNullOrEmpty()) settingsRepository.importAll(values)
            staging.deleteRecursively()
            added
        }
    }

    private fun writeJson(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }

    private fun safeEntry(name: String): Boolean {
        if (name.startsWith("/") || name.contains("..") || name.contains("\\")) return false
        return name == "bookmarks.json" || name == "settings.json" || name == "library.json" || name.startsWith("images/")
    }
}
