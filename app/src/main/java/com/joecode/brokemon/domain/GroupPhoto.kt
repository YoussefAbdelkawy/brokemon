package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.Bro
import kotlin.math.cos
import kotlin.math.sin

enum class PhotoBackdrop(val label: String) { SKY("Sunny sky"), SUNSET("Sunset"), NIGHT("Night"), PITCH("Pitch"), BRICK("Brick wall") }

enum class PhotoPose(val label: String) { LINE("Line-up"), PYRAMID("Pyramid"), ARC("Arc") }

/**
 * Draws a 9:16 "class photo" of up to six bros as pixel art. The canvas is [W]x[H] pixels; the UI
 * scales it up with nearest-neighbor so it stays crisp when exported.
 */
object GroupPhoto {
    const val W = 120
    const val H = 214
    const val MAX_BROS = 6
    private const val FLOOR = 150

    fun render(bros: List<Bro>, backdrop: PhotoBackdrop, pose: PhotoPose): IntArray {
        val px = IntArray(W * H)
        background(px, backdrop)
        val placed = layout(bros.take(MAX_BROS).size, pose)
        // Back row first so front bros overlap them.
        placed.withIndex().sortedBy { it.value.second }.forEach { (i, pos) ->
            val bro = bros[i]
            val sprite = HumanSprite.renderFullBody(bro.resolvedLook, Evolution.info(bro).stage.ordinal, bro.isShiny, sitting = false)
            val (x, feetY) = pos
            shadow(px, x + HumanSprite.BODY_W / 2, feetY)
            blit(px, sprite, x, feetY - HumanSprite.BODY_H)
        }
        return px
    }

    /** (x of the sprite's left edge, y of its feet) for each bro, in canvas pixels. */
    fun layout(n: Int, pose: PhotoPose): List<Pair<Int, Int>> {
        if (n == 0) return emptyList()
        val w = HumanSprite.BODY_W
        fun row(count: Int, feetY: Int, shift: Int = 0): List<Pair<Int, Int>> =
            List(count) { k -> ((W - count * w) / (count + 1) * (k + 1) + k * w + shift) to feetY }
        return when (pose) {
            PhotoPose.LINE -> if (n <= 3) row(n, FLOOR + 30) else row(n / 2, FLOOR + 6) + row(n - n / 2, FLOOR + 50)
            PhotoPose.PYRAMID -> {
                val front = (n + 1) / 2
                val back = n - front
                row(back, FLOOR + 8, shift = 0) + row(front, FLOOR + 48)
            }
            PhotoPose.ARC -> List(n) { k ->
                val a = Math.PI * (1.0 - (k + 0.5) / n)
                val cx = W / 2 + (cos(a) * 36).toInt() - w / 2
                val fy = FLOOR + 50 - (sin(a) * 38).toInt()
                cx to fy
            }
        }
    }

    private fun background(px: IntArray, b: PhotoBackdrop) {
        fun fill(y0: Int, y1: Int, color: Int) { for (y in y0 until y1) for (x in 0 until W) px[y * W + x] = color }
        when (b) {
            PhotoBackdrop.SKY -> {
                fill(0, 60, 0xFF5AA6F0.toInt()); fill(60, 110, 0xFF8CC8FF.toInt()); fill(110, FLOOR + 8, 0xFFBFE3FF.toInt())
                fill(FLOOR + 8, H, 0xFF4CAF50.toInt())
                cloud(px, 16, 22); cloud(px, 70, 40)
            }
            PhotoBackdrop.SUNSET -> {
                fill(0, 50, 0xFF4B3290.toInt()); fill(50, 90, 0xFFB04C9A.toInt()); fill(90, 130, 0xFFFF7A5A.toInt()); fill(130, FLOOR + 8, 0xFFFFB84D.toInt())
                fill(FLOOR + 8, H, 0xFF3A2A5C.toInt())
                disc(px, 60, 128, 18, 0xFFFFE08A.toInt())
            }
            PhotoBackdrop.NIGHT -> {
                fill(0, FLOOR + 8, 0xFF0B1030.toInt()); fill(90, FLOOR + 8, 0xFF161C48.toInt()); fill(FLOOR + 8, H, 0xFF22264A.toInt())
                listOf(10 to 12, 30 to 40, 55 to 8, 80 to 30, 105 to 14, 20 to 70, 95 to 80, 70 to 60, 45 to 100).forEach { (x, y) -> px[y * W + x] = 0xFFF5F5F7.toInt() }
                disc(px, 95, 26, 8, 0xFFF1F3D8.toInt())
            }
            PhotoBackdrop.PITCH -> {
                for (y in 0 until H) for (x in 0 until W) px[y * W + x] = if ((y / 16) % 2 == 0) 0xFF2E8B45.toInt() else 0xFF3AA155.toInt()
                for (x in 0 until W) px[(FLOOR - 40) * W + x] = 0xFFF5F5F7.toInt()
                disc(px, 60, 70, 14, 0xFF2E8B45.toInt())
            }
            PhotoBackdrop.BRICK -> {
                for (y in 0 until FLOOR + 8) for (x in 0 until W) {
                    val row = y / 8
                    val off = if (row % 2 == 0) 0 else 8
                    px[y * W + x] = if (y % 8 == 0 || (x + off) % 16 == 0) 0xFF5A2A22.toInt() else 0xFF8A4A3A.toInt()
                }
                fill(FLOOR + 8, H, 0xFF3A3A46.toInt())
            }
        }
    }

    private fun cloud(px: IntArray, x: Int, y: Int) {
        for ((dx, dy, w) in listOf(Triple(0, 2, 22), Triple(4, 0, 12), Triple(2, 1, 16))) {
            for (i in 0 until w) px[(y + dy) * W + x + dx + i] = 0xFFFFFFFF.toInt()
        }
    }

    private fun disc(px: IntArray, cx: Int, cy: Int, r: Int, color: Int) {
        for (y in -r..r) for (x in -r..r) if (x * x + y * y <= r * r) {
            val ax = cx + x; val ay = cy + y
            if (ax in 0 until W && ay in 0 until H) px[ay * W + ax] = color
        }
    }

    private fun shadow(px: IntArray, cx: Int, y: Int) {
        for (x in -12..12) for (dy in 0..2) {
            val ax = cx + x; val ay = y - 1 + dy
            if (ax in 0 until W && ay in 0 until H && x * x / 144f + dy * dy / 4f <= 1f) {
                val o = px[ay * W + ax]
                px[ay * W + ax] = blend(o, 0xFF000000.toInt(), 0.35f)
            }
        }
    }

    private fun blit(px: IntArray, sprite: IntArray, x0: Int, y0: Int) {
        for (y in 0 until HumanSprite.BODY_H) for (x in 0 until HumanSprite.BODY_W) {
            val c = sprite[y * HumanSprite.BODY_W + x]
            if (c == HumanSprite.CLEAR) continue
            val ax = x0 + x; val ay = y0 + y
            if (ax in 0 until W && ay in 0 until H) px[ay * W + ax] = c
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
