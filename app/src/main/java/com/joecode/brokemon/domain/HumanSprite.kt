package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookOptions
import kotlin.math.roundToInt

/**
 * Draws a bro as a 32x32 pixel-art portrait (head and shoulders), layer by
 * layer, straight into an ARGB pixel array. Pure Kotlin so it's unit-testable.
 *
 * Evolution adds bling: stage 1 gets a gold chain, stage 2 also gets a crown.
 * Shiny bros wear a gold fit.
 */
object HumanSprite {
    const val SIZE = 32

    const val CLEAR = 0x00000000
    private const val OUTLINE = 0xFF0B0B10.toInt()
    private const val EYE = 0xFF1A1A22.toInt()
    private const val WHITE = 0xFFF5F5F7.toInt()
    private const val MOUTH = 0xFF5A1E22.toInt()
    private const val FRAME = 0xFF16161C.toInt()
    private const val LENS = 0xFF0F1018.toInt()
    private const val GLINT = 0xFF8FA3B8.toInt()
    private const val GOLD = 0xFFFFD54A.toInt()
    private const val GOLD_DARK = 0xFFC99A1E.toInt()
    private const val RUBY = 0xFFE63946.toInt()
    private const val SAPPHIRE = 0xFF3A86FF.toInt()
    private const val BLUSH = 0xFFFF8A8A.toInt()

    // Hair style indices (see LookOptions.hairStyles).
    private const val BUZZ = 0
    private const val SHORT = 1
    private const val SPIKY = 2
    private const val CURLY = 3
    private const val LONG = 4
    private const val MOHAWK = 5
    private const val BALD = 6
    private const val BUN = 7
    private const val SIDE_PART = 8
    private const val MULLET = 9

    fun render(look: BroLook, stage: Int, shiny: Boolean): IntArray {
        val c = Canvas()
        val skin = LookOptions.skinTones[look.skin].toInt()
        val skinShade = blend(skin, 0xFF000000.toInt(), 0.18f)
        val hair = LookOptions.hairColors[look.hairColor].toInt()
        val hairShade = blend(hair, 0xFF000000.toInt(), 0.3f)
        val hairLight = blend(hair, 0xFFFFFFFF.toInt(), 0.25f)
        val outfit = if (shiny) GOLD else LookOptions.outfitColors[look.outfitColor].toInt()
        val outfitShade = blend(outfit, 0xFF000000.toInt(), 0.28f)
        val hatCovers = look.hat == 1 || look.hat == 2

        drawBody(c, look.outfit, outfit, outfitShade, skin, skinShade, shiny)
        drawBackHair(c, look.hair, hair, hairShade)
        drawNeckAndHead(c, skin, skinShade)
        drawFace(c, look, skin, skinShade, hair)
        drawFacialHair(c, look.facialHair, skin, hair, hairShade)
        drawMouth(c, look.expression, skin)
        drawFrontHair(c, look.hair, hair, hairShade, hairLight, skin, hatCovers)
        drawHat(c, look.hat, outfit, outfitShade)
        drawGlasses(c, look.glasses)
        if (stage >= 1) drawChain(c)
        if (stage >= 2) drawCrown(c)
        return c.outlined()
    }

    // --- Geometry ------------------------------------------------------------

    /** Half-width of the head on each row; the head spans x = 16-w .. 15+w. */
    private fun headHalf(y: Int): Int = when (y) {
        7 -> 4
        8 -> 6
        9 -> 7
        in 10..17 -> 8
        18 -> 7
        19 -> 6
        20 -> 5
        21 -> 4
        else -> 0
    }

    private fun inHead(x: Int, y: Int): Boolean {
        val w = headHalf(y)
        return w > 0 && x in (16 - w)..(15 + w)
    }

    // --- Layers --------------------------------------------------------------

