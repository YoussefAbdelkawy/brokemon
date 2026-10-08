package com.joecode.brokemon.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.data.model.RegionalDex
import com.joecode.brokemon.data.model.BroDexCrossRef
import com.joecode.brokemon.data.model.BattleRecord
import com.joecode.brokemon.data.model.Tournament

@Database(
    entities = [
        Bro::class,
        Squad::class,
        RegionalDex::class,
        BroDexCrossRef::class,
        BattleRecord::class,
        Tournament::class,
    ],
    version = 7,
    exportSchema = true,
    // Room writes these migrations from the exported schemas in app/schemas.
    // v2: `look`. v3: `voiceLine` + `eventFrame`. v4: `room`. v5: `flavorText`.
    // v6: `habitat`, `battle`, regional dexes, battle records, tournaments. v7: `cardFrame`, `stickers`.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(Converters::class)
abstract class BroDatabase : RoomDatabase() {
    abstract fun broDao(): BroDao
    abstract fun squadDao(): SquadDao
    abstract fun dexDao(): DexDao
    abstract fun battleDao(): BattleDao

    companion object {
        @Volatile
        private var instance: BroDatabase? = null

        fun get(context: Context): BroDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BroDatabase::class.java,
                "brokemon.db",
            ).build().also { instance = it }
        }
    }
}
