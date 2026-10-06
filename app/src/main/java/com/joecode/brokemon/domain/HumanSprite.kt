package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookOptions
import kotlin.math.roundToInt

/**
 * Draws a bro as pixel art, layer by layer, straight into an ARGB pixel array.
 * Pure Kotlin so it's unit-testable.
 *
 * - [render]: 32x32 portrait (head and shoulders) for cards.
 * - [renderFullBody]: 32x52 standing or sitting figure for the bro's room.
 *
 * Evolution adds bling: stage 1 gets a gold chain, stage 2 also gets a crown.
 * Shiny bros wear a gold fit.
 */
object HumanSprite {
    const val SIZE = 32
    const val BODY_W = 32
    const val BODY_H = 52

    const val CLEAR = 0x00000000
    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFF5F5F7.toInt()
    private const val OUTLINE = 0xFF0B0B10.toInt()
    private const val EYE = 0xFF1A1A22.toInt()
    private const val MOUTH = 0xFF5A1E22.toInt()
    private const val FRAME = 0xFF16161C.toInt()
    private const val LENS = 0xFF101522.toInt()
    private const val LENS_LOW = 0xFF2A3550.toInt()
    private const val LENS_TINT = 0xFFBFE3FF.toInt()
    private const val GLINT = 0xFFDDE8F5.toInt()
    private const val GOLD = 0xFFFFD54A.toInt()
    private const val GOLD_DARK = 0xFFC99A1E.toInt()
    private const val RUBY = 0xFFE63946.toInt()
    private const val SAPPHIRE = 0xFF3A86FF.toInt()
    private const val BLUSH = 0xFFFF8A8A.toInt()
    private const val DENIM = 0xFF2D3A5A.toInt()
    private const val SNEAKER = 0xFFEDEDF2.toInt()
    private const val SOLE = 0xFF8A8A98.toInt()
    private const val LEATHER = 0xFF2A2A33.toInt()

    // Hair styles (indices into LookOptions.hairStyles, append-only).
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
    private const val FADE = 10
    private const val AFRO = 11
    private const val BRAIDS = 12
    private const val LOCS = 13
    private const val PONYTAIL = 14
    private const val WAVES = 15

    // Hats
    private const val CAP = 1
    private const val BEANIE = 2
    private const val HEADBAND = 3
    private const val BUCKET = 4
    private const val BACKWARDS = 5
    private const val HIJAB = 6
    private const val TRAINER_CAP = 7

    // Outfits
    private const val TEE = 0
    private const val HOODIE = 1
    private const val JERSEY = 2
    private const val SUIT = 3
    private const val GALABEYA = 4
    private const val JACKET = 5

    /** Card portrait, 32x32. */
    fun render(look: BroLook, stage: Int, shiny: Boolean): IntArray {
        val c = Canvas(SIZE, SIZE)
        val p = Palette(look, shiny)
        drawBody(c, look, p, bottom = 31, fullBody = false)
        drawHead(c, look, p)
        if (stage >= 1) drawChain(c)
        if (stage >= 2) drawCrown(c)
        return c.outlined()
    }

    /** Full figure for the room, 32x52. Sitting bros have shorter, bent legs. */
    fun renderFullBody(look: BroLook, stage: Int, shiny: Boolean, sitting: Boolean): IntArray {
        val c = Canvas(BODY_W, BODY_H)
        val p = Palette(look, shiny)
        drawLegs(c, look, p, sitting)
        drawArms(c, look, p)
        drawBody(c, look, p, bottom = if (look.outfit == GALABEYA) (if (sitting) 46 else 49) else 39, fullBody = true)
        drawHead(c, look, p)
        if (stage >= 1) drawChain(c)
        if (stage >= 2) drawCrown(c)
        return c.outlined()
    }

