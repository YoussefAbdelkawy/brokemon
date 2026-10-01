package com.joecode.brokemon.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joecode.brokemon.data.model.Bro
import kotlinx.coroutines.flow.Flow

@Dao
interface BroDao {
    @Insert
    suspend fun insert(bro: Bro): Long

    @Update
    suspend fun update(bro: Bro)

    @Delete
    suspend fun delete(bro: Bro)

    @Query("SELECT * FROM bros ORDER BY id ASC")
    fun getAllBros(): Flow<List<Bro>>

    @Query("SELECT * FROM bros WHERE id = :id")
    fun getBroById(id: Long): Flow<Bro?>

    @Query("SELECT * FROM bros WHERE id = :id")
    suspend fun findBroById(id: Long): Bro?

    @Query("SELECT * FROM bros")
    suspend fun getAllBrosOnce(): List<Bro>

    @Query("DELETE FROM bros")
    suspend fun deleteAll()
}
