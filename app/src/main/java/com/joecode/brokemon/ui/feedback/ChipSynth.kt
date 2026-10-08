package com.joecode.brokemon.ui.feedback

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * A tiny 8-bit synthesizer. All the app's sounds and the home theme are generated
 * from note lists at runtime: original tones, no recordings, no audio files.
 */
object ChipSynth {
    const val RATE = 22_050

    enum class Wave { SQUARE, PULSE, TRIANGLE, NOISE }

    /** [freq] 0 = a rest. [slideTo] makes a pitch slide (laser / fall sounds). */
    data class Note(val freq: Float, val ms: Int, val vol: Float = 1f, val wave: Wave = Wave.SQUARE, val slideTo: Float? = null)

    fun render(notes: List<Note>): ShortArray {
        val total = notes.sumOf { it.ms * RATE / 1000 }
        val out = ShortArray(total)
        var pos = 0
        val noise = Random(42)
        for (note in notes) {
            val n = note.ms * RATE / 1000
            var phase = 0.0
            for (i in 0 until n) {
                val t = i / n.toFloat()
                val freq = note.slideTo?.let { note.freq + (it - note.freq) * t } ?: note.freq
                phase += freq / RATE
                val frac = (phase - kotlin.math.floor(phase)).toFloat()
                val raw = when (note.wave) {
                    Wave.SQUARE -> if (frac < 0.5f) 1f else -1f
                    Wave.PULSE -> if (frac < 0.25f) 1f else -1f
                    Wave.TRIANGLE -> (if (frac < 0.5f) frac * 4f - 1f else 3f - frac * 4f)
                    Wave.NOISE -> noise.nextFloat() * 2f - 1f
                }
                // Short attack, linear decay: keeps notes from clicking.
                val attack = (i / (RATE * 0.003f)).coerceAtMost(1f)
                val decay = 1f - t * 0.55f
                val tail = ((n - i) / (RATE * 0.004f)).coerceAtMost(1f)
                val v = if (note.freq <= 0f && note.wave != Wave.NOISE) 0f else raw * note.vol * attack * decay * tail
                out[pos + i] = (v * 9000f).toInt().coerceIn(-32000, 32000).toShort()
            }
            pos += n
        }
        return out
    }

    /** Sums several tracks (lead + bass...) into one buffer. */
    fun mix(tracks: List<ShortArray>): ShortArray {
        val len = tracks.maxOf { it.size }
        return ShortArray(len) { i -> tracks.sumOf { if (i < it.size) it[i].toInt() else 0 }.coerceIn(-32000, 32000).toShort() }
    }
}

/** The UI sound set. Each is a handful of notes. */
enum class Sfx(val notes: List<ChipSynth.Note>) {
    TAP(listOf(ChipSynth.Note(1320f, 22, 0.7f))),
    SUCCESS(listOf(ChipSynth.Note(1047f, 55), ChipSynth.Note(1319f, 55), ChipSynth.Note(1568f, 110))),
    ERROR(listOf(ChipSynth.Note(220f, 90, wave = ChipSynth.Wave.PULSE), ChipSynth.Note(165f, 150, wave = ChipSynth.Wave.PULSE))),
    CATCH(
        listOf(
            ChipSynth.Note(392f, 60), ChipSynth.Note(0f, 40), ChipSynth.Note(392f, 60), ChipSynth.Note(0f, 40),
            ChipSynth.Note(523f, 70), ChipSynth.Note(659f, 70), ChipSynth.Note(784f, 70), ChipSynth.Note(1047f, 70), ChipSynth.Note(1319f, 230),
        ),
    ),
    LEVEL_UP(
        listOf(
            ChipSynth.Note(523f, 80), ChipSynth.Note(659f, 80), ChipSynth.Note(784f, 80), ChipSynth.Note(1047f, 140),
            ChipSynth.Note(784f, 70), ChipSynth.Note(1047f, 70), ChipSynth.Note(1319f, 70), ChipSynth.Note(1568f, 260),
        ),
    ),
    FLIP(listOf(ChipSynth.Note(0f, 35, 0.5f, ChipSynth.Wave.NOISE), ChipSynth.Note(660f, 40, slideTo = 990f))),
    PACK_TEAR(listOf(ChipSynth.Note(0f, 160, 0.7f, ChipSynth.Wave.NOISE), ChipSynth.Note(0f, 90, 0.4f, ChipSynth.Wave.NOISE))),
    PACK_OPEN(
        listOf(
            ChipSynth.Note(784f, 60, wave = ChipSynth.Wave.PULSE), ChipSynth.Note(988f, 60, wave = ChipSynth.Wave.PULSE),
            ChipSynth.Note(1175f, 60, wave = ChipSynth.Wave.PULSE), ChipSynth.Note(1568f, 90, wave = ChipSynth.Wave.PULSE),
            ChipSynth.Note(1976f, 220, wave = ChipSynth.Wave.PULSE),
        ),
    ),
    SHINY(
        listOf(
            ChipSynth.Note(1319f, 60), ChipSynth.Note(1568f, 60), ChipSynth.Note(2093f, 60), ChipSynth.Note(1568f, 60),
            ChipSynth.Note(2093f, 60), ChipSynth.Note(2637f, 260),
        ),
    ),
    HIT(listOf(ChipSynth.Note(0f, 70, 0.8f, ChipSynth.Wave.NOISE), ChipSynth.Note(140f, 60, slideTo = 70f))),
    FAINT(listOf(ChipSynth.Note(440f, 80, slideTo = 330f), ChipSynth.Note(330f, 100, slideTo = 200f), ChipSynth.Note(200f, 220, slideTo = 110f))),
    SECRET(
        listOf(
            ChipSynth.Note(659f, 70), ChipSynth.Note(784f, 70), ChipSynth.Note(1047f, 70), ChipSynth.Note(784f, 70),
            ChipSynth.Note(1319f, 70), ChipSynth.Note(1047f, 70), ChipSynth.Note(1568f, 70), ChipSynth.Note(2093f, 300),
        ),
    ),
}

