package com.joecode.brokemon.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joecode.brokemon.data.model.Squad
import kotlinx.coroutines.flow.Flow

@Dao
interface SquadDao {
    @Insert
    suspend fun insert(squad: Squad): Long

    @Update
    suspend fun update(squad: Squad)

    @Delete
    suspend fun delete(squad: Squad)

    @Query("SELECT * FROM squads ORDER BY createdAt ASC")
    fun getAllSquads(): Flow<List<Squad>>

    @Query("SELECT * FROM squads WHERE id = :id")
    fun getSquadById(id: Long): Flow<Squad?>

    @Query("SELECT * FROM squads")
    suspend fun getAllSquadsOnce(): List<Squad>

    @Query("DELETE FROM squads")
    suspend fun deleteAll()
}