    private class Palette(look: BroLook, shiny: Boolean) {
        val skin = LookOptions.skinTones[look.skin].toInt()
        val skinShade = blend(skin, BLACK, 0.18f)
        val skinLight = blend(skin, WHITE, 0.15f)
        val hair = LookOptions.hairColors[look.hairColor].toInt()
        val hairShade = blend(hair, BLACK, 0.32f)
        val hairLight = blend(hair, WHITE, 0.28f)
        val outfit = if (shiny) GOLD else LookOptions.outfitColors[look.outfitColor].toInt()
        val outfitShade = blend(outfit, BLACK, 0.28f)
        val outfitLight = blend(outfit, WHITE, 0.2f)
    }

    // --- Head ------------------------------------------------------------------

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

    private fun drawHead(c: Canvas, look: BroLook, p: Palette) {
        val hijab = look.hat == HIJAB
        if (!hijab) drawBackHair(c, look.hair, p)
        drawNeckAndHead(c, p)
        drawFace(c, look, p)
        drawExtras(c, look.extra, p, hijab)
        drawFacialHair(c, look.facialHair, p)
        drawMouth(c, look.expression)
        if (!hijab) {
            drawFrontHair(c, look.hair, p, hatCovers = look.hat in listOf(CAP, BEANIE, BUCKET, BACKWARDS, TRAINER_CAP))
            c.lightHair(p.hair, p.hairLight, p.hairShade)
        }
        drawHat(c, look.hat, p)
        drawGlasses(c, look.glasses)
    }

    private fun drawNeckAndHead(c: Canvas, p: Palette) {
        for (y in 22..24) c.row(y, 13, 18, p.skin)
        c.row(22, 13, 18, p.skinShade)
        for (y in 7..21) {
            val w = headHalf(y)
            c.row(y, 16 - w, 15 + w, p.skin)
        }
        // Soft light from the top-left: highlight one cheek, shade the other side and the jaw.
        for (y in 10..18) c[9, y] = p.skinLight
        for (y in 11..19) c[23 - (if (y >= 18) y - 17 else 0), y] = p.skinShade
        c.row(21, 12, 19, p.skinShade)
        for (y in 13..15) {
            c[7, y] = p.skin
            c[24, y] = p.skinShade
        }
        c[7, 14] = p.skinShade
    }

    private fun drawFace(c: Canvas, look: BroLook, p: Palette) {
        val brow = if (look.hair == BALD) blend(p.skin, BLACK, 0.4f) else blend(p.hair, BLACK, 0.2f)
        val browY = if (look.expression == 4) 11 else 12
        c.row(browY, 10, 12, brow)
        c.row(browY, 19, 21, brow)
        when (look.expression) {
            2 -> { // Chill: half-closed
                c.row(14, 11, 12, p.skinShade); c.row(14, 19, 20, p.skinShade)
                c.row(15, 11, 12, EYE); c.row(15, 19, 20, EYE)
            }
            5 -> { // Sleepy: closed
                c.row(15, 11, 12, EYE); c.row(15, 19, 20, EYE)
                c[10, 15] = p.skinShade; c[21, 15] = p.skinShade
            }
            else -> {
                for (y in 14..15) {
                    c.row(y, 11, 12, EYE)
                    c.row(y, 19, 20, EYE)
                }
                c[12, 14] = WHITE
                c[20, 14] = WHITE
                if (look.expression == 4) { // Shocked: wide eyes
                    c[11, 13] = EYE; c[12, 13] = EYE; c[19, 13] = EYE; c[20, 13] = EYE
                }
            }
        }
        c[15, 17] = p.skinShade
        c[16, 17] = p.skinShade
        if (look.expression == 1) {
            c[10, 17] = blend(p.skin, BLUSH, 0.45f)
            c[21, 17] = blend(p.skin, BLUSH, 0.45f)
        }
    }

