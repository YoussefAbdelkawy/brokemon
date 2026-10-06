package com.joecode.brokemon.ui.room

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.joecode.brokemon.data.model.BroRoom
import com.joecode.brokemon.domain.RoomRenderer
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Cosmos version of a bro's room: the same decor options as the pixel
 * room, drawn as flat shapes with soft gradients and a big starry window.
 * Uses the room's 120x90 coordinate space so the bro stands in the same spot.
 */
object FlatRoom {
    private const val S = 4f
    private const val WALL_BOTTOM = 60f

    fun render(room: BroRoom): Bitmap {
        val bmp = Bitmap.createBitmap((RoomRenderer.W * S).toInt(), (RoomRenderer.H * S).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(S, S)
        wall(c, room.wallpaper)
        window(c)
        floor(c, room.floor)
        rug(c, room.rug)
        wallItem(c, room.wallLeft, 22f)
        wallItem(c, room.wallRight, 98f)
        floorItem(c, room.floorLeft, 4f)
        floorItem(c, room.floorRight, 92f)
        seat(c, room.seat)
        if (room.seat == 0) c.drawOval(RectF(46f, 83f, 74f, 88f), paint(0x40000000))
        return bmp
    }

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND }

    private fun wall(c: Canvas, style: Int) {
        val (top, bottom) = when (style) {
            1 -> 0xFF8A3A3A.toInt() to 0xFF5E2430.toInt()
            2 -> 0xFF1F7A7A.toInt() to 0xFF134E5E.toInt()
            3 -> 0xFFF2E3C6.toInt() to 0xFFD9C29C.toInt()
            4 -> 0xFF221A5C.toInt() to 0xFF120E33.toInt()
            5 -> 0xFF3E8A55.toInt() to 0xFF24563A.toInt()
            else -> 0xFF2B1D6B.toInt() to 0xFF1B1446.toInt()
        }
        c.drawRect(0f, 0f, 120f, WALL_BOTTOM, Paint().apply { shader = LinearGradient(0f, 0f, 0f, WALL_BOTTOM, top, bottom, Shader.TileMode.CLAMP) })
        if (style == 4) for (i in 0 until 26) {
            val x = (i * 37 % 120).toFloat(); val y = (i * 23 % 56).toFloat()
            c.drawCircle(x, y, if (i % 5 == 0) 0.9f else 0.5f, paint(0xCCFFFFFF.toInt()))
        }
        if (style == 1) for (y in 0 until 60 step 6) c.drawLine(0f, y.toFloat(), 120f, y.toFloat(), stroke(0x22000000, 0.6f))
    }

