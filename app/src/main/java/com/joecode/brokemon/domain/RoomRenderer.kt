package com.joecode.brokemon.domain

import com.joecode.brokemon.data.model.BroRoom
import kotlin.math.roundToInt
import kotlin.math.sin

/** Where the bro's 32x52 full-body sprite goes in the room, in room pixels. */
data class BroPlacement(val x: Int, val y: Int, val sitting: Boolean)

/**
 * Draws a bro's room as a 120x90 pixel scene (side view, like a little diorama).
 * Everything is original pixel art drawn with code. The bro is drawn on top by
 * the UI (so it can animate); [composite] bakes it in for thumbnails and images.
 */
object RoomRenderer {
    const val W = 120
    const val H = 90
    private const val WALL_BOTTOM = 59
    private const val FLOOR_TOP = 62

    private const val CLEAR = 0
    private const val OUTLINE = 0xFF0B0B10.toInt()
    private const val WHITE = 0xFFF5F5F7.toInt()
    private const val BLACK = 0xFF000000.toInt()
    private const val WOOD = 0xFF6B4A2E.toInt()
    private const val GOLD = 0xFFFFD54A.toInt()
    private const val PINK = 0xFFFF4FA3.toInt()
    private const val CYAN = 0xFF5CE1E6.toInt()
    private const val RED = 0xFFD7263D.toInt()
    private const val BLUE = 0xFF3A86FF.toInt()
    private const val YELLOW = 0xFFFFC23D.toInt()
    private const val GREEN = 0xFF4CAF50.toInt()
    private const val PURPLE = 0xFF8B5CF6.toInt()

    fun placement(room: BroRoom): BroPlacement = when (room.seat) {
        1 -> BroPlacement(44, 29, sitting = true) // couch
        2 -> BroPlacement(44, 31, sitting = true) // gaming chair
        3 -> BroPlacement(44, 35, sitting = true) // bean bag
        4 -> BroPlacement(44, 38, sitting = true) // floor cushion
        else -> BroPlacement(44, 35, sitting = false)
    }

    fun render(room: BroRoom): IntArray {
        val c = Px()
        wallpaper(c, room.wallpaper)
        window(c)
        floor(c, room.floor)
        rug(c, room.rug)
        wallItem(c, room.wallLeft, cx = 22)
        wallItem(c, room.wallRight, cx = 98)
        floorItem(c, room.floorLeft, x0 = 4)
        floorItem(c, room.floorRight, x0 = 92)
        seat(c, room.seat)
        if (room.seat == 0) {
            // Soft shadow where the bro stands.
            for (x in 46..73) for (y in 84..87) {
                val dx = (x - 59.5f) / 14f
                val dy = (y - 85.5f) / 2.2f
                if (dx * dx + dy * dy <= 1f) c[x, y] = blend(c[x, y], BLACK, 0.35f)
            }
        }
        return c.px
    }

    /** Room with the bro baked in (for decorate thumbnails and share images). */
    fun composite(room: BroRoom, bro: IntArray): IntArray {
        val out = render(room)
        val at = placement(room)
        for (y in 0 until HumanSprite.BODY_H) for (x in 0 until HumanSprite.BODY_W) {
            val color = bro[y * HumanSprite.BODY_W + x]
            val rx = at.x + x
            val ry = at.y + y
            if (color != CLEAR && rx in 0 until W && ry in 0 until H) out[ry * W + rx] = color
        }
        return out
    }

    // --- Walls & floor ---------------------------------------------------------

