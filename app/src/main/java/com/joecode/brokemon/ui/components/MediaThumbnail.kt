package com.joecode.brokemon.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.ui.theme.DexColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads a downsampled, correctly rotated preview off the main thread. */
@Composable
fun MediaThumbnail(
    fileUri: String,
    type: MediaType,
    modifier: Modifier = Modifier,
    maxPx: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (type == MediaType.AUDIO) {
        Box(modifier.background(DexColors.Screen), contentAlignment = Alignment.Center) {
            PixelWaveform(Modifier.fillMaxSize(0.6f), seed = fileUri.hashCode())
            Icon(Icons.Filled.Mic, contentDescription = "Voice note", tint = DexColors.Text, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp))
        }
        return
    }
    val bitmap by produceState<ImageBitmap?>(null, fileUri, maxPx) {
        value = withContext(Dispatchers.IO) { loadPreview(fileUri, type, maxPx)?.asImageBitmap() }
    }
    Box(modifier.background(DexColors.Screen), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        } ?: Icon(Icons.Filled.BrokenImage, contentDescription = null, tint = DexColors.Outline)
        if (type == MediaType.VIDEO) {
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Video",
                tint = DexColors.Text,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

private fun loadPreview(fileUri: String, type: MediaType, maxPx: Int): Bitmap? = runCatching {
    val file = fileUri.toUri().toFile()
    if (!file.exists() || file.length() == 0L) return null
    when (type) {
        MediaType.PHOTO -> {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
            val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val rotation = when (
                ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) decoded
            else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        }

        MediaType.AUDIO -> null

        MediaType.VIDEO -> {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.path)
                retriever.getFrameAtTime(0)?.let { frame ->
                    val scale = maxPx.toFloat() / maxOf(frame.width, frame.height)
                    if (scale >= 1f) frame
                    else frame.scale((frame.width * scale).toInt(), (frame.height * scale).toInt())
                }
            } finally {
                retriever.release()
            }
        }
    }
}.getOrNull()
