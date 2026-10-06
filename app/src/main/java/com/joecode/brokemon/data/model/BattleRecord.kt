package com.joecode.brokemon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BattleMode(val label: String) { CPU("Practice vs CPU"), LOCAL("Same phone"), NEARBY("Nearby") }

/**
 * One finished battle. Battles are deterministic, so [seed] + [actionsJson]
 * (+ the two team snapshots) is a complete replay in a few hundred bytes.
 */
@Entity(tableName = "battle_records")
data class BattleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mode: String,
    val opponentName: String,
    /** True = you won, false = you lost, null = draw/forfeit-by-disconnect undecided. */
    val won: Boolean?,
    val mvpBroId: Long? = null,
    val mvpName: String = "",
    val seed: Long,
    val fairMode: Boolean,
    val snapshotsJson: String,
    val actionsJson: String,
    val tournamentId: Long? = null,
    /** Which side you were (0 = host / player 1). Needed to replay from your point of view. */
    val mySide: Int = 0,
    val sideNames: List<String> = emptyList(),
)

/** A local tournament bracket, kept on the organizer's phone. */
@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    /** "BRACKET" or "ROUND_ROBIN". */
    val format: String,
    /** Player (trainer) names, in seeding order. */
    val players: List<String>,
    /** Matches as JSON (see TournamentMatch). */
    val matchesJson: String,
    val dexId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val champion: String? = null,
)
