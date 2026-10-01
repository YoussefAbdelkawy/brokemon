package com.joecode.brokemon.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.gson.Gson
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.MediaStorage
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** What goes in brodex.json. Memory file URIs are stored relative ("memories/<file>"). */
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: Long = 0,
    val bros: List<Bro> = emptyList(),
    val squads: List<Squad> = emptyList(),
) {
    companion object {
        const val FORMAT = "brokemon-backup"
    }
}

data class BackupSummary(val bros: Int, val memories: Int)

/**
 * Manual backup: one .zip the user saves wherever they like (Drive, a laptop,
 * a USB stick) with the whole Brodex inside, photos and videos included.
 */
class BackupManager(
    private val context: Context,
    private val repository: BroRepository,
    private val media: MediaStorage,
) {
    private val gson = Gson()

    suspend fun export(target: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val bros = repository.allBrosOnce()
            val squads = repository.allSquadsOnce()
            val files = mutableListOf<File>()
            val portable = bros.map { bro ->
                bro.copy(
                    memories = bro.memories.mapNotNull { memory ->
                        val file = runCatching { memory.fileUri.toUri().toFile() }.getOrNull()
                            ?.takeIf { it.exists() } ?: return@mapNotNull null
                        files += file
                        memory.copy(fileUri = MEMORY_PREFIX + file.name)
                    },
                )
            }
            val json = gson.toJson(BackupFile(exportedAt = System.currentTimeMillis(), bros = portable, squads = squads))
            val out = context.contentResolver.openOutputStream(target) ?: error("Couldn't open the file")
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(json.toByteArray())
                zip.closeEntry()
                files.distinctBy { it.name }.forEach { file ->
                    zip.putNextEntry(ZipEntry(MEMORY_PREFIX + file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            BackupSummary(bros.size, files.size)
        }
    }

    /** Replaces the current Brodex with the backup. Nothing changes if the file is invalid. */
    suspend fun restore(source: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        runCatching {
            var backup: BackupFile? = null
            val input = context.contentResolver.openInputStream(source) ?: error("Couldn't open the file")
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    when {
                        entry.isDirectory -> Unit
                        entry.name == JSON_ENTRY -> backup = gson.fromJson(zip.reader().readText(), BackupFile::class.java)
                        isSafeMemoryEntry(entry.name) ->
                            File(staging, entry.name.removePrefix(MEMORY_PREFIX)).outputStream().use { zip.copyTo(it) }
                        // Anything else (including path-traversal tricks like "../x") is ignored.
                    }
                }
            }
            val data = backup?.takeIf { it.format == BackupFile.FORMAT } ?: error("That isn't a Brokemon backup")

            // Only now, with a valid backup in hand, replace the current data.
            media.deleteAll()
            val memoriesDir = media.memoriesDir
            val restored = data.bros.map { bro ->
                bro.copy(
                    memories = bro.memories.orEmpty().mapNotNull { memory ->
                        val name = memory.fileUri.removePrefix(MEMORY_PREFIX)
                        val staged = File(staging, name).takeIf { isSafeMemoryEntry(memory.fileUri) && it.exists() }
                            ?: return@mapNotNull null
                        val dest = File(memoriesDir, name)
                        staged.copyTo(dest, overwrite = true)
                        memory.copy(fileUri = Uri.fromFile(dest).toString())
                    },
                    facts = bro.facts.orEmpty(),
                    moves = bro.moves.orEmpty(),
                )
            }
            repository.replaceAll(restored, data.squads.orEmpty())
            BackupSummary(restored.size, restored.sumOf { it.memories.size })
        }.also { staging.deleteRecursively() }
    }

    companion object {
        const val JSON_ENTRY = "brodex.json"
        const val MEMORY_PREFIX = "memories/"
        private val memoryName = Regex("""memories/[A-Za-z0-9-]{1,64}\.(jpg|mp4)""")

        /** Only flat "memories/<uuid>.jpg|mp4" names are accepted (blocks zip-slip). */
        fun isSafeMemoryEntry(name: String): Boolean = memoryName.matches(name)
    }
}
