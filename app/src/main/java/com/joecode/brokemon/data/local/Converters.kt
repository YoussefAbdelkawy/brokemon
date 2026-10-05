package com.joecode.brokemon.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroRoom
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

    /** Reads both the current stat names and the ones from the first build. */
    @TypeConverter
    fun jsonToStats(value: String): BroStats {
        val obj = runCatching { JsonParser.parseString(value).asJsonObject }.getOrNull() ?: return BroStats()
        fun stat(vararg keys: String): Int =
            keys.firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonPrimitive }?.asInt } ?: 50
        return BroStats(
            rizz = stat("rizz", "brains"),
            aura = stat("aura", "hype"),
            yap = stat("yap", "humor"),
            loyalty = stat("loyalty"),
            chaos = stat("chaos"),
            flake = stat("flake", "clutch"),
        )
    }

    @TypeConverter
    fun roomToJson(value: BroRoom?): String? = value?.let { gson.toJson(it.toList()) }

    @TypeConverter
    fun jsonToRoom(value: String?): BroRoom? = value?.let {
        runCatching { BroRoom.fromList(gson.fromJson(it, object : TypeToken<List<Int>>() {}.type)) }.getOrNull()
    }

    @TypeConverter
    fun lookToJson(value: BroLook?): String? = value?.let { gson.toJson(it.toList()) }

    @TypeConverter
    fun jsonToLook(value: String?): BroLook? = value?.let {
        runCatching { BroLook.fromList(gson.fromJson(it, object : TypeToken<List<Int>>() {}.type)) }.getOrNull()
    }

    @TypeConverter
    fun rarityToString(value: Rarity): String = value.name

    @TypeConverter
    fun stringToRarity(value: String): Rarity =
        Rarity.entries.firstOrNull { it.name == value } ?: Rarity.COMMON
}
