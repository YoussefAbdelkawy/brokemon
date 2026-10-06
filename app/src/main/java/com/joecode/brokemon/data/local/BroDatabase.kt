package com.joecode.brokemon.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad

@Database(
    entities = [Bro::class, Squad::class],
    version = 5,
    exportSchema = true,
    // Room writes these migrations from the exported schemas in app/schemas.
    // v2: `look`. v3: `voiceLine` + `eventFrame`. v4: `room`. v5: `flavorText`.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(Converters::class)
abstract class BroDatabase : RoomDatabase() {
    abstract fun broDao(): BroDao
    abstract fun squadDao(): SquadDao

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
