package com.joecode.brokemon.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.LookOptions
import kotlin.math.cos
import kotlin.math.sin

/**
 * The flat, vector character style used by the Cosmos app: round shapes, two-tone
 * shading, big friendly eyes, soft glows. Every BroLook option (hair, hats,
 * glasses, fits, accessories, backdrops) has a flat version here, so a bro made
 * in one style looks like the same person in the other.
 *
 * Drawn on a 100x100 grid (portrait) or 100x160 (full body), then scaled.
 */
object FlatAvatar {
    private const val INK = 0xFF1B1530.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GOLD = 0xFFFFC94A.toInt()
    private const val GOLD_DARK = 0xFFE09A1F.toInt()
    private const val RUBY = 0xFFFF4F6D.toInt()
    private const val BLUE = 0xFF4CC3FF.toInt()
    private const val NEON = 0xFF7CFF8A.toInt()
    private const val DENIM = 0xFF3B4A7A.toInt()

    fun portrait(look: BroLook, stage: Int, shiny: Boolean, size: Int = 256, tint: Int? = null): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(size / 100f, size / 100f)
        val p = Colors(look, shiny)
        if (look.background != 0 && tint == null) background(c, look.background)
        backBehind(c, look.back, p, full = false)
        bust(c, look, p)
        backFront(c, look.back, p, full = false)
        head(c, look, p, stage)
        if (stage >= 1) chain(c, 50f, 80f)
        handItem(c, look.hand, p, 80f, 90f)
        if (shiny && tint == null) shine(c)
        return tinted(bmp, tint)
    }

    fun fullBody(look: BroLook, stage: Int, shiny: Boolean, sitting: Boolean, width: Int = 200, tint: Int? = null): Bitmap {
        val height = width * 160 / 100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(width / 100f, height / 160f)
        val p = Colors(look, shiny)
        // Soft floor shadow.
        c.drawOval(RectF(24f, 150f, 76f, 160f), paint(0x33000000))
        backBehind(c, look.back, p, full = true)
        legs(c, look, p, sitting)
        arms(c, look, p)
        torso(c, look, p)
        backFront(c, look.back, p, full = true)
        c.save()
        c.translate(50f, 30f)
        c.scale(0.74f, 0.74f)
        c.translate(-50f, -42f)
        head(c, look, p, stage)
        c.restore()
        if (stage >= 1) chain(c, 50f, 66f)
        handItem(c, look.hand, p, 84f, 104f)
        if (shiny && tint == null) shine(c)
        return tinted(bmp, tint)
    }

    private fun tinted(bmp: Bitmap, tint: Int?): Bitmap {
        if (tint == null) return bmp
        val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(bmp, 0f, 0f, Paint().apply { colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN) })
        return out
    }

    private class Colors(look: BroLook, shiny: Boolean) {
        val skin = LookOptions.skinTones[look.skin].toInt()
        val skinShade = mix(skin, 0xFF3A1F2E.toInt(), 0.22f)
        val hair = LookOptions.hairColors[look.hairColor].toInt()
        val hairLight = mix(hair, WHITE, 0.25f)
        val hairShade = mix(hair, INK, 0.3f)
        val outfit = if (shiny) GOLD else LookOptions.outfitColors[look.outfitColor].toInt()
        val outfitShade = mix(outfit, INK, 0.25f)
        val outfitLight = mix(outfit, WHITE, 0.2f)
    }

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    // --- Body --------------------------------------------------------------------------

    private fun bust(c: Canvas, look: BroLook, p: Colors) {
        val base = when (look.outfit) { 4 -> mix(p.outfit, WHITE, 0.7f); 5 -> 0xFF2C2A3A.toInt(); else -> p.outfit }
        val shoulders = Path().apply {
            moveTo(10f, 100f)
            cubicTo(12f, 80f, 24f, 72f, 40f, 71f)
            lineTo(60f, 71f)
            cubicTo(76f, 72f, 88f, 80f, 90f, 100f)
            close()
        }
        c.drawPath(shoulders, paint(base))
        // Two-tone: the right side in shade.
        c.save(); c.clipPath(shoulders); c.drawRect(56f, 70f, 100f, 100f, paint(mix(base, INK, 0.18f))); c.restore()
        // Neck.
        c.drawRoundRect(RectF(42f, 58f, 58f, 76f), 6f, 6f, paint(p.skinShade))
        outfitDetails(c, look, p, base, shoulders, top = 71f, bottom = 100f, full = false)
    }

    private fun torso(c: Canvas, look: BroLook, p: Colors) {
        val base = when (look.outfit) { 4 -> mix(p.outfit, WHITE, 0.7f); 5 -> 0xFF2C2A3A.toInt(); else -> p.outfit }
        val bottom = if (look.outfit == 4) 140f else 108f
        val body = Path().apply { addRoundRect(RectF(28f, 56f, 72f, bottom), floatArrayOf(14f, 14f, 14f, 14f, 6f, 6f, 6f, 6f), Path.Direction.CW) }
        c.drawRoundRect(RectF(44f, 50f, 56f, 60f), 4f, 4f, paint(p.skinShade))
        c.drawPath(body, paint(base))
        c.save(); c.clipPath(body); c.drawRect(52f, 56f, 80f, bottom, paint(mix(base, INK, 0.18f))); c.restore()
        outfitDetails(c, look, p, base, body, top = 56f, bottom = bottom, full = true)
    }

    private fun outfitDetails(c: Canvas, look: BroLook, p: Colors, base: Int, shape: Path, top: Float, bottom: Float, full: Boolean) {
        when (look.outfit) {
            1 -> { // Hoodie: hood rim + strings
                c.drawArc(RectF(34f, top - 6f, 66f, top + 8f), 0f, 180f, false, stroke(p.outfitShade, 4f))
                c.drawLine(45f, top + 4f, 45f, top + 16f, stroke(WHITE, 1.6f)); c.drawLine(55f, top + 4f, 55f, top + 16f, stroke(WHITE, 1.6f))
            }
            2 -> { // Jersey: V-neck + number
                c.drawPath(vNeck(top), paint(p.skin))
                c.drawPath(Path().apply { moveTo(46f, top + 10f); lineTo(54f, top + 10f); lineTo(49f, top + 24f) }, stroke(WHITE, 3f))
            }
            3 -> { // Suit: shirt, lapels, tie
                c.drawPath(vNeck(top, depth = 24f), paint(WHITE))
                c.drawPath(Path().apply { moveTo(48f, top + 2f); lineTo(52f, top + 2f); lineTo(53.5f, top + 22f); lineTo(50f, top + 26f); lineTo(46.5f, top + 22f); close() }, paint(if (look.outfitColor == 0) BLUE else RUBY))
            }
            4 -> { // Galabeya: placket
                c.drawLine(50f, top + 2f, 50f, top + 16f, stroke(p.outfit, 3f))
            }
            5 -> { // Leather jacket over a tee
                c.drawRect(44f, top, 56f, bottom, paint(p.outfit))
                c.drawLine(44f, top + 4f, 44f, bottom, stroke(0xFF4A475C.toInt(), 1.5f))
            }
            6, 7 -> { // Made-up football kit: stripes + blank crest
                c.save(); c.clipPath(shape)
                var x = 0f
                while (x < 100f) { c.drawRect(x, top, x + 5f, bottom, paint(WHITE)); x += 10f }
                c.restore()
                c.drawPath(vNeck(top, depth = 8f), paint(p.skin))
                c.drawPath(Path().apply { moveTo(58f, top + 8f); lineTo(64f, top + 8f); lineTo(64f, top + 14f); lineTo(61f, top + 17f); lineTo(58f, top + 14f); close() }, paint(GOLD))
                if (look.outfit == 7) {
                    val y = if (full) top + 10f else top + 8f
                    c.drawRect(if (full) 20f else 12f, y, if (full) 28f else 22f, y + 5f, paint(0xFFFFD447.toInt()))
                }
            }
            else -> c.drawPath(vNeck(top, depth = 6f, round = true), paint(p.skin)) // Tee
        }
    }

    private fun vNeck(top: Float, depth: Float = 14f, round: Boolean = false) = Path().apply {
        moveTo(42f, top)
        if (round) quadTo(50f, top + depth * 1.6f, 58f, top) else { lineTo(50f, top + depth); lineTo(58f, top) }
        close()
    }

    private fun arms(c: Canvas, look: BroLook, p: Colors) {
        val sleeve = when (look.outfit) { 4 -> mix(p.outfit, WHITE, 0.7f); 5 -> 0xFF2C2A3A.toInt(); else -> p.outfit }
        val short = look.outfit in listOf(0, 2, 6, 7)
        for (x in listOf(18f, 72f)) {
            c.drawRoundRect(RectF(x, 58f, x + 10f, 104f), 6f, 6f, paint(p.skin))
            c.drawRoundRect(RectF(x, 58f, x + 10f, if (short) 74f else 98f), 6f, 6f, paint(if (x > 50) mix(sleeve, INK, 0.18f) else sleeve))
            c.drawCircle(x + 5f, 105f, 5.5f, paint(p.skin))
        }
    }

    private fun legs(c: Canvas, look: BroLook, p: Colors, sitting: Boolean) {
        val pants = if (look.outfit == 3) mix(p.outfit, INK, 0.4f) else DENIM
        if (sitting) {
            // Seated, facing us: shins hang down, knees and lap come toward the viewer.
            for (x in listOf(32f, 52f)) {
                c.drawRoundRect(RectF(x + 1f, 118f, x + 15f, 140f), 6f, 6f, paint(mix(pants, INK, 0.2f)))
                c.drawRoundRect(RectF(x - 1f, 136f, x + 17f, 145f), 5f, 5f, paint(WHITE))
            }
            c.drawRoundRect(RectF(28f, 100f, 72f, 124f), 12f, 12f, paint(pants))
            c.drawRoundRect(RectF(31f, 104f, 49f, 122f), 9f, 9f, paint(mix(pants, WHITE, 0.15f)))
            c.drawRoundRect(RectF(51f, 104f, 69f, 122f), 9f, 9f, paint(mix(pants, WHITE, 0.08f)))
        } else {
            for (x in listOf(32f, 52f)) {
                c.drawRoundRect(RectF(x, 100f, x + 16f, 146f), 6f, 6f, paint(if (x > 50) mix(pants, INK, 0.15f) else pants))
                c.drawRoundRect(RectF(x - 2f, 142f, x + 18f, 153f), 5f, 5f, paint(WHITE))
                c.drawRect(x - 2f, 150f, x + 18f, 153f, paint(0xFFB9B4D0.toInt()))
            }
        }
    }

    // --- Head --------------------------------------------------------------------------

    private val headOval = RectF(28f, 17f, 72f, 66f)

    private fun head(c: Canvas, look: BroLook, p: Colors, stage: Int) {
        val hijab = look.hat == 6
        if (hijab) hijabBack(c, p) else backHair(c, look.hair, p)
        // Ears.
        if (!hijab) {
            c.drawCircle(28.5f, 45f, 5f, paint(p.skin)); c.drawCircle(71.5f, 45f, 5f, paint(p.skinShade))
        }
        // Face with a soft shade on the right side.
        val face = Path().apply { addOval(headOval, Path.Direction.CW) }
        c.drawPath(face, paint(p.skin))
        c.save(); c.clipPath(face); c.drawOval(RectF(52f, 12f, 92f, 72f), paint(p.skinShade)); c.restore()
        // Blush, always a little.
        c.drawCircle(36f, 52f, 4f, paint(withAlpha(0xFFFF7A9C.toInt(), if (look.expression == 1 || look.extra == 3) 110 else 45)))
        c.drawCircle(64f, 52f, 4f, paint(withAlpha(0xFFFF7A9C.toInt(), if (look.expression == 1 || look.extra == 3) 110 else 45)))
        extras(c, look.extra, p, hijab)
        eyes(c, look, p)
        c.drawRoundRect(RectF(48f, 47f, 53f, 52f), 3f, 3f, paint(p.skinShade)) // nose
        facialHair(c, look.facialHair, p)
        mouth(c, look.expression)
        if (!hijab) frontHair(c, look.hair, p, hatCovers = look.hat in listOf(1, 2, 4, 5, 7))
        hat(c, look.hat, p)
        glasses(c, look.glasses)
        if (stage >= 2) crown(c)
    }

    private fun eyes(c: Canvas, look: BroLook, p: Colors) {
        val brow = if (look.hair == 6) mix(p.skin, INK, 0.45f) else p.hairShade
        val browY = if (look.expression == 4) 33f else 35f
        c.drawLine(36f, browY, 44f, browY - 1f, stroke(brow, 2.6f))
        c.drawLine(56f, browY - 1f, 64f, browY, stroke(brow, 2.6f))
        for (x in listOf(41f, 59f)) when (look.expression) {
            2 -> { c.drawArc(RectF(x - 4f, 39f, x + 4f, 46f), 0f, 180f, true, paint(INK)); c.drawLine(x - 4.5f, 42.5f, x + 4.5f, 42.5f, stroke(p.skinShade, 1.6f)) }
            5 -> c.drawArc(RectF(x - 4f, 40f, x + 4f, 45f), 0f, 180f, false, stroke(INK, 2f))
            4 -> { c.drawCircle(x, 42f, 5f, paint(WHITE)); c.drawCircle(x, 42f, 3f, paint(INK)) }
            else -> {
                c.drawOval(RectF(x - 3.4f, 38f, x + 3.4f, 46.5f), paint(INK))
                c.drawCircle(x + 1.1f, 40.4f, 1.4f, paint(WHITE))
            }
        }
    }

    private fun mouth(c: Canvas, expression: Int) {
        val mouth = 0xFF7A2A3E.toInt()
        when (expression) {
            1 -> { // Grin
                val m = Path().apply { moveTo(41f, 55f); quadTo(50f, 66f, 59f, 55f); close() }
                c.drawPath(m, paint(mouth))
                c.save(); c.clipPath(m); c.drawRect(40f, 54f, 60f, 58f, paint(WHITE)); c.restore()
            }
            2 -> c.drawLine(45f, 58f, 55f, 58f, stroke(mouth, 2.2f))
            3 -> c.drawPath(Path().apply { moveTo(44f, 58f); quadTo(52f, 60f, 57f, 54.5f) }, stroke(mouth, 2.2f))
            4 -> c.drawOval(RectF(47f, 55f, 53f, 62f), paint(mouth))
            5 -> c.drawLine(47f, 58f, 53f, 58f, stroke(mouth, 2f))
            else -> c.drawPath(Path().apply { moveTo(43f, 56f); quadTo(50f, 62f, 57f, 56f) }, stroke(mouth, 2.4f))
        }
    }

    private fun extras(c: Canvas, extra: Int, p: Colors, hijab: Boolean) {
        when (extra) {
            1 -> if (!hijab) c.drawCircle(28.5f, 52f, 1.8f, paint(GOLD))
            2 -> listOf(37f to 49f, 40f to 51f, 34f to 51f, 60f to 49f, 63f to 51f, 66f to 49f).forEach { (x, y) ->
                c.drawCircle(x, y, 0.9f, paint(mix(p.skin, 0xFF7A4A2A.toInt(), 0.6f)))
            }
            4 -> c.drawCircle(53f, 52f, 1.2f, paint(GOLD))
            5 -> c.drawLine(60f, 31f, 63f, 39f, stroke(mix(p.skin, WHITE, 0.45f), 1.6f))
            6 -> c.drawCircle(60f, 55f, 1.1f, paint(mix(p.skin, INK, 0.6f)))
        }
    }

    private fun facialHair(c: Canvas, style: Int, p: Colors) {
        when (style) {
            1 -> { // Stubble: soft shadow on the jaw
                val jaw = Path().apply { addOval(headOval, Path.Direction.CW) }
                c.save(); c.clipPath(jaw); c.drawRect(28f, 52f, 72f, 70f, paint(withAlpha(p.hair, 70))); c.restore()
            }
            2 -> c.drawPath(Path().apply { moveTo(42f, 55f); quadTo(50f, 50f, 58f, 55f); quadTo(50f, 53.5f, 42f, 55f) }, paint(p.hair))
            3 -> { // Full beard
                val beard = Path().apply {
                    moveTo(29f, 44f); quadTo(30f, 70f, 50f, 72f); quadTo(70f, 70f, 71f, 44f)
                    lineTo(66f, 46f); quadTo(64f, 56f, 58f, 56f); quadTo(50f, 54f, 42f, 56f); quadTo(36f, 56f, 34f, 46f); close()
                }
                c.drawPath(beard, paint(p.hair))
                c.drawPath(Path().apply { moveTo(42f, 55f); quadTo(50f, 51f, 58f, 55f); quadTo(50f, 54f, 42f, 55f) }, paint(p.hairShade))
            }
            4 -> {
                c.drawPath(Path().apply { moveTo(43f, 55f); quadTo(50f, 51f, 57f, 55f); quadTo(50f, 53.5f, 43f, 55f) }, paint(p.hair))
                c.drawRoundRect(RectF(46f, 61f, 54f, 68f), 4f, 4f, paint(p.hair))
            }
        }
    }

    private fun backHair(c: Canvas, style: Int, p: Colors) {
        when (style) {
            4 -> c.drawRoundRect(RectF(25f, 26f, 75f, 84f), 18f, 18f, paint(p.hair)) // long
            9 -> c.drawRoundRect(RectF(25f, 34f, 75f, 74f), 12f, 12f, paint(p.hair)) // mullet
            11 -> c.drawCircle(50f, 34f, 30f, paint(p.hair)) // afro
            12 -> for (x in listOf(27f, 31f, 69f, 73f)) { // braids
                c.drawRoundRect(RectF(x - 2.5f, 34f, x + 2.5f, 84f), 3f, 3f, paint(p.hair))
                c.drawCircle(x, 85f, 2.5f, paint(GOLD))
            }
            13 -> for (x in listOf(25f, 29f, 33f, 67f, 71f, 75f)) c.drawRoundRect(RectF(x - 2.2f, 26f, x + 2.2f, 80f), 3f, 3f, paint(p.hair)) // locs
            14 -> { c.drawOval(RectF(64f, 18f, 84f, 58f), paint(p.hair)); c.drawCircle(70f, 22f, 3f, paint(RUBY)) } // ponytail
        }
    }

    private fun frontHair(c: Canvas, style: Int, p: Colors, hatCovers: Boolean) {
        val hair = paint(p.hair)
        fun cap(top: Float = 13f, bottom: Float = 38f) {
            val cap = Path().apply {
                addArc(RectF(26f, top, 74f, bottom + 22f), 180f, 180f)
                lineTo(74f, bottom); quadTo(70f, bottom - 10f, 50f, bottom - 12f); quadTo(30f, bottom - 10f, 26f, bottom); close()
            }
            c.drawPath(cap, hair)
            c.drawArc(RectF(32f, top + 3f, 56f, bottom), 200f, 50f, false, stroke(p.hairLight, 2.5f))
        }
        if (hatCovers && style != 6) {
            // Only the sides peek out under a hat.
            c.drawRoundRect(RectF(26f, 34f, 32f, 46f), 3f, 3f, hair)
            c.drawRoundRect(RectF(68f, 34f, 74f, 46f), 3f, 3f, hair)
            return
        }
        when (style) {
            0 -> c.drawArc(RectF(28f, 16f, 72f, 50f), 185f, 170f, true, paint(withAlpha(p.hair, 150))) // buzz
            1, 9, 10 -> {
                cap()
                if (style == 10) c.drawArc(RectF(27f, 20f, 73f, 58f), 160f, 220f, false, stroke(p.hairShade, 2f))
            }
            2, 16 -> { // spiky / anime
                cap(14f, 36f)
                val tall = if (style == 16) 1.5f else 1f
                val spikes = listOf(30f to -1f, 40f to 0f, 50f to 0f, 60f to 0f, 70f to 1f)
                spikes.forEach { (x, lean) ->
                    c.drawPath(Path().apply {
                        moveTo(x - 7f, 26f); lineTo(x + lean * 6f, 26f - 16f * tall); lineTo(x + 7f, 26f); close()
                    }, hair)
                }
            }
            3 -> { // curly
                cap(12f, 34f)
                for (i in 0 until 9) {
                    val a = Math.toRadians(180.0 + i * 22.5)
                    c.drawCircle(50f + cos(a).toFloat() * 21f, 32f + sin(a).toFloat() * 17f, 6f, hair)
                }
            }
            4 -> cap(12f, 40f)
            5 -> { // mohawk
                c.drawArc(RectF(28f, 16f, 72f, 50f), 185f, 170f, true, paint(withAlpha(p.hair, 90)))
                c.drawPath(Path().apply { moveTo(44f, 26f); quadTo(50f, -2f, 56f, 26f); close() }, hair)
            }
            6 -> c.drawArc(RectF(36f, 22f, 52f, 34f), 200f, 80f, false, stroke(withAlpha(WHITE, 140), 2.5f)) // bald shine
            7 -> { cap(); c.drawCircle(50f, 11f, 9f, hair) } // bun
            8 -> { // side part swoop
                cap()
                c.drawPath(Path().apply { moveTo(40f, 20f); quadTo(66f, 18f, 72f, 38f); quadTo(58f, 26f, 40f, 30f); close() }, paint(p.hairLight))
            }
            11 -> { cap(10f, 32f) }
            12, 13, 14 -> cap()
            15 -> { // waves
                cap()
                for (y in listOf(22f, 27f, 32f)) c.drawPath(Path().apply {
                    moveTo(33f, y); quadTo(38f, y - 3f, 43f, y); quadTo(48f, y + 3f, 53f, y); quadTo(58f, y - 3f, 63f, y); quadTo(66f, y + 2f, 68f, y)
                }, stroke(p.hairShade, 1.5f))
            }
        }
    }

    private fun hijabBack(c: Canvas, p: Colors) {
        c.drawRoundRect(RectF(20f, 10f, 80f, 96f), 30f, 30f, paint(p.outfit))
        c.drawRoundRect(RectF(52f, 10f, 80f, 96f), 30f, 30f, paint(p.outfitShade))
    }

    private fun hat(c: Canvas, hat: Int, p: Colors) {
        val color = p.outfit
        val shade = p.outfitShade
        when (hat) {
            1, 5, 7 -> { // cap, backwards cap, trainer cap
                c.drawArc(RectF(26f, 12f, 74f, 52f), 180f, 180f, true, paint(color))
                if (hat == 7) {
                    c.drawArc(RectF(40f, 12f, 60f, 52f), 180f, 180f, true, paint(WHITE))
                    star(c, 50f, 23f, 4.5f, GOLD)
                }
                if (hat == 5) c.drawRoundRect(RectF(40f, 6f, 60f, 12f), 4f, 4f, paint(shade))
                else c.drawRoundRect(RectF(30f, 29f, 82f, 35f), 4f, 4f, paint(if (hat == 7) INK else shade))
            }
            2 -> { // beanie
                c.drawArc(RectF(26f, 10f, 74f, 56f), 180f, 180f, true, paint(color))
                c.drawRoundRect(RectF(25f, 27f, 75f, 36f), 4f, 4f, paint(p.outfitLight))
                c.drawCircle(50f, 9f, 5f, paint(WHITE))
            }
            3 -> { c.drawRect(28f, 30f, 72f, 35f, paint(RUBY)); c.drawRect(28f, 33f, 72f, 35f, paint(WHITE)) } // headband
            4 -> { // bucket
                c.drawRoundRect(RectF(32f, 10f, 68f, 34f), 10f, 10f, paint(color))
                c.drawOval(RectF(20f, 28f, 80f, 38f), paint(shade))
            }
            6 -> { // hijab front drape around the face
                val ring = Path().apply {
                    addRoundRect(RectF(22f, 12f, 78f, 70f), 28f, 28f, Path.Direction.CW)
                    addOval(RectF(31f, 22f, 69f, 66f), Path.Direction.CCW)
                    fillType = Path.FillType.EVEN_ODD
                }
                c.drawPath(ring, paint(color))
            }
            8, 9 -> { // headset / headphones
                val cup = if (hat == 8) 0xFF2C2A3A.toInt() else color
                c.drawArc(RectF(24f, 10f, 76f, 70f), 200f, 140f, false, stroke(if (hat == 8) 0xFF4A475C.toInt() else shade, 4f))
                c.drawRoundRect(RectF(20f, 36f, 30f, 54f), 5f, 5f, paint(cup))
                c.drawRoundRect(RectF(70f, 36f, 80f, 54f), 5f, 5f, paint(cup))
                if (hat == 8) {
                    c.drawPath(Path().apply { moveTo(26f, 52f); quadTo(30f, 62f, 42f, 60f) }, stroke(0xFF4A475C.toInt(), 2f))
                    c.drawCircle(43f, 60f, 2.2f, paint(RUBY))
                    c.drawCircle(75f, 45f, 1.6f, paint(NEON))
                }
            }
            10 -> { // ninja-style band with a blank plate
                c.drawRect(27f, 28f, 73f, 36f, paint(0xFF2B3A6B.toInt()))
                c.drawRoundRect(RectF(40f, 26.5f, 60f, 37.5f), 2f, 2f, paint(0xFFC9CEDB.toInt()))
                c.drawPath(Path().apply { moveTo(27f, 31f); lineTo(16f, 38f); lineTo(20f, 42f); close() }, paint(0xFF2B3A6B.toInt()))
            }
        }
    }

    private fun glasses(c: Canvas, style: Int) {
        val frame = INK
        when (style) {
            1 -> { c.drawCircle(41f, 42f, 6f, stroke(frame, 1.8f)); c.drawCircle(59f, 42f, 6f, stroke(frame, 1.8f)); c.drawLine(47f, 42f, 53f, 42f, stroke(frame, 1.6f)) }
            2 -> { c.drawRoundRect(RectF(34f, 37f, 47f, 47f), 2f, 2f, stroke(frame, 1.8f)); c.drawRoundRect(RectF(53f, 37f, 66f, 47f), 2f, 2f, stroke(frame, 1.8f)); c.drawLine(47f, 41f, 53f, 41f, stroke(frame, 1.6f)) }
            3 -> {
                c.drawRoundRect(RectF(33f, 37f, 48f, 47f), 4f, 4f, paint(0xFF151226.toInt())); c.drawRoundRect(RectF(52f, 37f, 67f, 47f), 4f, 4f, paint(0xFF151226.toInt()))
                c.drawLine(48f, 40f, 52f, 40f, stroke(frame, 2f)); c.drawLine(36f, 39.5f, 40f, 39.5f, stroke(withAlpha(WHITE, 160), 1.2f))
            }
            4 -> {
                c.drawOval(RectF(33f, 37f, 48f, 49f), paint(0x8833304A.toInt())); c.drawOval(RectF(52f, 37f, 67f, 49f), paint(0x8833304A.toInt()))
                c.drawOval(RectF(33f, 37f, 48f, 49f), stroke(GOLD_DARK, 1.4f)); c.drawOval(RectF(52f, 37f, 67f, 49f), stroke(GOLD_DARK, 1.4f))
                c.drawLine(46f, 38f, 54f, 38f, stroke(GOLD_DARK, 1.4f))
            }
            5 -> { c.drawRoundRect(RectF(33f, 36f, 48f, 48f), 3f, 3f, stroke(frame, 3.2f)); c.drawRoundRect(RectF(52f, 36f, 67f, 48f), 3f, 3f, stroke(frame, 3.2f)); c.drawLine(48f, 41f, 52f, 41f, stroke(frame, 2.6f)) }
            6 -> { // generic domino mask
                val mask = Path().apply {
                    addRoundRect(RectF(30f, 35f, 70f, 49f), 7f, 7f, Path.Direction.CW)
                    addOval(RectF(37f, 38.5f, 45f, 45.5f), Path.Direction.CCW)
                    addOval(RectF(55f, 38.5f, 63f, 45.5f), Path.Direction.CCW)
                    fillType = Path.FillType.EVEN_ODD
                }
                c.drawPath(mask, paint(0xFF221B3A.toInt()))
            }
        }
    }

    private fun crown(c: Canvas) {
        val crown = Path().apply {
            moveTo(36f, 16f); lineTo(36f, 4f); lineTo(43f, 10f); lineTo(50f, 1f); lineTo(57f, 10f); lineTo(64f, 4f); lineTo(64f, 16f); close()
        }
        c.drawPath(crown, paint(GOLD))
        c.drawRect(36f, 13f, 64f, 16f, paint(GOLD_DARK))
        c.drawCircle(50f, 9f, 1.8f, paint(RUBY))
    }

    private fun chain(c: Canvas, cx: Float, y: Float) {
        c.drawArc(RectF(cx - 10f, y - 10f, cx + 10f, y + 4f), 20f, 140f, false, stroke(GOLD, 2.2f))
        c.drawCircle(cx, y + 5f, 2.4f, paint(GOLD))
    }

    // --- Accessories ---------------------------------------------------------------------

    private fun backBehind(c: Canvas, back: Int, p: Colors, full: Boolean) {
        when (back) {
            1, 2 -> {
                val cape = if (back == 1) 0xFFE0334F.toInt() else 0xFF6A3FB5.toInt()
                val path = if (full) Path().apply { moveTo(30f, 56f); lineTo(70f, 56f); lineTo(82f, 140f); lineTo(18f, 140f); close() }
                else Path().apply { moveTo(26f, 72f); lineTo(74f, 72f); lineTo(98f, 100f); lineTo(2f, 100f); close() }
                c.drawPath(path, paint(cape))
                if (back == 2) c.drawPath(path, stroke(GOLD, 2f))
            }
            3 -> if (full) c.drawRoundRect(RectF(68f, 60f, 84f, 96f), 6f, 6f, paint(0xFF2E8C76.toInt()))
            else { c.drawRoundRect(RectF(4f, 80f, 16f, 100f), 5f, 5f, paint(0xFF2E8C76.toInt())); c.drawRoundRect(RectF(84f, 80f, 96f, 100f), 5f, 5f, paint(0xFF2E8C76.toInt())) }
        }
    }

    private fun backFront(c: Canvas, back: Int, p: Colors, full: Boolean) {
        when (back) {
            1, 2 -> { val y = if (full) 58f else 74f; c.drawCircle(38f, y, 2.5f, paint(GOLD)); c.drawCircle(62f, y, 2.5f, paint(GOLD)) }
            3 -> { val top = if (full) 56f else 72f; val bottom = if (full) 98f else 100f
                c.drawLine(38f, top, 36f, bottom, stroke(0xFF1F5C4E.toInt(), 3f)); c.drawLine(62f, top, 64f, bottom, stroke(0xFF1F5C4E.toInt(), 3f)) }
            4 -> { // supporter scarf in made-up colors
                val y = if (full) 52f else 66f
                c.drawRoundRect(RectF(36f, y, 64f, y + 8f), 4f, 4f, paint(0xFFE0334F.toInt()))
                c.drawRoundRect(RectF(38f, y + 4f, 46f, y + 28f), 3f, 3f, paint(0xFFE0334F.toInt()))
                for (i in 0..2) c.drawRect(38f, y + 8f + i * 7f, 46f, y + 11f + i * 7f, paint(WHITE))
            }
        }
    }

    private fun handItem(c: Canvas, item: Int, p: Colors, hx: Float, hy: Float) {
        if (item == 0) return
        fun hand() = c.drawCircle(hx, hy, 5.5f, paint(p.skin))
        when (item) {
            1, 2 -> { // manga / comic
                c.drawRoundRect(RectF(hx - 9f, hy - 22f, hx + 7f, hy - 2f), 2f, 2f, paint(if (item == 1) WHITE else 0xFFFFD447.toInt()))
                if (item == 1) { c.drawRect(hx - 9f, hy - 22f, hx + 7f, hy - 17f, paint(INK)); c.drawLine(hx - 6f, hy - 12f, hx + 4f, hy - 12f, stroke(INK, 1.4f)) }
                else star(c, hx - 1f, hy - 12f, 6f, RUBY)
                hand()
            }
            3 -> { c.drawCircle(hx, hy - 12f, 9f, paint(WHITE)); c.drawCircle(hx, hy - 12f, 3f, paint(INK)); c.drawCircle(hx - 5f, hy - 17f, 1.8f, paint(INK)); hand() }
            4 -> {
                c.drawRoundRect(RectF(hx - 13f, hy - 10f, hx + 9f, hy + 2f), 6f, 6f, paint(0xFF2C2A3A.toInt()))
                c.drawCircle(hx + 4f, hy - 6f, 1.6f, paint(RUBY)); c.drawCircle(hx + 1f, hy - 3f, 1.6f, paint(BLUE)); c.drawCircle(hx - 8f, hy - 4f, 2f, paint(WHITE))
            }
            5 -> {
                c.drawPath(Path().apply { moveTo(hx - 7f, hy - 22f); lineTo(hx + 7f, hy - 22f); lineTo(hx + 5f, hy - 2f); lineTo(hx - 5f, hy - 2f); close() }, paint(WHITE))
                c.drawRect(hx - 6.5f, hy - 15f, hx + 6.5f, hy - 9f, paint(0xFFB5651D.toInt()))
                c.drawRoundRect(RectF(hx - 8f, hy - 25f, hx + 8f, hy - 21f), 2f, 2f, paint(0xFF3A2A22.toInt()))
                hand()
            }
            6 -> { // shawarma wrap
                c.drawRoundRect(RectF(hx - 6f, hy - 24f, hx + 6f, hy - 4f), 6f, 6f, paint(0xFFE3B072.toInt()))
                c.drawCircle(hx - 2f, hy - 24f, 3f, paint(0xFF5CCB5F.toInt())); c.drawCircle(hx + 2.5f, hy - 23f, 2.5f, paint(RUBY))
                c.drawRect(hx - 6.5f, hy - 10f, hx + 6.5f, hy - 2f, paint(WHITE))
                hand()
            }
            7 -> { // gym bag
                c.drawArc(RectF(hx - 6f, hy - 4f, hx + 6f, hy + 8f), 180f, 180f, false, stroke(INK, 2f))
                c.drawRoundRect(RectF(hx - 12f, hy + 2f, hx + 12f, hy + 14f), 6f, 6f, paint(p.outfitShade))
                c.drawLine(hx - 10f, hy + 7f, hx + 10f, hy + 7f, stroke(WHITE, 1.5f))
                hand()
            }
            8 -> for (x in listOf(hx, 100f - hx)) { // keeper gloves on both hands
                c.drawRoundRect(RectF(x - 7f, hy - 9f, x + 7f, hy + 6f), 5f, 5f, paint(NEON))
                c.drawRect(x - 7f, hy + 2f, x + 7f, hy + 6f, paint(0xFF2C2A3A.toInt()))
            }
        }
    }

    private fun background(c: Canvas, bg: Int) {
        when (bg) {
            1 -> { // POW! burst
                c.drawColor(0xFF3A1E7A.toInt())
                val burst = Path()
                for (i in 0 until 24) {
                    val r = if (i % 2 == 0) 48f else 34f
                    val a = Math.toRadians(i * 15.0)
                    val x = 50f + cos(a).toFloat() * r
                    val y = 44f + sin(a).toFloat() * r
                    if (i == 0) burst.moveTo(x, y) else burst.lineTo(x, y)
                }
                burst.close()
                c.drawPath(burst, paint(0xFFFFD447.toInt()))
                c.drawPath(burst, stroke(0xFFFF4F6D.toInt(), 2.5f))
            }
            2 -> { // gaming chair
                c.drawColor(0xFF1E1A33.toInt())
                c.drawRoundRect(RectF(18f, 6f, 82f, 100f), 18f, 18f, paint(0xFF2E2A45.toInt()))
                c.drawRoundRect(RectF(30f, 6f, 38f, 100f), 4f, 4f, paint(0xFFFF4F6D.toInt()))
                c.drawRoundRect(RectF(62f, 6f, 70f, 100f), 4f, 4f, paint(0xFFFF4F6D.toInt()))
            }
            3 -> { // pitch
                for (i in 0 until 6) c.drawRect(0f, i * 17f, 100f, i * 17f + 17f, paint(if (i % 2 == 0) 0xFF2FA35A.toInt() else 0xFF38B567.toInt()))
                c.drawLine(0f, 70f, 100f, 70f, stroke(WHITE, 1.5f))
                c.drawCircle(50f, 70f, 16f, stroke(WHITE, 1.5f))
            }
            4 -> { // speed lines
                c.drawColor(0xFFF4F1FF.toInt())
                for (i in 0 until 28) {
                    val a = Math.toRadians(i * 360.0 / 28)
                    c.drawLine(50f + cos(a).toFloat() * 28f, 44f + sin(a).toFloat() * 28f, 50f + cos(a).toFloat() * 80f, 44f + sin(a).toFloat() * 80f, stroke(0xFFB7B0D8.toInt(), 1.6f))
                }
            }
            5 -> { // trophy glow
                c.drawColor(0xFFE09A1F.toInt())
                c.drawCircle(50f, 44f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(50f, 44f, 70f, 0xFFFFF0A8.toInt(), 0xFFE09A1F.toInt(), Shader.TileMode.CLAMP)
                })
            }
        }
    }

    private fun shine(c: Canvas) {
        star(c, 18f, 20f, 4f, GOLD)
        star(c, 84f, 30f, 3f, WHITE)
        star(c, 80f, 74f, 3.5f, GOLD)
    }

    private fun star(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val s = Path().apply {
            moveTo(cx, cy - r); quadTo(cx, cy, cx + r, cy); quadTo(cx, cy, cx, cy + r); quadTo(cx, cy, cx - r, cy); quadTo(cx, cy, cx, cy - r); close()
        }
        c.drawPath(s, paint(color))
    }

    private fun withAlpha(color: Int, alpha: Int) = (alpha shl 24) or (color and 0xFFFFFF)

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt().coerceIn(0, 255)
        return Color.argb(255, m(16), m(8), m(0))
    }
}