    private fun drawExtras(c: Canvas, extra: Int, p: Palette, hijab: Boolean) {
        when (extra) {
            1 -> if (!hijab) { c[7, 16] = GOLD; c[24, 16] = GOLD } // Earring
            2 -> for ((x, y) in listOf(10 to 16, 12 to 17, 11 to 18, 19 to 17, 21 to 16, 20 to 18)) {
                c[x, y] = blend(p.skin, 0xFF7A4A2A.toInt(), 0.45f) // Freckles
            }
            3 -> for (x in listOf(10, 11, 20, 21)) c[x, 17] = blend(p.skin, BLUSH, 0.55f) // Blush
            4 -> c[17, 18] = GOLD // Nose ring
            5 -> { // Scar through the right eyebrow
                c[20, 11] = blend(p.skin, WHITE, 0.4f)
                c[20, 12] = blend(p.skin, WHITE, 0.4f)
                c[21, 13] = blend(p.skin, WHITE, 0.4f)
            }
            6 -> c[19, 18] = blend(p.skin, BLACK, 0.55f) // Mole
        }
    }

    private fun drawFacialHair(c: Canvas, style: Int, p: Palette) {
        fun mustache() = c.row(18, 13, 18, p.hair)
        when (style) {
            1 -> for (y in 18..21) for (x in 0 until SIZE) {
                val mouthArea = y in 19..20 && x in 13..18
                if (inHead(x, y) && !mouthArea && (x + y) % 2 == 0) c[x, y] = blend(p.skin, p.hair, 0.45f)
            }
            2 -> {
                mustache()
                c[13, 19] = p.hair
                c[18, 19] = p.hair
                c[13, 18] = p.hairShade
                c[18, 18] = p.hairShade
            }
            3 -> {
                for (y in 16..21) for (x in 0 until SIZE) {
                    if (!inHead(x, y)) continue
                    val sideburn = x <= 9 || x >= 22
                    if (y >= 18 || sideburn) c[x, y] = if ((x + y) % 5 == 0) p.hairShade else p.hair
                }
                c.row(22, 12, 19, p.hair)
                c.row(23, 14, 17, p.hairShade)
                c.row(18, 13, 18, p.hairLight)
            }
            4 -> {
                mustache()
                for (y in 20..22) c.row(y, 14, 17, p.hair)
                c[14, 22] = p.hairShade
                c[17, 22] = p.hairShade
            }
        }
    }

