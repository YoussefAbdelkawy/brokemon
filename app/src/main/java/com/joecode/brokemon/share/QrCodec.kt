package com.joecode.brokemon.share

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.LookPart
import com.joecode.brokemon.data.model.Rarity

/**
 * Compact card format carried by trade QR codes. Short keys keep the code
 * small and easy to scan. Photos, videos, facts and dates are never included.
 */
data class QrPayload(
    val v: Int?,
    val n: String?,
    val t: List<String>?,
    val s: List<Int>?,
    val m: List<String>?,
    val r: String?,
    val sh: Int?,
    val a: Long?,
    /** The character's look as a list of option indices (v2+). */
    val l: List<Int>? = null,
)

object QrCodec {
    const val PREFIX = "BRKM1:"
    private const val VERSION = 2
    const val MAX_NAME = 24
    /** Four picked at catch plus two learned through evolution. */
    const val MAX_MOVES = 6
    const val MAX_MOVE_LENGTH = 24

    private val gson = Gson()

    fun encode(bro: Bro): String {
        val payload = QrPayload(
            v = VERSION,
            n = bro.name.take(MAX_NAME),
            t = listOfNotNull(bro.type1, bro.type2),
            s = bro.stats.toArray(),
            m = bro.moves.take(MAX_MOVES).map { it.take(MAX_MOVE_LENGTH) },
            r = bro.rarity.code,
            sh = if (bro.isShiny) 1 else 0,
            a = bro.avatarSeed,
            l = bro.resolvedLook.toList(),
        )
        return PREFIX + gson.toJson(payload)
    }

    /** Returns a ready-to-insert bro, or null if the text isn't a valid Brokemon card. */
    fun decode(text: String, now: Long = System.currentTimeMillis()): Bro? {
        if (!text.startsWith(PREFIX)) return null
        val p = try {
            gson.fromJson(text.removePrefix(PREFIX), QrPayload::class.java)
        } catch (e: JsonParseException) {
            return null
        } ?: return null

        if (p.v == null || p.v !in 1..VERSION) return null
        val name = p.n?.trim()?.take(MAX_NAME)?.takeIf { it.isNotEmpty() } ?: return null
        val types = p.t.orEmpty().mapNotNull { BroType.from(it) }.distinct().take(2)
        if (types.isEmpty()) return null
        val stats = p.s?.takeIf { it.size == 6 }?.let { BroStats.fromArray(it) } ?: return null
        val moves = p.m.orEmpty()
            .map { it.trim().take(MAX_MOVE_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_MOVES)

        return Bro(
            name = name,
            type1 = types[0].name,
            type2 = types.getOrNull(1)?.name,
            stats = stats,
            moves = moves,
            catchLocation = "Trade",
            rarity = Rarity.fromCode(p.r),
            isShiny = p.sh == 1,
            catchDate = now,
            avatarSeed = p.a ?: name.hashCode().toLong(),
            look = p.l?.takeIf { it.size == LookPart.entries.size }?.let { BroLook.fromList(it) },
            isTraded = true,
        )
    }
}
