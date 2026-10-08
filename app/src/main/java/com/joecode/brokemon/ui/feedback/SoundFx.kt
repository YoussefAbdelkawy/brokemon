package com.joecode.brokemon.ui.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Plays the synthesized sounds with AudioTrack. Everything is best-effort:
 * if audio can't start on some device, the app just stays quiet.
 */
class SoundFx {
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = HashMap<Sfx, ShortArray>()
    private var music: AudioTrack? = null

    @Volatile var effectsOn = true
    @Volatile var musicOn = false

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun track(data: ShortArray, loop: Boolean): AudioTrack? = runCatching {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(ChipSynth.RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also {
                it.write(data, 0, data.size)
                if (loop) it.setLoopPoints(0, data.size, -1)
            }
    }.getOrNull()

    fun play(sfx: Sfx, volume: Float = 0.5f) {
        if (!effectsOn) return
        io.execute {
            runCatching {
                val data = synchronized(cache) { cache.getOrPut(sfx) { ChipSynth.render(sfx.notes) } }
                val t = track(data, loop = false) ?: return@runCatching
                t.setVolume(volume)
                t.play()
                val ms = data.size * 1000L / ChipSynth.RATE + 120
                main.postDelayed({ runCatching { t.release() } }, ms)
            }
        }
    }

    fun startMusic() {
        if (!musicOn) return
        io.execute {
            runCatching {
                if (music != null) return@runCatching
                val data = synchronized(cache) { musicBuffer }
                val t = track(data, loop = true) ?: return@runCatching
                t.setVolume(0.22f)
                t.play()
                music = t
            }
        }
    }

    fun stopMusic() {
        io.execute {
            runCatching {
                music?.let { it.stop(); it.release() }
                music = null
            }
        }
    }

    private val musicBuffer: ShortArray by lazy { HomeTheme.render() }
}
