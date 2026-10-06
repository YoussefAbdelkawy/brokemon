package com.joecode.brokemon.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joecode.brokemon.data.model.BattleRecord
import com.joecode.brokemon.data.model.Tournament
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleDao {
    @Insert
    suspend fun insertRecord(record: BattleRecord): Long

    @Query("SELECT * FROM battle_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BattleRecord>>

    @Query("SELECT * FROM battle_records WHERE id = :id")
    suspend fun findRecord(id: Long): BattleRecord?

    @Insert
    suspend fun insertTournament(tournament: Tournament): Long

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Delete
    suspend fun deleteTournament(tournament: Tournament)

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournaments(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournament(id: Long): Flow<Tournament?>

    @Query("DELETE FROM battle_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()
}
