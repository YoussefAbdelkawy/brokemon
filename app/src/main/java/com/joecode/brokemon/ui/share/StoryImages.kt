package com.joecode.brokemon.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.joecode.brokemon.R
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.EventIcon
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.PixelIcons
import com.joecode.brokemon.domain.SeasonEvents
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.domain.Wrapped
import com.joecode.brokemon.domain.WrappedSummary
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders Instagram/WhatsApp story-sized (1080x1920) images with a plain
 * Android Canvas, so the output is pixel-exact and independent of screen size.
 */
class StoryImages(private val context: Context) {
    private val pixelFont: Typeface =
        ResourcesCompat.getFont(context, R.font.press_start_2p) ?: Typeface.MONOSPACE

    private val bg = DexColors.Background.toArgb()
    private val surface = DexColors.Surface.toArgb()
    private val screen = DexColors.Screen.toArgb()
    private val text = DexColors.Text.toArgb()
    private val muted = DexColors.TextMuted.toArgb()
    private val red = DexColors.DexRed.toArgb()
    private val teal = DexColors.ScreenText.toArgb()
    private val gold = DexColors.Gold.toArgb()

    fun card(bro: Bro, stage: EvolutionStage): Bitmap {
        val bmp = createBitmap(W, H)
        val c = Canvas(bmp)
        val event = SeasonEvents.parse(bro.eventFrame)
        // Limited event cards wear the event's colors instead of the type colors.
        val type1 = event?.event?.primary?.toInt() ?: bro.primaryType.color.toArgb()
        val type2 = event?.event?.accent?.toInt() ?: (bro.types.getOrNull(1) ?: bro.primaryType).color.toArgb()

        background(c, type1)
        header(c, "BROKEMON", bro.dexNumber)

        // The card itself.
        val card = RectF(90f, 250f, W - 90f, 1560f)
        when (bro.rarity) {
            Rarity.LEGENDARY -> glow(c, card, gold, 90f)
            Rarity.RARE -> glow(c, card, DexColors.RareBlue.toArgb(), 50f)
            Rarity.EPIC -> glow(c, card, DexColors.Epic.toArgb(), 70f)
            Rarity.COMMON -> Unit
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface }
        c.drawRoundRect(card, 40f, 40f, cardPaint)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            shader = LinearGradient(card.left, card.top, card.right, card.bottom, type1, type2, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(card, 40f, 40f, border)

        val inner = 140f
        // Dex number + rarity stars.
        drawText(c, bro.dexNumber, inner, 345f, 38f, muted)
        stars(c, bro.rarity, card.right - 50f, 330f)

        // Sprite window.
        val window = RectF(inner, 390f, W - inner, 390f + (W - 2 * inner))
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                window.centerX(), window.centerY(), window.width() * 0.7f,
                intArrayOf(withAlpha(type1, 110), screen), null, Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(window, 18f, 18f, windowPaint)
        scanlines(c, window)
        sprite(c, bro, stage.ordinal, window, inset = 40f)
        if (bro.isShiny) sparkles(c, window, bro.id.toInt(), 12)
        event?.let { stamp ->
            val e = stamp.event
            pixelIcon(c, e.icon, window.left + 20f, window.top + 20f, 9f, e.primary.toInt(), e.accent.toInt())
            val w = PixelIcons.rows(e.icon)[0].length * 9f
            pixelIcon(c, e.icon, window.right - 20f - w, window.top + 20f, 9f, e.primary.toInt(), e.accent.toInt())
            val ribbon = "LIMITED · ${stamp.label}"
            val rw = measure(ribbon, 26f) + 40f
            val r = RectF(window.centerX() - rw / 2, window.bottom - 70f, window.centerX() + rw / 2, window.bottom - 20f)
            c.drawRoundRect(r, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(0xFF0B0B10.toInt(), 220) })
            c.drawRoundRect(r, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = e.accent.toInt() })
            drawText(c, ribbon, r.left + 20f, r.bottom - 15f, 26f, e.accent.toInt())
        }

        // Name, types, stage.
        var y = window.bottom + 110f
        val name = bro.name.uppercase()
        val nameSize = autoSize(name, W - 2 * inner, max = 62f, min = 40f)
        drawText(c, fit(name, nameSize, W - 2 * inner), inner, y, nameSize, text)
        y += 50f
        var x = inner
        bro.types.forEach { type ->
            val w = badge(c, type.label.uppercase(), x, y, type.color.toArgb())
            x += w + 20f
        }
        y += 120f
        drawText(c, stage.title.uppercase(), inner, y, 34f, teal)
        if (bro.isShiny) drawText(c, "SHINY", card.right - 50f - measure("SHINY", 34f), y, 34f, gold)

