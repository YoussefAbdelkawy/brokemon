package com.joecode.brokemon.domain

import kotlin.random.Random

/**
 * Procedurally generates an original, horizontally symmetric 16x16 pixel
 * creature from a seed. Same seed + stage always gives the same sprite, so we
 * only store the seed in Room.
 *
 * Output is a palette-index grid; the UI maps indices to colors.
 */
object SpriteGenerator {
    const val SIZE = 16

    const val EMPTY = 0
    const val OUTLINE = 1
    const val BODY = 2
    const val SHADE = 3
    const val ACCENT = 4
    const val EYE = 5
    const val EYE_SHINE = 6
    const val CROWN = 7

    // Half-masks (8 columns, mirrored). 0 = never, 1 = maybe, 2 = always body,
    // 3 = maybe accent, 5 = eye.
    private val rookieMask = listOf(
        "00000000",
        "00000000",
        "00000000",
        "00000000",
        "00000111",
        "00001122",
        "00011222",
        "00012252",
        "00012222",
        "00011222",
        "00001132",
        "00011132",
        "00011222",
        "00001122",
        "00001010",
        "00000000",
    )

    private val homieMask = listOf(
        "00000000",
        "00010000",
        "00011000",
        "00011111",
        "00011122",
        "00112222",
        "01122222",
        "01122252",
        "01122222",
        "00112222",
        "01112232",
        "11112232",
        "01112232",
        "00112222",
        "00011011",
        "00011011",
    )

    private val dayOneMask = listOf(
        "00100100",
        "00110110",
        "01111111",
        "01112222",
        "01122222",
        "11122222",
        "11222252",
        "11222222",
        "11122222",
        "11112232",
        "11112232",
        "11122232",
        "01112232",
        "01112222",
        "00111011",
        "00111011",
    )

    fun generate(seed: Long, stage: Int): Array<IntArray> {
        val random = Random(seed)
        val mask = when (stage) {
            0 -> rookieMask
            1 -> homieMask
            else -> dayOneMask
        }
        val half = SIZE / 2
        val grid = Array(SIZE) { IntArray(SIZE) }

        // The random sequence is consumed in the same order for every stage,
        // so evolutions of the same bro share a family resemblance.
        val fill = Array(SIZE) { BooleanArray(half) { random.nextFloat() < 0.6f } }
        val accentFill = Array(SIZE) { BooleanArray(half) { random.nextFloat() < 0.5f } }

        for (y in 0 until SIZE) {
            for (x in 0 until half) {
                val cell = when (mask[y][x]) {
                    '2' -> BODY
                    '1' -> if (fill[y][x]) BODY else EMPTY
                    '3' -> if (accentFill[y][x]) ACCENT else BODY
                    '5' -> EYE
                    else -> EMPTY
                }
                grid[y][x] = cell
                grid[y][SIZE - 1 - x] = cell
            }
        }

        // Eye shine sits up-left of each eye.
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            if (grid[y][x] == EYE && y > 0 && grid[y - 1][x] == BODY) grid[y - 1][x] = EYE_SHINE
        }

        // Shade the lower third of the body for depth.
        for (y in SIZE * 2 / 3 until SIZE) for (x in 0 until SIZE) {
            if (grid[y][x] == BODY) grid[y][x] = SHADE
        }

        // Crown pixels on the final stage.
        if (stage >= 2) {
            for (x in 0 until SIZE) if (grid[0][x] == BODY) grid[0][x] = CROWN
        }

        // Outline: any empty pixel touching a filled one.
        val outlined = Array(SIZE) { grid[it].clone() }
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            if (grid[y][x] != EMPTY) continue
            val touches = listOf(y - 1 to x, y + 1 to x, y to x - 1, y to x + 1).any { (ny, nx) ->
                ny in 0 until SIZE && nx in 0 until SIZE && grid[ny][nx] != EMPTY
            }
            if (touches) outlined[y][x] = OUTLINE
        }
        return outlined
    }
}
