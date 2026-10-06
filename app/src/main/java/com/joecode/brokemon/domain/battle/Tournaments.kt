package com.joecode.brokemon.domain.battle

import com.joecode.brokemon.data.model.BroType
import java.time.LocalDate
import kotlin.random.Random

/** 6-character codes without look-alike characters (no 0/O, 1/I/L). */
object RoomCodes {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 6

    fun generate(random: Random = Random.Default): String = buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Upper-cases and maps common typos (O→0 is not in the set, so O stays invalid). */
    fun normalize(input: String): String = input.uppercase().filter { !it.isWhitespace() && it != '-' }

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}

enum class TournamentFormat(val label: String) { BRACKET("Single elimination"), ROUND_ROBIN("Round robin") }

/** One match. [a]/[b] null = still waiting on an earlier round; a lone player vs null in round 1 is a bye. */
data class TournamentMatch(
    val round: Int,
    val index: Int,
    val a: String?,
    val b: String?,
    val winner: String? = null,
    val battleId: Long? = null,
) {
    val isBye: Boolean get() = round == 0 && (a == null) != (b == null)
    val ready: Boolean get() = a != null && b != null && winner == null
}

object Brackets {

    fun create(format: TournamentFormat, players: List<String>): List<TournamentMatch> = when (format) {
        TournamentFormat.ROUND_ROBIN -> roundRobin(players)
        TournamentFormat.BRACKET -> bracket(players)
    }

    /** Everyone plays everyone once. */
    private fun roundRobin(players: List<String>): List<TournamentMatch> {
        val matches = mutableListOf<TournamentMatch>()
        for (i in players.indices) for (j in i + 1 until players.size) {
            matches += TournamentMatch(round = 0, index = matches.size, a = players[i], b = players[j])
        }
        return matches
    }

    /** Single elimination with byes: the bracket is padded to a power of two, byes advance automatically. */
    private fun bracket(players: List<String>): List<TournamentMatch> {
        var size = 1
        while (size < players.size) size *= 2
        val slots = players + List(size - players.size) { null }
        // Spread byes out: pair slot i with slot size-1-i.
        val first = (0 until size / 2).map { i -> TournamentMatch(0, i, slots[i], slots[size - 1 - i]) }
        val all = first.toMutableList()
        var round = 1
        var count = size / 4
        while (count >= 1) {
            repeat(count) { all += TournamentMatch(round, it, null, null) }
            round++
            count /= 2
        }
        return advanceByes(all)
    }

    private fun advanceByes(matches: List<TournamentMatch>): List<TournamentMatch> {
        var result = matches
        matches.filter { it.isBye }.forEach { bye -> result = record(result, bye.round, bye.index, bye.a ?: bye.b!!) }
        return result
    }

    /** Sets a match winner and, in a bracket, moves them into the next round. */
    fun record(matches: List<TournamentMatch>, round: Int, index: Int, winner: String, battleId: Long? = null): List<TournamentMatch> {
        val updated = matches.map { if (it.round == round && it.index == index) it.copy(winner = winner, battleId = battleId ?: it.battleId) else it }.toMutableList()
        val nextIndex = index / 2
        val next = updated.indexOfFirst { it.round == round + 1 && it.index == nextIndex }
        if (next >= 0) {
            val m = updated[next]
            updated[next] = if (index % 2 == 0) m.copy(a = winner) else m.copy(b = winner)
        }
        return updated
    }

    data class Standing(val player: String, val wins: Int, val losses: Int)

    fun standings(players: List<String>, matches: List<TournamentMatch>): List<Standing> =
        players.map { p ->
            val played = matches.filter { (it.a == p || it.b == p) && it.winner != null && !it.isBye }
            Standing(p, played.count { it.winner == p }, played.count { it.winner != p })
        }.sortedWith(compareByDescending<Standing> { it.wins }.thenBy { it.losses })

    fun champion(format: TournamentFormat, players: List<String>, matches: List<TournamentMatch>): String? = when (format) {
        TournamentFormat.BRACKET -> matches.maxByOrNull { it.round }?.winner
        TournamentFormat.ROUND_ROBIN -> if (matches.all { it.winner != null }) standings(players, matches).firstOrNull()?.player else null
    }

    fun rounds(matches: List<TournamentMatch>): Int = (matches.maxOfOrNull { it.round } ?: 0) + 1
}

/** One daily battle quest: "Win a battle with a <type>-type bro on your team". */
object DailyBattleQuest {
    const val XP = 60
    fun typeFor(day: LocalDate = LocalDate.now()): BroType = BroType.entries[day.toEpochDay().mod(BroType.entries.size)]
}
