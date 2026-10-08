package com.joecode.brokemon.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.joecode.brokemon.data.local.BroDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Walks a real version-1 database through every automatic migration up to the current version,
 * checking that the original bro survives with sensible defaults for each new column.
 * The schemas come straight from app/schemas, which Room exports on every build.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BroDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val dbName = "migration-test"

    @Test
    fun v1_to_latest_keeps_the_bro_and_fills_defaults() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO bros (id, name, type1, type2, stats, moves, catchLocation, rarity, isShiny, catchDate, " +
                    "realMeetDate, checkInCount, lastCheckIn, memories, facts, avatarSeed, isTradeable, isTraded) " +
                    "VALUES (1, 'Omar', 'CHILL', NULL, '{}', '[]', 'Cafe', 'RARE', 0, 1700000000000, NULL, 3, NULL, '[]', '[]', 42, 1, 0)",
            )
            execSQL("INSERT INTO squads (id, name, memberIds, createdAt) VALUES (1, 'Day ones', '[1]', 1700000000000)")
            close()
        }

        // 7 is the current version; validateDroppedTables = true catches schema drift too.
        val db = helper.runMigrationsAndValidate(dbName, 7, true)

        db.query("SELECT name, rarity, checkInCount, flavorText, stickers, cardFrame, habitat, look FROM bros WHERE id = 1").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Omar", c.getString(0))
            assertEquals("RARE", c.getString(1))
            assertEquals(3, c.getInt(2))
            assertEquals("", c.getString(3)) // flavorText default
            assertEquals("[]", c.getString(4)) // stickers default
            assertNull(c.getString(5).takeIf { !c.isNull(5) }) // cardFrame stays null
            assertNull(c.getString(6).takeIf { !c.isNull(6) }) // habitat stays null
            assertNull(c.getString(7).takeIf { !c.isNull(7) }) // look stays null until the user edits
        }
        db.query("SELECT name FROM squads WHERE id = 1").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Day ones", c.getString(0))
        }
        // New tables from v6 exist and are empty.
        db.query("SELECT COUNT(*) FROM regional_dexes").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM tournaments").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.close()
    }

    @Test
    fun v6_to_v7_adds_card_frame_and_stickers_without_touching_rows() {
        helper.createDatabase(dbName, 6).apply {
            execSQL(
                "INSERT INTO bros (id, name, type1, type2, stats, moves, catchLocation, rarity, isShiny, catchDate, " +
                    "realMeetDate, checkInCount, lastCheckIn, memories, facts, avatarSeed, look, isTradeable, isTraded, " +
                    "voiceLine, eventFrame, room, flavorText, habitat, battle) " +
                    "VALUES (7, 'Layla', 'NERD', 'GHOST', '{}', '[\"Deep Talk\"]', '', 'EPIC', 1, 1700000000000, " +
                    "NULL, 9, NULL, '[]', '[]', 7, NULL, 1, 0, NULL, NULL, NULL, 'Reads the manual.', 'Library', NULL)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 7, true)
        db.query("SELECT name, isShiny, flavorText, habitat, stickers FROM bros WHERE id = 7").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Layla", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals("Reads the manual.", c.getString(2))
            assertEquals("Library", c.getString(3))
            assertEquals("[]", c.getString(4))
        }
        db.close()
    }
}
