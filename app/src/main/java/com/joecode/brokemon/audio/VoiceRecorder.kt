package com.joecode.brokemon.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Thin wrapper over MediaRecorder: AAC voice in an .m4a file, mono, 64 kbps. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    val isRecording: Boolean get() = recorder != null

    fun start(target: File, maxMillis: Int, onMaxReached: () -> Unit) {
        stop()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioChannels(1)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(64_000)
        r.setMaxDuration(maxMillis)
        r.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onMaxReached()
        }
        r.setOutputFile(target.path)
        r.prepare()
        r.start()
        recorder = r
    }

    /** 0..1 loudness since the last call, for the level meter. */
    fun level(): Float = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f).coerceIn(0f, 1f)

    /** Returns false if nothing usable was recorded (e.g. stopped instantly). */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        return ok
    }
}
