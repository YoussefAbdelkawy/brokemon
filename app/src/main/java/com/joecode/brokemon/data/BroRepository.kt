package com.joecode.brokemon.data

import com.joecode.brokemon.data.local.BroDao
import com.joecode.brokemon.data.local.SquadDao
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad
import kotlinx.coroutines.flow.Flow

class BroRepository(
    private val broDao: BroDao,
    private val squadDao: SquadDao,
    private val media: MediaStorage,
    private val prefs: UserPrefs,
) {
    val bros: Flow<List<Bro>> = broDao.getAllBros()
    val squads: Flow<List<Squad>> = squadDao.getAllSquads()

    fun bro(id: Long): Flow<Bro?> = broDao.getBroById(id)
    fun squad(id: Long): Flow<Squad?> = squadDao.getSquadById(id)

    suspend fun findBro(id: Long): Bro? = broDao.findBroById(id)
    suspend fun allBrosOnce(): List<Bro> = broDao.getAllBrosOnce()

    suspend fun insert(bro: Bro): Long = broDao.insert(bro)
    suspend fun update(bro: Bro) = broDao.update(bro)

    /** Deleting a bro also removes its media files and drops it from every squad. */
    suspend fun delete(bro: Bro) {
        bro.memories.forEach { media.delete(it.fileUri) }
        broDao.delete(bro)
        squadDao.getAllSquadsOnce()
            .filter { bro.id in it.memberIds }
            .forEach { squadDao.update(it.copy(memberIds = it.memberIds - bro.id)) }
    }

    suspend fun insertSquad(squad: Squad): Long = squadDao.insert(squad)
    suspend fun updateSquad(squad: Squad) = squadDao.update(squad)
    suspend fun deleteSquad(squad: Squad) = squadDao.delete(squad)

    suspend fun wipeEverything() {
        broDao.deleteAll()
        squadDao.deleteAll()
        media.deleteAll()
        prefs.clear()
    }
}
