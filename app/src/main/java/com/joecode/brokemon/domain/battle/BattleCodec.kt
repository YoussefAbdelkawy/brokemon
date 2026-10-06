package com.joecode.brokemon.domain.battle

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * Messages between two phones in a Nearby battle. Only actions and the
 * battle snapshot travel; results are computed on each phone.
 */
data class BattleMessage(
    val type: String,
    val trainer: String? = null,
    val look: List<Int>? = null,
    val team: List<FighterSnapshot>? = null,
    val format: Int? = null,
    val fair: Boolean? = null,
    val seed: Long? = null,
    val turn: Int? = null,
    val action: String? = null,
    val emote: String? = null,
    val version: Int = BattleCodec.VERSION,
) {
    companion object {
        const val HELLO = "hello"
        const val CONFIG = "config"
        const val TEAM = "team"
        const val READY = "ready"
        const val ACTION = "action"
        const val EMOTE = "emote"
        const val REMATCH = "rematch"
        const val LEAVE = "leave"
    }
}

object BattleCodec {
    const val VERSION = 1
    private val gson = Gson()

    fun encode(message: BattleMessage): ByteArray = gson.toJson(message).toByteArray()

    fun decode(bytes: ByteArray): BattleMessage? = try {
        gson.fromJson(String(bytes), BattleMessage::class.java)?.takeIf { it.type != null && it.version == VERSION }
    } catch (e: JsonParseException) {
        null
    }

    fun teamsToJson(teams: List<List<FighterSnapshot>>): String = gson.toJson(teams)

    fun teamsFromJson(json: String): List<List<FighterSnapshot>>? = runCatching {
        gson.fromJson<List<List<FighterSnapshot>>>(json, object : TypeToken<List<List<FighterSnapshot>>>() {}.type)
    }.getOrNull()

    /** Replays store each turn as "M0,S1". */
    fun actionsToJson(actions: List<Pair<BattleAction, BattleAction>>): String =
        gson.toJson(actions.map { "${BattleAction.encode(it.first)},${BattleAction.encode(it.second)}" })

    fun actionsFromJson(json: String): List<Pair<BattleAction, BattleAction>> = runCatching {
        gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type).mapNotNull { turn ->
            val (a, b) = turn.split(",").takeIf { it.size == 2 } ?: return@mapNotNull null
            (BattleAction.decode(a) ?: return@mapNotNull null) to (BattleAction.decode(b) ?: return@mapNotNull null)
        }
    }.getOrDefault(emptyList())

    fun matchesToJson(matches: List<TournamentMatch>): String = gson.toJson(matches)

    fun matchesFromJson(json: String): List<TournamentMatch> = runCatching {
        gson.fromJson<List<TournamentMatch>>(json, object : TypeToken<List<TournamentMatch>>() {}.type)
    }.getOrNull().orEmpty()
}