        // Stats strip under the card: two rows of three.
        val stats = bro.stats.asList()
        stats.forEachIndexed { i, (info, value) ->
            val col = i % 3
            val row = i / 3
            val sx = 110f + col * 300f
            val sy = 1650f + row * 90f
            drawText(c, info.label, sx, sy, 28f, teal)
            drawText(c, value.toString(), sx + 190f, sy, 28f, text)
        }
        footer(c)
        return bmp
    }

    fun wrapped(summary: WrappedSummary, stages: Map<Long, EvolutionStage>): Bitmap {
        val bmp = createBitmap(W, H)
        val c = Canvas(bmp)
        background(c, red)
        header(c, "BRODEX WRAPPED", summary.year.toString())

        drawCentered(c, "MY ${summary.year}", 300f, 52f, text)
        drawCentered(c, "IN BROS", 380f, 52f, text)
        drawCentered(c, Wrapped.title(summary).uppercase(), 480f, 40f, gold)

        drawCentered(c, "%03d".format(summary.caughtCount), 700f, 150f, teal)
        drawCentered(c, "BROS CAUGHT", 790f, 34f, muted)

        var y = 920f
        drawText(c, "TOP TYPES", 110f, y, 34f, text)
        y += 30f
        val max = summary.topTypes.maxOfOrNull { it.second } ?: 1
        summary.topTypes.forEach { (type, n) ->
            y += 70f
            drawText(c, type.label.uppercase(), 110f, y, 28f, type.color.toArgb())
            val barLeft = 560f
            val barRight = W - 160f
            val fill = barLeft + (barRight - barLeft) * n / max
            val p = Paint().apply { color = type.color.toArgb() }
            c.drawRect(barLeft, y - 28f, fill, y, p)
            drawText(c, "x$n", barRight + 20f, y, 28f, text)
        }
        if (summary.topTypes.isEmpty()) {
            y += 70f
            drawText(c, "NO CATCHES YET", 110f, y, 28f, muted)
        }

        // First catch and memory MVP portraits.
        val slots = listOfNotNull(
            summary.firstBro?.let { "FIRST CATCH" to it },
            summary.mostMemories?.let { "MEMORY MVP" to it.first },
        )
        slots.forEachIndexed { i, (label, bro) ->
            val left = if (slots.size == 1) (W - 420f) / 2 else 110f + i * 460f
            val box = RectF(left, 1320f, left + 420f, 1700f)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = screen }
            c.drawRoundRect(box, 24f, 24f, p)
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 8f; color = bro.primaryType.color.toArgb()
            }
            c.drawRoundRect(box, 24f, 24f, stroke)
            sprite(c, bro, (stages[bro.id] ?: EvolutionStage.ROOKIE).ordinal, box, inset = 30f)
            drawText(c, label, box.left, box.top - 24f, 26f, muted)
            drawText(c, fit(bro.name.uppercase(), 30f, box.width()), box.left, box.bottom + 56f, 30f, text)
        }
        footer(c)
        return bmp
    }

    // --- Pieces ------------------------------------------------------------------

    private fun background(c: Canvas, accent: Int) {
        c.drawColor(bg)
        val glow = Paint().apply {
            shader = RadialGradient(W / 2f, 520f, 900f, intArrayOf(withAlpha(accent, 90), 0x00000000), null, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glow)
    }

    private fun header(c: Canvas, title: String, right: String) {
        val bar = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, 170f, red, DexColors.DexRedDark.toArgb(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), 170f, bar)
        c.drawRect(0f, 170f, W.toFloat(), 182f, Paint().apply { color = 0x73000000 })
        // Lens and LEDs.
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFFFFFFFF.toInt(); c.drawCircle(95f, 85f, 46f, p)
        p.color = 0xFF0B0B10.toInt(); c.drawCircle(95f, 85f, 39f, p)
        p.color = DexColors.LedBlue.toArgb(); c.drawCircle(95f, 85f, 32f, p)
        p.color = 0xCCFFFFFF.toInt(); c.drawCircle(84f, 74f, 8f, p)
        listOf(DexColors.LedRed, DexColors.LedYellow, DexColors.LedGreen).forEachIndexed { i, led ->
            p.color = led.toArgb(); c.drawCircle(175f + i * 34f, 85f, 11f, p)
        }
        drawText(c, title, 290f, 102f, 40f, text)
        drawText(c, right, W - 60f - measure(right, 30f), 100f, 30f, 0xCCFFFFFF.toInt())
    }

    private fun footer(c: Canvas) {
        drawCentered(c, "CAUGHT WITH BROKEMON", H - 70f, 26f, muted)
    }

    private fun glow(c: Canvas, rect: RectF, color: Int, radius: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = withAlpha(color, 170)
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.OUTER)
        }
        c.drawRoundRect(rect, 40f, 40f, p)
    }

    private fun sprite(c: Canvas, bro: Bro, stage: Int, box: RectF, inset: Float) {
        val sprite = com.joecode.brokemon.ui.components.AvatarBitmaps.portrait(bro.resolvedLook, stage, bro.isShiny)
        val s = sprite.width
        val side = minOf(box.width(), box.height()) - inset * 2
        val dst = RectF(box.centerX() - side / 2, box.centerY() - side / 2, box.centerX() + side / 2, box.centerY() + side / 2)
        // Nearest-neighbor: keep the pixels crisp.
        c.drawBitmap(sprite, Rect(0, 0, s, s), dst, Paint().apply { isFilterBitmap = false; isAntiAlias = false })
    }

    private fun pixelIcon(c: Canvas, icon: EventIcon, left: Float, top: Float, px: Float, primary: Int, accent: Int) {
        val p = Paint()
        PixelIcons.rows(icon).forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                p.color = when (ch) {
                    'P' -> primary
                    'A' -> accent
                    'W' -> 0xFFFFFFFF.toInt()
                    'D' -> 0xFF0B0B10.toInt()
                    else -> return@forEachIndexed
                }
                c.drawRect(left + x * px, top + y * px, left + (x + 1) * px, top + (y + 1) * px, p)
            }
        }
    }

    private fun scanlines(c: Canvas, rect: RectF) {
        val p = Paint().apply { color = 0x14000000 }
        var y = rect.top
        while (y < rect.bottom) {
            c.drawRect(rect.left, y, rect.right, y + 3f, p)
            y += 9f
        }
    }

    private fun sparkles(c: Canvas, rect: RectF, seed: Int, count: Int) {
        val r = Random(seed)
        val p = Paint()
        repeat(count) {
            val cx = rect.left + r.nextFloat() * rect.width()
            val cy = rect.top + r.nextFloat() * rect.height()
            val arm = 2 + r.nextInt(3)
            val px = 8f
            p.color = 0xFFFFFFFF.toInt()
            c.drawRect(cx, cy, cx + px, cy + px, p)
            p.color = gold
            for (i in 1..arm) {
                c.drawRect(cx + i * px, cy, cx + (i + 1) * px, cy + px, p)
                c.drawRect(cx - i * px, cy, cx - (i - 1) * px, cy + px, p)
                c.drawRect(cx, cy + i * px, cx + px, cy + (i + 1) * px, p)
                c.drawRect(cx, cy - i * px, cx + px, cy - (i - 1) * px, p)
            }
        }
    }

    private fun stars(c: Canvas, rarity: Rarity, right: Float, centerY: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rarity.color.toArgb() }
        repeat(rarity.ordinal + 1) { i ->
            val cx = right - 26f - i * 60f
            val path = Path()
            for (k in 0 until 10) {
                val radius = if (k % 2 == 0) 26f else 11f
                val angle = Math.toRadians(-90.0 + k * 36.0)
                val px = cx + (cos(angle) * radius).toFloat()
                val py = centerY + (sin(angle) * radius).toFloat()
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            c.drawPath(path, p)
        }
    }

    private fun badge(c: Canvas, label: String, x: Float, top: Float, color: Int): Float {
        val size = 30f
        val w = measure(label, size) + 44f
        val rect = RectF(x, top, x + w, top + 66f)
        c.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        drawText(c, label, x + 22f, top + 48f, size, 0xFF101014.toInt())
        return w
    }

    private fun textPaint(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = pixelFont
        textSize = size
        this.color = color
    }

    private fun measure(s: String, size: Float) = textPaint(size, 0).measureText(s)

    private fun drawText(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int) =
        c.drawText(s, x, y, textPaint(size, color))

    private fun drawCentered(c: Canvas, s: String, y: Float, size: Float, color: Int) =
        drawText(c, s, (W - measure(s, size)) / 2, y, size, color)

    /** Largest size between [min] and [max] at which [s] fits in [maxWidth]. */
    private fun autoSize(s: String, maxWidth: Float, max: Float, min: Float): Float {
        var size = max
        while (size > min && measure(s, size) > maxWidth) size -= 2f
        return size
    }

    /** Trims with "..." so long names never run off the card. */
    private fun fit(s: String, size: Float, maxWidth: Float): String {
        if (measure(s, size) <= maxWidth) return s
        var t = s
        while (t.isNotEmpty() && measure("$t...", size) > maxWidth) t = t.dropLast(1)
        return "$t..."
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

    companion object {
        const val W = 1080
        const val H = 1920
    }
}