    private fun window(c: Canvas) {
        val r = RectF(44f, 8f, 76f, 36f)
        c.drawRoundRect(r, 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 8f, 0f, 36f, 0xFF0E0A27.toInt(), 0xFF3B2C8A.toInt(), Shader.TileMode.CLAMP) })
        c.drawCircle(68f, 16f, 5f, paint(0xFFFFD447.toInt()))
        c.drawOval(RectF(61f, 15f, 75f, 17.5f), stroke(0xFFFF6B4A.toInt(), 0.8f))
        listOf(49f to 13f, 55f to 22f, 51f to 30f, 72f to 30f).forEach { (x, y) -> c.drawCircle(x, y, 0.6f, paint(0xFFFFFFFF.toInt())) }
        c.drawRoundRect(r, 4f, 4f, stroke(0xFFEFE7FF.toInt(), 1.6f))
        c.drawLine(60f, 8f, 60f, 36f, stroke(0xFFEFE7FF.toInt(), 1f))
    }

    private fun floor(c: Canvas, style: Int) {
        val (a, b) = when (style) {
            1 -> 0xFFDCD7EE.toInt() to 0xFFB9B2D8.toInt()
            2 -> 0xFF5B3F8F.toInt() to 0xFF42296E.toInt()
            3 -> 0xFF7D7A90.toInt() to 0xFF5E5B70.toInt()
            else -> 0xFFB97A4A.toInt() to 0xFF8E5A35.toInt()
        }
        c.drawRect(0f, WALL_BOTTOM, 120f, 90f, Paint().apply { shader = LinearGradient(0f, WALL_BOTTOM, 0f, 90f, a, b, Shader.TileMode.CLAMP) })
        c.drawRect(0f, WALL_BOTTOM - 1.5f, 120f, WALL_BOTTOM + 1f, paint(0x55000000))
        if (style == 0) for (y in listOf(68f, 76f, 84f)) c.drawLine(0f, y, 120f, y, stroke(0x22000000, 0.5f))
        if (style == 1) for (x in 0..120 step 12) c.drawLine(x.toFloat(), WALL_BOTTOM, x.toFloat() - 6f, 90f, stroke(0x22000000, 0.4f))
    }

    private fun rug(c: Canvas, style: Int) {
        if (style == 0) return
        val color = when (style) { 1 -> 0xFFE0334F.toInt(); 2 -> 0xFF4CC3FF.toInt(); else -> 0xFFE09A1F.toInt() }
        c.drawOval(RectF(30f, 74f, 90f, 89f), paint(color))
        c.drawOval(RectF(36f, 77f, 84f, 86f), stroke(0x66FFFFFF, 0.8f))
    }

    private fun wallItem(c: Canvas, item: Int, cx: Float) {
        when (item) {
            1 -> { c.drawRoundRect(RectF(cx - 9f, 14f, cx + 9f, 38f), 2f, 2f, paint(0xFF6B2FA3.toInt())); c.drawCircle(cx, 24f, 5f, paint(0xFFFF6B4A.toInt())); c.drawRect(cx - 6f, 32f, cx + 6f, 34f, paint(0xFFFFD447.toInt())) }
            2 -> c.drawPath(Path().apply { moveTo(cx - 10f, 16f); lineTo(cx + 10f, 21f); lineTo(cx - 10f, 26f); close() }, paint(0xFFE0334F.toInt()))
            3 -> { c.drawRoundRect(RectF(cx - 11f, 20f, cx + 11f, 34f), 1.5f, 1.5f, paint(0xFF6B4A2E.toInt())); listOf(0xFFFF6B4A, 0xFF4CC3FF, 0xFFFFD447, 0xFF3EE6B0, 0xFFD08CFF).forEachIndexed { i, col -> c.drawRect(cx - 9f + i * 4f, 22f, cx - 6.5f + i * 4f, 32f, paint(col.toInt())) } }
            4 -> { c.drawRoundRect(RectF(cx - 12f, 14f, cx + 12f, 32f), 2f, 2f, paint(0xFF151226.toInt())); c.drawRoundRect(RectF(cx - 10f, 16f, cx + 10f, 30f), 1f, 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(cx - 10f, 16f, cx + 10f, 30f, 0xFF4CC3FF.toInt(), 0xFFD08CFF.toInt(), Shader.TileMode.CLAMP) }) }
            5 -> { c.drawRoundRect(RectF(cx - 11f, 18f, cx + 11f, 28f), 5f, 5f, stroke(0xFFFF4FA3.toInt(), 1.6f)); c.drawCircle(cx, 23f, 2f, paint(0xFF8FF5E4.toInt())) }
            6 -> { c.drawCircle(cx, 24f, 8f, paint(0xFFF7F3FF.toInt())); c.drawLine(cx, 24f, cx, 19f, stroke(0xFF1B1530.toInt(), 1f)); c.drawLine(cx, 24f, cx + 4f, 24f, stroke(0xFF1B1530.toInt(), 1f)) }
            7 -> for (i in 0..6) { val x = cx - 12f + i * 4f; c.drawCircle(x, 16f + if (i % 2 == 0) 1.5f else 3f, 1.4f, paint(listOf(0xFFFFD447, 0xFFFF4FA3, 0xFF4CC3FF)[i % 3].toInt())) }
            8 -> { c.drawLine(cx, 10f, cx, 16f, stroke(0xFFE09A1F.toInt(), 0.8f)); c.drawRoundRect(RectF(cx - 5f, 16f, cx + 5f, 30f), 4f, 4f, paint(0xFFE09A1F.toInt())); c.drawCircle(cx, 23f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, 23f, 6f, 0xFFFFF0A8.toInt(), 0x00FFF0A8, Shader.TileMode.CLAMP) }) }
        }
    }

    private fun floorItem(c: Canvas, item: Int, x0: Float) {
        val x = x0 + 12f
        when (item) {
            1 -> { c.drawRoundRect(RectF(x - 6f, 70f, x + 6f, 84f), 2f, 2f, paint(0xFFE0663D.toInt())); for (i in 0 until 5) { val a = Math.toRadians(-150.0 + i * 30.0); c.drawOval(RectF(x - 3f + cos(a).toFloat() * 8f, 56f + sin(a).toFloat() * 8f, x + 3f + cos(a).toFloat() * 8f, 66f + sin(a).toFloat() * 8f), paint(0xFF3EAE6B.toInt())) } }
            2 -> { c.drawLine(x, 48f, x, 84f, stroke(0xFF2C2A3A.toInt(), 1.4f)); c.drawPath(Path().apply { moveTo(x - 7f, 50f); lineTo(x + 7f, 50f); lineTo(x + 4f, 40f); lineTo(x - 4f, 40f); close() }, paint(0xFFFFD447.toInt())) }
            3 -> listOf(0xFFFF6B4A, 0xFF4CC3FF, 0xFFFFD447, 0xFFD08CFF, 0xFF3EE6B0).forEachIndexed { i, col -> c.drawRoundRect(RectF(x - 7f + (i % 2), 80f - i * 4f, x + 7f + (i % 2), 84f - i * 4f), 1f, 1f, paint(col.toInt())) }
            4 -> { c.drawRoundRect(RectF(x - 12f, 64f, x + 12f, 68f), 1.5f, 1.5f, paint(0xFF2C2A3A.toInt())); c.drawRect(x - 10f, 68f, x - 8f, 84f, paint(0xFF2C2A3A.toInt())); c.drawRect(x + 8f, 68f, x + 10f, 84f, paint(0xFF2C2A3A.toInt())); c.drawRoundRect(RectF(x - 8f, 52f, x + 8f, 63f), 1.5f, 1.5f, paint(0xFF151226.toInt())); c.drawRect(x - 7f, 53f, x + 7f, 62f, paint(0xFF4CC3FF.toInt())) }
            5 -> { c.drawRoundRect(RectF(x - 7f, 62f, x + 7f, 84f), 3f, 3f, paint(0xFFE9E4F7.toInt())); c.drawLine(x - 7f, 70f, x + 7f, 70f, stroke(0xFFB9B2D8.toInt(), 0.8f)); c.drawLine(x + 4f, 64f, x + 4f, 68f, stroke(0xFF6E6890.toInt(), 1f)) }
            6 -> { c.drawOval(RectF(x - 7f, 68f, x + 7f, 84f), paint(0xFFB5651D.toInt())); c.drawRoundRect(RectF(x - 1.2f, 46f, x + 1.2f, 70f), 1f, 1f, paint(0xFF3A2A22.toInt())); c.drawCircle(x, 75f, 2.5f, paint(0xFF3A2A22.toInt())) }
            7 -> for (dx in listOf(-6f, 6f)) { c.drawRoundRect(RectF(x + dx - 4f, 79f, x + dx + 4f, 84f), 1.5f, 1.5f, paint(0xFF2C2A3A.toInt())); c.drawRect(x + dx - 1f, 77f, x + dx + 1f, 86f, paint(0xFF6E6890.toInt())) }
            8 -> { c.drawRoundRect(RectF(x - 6f, 60f, x + 6f, 84f), 3f, 3f, paint(0xFF2C2A3A.toInt())); c.drawCircle(x, 67f, 3f, paint(0xFF4A475C.toInt())); c.drawCircle(x, 77f, 4f, paint(0xFF4A475C.toInt())) }
        }
    }

    private fun seat(c: Canvas, style: Int) {
        when (style) {
            1 -> { // couch
                c.drawRoundRect(RectF(36f, 58f, 84f, 74f), 6f, 6f, paint(0xFF6B2FA3.toInt()))
                c.drawRoundRect(RectF(38f, 68f, 82f, 82f), 5f, 5f, paint(0xFF8A4BC4.toInt()))
                c.drawRoundRect(RectF(32f, 64f, 40f, 84f), 4f, 4f, paint(0xFF5A2690.toInt())); c.drawRoundRect(RectF(80f, 64f, 88f, 84f), 4f, 4f, paint(0xFF5A2690.toInt()))
            }
            2 -> { // gaming chair
                c.drawRoundRect(RectF(46f, 40f, 74f, 74f), 8f, 8f, paint(0xFF2C2A3A.toInt()))
                c.drawRoundRect(RectF(52f, 40f, 56f, 74f), 2f, 2f, paint(0xFFFF4F6D.toInt())); c.drawRoundRect(RectF(64f, 40f, 68f, 74f), 2f, 2f, paint(0xFFFF4F6D.toInt()))
                c.drawRect(58f, 76f, 62f, 84f, paint(0xFF4A475C.toInt())); c.drawOval(RectF(50f, 83f, 70f, 87f), paint(0xFF4A475C.toInt()))
            }
            3 -> c.drawOval(RectF(42f, 66f, 78f, 88f), paint(0xFFFF6B4A.toInt())) // bean bag
            4 -> c.drawOval(RectF(44f, 79f, 76f, 88f), paint(0xFF3EE6B0.toInt())) // floor cushion
        }
    }
}
