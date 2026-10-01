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
    version = 2,
    exportSchema = true,
    // v2 adds the nullable `look` column. Room writes the migration from the exported schemas.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
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