    private fun wallpaper(c: Px, style: Int) {
        for (y in 0..WALL_BOTTOM) for (x in 0 until W) {
            c[x, y] = when (style) {
                1 -> { // Brick
                    val row = y / 6
                    val mortar = y % 6 == 5 || (x + if (row % 2 == 0) 0 else 6) % 12 == 0
                    if (mortar) 0xFF5A2A20.toInt() else if ((x / 12 + row) % 3 == 0) 0xFF84412F.toInt() else 0xFF7A3B2E.toInt()
                }
                2 -> if (x % 10 == 3 && y % 10 == 3) 0xFF2A6573.toInt() else 0xFF1F4E5A.toInt() // Teal dots
                3 -> if (x % 10 < 2) 0xFFDCCFB4.toInt() else 0xFFE9DFC9.toInt() // Cream stripes
                4 -> { // Stars
                    val h = (x * 73 + y * 151) % 97
                    when {
                        h == 0 -> 0xFFFFE9A8.toInt()
                        h == 1 || h == 2 -> 0xFF8890C0.toInt()
                        else -> 0xFF141833.toInt()
                    }
                }
                5 -> if (x % 8 < 4) 0xFF336644.toInt() else 0xFF2D5A3D.toInt() // Green stripes
                else -> if (x % 8 == 0) 0xFF262B45.toInt() else 0xFF1E2238.toInt() // Midnight
            }
        }
        // Crown molding and baseboard.
        c.rect(0, 0, W - 1, 1, blend(c[5, 5], BLACK, 0.35f))
        c.rect(0, WALL_BOTTOM + 1, W - 1, WALL_BOTTOM + 2, 0xFF2A1F1A.toInt())
        // Light falls off toward the floor.
        for (y in 44..WALL_BOTTOM) for (x in 0 until W) c[x, y] = blend(c[x, y], BLACK, (y - 44) / 60f)
    }

    private fun window(c: Px) {
        c.rect(46, 32, 73, 33, WOOD) // sill
        c.rect(48, 8, 71, 31, WOOD)
        for (y in 10..29) for (x in 50..69) {
            c[x, y] = blend(0xFF1A2550.toInt(), 0xFF2B3A70.toInt(), (y - 10) / 19f)
        }
        for ((x, y) in listOf(52 to 12, 56 to 16, 53 to 25, 67 to 24, 61 to 27)) c[x, y] = 0xFFFFF3C4.toInt()
        for (y in 11..17) for (x in 62..68) {
            val dx = x - 65
            val dy = y - 14
            if (dx * dx + dy * dy <= 9) c[x, y] = 0xFFF3E9C0.toInt()
        }
        c.rect(59, 10, 60, 29, WOOD)
        c.rect(50, 19, 69, 20, WOOD)
    }

    private fun floor(c: Px, style: Int) {
        for (y in FLOOR_TOP until H) for (x in 0 until W) {
            val fy = y - FLOOR_TOP
            c[x, y] = when (style) {
                1 -> { // Tiles
                    if (x % 8 == 0 || fy % 8 == 0) 0xFF9898A6.toInt()
                    else if ((x / 8 + fy / 8) % 2 == 0) 0xFFD8D8E0.toInt() else 0xFFB4B4C2.toInt()
                }
                2 -> if ((x * 7 + y * 13) % 11 == 0) 0xFF8A3644.toInt() else 0xFF7A2E3A.toInt() // Carpet
                3 -> when ((x * 31 + y * 17) % 23) { // Concrete
                    0 -> 0xFF6A6A76.toInt()
                    1 -> 0xFF4E4E5A.toInt()
                    else -> 0xFF5A5A66.toInt()
                }
                else -> { // Wood planks
                    val plank = fy / 5
                    when {
                        fy % 5 == 4 -> 0xFF5E3B1C.toInt()
                        (x + plank * 7) % 24 == 0 -> 0xFF5E3B1C.toInt()
                        ((x + plank * 7) / 24 + plank) % 2 == 0 -> 0xFF8B5A2B.toInt()
                        else -> 0xFF946234.toInt()
                    }
                }
            }
        }
        // Shadow along the wall.
        for (x in 0 until W) {
            c[x, FLOOR_TOP] = blend(c[x, FLOOR_TOP], BLACK, 0.4f)
            c[x, FLOOR_TOP + 1] = blend(c[x, FLOOR_TOP + 1], BLACK, 0.2f)
        }
    }

    private fun rug(c: Px, style: Int) {
        if (style == 0) return
        for (y in 76..87) for (x in 24..95) {
            val dx = (x - 59.5f) / 36f
            val dy = (y - 81.5f) / 6f
            if (dx * dx + dy * dy > 1f) continue
            val edge = dx * dx + dy * dy > 0.72f
            c[x, y] = when (style) {
                2 -> if (edge) WHITE else if ((x + y) % 6 == 0) 0xFF3A6CB8.toInt() else 0xFF2F5FA8.toInt()
                3 -> { // Kilim: diamonds
                    val d = (kotlin.math.abs(x - 59) + kotlin.math.abs(y - 81) * 3) % 10
                    if (edge) 0xFFE9DFC9.toInt() else when {
                        d < 2 -> 0xFFE9DFC9.toInt()
                        d < 5 -> 0xFFE07A2F.toInt()
                        else -> 0xFFA83232.toInt()
                    }
                }
                else -> if (edge) 0xFFE8C170.toInt() else if ((x * 3 + y) % 7 == 0) 0xFFC24444.toInt() else 0xFFB33A3A.toInt()
            }
        }
    }

