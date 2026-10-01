package com.joecode.brokemon.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bros")
data class Bro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type1: String,
    val type2: String? = null,
    val stats: BroStats = BroStats(),
    val moves: List<String> = emptyList(),
    val catchLocation: String = "",
    val rarity: Rarity = Rarity.COMMON,
    val isShiny: Boolean = false,
    /** Stamped automatically when the bro is caught. */
    val catchDate: Long,
    /** Optional "when we actually met" date the user can fill in later. */
    val realMeetDate: Long? = null,
    val checkInCount: Int = 0,
    val lastCheckIn: Long? = null,
    val memories: List<Memory> = emptyList(),
    val facts: List<Fact> = emptyList(),
    /** Seed for the procedurally generated pixel avatar. */
    val avatarSeed: Long,
    /** Tradeable bros can be shared by QR; locked ones can't. */
    val isTradeable: Boolean = true,
    /** True if this card arrived through a QR trade instead of a catch. */
    val isTraded: Boolean = false,
) {
    val dexNumber: String get() = "#%03d".format(id)
    val types: List<BroType> get() = listOfNotNull(BroType.from(type1), BroType.from(type2))
    val primaryType: BroType get() = BroType.from(type1) ?: BroType.CHILL
}