    private fun drawMouth(c: Canvas, expression: Int) {
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
            4 -> { // Shocked: little "o"
                c.row(19, 15, 16, MOUTH)
                c.row(20, 15, 16, MOUTH)
            }
            5 -> c.row(19, 15, 16, MOUTH) // Sleepy
            else -> { // Smile
                c[13, 19] = MOUTH; c[18, 19] = MOUTH
                c.row(20, 14, 17, MOUTH)
            }
        }
    }

    // --- Hair ------------------------------------------------------------------

    private fun drawBackHair(c: Canvas, style: Int, p: Palette) {
        fun strand(x: Int, y0: Int, y1: Int, alt: Boolean = false) {
            for (y in y0..y1) c.hair(x, y, if (alt && y % 2 == 0) p.hairShade else p.hair)
        }
        when (style) {
            LONG -> for (y in 10..28) {
                for (x in 6..9) c.hair(x, y, p.hair)
                for (x in 22..25) c.hair(x, y, p.hair)
            }
            MULLET -> for (y in 11..25) {
                for (x in 5..8) c.hair(x, y, p.hair)
                for (x in 23..26) c.hair(x, y, p.hair)
            }
            BRAIDS -> {
                for (x in listOf(7, 8, 23, 24)) strand(x, 12, 27, alt = true)
                c[7, 28] = GOLD; c[8, 28] = GOLD; c[23, 28] = GOLD; c[24, 28] = GOLD
            }
            LOCS -> for (x in listOf(5, 7, 9, 22, 24, 26)) strand(x, 9, if (x == 5 || x == 26) 24 else 27, alt = true)
            PONYTAIL -> {
                for (y in 8..21) {
                    val half = if (y < 12) 1 else if (y < 18) 2 else 1
                    for (x in (25 - half)..(25 + half)) c.hair(x, y, p.hair)
                }
                c[24, 9] = RUBY; c[25, 9] = RUBY; c[26, 9] = RUBY
            }
        }
    }

    private fun drawFrontHair(c: Canvas, style: Int, p: Palette, hatCovers: Boolean) {
        val hair = p.hair
        val shade = p.hairShade
        val light = p.hairLight
        // A hat hides everything on top of the head; only the sides show.
        fun put(x: Int, y: Int, color: Int) {
            if (hatCovers && y <= 9) return
            c.hair(x, y, color)
        }
        fun row(y: Int, x0: Int, x1: Int, color: Int = hair) {
            for (x in x0..x1) put(x, y, color)
        }
        fun top(fringe: () -> Unit) {
            row(4, 12, 19)
            row(5, 10, 21)
            row(6, 9, 22)
            for (y in 7..9) row(y, 8, 23)
            for (y in 10..12) {
                row(y, 7, 9)
                row(y, 22, 24)
            }
            // Strand lines for texture.
            for ((x, y) in listOf(13 to 7, 14 to 8, 18 to 6, 19 to 7, 20 to 8)) put(x, y, shade)
            fringe()
        }
        fun shortTop() = top {
            row(10, 10, 15)
            row(11, 10, 12)
            put(15, 10, shade)
        }
        when (style) {
            BUZZ -> {
                val buzz = blend(hair, p.skin, 0.45f)
                for (y in 7..9) for (x in 0 until SIZE) if (inHead(x, y)) put(x, y, buzz)
                row(10, 8, 9, buzz)
                row(10, 22, 23, buzz)
            }
            SHORT -> shortTop()
            SPIKY -> {
                shortTop()
                for (t in listOf(9, 12, 15, 19, 22)) {
                    row(4, t - 1, t + 1)
                    put(t, 3, hair)
                    if (t in 12..19) put(t, 2, light)
                }
            }
            CURLY -> for (y in 1..14) for (x in 3..28) {
                val dx = (x - 15.5f) / 12.5f
                val dy = (y - 8.5f) / 7.5f
                if (dx * dx + dy * dy > 1f || (y >= 10 && x in 10..21)) continue
                put(x, y, if ((x * 7 + y * 3) % 5 == 0) shade else if ((x + y * 2) % 7 == 0) light else hair)
            }
            LONG -> top {
                row(10, 9, 13)
                row(11, 9, 11)
                row(10, 18, 22)
                row(11, 20, 22)
                for (y in 4..7) row(y, 15, 16, shade)
            }
            MOHAWK -> {
                val shaved = blend(hair, p.skin, 0.6f)
                for (y in 8..10) for (x in 0 until SIZE) if (inHead(x, y)) put(x, y, shaved)
                for (y in 1..9) row(y, 14, 17, if (y % 3 == 0) light else hair)
            }
            BALD -> {
                val shine = blend(p.skin, WHITE, 0.5f)
                put(11, 8, shine); put(12, 8, shine); put(11, 9, shine)
            }
            BUN -> top {
                row(0, 14, 17)
                row(1, 13, 18)
                row(2, 12, 19, light)
                row(3, 13, 18)
                row(4, 14, 17, shade)
            }
            SIDE_PART -> top {
                for (y in 5..9) put(12, y, blend(p.skin, hair, 0.35f))
                row(10, 13, 22)
                row(11, 18, 22)
                row(10, 13, 15, light)
            }
            MULLET -> top {
                row(10, 10, 12)
                row(10, 15, 16)
                row(10, 19, 21)
            }
            FADE -> {
                row(5, 11, 20)
                for (y in 6..9) row(y, 9, 22)
                val faded = blend(hair, p.skin, 0.55f)
                for (y in 10..12) {
                    row(y, 7, 9, faded)
                    row(y, 22, 24, faded)
                }
                row(9, 10, 21, shade) // crisp line-up
            }
            AFRO -> for (y in 0..16) for (x in 1..30) {
                val dx = (x - 15.5f) / 14.5f
                val dy = (y - 7.5f) / 8.8f
                if (dx * dx + dy * dy > 1f || (y >= 10 && x in 10..21)) continue
                put(x, y, if ((x * 3 + y * 5) % 4 == 0) shade else if ((x * 5 + y) % 9 == 0) light else hair)
            }
            BRAIDS -> {
                for (y in 5..9) for (x in 9..22) {
                    if (y == 5 && (x < 11 || x > 20)) continue
                    put(x, y, if (x % 2 == 0) hair else blend(hair, p.skin, 0.35f)) // cornrows
                }
                for (y in 10..12) { row(y, 7, 9); row(y, 22, 24) }
            }
            LOCS -> top {
                for (x in listOf(10, 12, 19, 21)) for (y in 10..13) put(x, y, if (y % 2 == 0) shade else hair)
            }
            PONYTAIL -> top {
                row(9, 10, 21, shade) // slicked back
            }
            WAVES -> {
                row(5, 11, 20)
                for (y in 6..9) row(y, 9, 22)
                // 360 waves: thin curved highlight lines, one every other row.
                for (y in listOf(6, 8)) for (x in 10..21) if ((x + (if (y == 6) 0 else 2)) % 4 != 3) put(x, y, light)
                for (x in 11..20 step 3) put(x, 7, shade)
                for (y in 10..11) { row(y, 8, 9); row(y, 22, 23) }
                row(9, 10, 21, shade)
            }
        }
    }

    // --- Hats & glasses --------------------------------------------------------

    private fun drawHat(c: Canvas, hat: Int, p: Palette) {
        val color = p.outfit
        val shade = p.outfitShade
        val light = p.outfitLight
        when (hat) {
            CAP -> {
                c.row(4, 12, 19, color)
                c.row(5, 10, 21, color)
                for (y in 6..8) c.row(y, 8, 23, color)
                c.row(5, 12, 14, light)
                c.row(9, 7, 24, shade)
                c.row(10, 7, 24, shade)
                c[15, 6] = WHITE; c[16, 6] = WHITE
            }
            BEANIE -> {
                c.row(3, 12, 19, color)
                c.row(4, 10, 21, color)
                for (y in 5..8) c.row(y, 8, 23, color)
                for (y in 5..8) for (x in 9..22 step 3) c[x, y] = shade
                c.row(9, 7, 24, light)
                c.row(10, 7, 24, light)
                c.row(1, 15, 16, WHITE)
                c.row(2, 14, 17, WHITE)
            }
            HEADBAND -> {
                c.row(9, 8, 23, RUBY)
                c.row(10, 8, 23, WHITE)
            }
            BUCKET -> {
                c.row(3, 12, 19, color)
                for (y in 4..7) c.row(y, 10, 21, color)
                c.row(8, 10, 21, shade)
                c.row(4, 12, 14, light)
                c.row(9, 5, 26, color)
                c.row(10, 4, 27, shade)
            }
            BACKWARDS -> {
                c.row(4, 12, 19, color)
                c.row(5, 10, 21, color)
                for (y in 6..8) c.row(y, 8, 23, color)
                c.row(9, 8, 23, shade)
                c.row(3, 13, 18, shade) // brim peeking over the top
                c.row(8, 14, 17, p.hair) // strap opening
                c[14, 9] = WHITE; c[17, 9] = WHITE
            }
            TRAINER_CAP -> {
                // Two-tone cap: colored crown, white front panel with a gold star, dark brim.
                c.row(4, 12, 19, color)
                c.row(5, 10, 21, color)
                for (y in 6..8) c.row(y, 8, 23, color)
                for (y in 4..8) c.row(y, 13, 18, WHITE)
                c[15, 5] = GOLD; c[16, 5] = GOLD
                c.row(6, 14, 17, GOLD)
                c[15, 7] = GOLD_DARK; c[16, 7] = GOLD_DARK
                c.row(9, 7, 24, shade)
                c.row(10, 6, 25, LEATHER)
            }
            HIJAB -> {
                for (y in 2..26) for (x in 3..28) {
                    val dx = (x - 15.5f) / 12.5f
                    val dy = (y - 13.5f) / 12f
                    val inShape = dx * dx + dy * dy <= 1f || (y in 21..26 && x in 7..24)
                    val face = y in 9..21 && x in 10..21 && inHead(x, y)
                    if (!inShape || face) continue
                    c[x, y] = when {
                        x < 12 && y < 10 -> light
                        x > 22 || y > 23 -> shade
                        else -> color
                    }
                }
                // Folds framing the face.
                for (y in 9..20) { c[9, y] = shade; c[22, y] = shade }
            }
        }
    }

    private fun drawGlasses(c: Canvas, style: Int) {
        fun tintLens(x0: Int, x1: Int, y0: Int, y1: Int) {
            for (x in x0..x1) for (y in y0..y1) c[x, y] = blend(c[x, y], LENS_TINT, 0.22f)
        }
        val lenses = listOf(10 to 13, 18 to 21)
        when (style) {
            1, 2 -> { // Round, Square
                lenses.forEach { (x0, x1) ->
                    tintLens(x0 + 1, x1 - 1, 14, 15)
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
            3 -> { // Shades: dark gradient lenses with a diagonal glint
                lenses.forEach { (x0, x1) ->
                    c.row(13, x0, x1, LENS)
                    c.row(14, x0, x1, LENS)
                    c.row(15, x0, x1, LENS_LOW)
                    c[x0 + 1, 13] = GLINT
                    c[x0 + 2, 14] = blend(LENS, GLINT, 0.5f)
                }
                c.row(13, 14, 17, FRAME)
                c.row(13, 8, 9, FRAME)
                c.row(13, 22, 23, FRAME)
            }
            4 -> { // Aviators: gold frame, teardrop lenses
                lenses.forEach { (x0, x1) ->
                    c.row(13, x0, x1, GOLD_DARK)
                    c.row(14, x0, x1, LENS)
                    c.row(15, x0, x1, LENS_LOW)
                    c.row(16, x0 + 1, x1 - 1, LENS_LOW)
                    c[x0 + 1, 14] = GLINT
                }
                c.row(13, 14, 17, GOLD_DARK)
                c.row(13, 8, 9, GOLD_DARK)
                c.row(13, 22, 23, GOLD_DARK)
            }
            5 -> { // Thick frames
                lenses.forEach { (x0, x1) ->
                    tintLens(x0, x1, 14, 15)
                    c.row(12, x0 - 1, x1 + 1, FRAME)
                    c.row(13, x0 - 1, x1 + 1, FRAME)
                    c.row(16, x0 - 1, x1 + 1, FRAME)
                    for (y in 14..15) { c[x0 - 1, y] = FRAME; c[x1 + 1, y] = FRAME }
                    c[x0, 14] = GLINT
                }
                c.row(14, 15, 16, FRAME)
                c.row(13, 7, 8, FRAME)
                c.row(13, 23, 24, FRAME)
            }
        }
    }

    // --- Body ------------------------------------------------------------------

    /** Torso with outfit details. Portrait shoulders are broader than the full-body figure's. */
    private fun drawBody(c: Canvas, look: BroLook, p: Palette, bottom: Int, fullBody: Boolean) {
        val style = look.outfit
        val galabeya = style == GALABEYA
        val base = when (style) {
            GALABEYA -> blend(p.outfit, WHITE, 0.72f)
            JACKET -> LEATHER
            else -> p.outfit
        }
        val baseShade = blend(base, BLACK, 0.25f)
        for (y in 25..bottom) {
            val half = when {
                y == 25 -> 7
                y == 26 -> if (fullBody) 9 else 10
                fullBody && galabeya && y > 39 -> 9 // robe flares a little
                fullBody -> 8
                else -> 12
            }
            c.row(y, 16 - half, 15 + half, base)
            c[16 - half, y] = baseShade
            c[15 + half, y] = baseShade
            if (fullBody) c[17 - half, y] = blend(base, WHITE, 0.12f)
        }
        when (style) {
            HOODIE -> {
                c.row(24, 10, 12, p.outfitShade)
                c.row(24, 19, 21, p.outfitShade)
                c.row(25, 9, 22, p.outfitShade)
                c.row(bottom - 1, 11, 20, p.outfitShade)
                c.row(bottom, 11, 20, p.outfitShade)
                for (y in 26..28) { c[14, y] = WHITE; c[17, y] = WHITE }
            }
            JERSEY -> {
                c.row(25, 14, 17, p.skin)
                c.row(26, 15, 16, p.skin)
                c[13, 25] = WHITE; c[18, 25] = WHITE; c[14, 26] = WHITE; c[17, 26] = WHITE
                if (!fullBody) for (y in 27..31) {
                    c[6, y] = WHITE; c[7, y] = WHITE
                    c[24, y] = WHITE; c[25, y] = WHITE
                }
                c.row(28, 14, 17, WHITE)
                c[17, 29] = WHITE; c[16, 30] = WHITE; c[16, 31] = WHITE
            }
            SUIT -> {
                c.row(25, 13, 18, WHITE)
                c.row(26, 14, 17, WHITE)
                c.row(27, 14, 17, WHITE)
                for (y in 28..bottom) c.row(y, 15, 16, WHITE)
                val tie = if (look.outfitColor == 0) SAPPHIRE else RUBY
                c.row(26, 15, 16, tie)
                for (y in 27..minOf(bottom, 34)) c.row(y, 15, 16, tie)
                for (y in 26..29) { c[13, y] = p.outfitShade; c[18, y] = p.outfitShade }
            }
            GALABEYA -> {
                c.row(25, 15, 16, p.skin)
                for (y in 26..29) { c[15, y] = p.outfit; c[16, y] = p.outfit }
                c.row(26, 13, 14, p.outfit)
                c.row(26, 17, 18, p.outfit)
                c.row(bottom, 16 - (if (fullBody) 9 else 12), 15 + (if (fullBody) 9 else 12), p.outfit) // trim
            }
            JACKET -> {
                for (y in 25..bottom) c.row(y, 13, 18, p.outfit) // tee underneath
                c.row(25, 14, 17, p.skin)
                for (y in 26..30) { c[12, y] = blend(LEATHER, WHITE, 0.2f); c[19, y] = blend(LEATHER, WHITE, 0.2f) }
                for (y in 27..bottom) c[13, y] = 0xFFB8BCC8.toInt() // zipper
            }
            else -> { // Tee
                c.row(25, 14, 17, p.skin)
                c.row(26, 15, 16, p.skinShade)
                if (!fullBody) for (y in 27..31) { c[8, y] = p.outfitShade; c[23, y] = p.outfitShade }
            }
        }
    }

    private fun drawArms(c: Canvas, look: BroLook, p: Palette) {
        val style = look.outfit
        val sleeve = when (style) {
            GALABEYA -> blend(p.outfit, WHITE, 0.72f)
            JACKET -> LEATHER
            else -> p.outfit
        }
        val sleeveEnd = if (style == TEE || style == JERSEY) 30 else 36
        for ((x0, x1) in listOf(5 to 7, 24 to 26)) {
            for (y in 26..36) {
                val color = if (y <= sleeveEnd) sleeve else p.skin
                c.row(y, x0, x1, color)
                c[if (x0 < 16) x0 else x1, y] = if (y <= sleeveEnd) blend(sleeve, BLACK, 0.25f) else p.skinShade
            }
            if (style == JERSEY) c.row(29, x0, x1, WHITE)
            c.row(37, x0, x1, p.skin) // hands
            c.row(38, x0 + 1, x1 - 1, p.skinShade)
        }
    }

    private fun drawLegs(c: Canvas, look: BroLook, p: Palette, sitting: Boolean) {
        val pants = if (look.outfit == SUIT) blend(p.outfit, BLACK, 0.35f) else DENIM
        val pantsShade = blend(pants, BLACK, 0.3f)
        val pantsLight = blend(pants, WHITE, 0.18f)
        val legs = listOf(9 to 14, 17 to 22)
        if (sitting) {
            for (y in 40..41) c.row(y, 8, 23, pants)
            // Thighs come toward the viewer: lighter tops, then shins and shoes.
            for (y in 42..44) legs.forEach { (x0, x1) -> c.row(y, x0 - 1, x1 + 1, if (y == 42) pantsLight else pants) }
            for (y in 45..47) legs.forEach { (x0, x1) -> c.row(y, x0, x1, pants); c[x1, y] = pantsShade }
            for (y in 48..49) legs.forEach { (x0, x1) -> c.row(y, x0 - 1, x1, SNEAKER) }
            legs.forEach { (x0, x1) -> c.row(50, x0 - 1, x1, SOLE) }
        } else {
            for (y in 40..41) c.row(y, 9, 22, pants)
            for (y in 42..48) legs.forEach { (x0, x1) ->
                c.row(y, x0, x1, pants)
                c[x1, y] = pantsShade
                c[x0, y] = pantsLight
            }
            for (y in 49..50) legs.forEach { (x0, x1) -> c.row(y, x0 - 1, x1, SNEAKER) }
            legs.forEach { (x0, x1) -> c.row(51, x0 - 1, x1, SOLE) }
        }
    }

    // --- Bling -----------------------------------------------------------------

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

    // --- Canvas ----------------------------------------------------------------

    private class Canvas(val w: Int, val h: Int) {
        val px = IntArray(w * h)
        private val hairMask = BooleanArray(w * h)

        operator fun set(x: Int, y: Int, color: Int) {
            if (x in 0 until w && y in 0 until h) {
                px[y * w + x] = color
                hairMask[y * w + x] = false
            }
        }

        operator fun get(x: Int, y: Int): Int = if (x in 0 until w && y in 0 until h) px[y * w + x] else CLEAR

        fun row(y: Int, x0: Int, x1: Int, color: Int) {
            for (x in x0..x1) this[x, y] = color
        }

        /** Draws a hair pixel and remembers it for the lighting pass. */
        fun hair(x: Int, y: Int, color: Int) {
            if (x in 0 until w && y in 0 until h) {
                px[y * w + x] = color
                hairMask[y * w + x] = true
            }
        }

        private fun isHair(x: Int, y: Int) = x in 0 until w && y in 0 until h && hairMask[y * w + x]

        /**
         * Light comes from the top-left: plain hair pixels on a top/left edge get
         * a highlight, ones on a bottom/right edge get shaded. Styled pixels
         * (curls, stripes) are left alone.
         */
        fun lightHair(base: Int, light: Int, shade: Int) {
            val out = px.clone()
            for (y in 0 until h) for (x in 0 until w) {
                if (!isHair(x, y) || px[y * w + x] != base) continue
                when {
                    (!isHair(x, y - 1) || !isHair(x - 1, y)) && x < w / 2 + 2 -> out[y * w + x] = light
                    !isHair(x, y + 1) || (!isHair(x + 1, y) && x > w / 2) -> out[y * w + x] = shade
                }
            }
            out.copyInto(px)
        }

        /** Adds a 1px dark outline around everything that was drawn. */
        fun outlined(): IntArray {
            val out = px.clone()
            for (y in 0 until h) for (x in 0 until w) {
                if (this[x, y] != CLEAR) continue
                val touches = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) -> this[nx, ny] != CLEAR }
                if (touches) out[y * w + x] = OUTLINE
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