    // --- Wall decor ------------------------------------------------------------

    private fun wallItem(c: Px, item: Int, cx: Int) {
        when (item) {
            1 -> { // Game poster
                c.box(cx - 10, 10, cx + 9, 39, OUTLINE, BLUE)
                c.rect(cx - 9, 34, cx + 8, 38, RED)
                c.rect(cx - 6, 36, cx + 5, 36, WHITE)
                val invader = listOf("..X....X..", "...X..X...", "..XXXXXX..", ".XX.XX.XX.", "XXXXXXXXXX", "X.XXXXXX.X", "X.X....X.X", "...XX.XX..")
                invader.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> if (ch == 'X') c[cx - 5 + x, 16 + y * 2] = YELLOW; if (ch == 'X') c[cx - 5 + x, 17 + y * 2] = YELLOW } }
                c[cx - 10, 10] = 0xFFE8E0C8.toInt(); c[cx + 9, 10] = 0xFFE8E0C8.toInt() // tape
            }
            2 -> { // Pennant
                c.rect(cx - 12, 14, cx - 11, 32, WOOD)
                for (x in cx - 10..cx + 12) {
                    val half = ((cx + 12 - x) / 3f).roundToInt().coerceAtLeast(0)
                    for (y in 22 - half..22 + half) c[x, y] = if (y == 22) WHITE else RED
                }
                c[cx - 4, 22] = GOLD; c[cx - 3, 22] = GOLD
            }
            3 -> { // Manga shelf: two shelves of colorful spines
                val colors = listOf(RED, BLUE, YELLOW, GREEN, PURPLE, WHITE, PINK, 0xFFE07A2F.toInt())
                for ((shelfY, seed) in listOf(24 to 0, 38 to 3)) {
                    c.rect(cx - 14, shelfY, cx + 13, shelfY + 1, WOOD)
                    var x = cx - 13
                    var i = seed
                    while (x < cx + 12) {
                        val h = 8 + (i * 5) % 4
                        val color = colors[i % colors.size]
                        c.rect(x, shelfY - h, x + 1, shelfY - 1, color)
                        c[x, shelfY - h + 2] = blend(color, WHITE, 0.5f)
                        x += 3
                        i++
                    }
                }
            }
            4 -> { // Wall TV showing a tiny platformer
                c.box(cx - 15, 13, cx + 14, 34, 0xFF111118.toInt(), 0xFF2B3A70.toInt())
                c.rect(cx - 13, 28, cx + 12, 32, GREEN)
                c.rect(cx - 13, 30, cx + 12, 32, 0xFF7A4E25.toInt())
                c.rect(cx - 3, 23, cx - 1, 27, RED)
                c.rect(cx + 5, 20, cx + 9, 21, YELLOW)
                c[cx + 11, 16] = WHITE
                c.rect(cx - 2, 35, cx + 1, 36, 0xFF111118.toInt())
            }
            5 -> { // Neon "BRO" sign with glow
                val font = mapOf(
                    'B' to listOf("XX.", "X.X", "XX.", "X.X", "XX."),
                    'R' to listOf("XX.", "X.X", "XX.", "X.X", "X.X"),
                    'O' to listOf(".X.", "X.X", "X.X", "X.X", ".X."),
                )
                val lit = mutableListOf<Pair<Int, Int>>()
                "BRO".forEachIndexed { i, ch ->
                    font.getValue(ch).forEachIndexed { y, row ->
                        row.forEachIndexed { x, v -> if (v == 'X') for (dy in 0..1) for (dx in 0..1) lit += (cx - 13 + i * 9 + x * 2 + dx) to (18 + y * 2 + dy) }
                    }
                }
                for ((x, y) in lit) for (gy in -2..2) for (gx in -2..2) c[x + gx, y + gy] = blend(c[x + gx, y + gy], PINK, 0.12f)
                for ((x, y) in lit) c[x, y] = 0xFFFFB3D9.toInt()
            }
            6 -> { // Clock
                for (y in 14..34) for (x in cx - 10..cx + 10) {
                    val d = (x - cx) * (x - cx) + (y - 24) * (y - 24)
                    if (d <= 81) c[x, y] = if (d >= 64) OUTLINE else WHITE
                }
                c.rect(cx, 18, cx, 24, OUTLINE)
                c.rect(cx, 24, cx + 5, 24, RED)
                c[cx, 16] = OUTLINE; c[cx, 32] = OUTLINE; c[cx - 8, 24] = OUTLINE; c[cx + 8, 24] = OUTLINE
            }
            7 -> { // Fairy lights
                val bulbs = listOf(YELLOW, PINK, CYAN)
                for (x in cx - 17..cx + 16) {
                    val y = 16 + (sin((x - cx) / 4.0) * 3).roundToInt()
                    c[x, y] = 0xFF2A2A30.toInt()
                    if ((x - cx) % 5 == 0) {
                        val color = bulbs[((x - cx) / 5).mod(3)]
                        for (gy in -2..2) for (gx in -2..2) c[x + gx, y + 2 + gy] = blend(c[x + gx, y + 2 + gy], color, 0.18f)
                        c[x, y + 1] = color
                        c[x, y + 2] = color
                    }
                }
            }
            8 -> { // Ramadan lantern (fanous) hanging on a chain
                c.rect(cx, 2, cx, 10, 0xFF8A8A98.toInt())
                for (y in 8..38) for (x in cx - 12..cx + 12) {
                    val d = (x - cx) * (x - cx) + (y - 24) * (y - 24)
                    if (d < 160) c[x, y] = blend(c[x, y], GOLD, 0.14f * (1f - d / 160f))
                }
                PixelIcons.rows(EventIcon.LANTERN).forEachIndexed { y, row ->
                    row.forEachIndexed { x, ch ->
                        val color = when (ch) {
                            'P' -> 0xFF7B4FD6.toInt()
                            'A' -> 0xFFFFC94A.toInt()
                            'W' -> WHITE
                            'D' -> OUTLINE
                            else -> null
                        }
                        if (color != null) c.rect(cx - 7 + x * 2, 12 + y * 2, cx - 6 + x * 2, 13 + y * 2, color)
                    }
                }
            }
        }
    }

    // --- Floor decor -----------------------------------------------------------

    private fun floorItem(c: Px, item: Int, x0: Int) {
        when (item) {
            1 -> { // Plant
                c.box(x0 + 7, 74, x0 + 16, 85, OUTLINE, 0xFFB5651D.toInt())
                c.rect(x0 + 6, 74, x0 + 17, 75, 0xFFC97A33.toInt())
                val leaves = listOf(
                    Triple(x0 + 11, 60, 5), Triple(x0 + 6, 66, 4), Triple(x0 + 17, 65, 4),
                    Triple(x0 + 9, 54, 3), Triple(x0 + 15, 56, 3),
                )
                for ((lx, ly, r) in leaves) for (y in ly - r..ly + r) for (x in lx - r..lx + r) {
                    val d = (x - lx) * (x - lx) + (y - ly) * (y - ly)
                    if (d <= r * r) c[x, y] = if (d <= r * r / 3 && x < lx) 0xFF6CCB6E.toInt() else if (d > r * r * 2 / 3) 0xFF2E7D32.toInt() else GREEN
                }
                c.rect(x0 + 11, 64, x0 + 12, 73, 0xFF2E7D32.toInt())
            }
            2 -> { // Floor lamp with a warm glow
                for (y in 38..70) for (x in x0 - 6..x0 + 29) {
                    val d = (x - x0 - 11) * (x - x0 - 11) / 2 + (y - 52) * (y - 52)
                    if (d < 260) c[x, y] = blend(c[x, y], 0xFFFFE1A0.toInt(), 0.2f * (1f - d / 260f))
                }
                for (y in 46..55) {
                    val half = 4 + (y - 46) / 2
                    c.rect(x0 + 11 - half, y, x0 + 12 + half, y, if (y == 55) 0xFFE0C98A.toInt() else 0xFFF3E3B0.toInt())
                }
                c.rect(x0 + 11, 56, x0 + 12, 83, 0xFF3A3A44.toInt())
                c.rect(x0 + 7, 84, x0 + 16, 85, 0xFF3A3A44.toInt())
            }
            3 -> { // Manga tower: a wobbly stack of books
                val colors = listOf(RED, BLUE, YELLOW, GREEN, PURPLE, WHITE, PINK)
                var y = 85
                var i = 0
                while (y > 50) {
                    val shift = (i * 3) % 3 - 1
                    val color = colors[i % colors.size]
                    c.rect(x0 + 4 + shift, y - 2, x0 + 19 + shift, y, color)
                    c.rect(x0 + 4 + shift, y - 2, x0 + 5 + shift, y, blend(color, BLACK, 0.3f))
                    c.rect(x0 + 7 + shift, y - 1, x0 + 16 + shift, y - 1, blend(color, WHITE, 0.45f))
                    y -= 3
                    i++
                }
            }
            4 -> { // Gaming desk with a glowing monitor
                c.box(x0 + 3, 48, x0 + 20, 62, 0xFF111118.toInt(), 0xFF1B3B5A.toInt())
                for (y in 50..60) for (x in x0 + 5..x0 + 18) c[x, y] = blend(0xFF1B3B5A.toInt(), CYAN, (60 - y) / 22f)
                c.rect(x0 + 10, 63, x0 + 13, 64, 0xFF111118.toInt())
                c.rect(x0, 65, x0 + 23, 66, 0xFF2A2A33.toInt())
                listOf(RED, YELLOW, GREEN, CYAN, BLUE, PURPLE).forEachIndexed { i, color -> c[x0 + 5 + i * 2, 64] = color }
                c.rect(x0 + 1, 67, x0 + 2, 85, 0xFF2A2A33.toInt())
                c.rect(x0 + 21, 67, x0 + 22, 85, 0xFF2A2A33.toInt())
            }
            5 -> { // Mini fridge
                c.box(x0 + 4, 60, x0 + 19, 85, OUTLINE, 0xFFDADDE5.toInt())
                c.rect(x0 + 5, 69, x0 + 18, 69, 0xFFAEB2BE.toInt())
                c.rect(x0 + 16, 63, x0 + 16, 67, 0xFF8A8E9A.toInt())
                c.rect(x0 + 16, 72, x0 + 16, 78, 0xFF8A8E9A.toInt())
                c.rect(x0 + 7, 73, x0 + 11, 77, RED) // sticker
                c.rect(x0 + 7, 62, x0 + 10, 64, YELLOW)
            }
            6 -> { // Guitar on a stand
                for (y in 68..84) for (x in x0 + 5..x0 + 18) {
                    val upper = (x - x0 - 11.5f) / 4.5f
                    val lower = (x - x0 - 11.5f) / 6.5f
                    val inside = if (y < 75) upper * upper + ((y - 71f) / 3.5f).let { it * it } <= 1f
                    else lower * lower + ((y - 79f) / 5.5f).let { it * it } <= 1f
                    if (inside) c[x, y] = if (x < x0 + 9) 0xFFE0954A.toInt() else 0xFFC4782A.toInt()
                }
                c.rect(x0 + 10, 76, x0 + 13, 78, OUTLINE) // sound hole
                c.rect(x0 + 11, 46, x0 + 12, 70, 0xFF3A2414.toInt())
                c.rect(x0 + 10, 43, x0 + 13, 46, 0xFF3A2414.toInt())
                c.rect(x0 + 6, 85, x0 + 17, 85, 0xFF2A2A33.toInt())
            }
            7 -> { // Dumbbells and a kettlebell
                fun dumbbell(x: Int, y: Int) {
                    c.rect(x, y + 1, x + 9, y + 1, 0xFF9A9AA8.toInt())
                    c.rect(x, y - 1, x + 2, y + 3, 0xFF2A2A33.toInt())
                    c.rect(x + 7, y - 1, x + 9, y + 3, 0xFF2A2A33.toInt())
                }
                dumbbell(x0 + 2, 82)
                dumbbell(x0 + 4, 77)
                for (y in 74..85) for (x in x0 + 14..x0 + 23) {
                    val d = (x - x0 - 18.5f).let { it * it } / 20f + ((y - 80.5f) / 5f).let { it * it }
                    if (d <= 1f) c[x, y] = if (x < x0 + 17) 0xFF4A4A56.toInt() else 0xFF2A2A33.toInt()
                }
                c.rect(x0 + 16, 71, x0 + 21, 72, 0xFF2A2A33.toInt())
                c.rect(x0 + 16, 71, x0 + 16, 74, 0xFF2A2A33.toInt())
                c.rect(x0 + 21, 71, x0 + 21, 74, 0xFF2A2A33.toInt())
            }
            8 -> { // Speaker
                c.box(x0 + 5, 54, x0 + 18, 85, OUTLINE, 0xFF1A1A22.toInt())
                for ((cy, r) in listOf(62 to 4, 76 to 5)) for (y in cy - r..cy + r) for (x in x0 + 11 - r..x0 + 12 + r) {
                    val d = (x - x0 - 11.5f).let { it * it } + (y - cy).let { (it * it).toFloat() }
                    if (d <= r * r) c[x, y] = if (d < (r - 2) * (r - 2)) 0xFF2E2E38.toInt() else 0xFF5A5A66.toInt()
                }
                c[x0 + 16, 56] = GREEN
            }
        }
    }

    // --- Seats -----------------------------------------------------------------

    private fun seat(c: Px, style: Int) {
        when (style) {
            1 -> { // Couch
                val base = 0xFF4E5BA6.toInt()
                val shade = blend(base, BLACK, 0.28f)
                val light = blend(base, WHITE, 0.18f)
                for (y in 52..68) {
                    val inset = if (y < 55) 55 - y else 0
                    c.rect(34 + inset, y, 85 - inset, y, base)
                }
                c.rect(36, 53, 83, 54, light)
                c.rect(59, 56, 60, 67, shade)
                c.box(31, 60, 39, 80, OUTLINE, base)
                c.box(80, 60, 88, 80, OUTLINE, base)
                c.rect(32, 61, 38, 62, light)
                c.rect(81, 61, 87, 62, light)
                c.rect(39, 68, 80, 75, light)
                c.rect(59, 68, 60, 75, shade)
                c.rect(34, 76, 85, 82, shade)
                c.rect(36, 83, 37, 85, OUTLINE)
                c.rect(82, 83, 83, 85, OUTLINE)
            }
            2 -> { // Gaming chair
                val black = 0xFF1C1C24.toInt()
                c.box(49, 34, 70, 70, OUTLINE, black)
                c.rect(51, 36, 52, 68, RED)
                c.rect(67, 36, 68, 68, RED)
                c.rect(55, 37, 64, 41, 0xFF2E2E38.toInt()) // headrest pillow
                c.rect(45, 70, 74, 75, black)
                c.rect(45, 70, 74, 70, 0xFF34343F.toInt())
                c.rect(59, 76, 60, 80, 0xFF6A6A76.toInt())
                c.rect(49, 81, 70, 82, 0xFF4A4A56.toInt())
                for (x in listOf(49, 59, 70)) c.rect(x - 1, 83, x + 1, 84, OUTLINE)
            }
            3 -> { // Bean bag
                for (y in 66..88) for (x in 38..81) {
                    val dx = (x - 59.5f) / 21f
                    val dy = (y - 78f) / 10f
                    val d = dx * dx + dy * dy
                    if (d <= 1f) c[x, y] = when {
                        d > 0.85f -> 0xFFB85A1E.toInt()
                        x < 52 && y < 74 -> 0xFFF0954A.toInt()
                        else -> 0xFFE07A2F.toInt()
                    }
                }
            }
            4 -> { // Floor cushion (majlis style)
                val maroon = 0xFF7A1F2E.toInt()
                c.box(42, 66, 77, 79, OUTLINE, blend(maroon, WHITE, 0.08f))
                c.rect(43, 67, 76, 67, GOLD)
                c.box(36, 79, 83, 87, OUTLINE, maroon)
                c.rect(37, 80, 82, 80, GOLD)
                for (x in 40..80 step 6) c[x, 84] = GOLD
            }
        }
    }

    // --- Pixel canvas ----------------------------------------------------------

    private class Px {
        val px = IntArray(W * H)

        operator fun set(x: Int, y: Int, color: Int) {
            if (x in 0 until W && y in 0 until H) px[y * W + x] = color
        }

        operator fun get(x: Int, y: Int): Int = if (x in 0 until W && y in 0 until H) px[y * W + x] else 0

        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            for (y in y0..y1) for (x in x0..x1) this[x, y] = color
        }

        /** Filled rectangle with a 1px border. */
        fun box(x0: Int, y0: Int, x1: Int, y1: Int, border: Int, fill: Int) {
            rect(x0, y0, x1, y1, border)
            rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill)
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int = HumanSprite.blend(a, b, t.coerceIn(0f, 1f))
}
