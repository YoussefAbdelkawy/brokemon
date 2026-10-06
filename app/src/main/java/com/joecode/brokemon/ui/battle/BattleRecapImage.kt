package com.joecode.brokemon.ui.battle

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import android.content.Context
import com.joecode.brokemon.ui.theme.AppStyle
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.domain.battle.BattleState

/** A 1080x1920 (9:16) battle recap for stories: winner, MVP, both teams. */
object BattleRecapImage {
    private const val W = 1080
    private const val H = 1920

    fun render(context: Context, battle: BattleState, result: BattleResult): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val font = runCatching { ResourcesCompat.getFont(context, AppStyle.displayFontRes) }.getOrNull() ?: Typeface.MONOSPACE
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF1B1036.toInt(), 0xFF0E171A.toInt(), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        fun text(s: String, y: Float, size: Float, color: Int) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = size; this.color = color; textAlign = Paint.Align.CENTER }
            c.drawText(s, W / 2f, y, p)
        }
        text("BRO BATTLE", 170f, 64f, 0xFFFF5A6E.toInt())
        text(battle.sides[0].name.uppercase() + "  VS  " + battle.sides[1].name.uppercase(), 260f, 34f, 0xFFECECF1.toInt())
        text(if (result.youWon == null && battle.winner == -1) "DRAW" else "${result.winnerName.uppercase()} WINS", 420f, 58f, 0xFFFFD54A.toInt())

        // MVP portrait.
        val card = RectF(240f, 520f, W - 240f, 1140f)
        c.drawRoundRect(card, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF25252D.toInt() })
        c.drawRoundRect(card, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; color = 0xFFFFD54A.toInt() })
        drawSprite(c, BroLook.fromList(result.mvp.look), result.mvp.shiny, Rect(330, 560, W - 330, 560 + (W - 660)))
        text("MVP", 1040f, 40f, 0xFFFFD54A.toInt())
        text(result.mvp.name.uppercase(), 1100f, 44f, 0xFFECECF1.toInt())
        text("${result.mvpDamage} DAMAGE", 1220f, 30f, 0xFF8FF0DC.toInt())

        // Both teams.
        battle.sides.forEachIndexed { i, side ->
            val y = 1340 + i * 230
            text(side.name.uppercase(), y.toFloat(), 30f, 0xFF9A9AA8.toInt())
            val size = 150
            val total = side.fighters.size * (size + 30) - 30
            side.fighters.forEachIndexed { j, f ->
                val x = (W - total) / 2 + j * (size + 30)
                drawSprite(c, BroLook.fromList(f.snap.look), f.snap.shiny, Rect(x, y + 20, x + size, y + 20 + size), dim = f.fainted)
            }
        }
        text("${result.turns} TURNS · BROKEMON", 1860f, 28f, 0xFF9A9AA8.toInt())
        return bmp
    }

    private fun drawSprite(c: Canvas, look: BroLook, shiny: Boolean, dst: Rect, dim: Boolean = false) {
        val sprite = com.joecode.brokemon.ui.components.AvatarBitmaps.portrait(look, 0, shiny)
        c.drawBitmap(sprite, null, dst, Paint().apply { isFilterBitmap = com.joecode.brokemon.ui.components.AvatarBitmaps.smooth; if (dim) alpha = 90 })
    }
}
