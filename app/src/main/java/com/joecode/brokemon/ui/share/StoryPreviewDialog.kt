package com.joecode.brokemon.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows the rendered story image first, so the user sees exactly what they're posting. */
@Composable
fun StoryPreviewDialog(
    title: String,
    render: () -> Bitmap,
    onShare: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(null) { value = withContext(Dispatchers.Default) { render() } }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(DexColors.Surface, CutCornerShape(8.dp))
                .border(2.dp, DexColors.Outline, CutCornerShape(8.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title.uppercase(), style = PixelText.Label, color = DexColors.Text)
            Box(
                Modifier
                    .heightIn(max = 460.dp)
                    .aspectRatio(9f / 16f)
                    .background(DexColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    Image(it.asImageBitmap(), contentDescription = "Story image preview", contentScale = ContentScale.Fit)
                } ?: CircularProgressIndicator(color = DexColors.DexRed)
            }
            PixelButton("Share", { bitmap?.let(onShare) }, Modifier.fillMaxWidth(), enabled = bitmap != null)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}
