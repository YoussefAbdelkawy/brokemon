package com.joecode.brokemon.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.joecode.brokemon.data.model.BroDexCrossRef
import com.joecode.brokemon.data.model.RegionalDex
import com.joecode.brokemon.data.model.RegionalDexWithBros
import kotlinx.coroutines.flow.Flow

@Dao
interface DexDao {
    @Insert
    suspend fun insert(dex: RegionalDex): Long

    @Update
    suspend fun update(dex: RegionalDex)

    @Delete
    suspend fun delete(dex: RegionalDex)

    @Query("SELECT * FROM regional_dexes ORDER BY createdAt ASC")
    fun getAllDexes(): Flow<List<RegionalDex>>

    @Transaction
    @Query("SELECT * FROM regional_dexes WHERE id = :id")
    fun getDexWithBros(id: Long): Flow<RegionalDexWithBros?>

    @Query("SELECT * FROM bro_dex_cross_ref")
    fun getAllCrossRefs(): Flow<List<BroDexCrossRef>>

    @Query("SELECT * FROM bro_dex_cross_ref WHERE dexId = :dexId")
    suspend fun crossRefsFor(dexId: Long): List<BroDexCrossRef>

    @Query("SELECT * FROM bro_dex_cross_ref")
    suspend fun getAllCrossRefsOnce(): List<BroDexCrossRef>

    @Query("SELECT * FROM regional_dexes")
    suspend fun getAllDexesOnce(): List<RegionalDex>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: BroDexCrossRef)

    @Query("DELETE FROM bro_dex_cross_ref WHERE broId = :broId AND dexId = :dexId")
    suspend fun removeCrossRef(broId: Long, dexId: Long)

    @Query("DELETE FROM regional_dexes")
    suspend fun deleteAll()
}
