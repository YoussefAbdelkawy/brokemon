package com.joecode.brokemon.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.joecode.brokemon.data.model.MediaType
import java.io.File
import java.util.UUID

/**
 * All memory media lives in filesDir/memories. That folder is app-private:
 * other apps can't read it, it isn't in the gallery and it's removed on uninstall.
 */
class MediaStorage(private val context: Context) {

    val memoriesDir: File
        get() = File(context.filesDir, "memories").apply { mkdirs() }

    /** A fresh empty file plus a content:// Uri the camera app is allowed to write into. */
    fun newCaptureTarget(type: MediaType): Pair<File, Uri> {
        val file = File(memoriesDir, "${UUID.randomUUID()}.${type.extension}")
        file.createNewFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /** Copies a picked gallery item into private storage so we never depend on the original. */
    fun importFromPicker(source: Uri, type: MediaType): File? {
        val target = File(memoriesDir, "${UUID.randomUUID()}.${type.extension}")
        return runCatching {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return null
            target
        }.getOrNull()
    }

    /** Uri that can be handed to a viewer app with a temporary read grant. */
    fun shareableUri(fileUri: String): Uri? = runCatching {
        val file = fileUri.toUri().toFile()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    fun delete(fileUri: String) {
        runCatching { fileUri.toUri().toFile().delete() }
    }

    /** A fresh file for the in-app voice recorder. */
    fun newAudioFile(prefix: String = ""): File =
        File(memoriesDir, "$prefix${UUID.randomUUID()}.${MediaType.AUDIO.extension}")

    fun deleteAll() {
        memoriesDir.deleteRecursively()
    }

    fun discardIfEmpty(file: File) {
        if (file.exists() && file.length() == 0L) file.delete()
    }
}