    private fun drawBody(c: Canvas, style: Int, color: Int, shade: Int, skin: Int, skinShade: Int, shiny: Boolean) {
        for (y in 25..31) {
            val half = when (y) {
                25 -> 7
                26 -> 10
                else -> 12
            }
            c.row(y, 16 - half, 15 + half, color)
            c[16 - half, y] = shade
            c[15 + half, y] = shade
        }
        when (style) {
            1 -> { // Hoodie: hood bunched around the neck, strings, front pocket.
                c.row(24, 10, 12, shade)
                c.row(24, 19, 21, shade)
                c.row(25, 9, 22, shade)
                c.row(30, 11, 20, shade)
                c.row(31, 11, 20, shade)
                for (y in 26..28) {
                    c[14, y] = WHITE
                    c[17, y] = WHITE
                }
            }
            2 -> { // Jersey: V collar trim, sleeve stripes, a number 7.
                c.row(25, 14, 17, skin)
                c.row(26, 15, 16, skin)
                c[13, 25] = WHITE; c[18, 25] = WHITE; c[14, 26] = WHITE; c[17, 26] = WHITE
                for (y in 27..31) {
                    c[6, y] = WHITE; c[7, y] = WHITE
                    c[24, y] = WHITE; c[25, y] = WHITE
                }
                c.row(28, 14, 17, WHITE)
                c[17, 29] = WHITE; c[16, 30] = WHITE; c[16, 31] = WHITE
            }
            3 -> { // Suit: white shirt, tie, lapels.
                c.row(25, 13, 18, WHITE)
                c.row(26, 14, 17, WHITE)
                c.row(27, 14, 17, WHITE)
                for (y in 28..31) c.row(y, 15, 16, WHITE)
                val tie = if (shiny) RUBY else blend(color, RUBY, 0.7f)
                c.row(26, 15, 16, tie)
                for (y in 27..31) c.row(y, 15, 16, tie)
                for (y in 26..29) {
                    c[13, y] = shade
                    c[18, y] = shade
                }
            }
            else -> { // Tee: round collar and sleeve seams.
                c.row(25, 14, 17, skin)
                c.row(26, 15, 16, skinShade)
                for (y in 27..31) {
                    c[8, y] = shade
                    c[23, y] = shade
                }
            }
        }
    }

    private fun drawBackHair(c: Canvas, style: Int, hair: Int, shade: Int) {
        when (style) {
            LONG -> for (y in 10..28) {
                c.row(y, 6, 9, if (y % 4 == 0) shade else hair)
                c.row(y, 22, 25, if (y % 4 == 0) shade else hair)
            }
            MULLET -> for (y in 11..25) {
                c.row(y, 5, 8, if (y % 3 == 0) shade else hair)
                c.row(y, 23, 26, if (y % 3 == 0) shade else hair)
            }
        }
    }

    private fun drawNeckAndHead(c: Canvas, skin: Int, skinShade: Int) {
        for (y in 22..24) c.row(y, 13, 18, skin)
        c.row(22, 13, 18, skinShade)
        for (y in 7..21) {
            val w = headHalf(y)
            c.row(y, 16 - w, 15 + w, skin)
        }
        // Jaw shading and ears.
        c.row(21, 12, 19, skinShade)
        for (y in 13..15) {
            c[7, y] = skin
            c[24, y] = skin
        }
        c[7, 14] = skinShade
        c[24, 14] = skinShade
    }

    private fun drawFace(c: Canvas, look: BroLook, skin: Int, skinShade: Int, hair: Int) {
        val brow = if (look.hair == BALD) blend(skin, 0xFF000000.toInt(), 0.4f) else blend(hair, 0xFF000000.toInt(), 0.2f)
        c.row(12, 10, 12, brow)
        c.row(12, 19, 21, brow)
        if (look.expression == 2) {
            // "Chill": half-closed eyes.
            c.row(14, 11, 12, skinShade); c.row(14, 19, 20, skinShade)
            c.row(15, 11, 12, EYE); c.row(15, 19, 20, EYE)
        } else {
            for (y in 14..15) {
                c.row(y, 11, 12, EYE)
                c.row(y, 19, 20, EYE)
            }
            c[12, 14] = WHITE
            c[20, 14] = WHITE
        }
        // Nose
        c[15, 17] = skinShade
        c[16, 17] = skinShade
        if (look.expression == 1) {
            c[10, 17] = blend(skin, BLUSH, 0.45f)
            c[21, 17] = blend(skin, BLUSH, 0.45f)
        }
    }

    private fun drawFacialHair(c: Canvas, style: Int, skin: Int, hair: Int, hairShade: Int) {
        fun mustache() {
            c.row(18, 13, 18, hair)
        }
        when (style) {
            1 -> for (y in 18..21) for (x in 0 until SIZE) {
                val mouthArea = y in 19..20 && x in 13..18
                if (inHead(x, y) && !mouthArea && (x + y) % 2 == 0) c[x, y] = blend(skin, hair, 0.45f)
            }
            2 -> {
                mustache()
                c[13, 19] = hair
                c[18, 19] = hair
            }
            3 -> {
                for (y in 16..21) for (x in 0 until SIZE) {
                    if (!inHead(x, y)) continue
                    val sideburn = x <= 9 || x >= 22
                    if (y >= 18 || sideburn) c[x, y] = if ((x + y) % 5 == 0) hairShade else hair
                }
                c.row(22, 12, 19, hair)
                c.row(23, 14, 17, hairShade)
            }
            4 -> {
                mustache()
                for (y in 20..22) c.row(y, 14, 17, hair)
            }
        }
    }

