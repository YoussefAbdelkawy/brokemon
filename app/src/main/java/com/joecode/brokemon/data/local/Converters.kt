package com.joecode.brokemon.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joecode.brokemon.data.model.BroStats
import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun stringListToJson(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter
    fun longListToJson(value: List<Long>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToLongList(value: String): List<Long> =
        gson.fromJson(value, object : TypeToken<List<Long>>() {}.type) ?: emptyList()

    @TypeConverter
    fun memoriesToJson(value: List<Memory>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToMemories(value: String): List<Memory> =
        gson.fromJson(value, object : TypeToken<List<Memory>>() {}.type) ?: emptyList()

    @TypeConverter
    fun factsToJson(value: List<Fact>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToFacts(value: String): List<Fact> =
        gson.fromJson(value, object : TypeToken<List<Fact>>() {}.type) ?: emptyList()

    @TypeConverter
    fun statsToJson(value: BroStats): String = gson.toJson(value)

    @TypeConverter
    fun jsonToStats(value: String): BroStats = gson.fromJson(value, BroStats::class.java) ?: BroStats()

    @TypeConverter
    fun rarityToString(value: Rarity): String = value.name

    @TypeConverter
    fun stringToRarity(value: String): Rarity =
        Rarity.entries.firstOrNull { it.name == value } ?: Rarity.COMMON
}
