package com.joecode.brokemon.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.joecode.brokemon.audio.VoiceRecorder
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.delay
import java.io.File

// --- Playback -------------------------------------------------------------------

@Stable
class AudioPlayerState internal constructor(private val context: android.content.Context) {
    var playingUri by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        internal set
    internal var player: MediaPlayer? = null

    fun isPlaying(uri: String?) = uri != null && playingUri == uri

    fun toggle(uri: String) = if (isPlaying(uri)) stop() else play(uri)

    fun play(uri: String) {
        stop()
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri.toUri())
                setOnCompletionListener { this@AudioPlayerState.stop() }
                prepare()
                start()
            }
        }.getOrNull()
        if (player != null) playingUri = uri
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        playingUri = null
        progress = 0f
    }
}

/** One shared player per screen, released when the screen leaves composition. */
@Composable
fun rememberAudioPlayer(): AudioPlayerState {
    val context = LocalContext.current
    val state = remember { AudioPlayerState(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    LaunchedEffect(state.playingUri) {
        while (state.playingUri != null) {
            state.player?.let { p -> runCatching { state.progress = p.currentPosition.toFloat() / p.duration.coerceAtLeast(1) } }
            delay(60)
        }
    }
    return state
}

@Composable
fun AudioPlayButton(player: AudioPlayerState, uri: String, modifier: Modifier = Modifier, label: String? = null) {
    val playing = player.isPlaying(uri)
    Row(
        modifier
            .background(DexColors.SurfaceHigh, CutCornerShape(6.dp))
            .border(2.dp, DexColors.LedGreen.copy(alpha = 0.6f), CutCornerShape(6.dp))
            .clickable { player.toggle(uri) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = if (playing) "Pause" else "Play ${label ?: "voice note"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = DexColors.LedGreen)
        Spacer(Modifier.size(8.dp))
        if (label != null) {
            Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.Text)
            Spacer(Modifier.size(10.dp))
        }
        SegmentedBar(if (playing) player.progress else 0f, DexColors.LedGreen, Modifier.weight(1f).height(10.dp), segments = 16)
    }
}

/** Static pixel waveform used as the "thumbnail" for voice memories. */
@Composable
fun PixelWaveform(modifier: Modifier = Modifier, color: Color = DexColors.LedGreen, seed: Int = 3) {
    val heights = remember(seed) { List(11) { i -> 0.25f + ((i * 37 + seed * 11) % 7) / 9f } }
    Canvas(modifier) {
        val w = size.width / (heights.size * 2 - 1)
        heights.forEachIndexed { i, h ->
            val bar = size.height * h
            drawRect(color, Offset(i * 2 * w, (size.height - bar) / 2), Size(w, bar))
        }
    }
}

// --- Recording ------------------------------------------------------------------

private enum class RecPhase { IDLE, RECORDING, RECORDED }

/**
 * Tap the big button to record, tap again to stop (or it stops at [maxSeconds]).
 * Listen back, redo, or save. The mic permission is asked for on first use.
 */
@Composable
fun VoiceRecorderDialog(
    title: String,
    hint: String,
    maxSeconds: Int,
    newFile: () -> File,
    onSaved: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    val player = rememberAudioPlayer()
    var phase by remember { mutableStateOf(RecPhase.IDLE) }
    var file by remember { mutableStateOf<File?>(null) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var denied by remember { mutableStateOf(false) }
    val levels = remember { mutableStateListOf<Float>() }

    fun discard() {
        player.stop()
        file?.delete()
        file = null
    }

    fun stopRecording() {
        val ok = recorder.stop()
        phase = if (ok && (file?.length() ?: 0) > 0) RecPhase.RECORDED else RecPhase.IDLE.also { discard() }
    }

    fun startRecording() {
        discard()
        levels.clear()
        elapsed = 0f
        val target = newFile()
        file = target
        runCatching { recorder.start(target, maxSeconds * 1000) { stopRecording() } }
            .onSuccess { phase = RecPhase.RECORDING }
            .onFailure { discard() }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) startRecording()
    }

    fun onRecordTap() {
        when (phase) {
            RecPhase.RECORDING -> stopRecording()
            else -> if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(phase) {
        while (phase == RecPhase.RECORDING) {
            delay(80)
            elapsed += 0.08f
            levels.add(recorder.level())
            if (levels.size > 28) levels.removeAt(0)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) {
                recorder.stop()
                file?.delete()
            }
        }
    }

    fun close() {
        if (recorder.isRecording) recorder.stop()
        discard()
        onDismiss()
    }

    Dialog(onDismissRequest = ::close) {
        Column(
            Modifier
                .background(DexColors.Surface, CutCornerShape(8.dp))
                .border(2.dp, DexColors.Outline, CutCornerShape(8.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title.uppercase(), style = PixelText.Label, color = DexColors.Text, textAlign = TextAlign.Center)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = DexColors.TextMuted, textAlign = TextAlign.Center)

            // Level meter
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(DexColors.Screen, CutCornerShape(4.dp))
                    .padding(8.dp),
            ) {
                Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                    val n = 28
                    val w = size.width / (n * 1.5f)
                    for (i in 0 until n) {
                        val level = levels.getOrNull(i - (n - levels.size)) ?: 0f
                        val h = (size.height * (0.08f + level)).coerceAtMost(size.height)
                        drawRect(
                            if (phase == RecPhase.RECORDING) DexColors.LedRed else DexColors.ScreenBorder,
                            Offset(i * w * 1.5f, (size.height - h) / 2),
                            Size(w, h),
                        )
                    }
                }
            }
            Text(
                "%.1fs / %ds".format(elapsed.coerceAtMost(maxSeconds.toFloat()), maxSeconds),
                style = PixelText.Tiny,
                color = if (phase == RecPhase.RECORDING) DexColors.LedRed else DexColors.TextMuted,
            )

            Box(
                Modifier
                    .size(76.dp)
                    .background(if (phase == RecPhase.RECORDING) DexColors.LedRed else DexColors.DexRed, CircleShape)
                    .border(4.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(onClick = ::onRecordTap)
                    .semantics { contentDescription = if (phase == RecPhase.RECORDING) "Stop recording" else "Start recording" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (phase == RecPhase.RECORDING) Icons.Filled.Stop else Icons.Filled.Mic,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            if (denied) {
                Text(
                    "Brokemon needs the microphone to record. You can allow it in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DexColors.LedYellow,
                    textAlign = TextAlign.Center,
                )
            }
            val recorded = file
            if (phase == RecPhase.RECORDED && recorded != null) {
                AudioPlayButton(player, Uri.fromFile(recorded).toString(), Modifier.fillMaxWidth(), label = "Listen")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = ::close) { Text("Cancel") }
                TextButton(
                    enabled = phase == RecPhase.RECORDED && recorded != null,
                    onClick = {
                        player.stop()
                        recorded?.let(onSaved)
                        file = null // ownership moves to the caller; don't delete on close
                        onDismiss()
                    },
                ) { Text("Save") }
            }
        }
    }
}