/** The optional home theme: an original 16-bar loop in C major pentatonic with a bouncy bass. */
object HomeTheme {
    private const val EIGHTH = 215 // ms, about 140 bpm

    private val lead: List<Pair<Float, Int>> = listOf(
        // bar 1-2
        659f to 1, 784f to 1, 880f to 1, 784f to 1, 659f to 1, 587f to 1, 523f to 2,
        587f to 1, 659f to 1, 784f to 1, 659f to 1, 587f to 1, 523f to 1, 440f to 2,
        // bar 3-4
        523f to 1, 659f to 1, 784f to 1, 1047f to 1, 880f to 1, 784f to 1, 659f to 2,
        784f to 1, 880f to 1, 784f to 1, 659f to 1, 587f to 1, 659f to 3,
        // bar 5-6
        880f to 1, 1047f to 1, 880f to 1, 784f to 1, 659f to 1, 784f to 1, 880f to 2,
        784f to 1, 659f to 1, 587f to 1, 523f to 1, 587f to 1, 659f to 1, 784f to 2,
        // bar 7-8
        1047f to 1, 880f to 1, 784f to 1, 659f to 1, 587f to 1, 523f to 1, 587f to 2,
        659f to 1, 587f to 1, 523f to 1, 440f to 1, 523f to 4,
    )

    private val bassRoots = listOf(131f, 131f, 110f, 110f, 175f, 175f, 196f, 196f) // per 2 bars: C C A A F F G G (pairs)

    fun render(): ShortArray {
        val leadNotes = lead.map { (f, len) -> ChipSynth.Note(f, EIGHTH * len - 18, 0.55f, ChipSynth.Wave.PULSE).also { } }
            .flatMap { listOf(it, ChipSynth.Note(0f, 18)) }
        val totalMs = leadNotes.sumOf { it.ms }
        // Bass: root on every beat (two eighths), octave bounce.
        val bassNotes = mutableListOf<ChipSynth.Note>()
        var elapsed = 0
        var bar = 0
        while (elapsed < totalMs) {
            val root = bassRoots[(bar / 2) % bassRoots.size]
            for (beat in 0 until 4) {
                if (elapsed >= totalMs) break
                val f = if (beat % 2 == 0) root else root * 2f
                bassNotes += ChipSynth.Note(f, EIGHTH * 2 - 25, 0.7f, ChipSynth.Wave.TRIANGLE)
                bassNotes += ChipSynth.Note(0f, 25)
                elapsed += EIGHTH * 2
            }
            bar++
        }
        return ChipSynth.mix(listOf(ChipSynth.render(leadNotes), ChipSynth.render(bassNotes)))
    }
}