    private fun drawMouth(c: Canvas, expression: Int, skin: Int) {
        when (expression) {
            1 -> { // Grin
                c[13, 19] = MOUTH; c[18, 19] = MOUTH
                c.row(19, 14, 17, WHITE)
                c.row(20, 14, 17, MOUTH)
            }
            2 -> c.row(19, 14, 17, MOUTH) // Chill
            3 -> { // Smirk
                c.row(19, 14, 16, MOUTH)
                c[17, 18] = MOUTH
            }
            else -> { // Smile
                c[13, 19] = MOUTH; c[18, 19] = MOUTH
                c.row(20, 14, 17, MOUTH)
            }
        }
    }

    private fun drawFrontHair(c: Canvas, style: Int, hair: Int, shade: Int, light: Int, skin: Int, hatCovers: Boolean) {
        // A hat hides everything on top of the head; only the sides show.
        fun put(x: Int, y: Int, color: Int) {
            if (hatCovers && y <= 9) return
            c[x, y] = color
        }
        fun row(y: Int, x0: Int, x1: Int, color: Int) {
            for (x in x0..x1) put(x, y, color)
        }
        /** Full hair cap with volume above the head; [fringe] draws the bangs. */
        fun top(fringe: () -> Unit) {
            row(4, 12, 19, hair)
            row(5, 10, 21, hair)
            row(6, 9, 22, hair)
            for (y in 7..9) row(y, 8, 23, hair)
            for (y in 10..12) {
                row(y, 7, 9, hair)
                row(y, 22, 24, hair)
            }
            row(5, 12, 14, light)
            row(6, 11, 12, light)
            row(9, 8, 9, shade)
            row(9, 22, 23, shade)
            fringe()
        }
        fun shortTop() = top {
            // Swept bangs.
            row(10, 10, 15, hair)
            row(11, 10, 12, hair)
            put(15, 10, shade)
        }
        when (style) {
            BUZZ -> {
                val buzz = blend(hair, skin, 0.45f)
                for (y in 7..9) for (x in 0 until SIZE) if (inHead(x, y)) put(x, y, buzz)
                row(10, 8, 9, buzz)
                row(10, 22, 23, buzz)
            }
            SHORT -> shortTop()
            SPIKY -> {
                shortTop()
                for (t in listOf(9, 12, 15, 19, 22)) {
                    row(4, t - 1, t + 1, hair)
                    put(t, 3, hair)
                    if (t in 12..19) put(t, 2, light)
                }
            }
            CURLY -> for (y in 1..14) for (x in 3..28) {
                val dx = (x - 15.5f) / 12.5f
                val dy = (y - 8.5f) / 7.5f
                if (dx * dx + dy * dy > 1f) continue
                val face = y >= 10 && x in 10..21
                if (face) continue
                if ((x * 7 + y * 3) % 5 == 0) put(x, y, shade)
                else if ((x + y * 2) % 7 == 0) put(x, y, light)
                else put(x, y, hair)
            }
            LONG -> top {
                // Curtain bangs with a middle part.
                row(10, 9, 13, hair)
                row(11, 9, 11, hair)
                row(10, 18, 22, hair)
                row(11, 20, 22, hair)
                for (y in 4..7) row(y, 15, 16, shade)
            }
            MOHAWK -> {
                val shaved = blend(hair, skin, 0.6f)
                for (y in 8..10) for (x in 0 until SIZE) if (inHead(x, y)) put(x, y, shaved)
                for (y in 1..9) row(y, 14, 17, if (y % 3 == 0) light else hair)
            }
            BALD -> {
                val shine = blend(skin, 0xFFFFFFFF.toInt(), 0.5f)
                put(11, 8, shine)
                put(12, 8, shine)
                put(11, 9, shine)
            }
            BUN -> top {
                // Slicked back: no bangs, big bun on top, tie in the middle.
                row(0, 14, 17, hair)
                row(1, 13, 18, hair)
                row(2, 12, 19, light)
                row(3, 13, 18, hair)
                row(4, 14, 17, shade)
            }
            SIDE_PART -> top {
                // Deep side part with the hair swept across to the right.
                for (y in 5..9) put(12, y, blend(skin, hair, 0.35f))
                row(10, 13, 22, hair)
                row(11, 18, 22, hair)
                row(10, 13, 15, light)
            }
            MULLET -> top {
                // Short spiky front... party in the back.
                row(10, 10, 12, hair)
                row(10, 15, 16, hair)
                row(10, 19, 21, hair)
            }
        }
    }

