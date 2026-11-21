package com.pinfold.app.backup

import android.content.Context
import android.net.Uri
import com.pinfold.app.data.db.PinfoldDatabase
import com.pinfold.app.data.model.Bookmark
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupFile(val format: Int = 1, val bookmarks: List<Bookmark>)

class BackupManager(private val context: Context) {
    private val db = PinfoldDatabase.get(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun export(): Uri {
        val bookmarks = allBookmarks()
        val file = File(context.cacheDir, "pinfold-backup.zip")
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("bookmarks.json"))
            zip.write(json.encodeToString(BackupFile(bookmarks = bookmarks)).toByteArray())
            zip.closeEntry()
            for (bookmark in bookmarks) {
                val path = bookmark.imagePath ?: continue
                val image = File(path)
                if (!image.exists()) continue
                zip.putNextEntry(ZipEntry("images/${image.name}"))
                image.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun import(source: Uri): Int {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: return 0
        var added = 0
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry: ZipEntry?
            while (zip.nextEntry.also { entry = it } != null) {
                when {
                    entry!!.name == "bookmarks.json" -> {
                        val text = zip.bufferedReader().readText()
                        val backup = runCatching { json.decodeFromString<BackupFile>(text) }.getOrNull()
                            ?: continue
                        for (bookmark in backup.bookmarks) {
                            if (db.bookmarkDao().findByNormalizedUrl(bookmark.normalizedUrl) != null) continue
                            db.bookmarkDao().insert(bookmark.copy(id = 0))
                            added++
                        }
                    }
                    entry!!.name.startsWith("images/") -> {
                        val name = File(entry!!.name).name
                        val id = name.removeSuffix(".webp").toLongOrNull() ?: continue
                        val target = File(context.filesDir, "images/$id.webp")
                        if (!target.exists()) {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return added
    }

    private fun allBookmarks(): List<Bookmark> = kotlinx.coroutines.runBlocking(
        kotlinx.coroutines.Dispatchers.IO,
    ) { db.bookmarkDao().getAll() }
}