    private fun drawHat(c: Canvas, hat: Int, color: Int, shade: Int) {
        when (hat) {
            1 -> { // Cap
                c.row(4, 12, 19, color)
                c.row(5, 10, 21, color)
                for (y in 6..8) c.row(y, 8, 23, color)
                c.row(9, 7, 24, shade)
                c.row(10, 7, 24, shade)
                c[15, 6] = WHITE
                c[16, 6] = WHITE
            }
            2 -> { // Beanie
                c.row(3, 12, 19, color)
                c.row(4, 10, 21, color)
                for (y in 5..8) c.row(y, 8, 23, color)
                val fold = blend(color, 0xFFFFFFFF.toInt(), 0.25f)
                c.row(9, 7, 24, fold)
                c.row(10, 7, 24, fold)
                for (y in 5..8) for (x in 9..22 step 3) c[x, y] = shade
                c.row(1, 15, 16, WHITE)
                c.row(2, 14, 17, WHITE)
            }
            3 -> { // Headband
                c.row(9, 8, 23, RUBY)
                c.row(10, 8, 23, WHITE)
            }
        }
    }

    private fun drawGlasses(c: Canvas, style: Int) {
        when (style) {
            1, 2 -> {
                for ((x0, x1) in listOf(10 to 13, 18 to 21)) {
                    for (x in x0..x1) for (y in 13..16) {
                        val border = x == x0 || x == x1 || y == 13 || y == 16
                        val corner = (x == x0 || x == x1) && (y == 13 || y == 16)
                        if (border && (style == 2 || !corner)) c[x, y] = FRAME
                    }
                }
                c.row(14, 14, 17, FRAME)
                c.row(14, 8, 9, FRAME)
                c.row(14, 22, 23, FRAME)
            }
            3 -> {
                for (y in 13..15) {
                    c.row(y, 10, 13, LENS)
                    c.row(y, 18, 21, LENS)
                }
                c.row(13, 14, 17, FRAME)
                c.row(13, 8, 9, FRAME)
                c.row(13, 22, 23, FRAME)
                c[11, 13] = GLINT
                c[19, 13] = GLINT
            }
        }
    }

    private fun drawChain(c: Canvas) {
        val links = listOf(13 to 24, 13 to 25, 14 to 26, 15 to 26, 16 to 26, 17 to 26, 18 to 25, 18 to 24)
        links.forEachIndexed { i, (x, y) -> c[x, y] = if (i % 2 == 0) GOLD else GOLD_DARK }
        c.row(27, 15, 16, GOLD)
        c[15, 28] = GOLD_DARK
        c[16, 28] = GOLD_DARK
    }

    private fun drawCrown(c: Canvas) {
        c.row(3, 11, 20, GOLD_DARK)
        c.row(2, 11, 20, GOLD)
        for (x in listOf(11, 14, 15, 16, 17, 20)) c[x, 1] = GOLD
        for (x in listOf(11, 15, 16, 20)) c[x, 0] = GOLD
        c[13, 2] = RUBY
        c[18, 2] = SAPPHIRE
        c[15, 2] = RUBY
        c[16, 2] = RUBY
    }

    // --- Helpers -------------------------------------------------------------

    private class Canvas {
        val px = IntArray(SIZE * SIZE)

        operator fun set(x: Int, y: Int, color: Int) {
            if (x in 0 until SIZE && y in 0 until SIZE) px[y * SIZE + x] = color
        }

        operator fun get(x: Int, y: Int): Int = px[y * SIZE + x]

        fun row(y: Int, x0: Int, x1: Int, color: Int) {
            for (x in x0..x1) this[x, y] = color
        }

        /** Adds a 1px dark outline around everything that was drawn. */
        fun outlined(): IntArray {
            val out = px.clone()
            for (y in 0 until SIZE) for (x in 0 until SIZE) {
                if (this[x, y] != CLEAR) continue
                val touches = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) ->
                    nx in 0 until SIZE && ny in 0 until SIZE && this[nx, ny] != CLEAR
                }
                if (touches) out[y * SIZE + x] = OUTLINE
            }
            return out
        }
    }

    internal fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, shift: Int) = (c shr shift) and 0xFF
        fun mix(shift: Int) = (ch(a, shift) + (ch(b, shift) - ch(a, shift)) * t).roundToInt()
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
