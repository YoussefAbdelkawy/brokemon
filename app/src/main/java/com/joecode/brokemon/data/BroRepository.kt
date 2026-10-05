package com.joecode.brokemon.data

import androidx.room.withTransaction
import com.joecode.brokemon.data.local.BroDao
import com.joecode.brokemon.data.local.BroDatabase
import com.joecode.brokemon.data.local.SquadDao
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import kotlinx.coroutines.flow.Flow

enum class CheckInResult { CHECKED_IN, ALREADY_TODAY, NOT_FOUND }

class BroRepository(
    private val database: BroDatabase,
    private val media: MediaStorage,
    private val prefs: UserPrefs,
    /** Called after every write, e.g. to refresh the home-screen widget. */
    private val onChanged: () -> Unit = {},
) {
    private val broDao: BroDao = database.broDao()
    private val squadDao: SquadDao = database.squadDao()

    val bros: Flow<List<Bro>> = broDao.getAllBros()
    val squads: Flow<List<Squad>> = squadDao.getAllSquads()

    fun bro(id: Long): Flow<Bro?> = broDao.getBroById(id)
    fun squad(id: Long): Flow<Squad?> = squadDao.getSquadById(id)

    suspend fun findBro(id: Long): Bro? = broDao.findBroById(id)
    suspend fun allBrosOnce(): List<Bro> = broDao.getAllBrosOnce()

    suspend fun insert(bro: Bro): Long = broDao.insert(bro).also { onChanged() }
    suspend fun update(bro: Bro) = broDao.update(bro).also { onChanged() }

    /** One check-in per bro per day, so spamming the button can't farm bond points. */
    suspend fun checkIn(id: Long, now: Long = System.currentTimeMillis()): CheckInResult {
        val bro = broDao.findBroById(id) ?: return CheckInResult.NOT_FOUND
        if (CheckOnBro.checkedInToday(bro, now)) return CheckInResult.ALREADY_TODAY
        update(bro.copy(checkInCount = bro.checkInCount + 1, lastCheckIn = now))
        return CheckInResult.CHECKED_IN
    }

    /** Deleting a bro also removes its media files and drops it from every squad. */
    suspend fun delete(bro: Bro) {
        bro.memories.forEach { media.delete(it.fileUri) }
        bro.voiceLine?.let { media.delete(it) }
        broDao.delete(bro)
        squadDao.getAllSquadsOnce()
            .filter { bro.id in it.memberIds }
            .forEach { squadDao.update(it.copy(memberIds = it.memberIds - bro.id)) }
        onChanged()
    }

    suspend fun insertSquad(squad: Squad): Long = squadDao.insert(squad)
    suspend fun updateSquad(squad: Squad) = squadDao.update(squad)
    suspend fun deleteSquad(squad: Squad) = squadDao.delete(squad)
    suspend fun allSquadsOnce(): List<Squad> = squadDao.getAllSquadsOnce()

    suspend fun wipeEverything() {
        broDao.deleteAll()
        squadDao.deleteAll()
        media.deleteAll()
        prefs.clear()
        onChanged()
    }

    /**
     * Replaces the whole Brodex with restored data in one transaction, keeping
     * the original ids so dex numbers and squad memberships survive.
     */
    suspend fun replaceAll(bros: List<Bro>, squads: List<Squad>) {
        database.withTransaction {
            broDao.deleteAll()
            squadDao.deleteAll()
            bros.forEach { broDao.insert(it) }
            squads.forEach { squadDao.insert(it) }
        }
        prefs.clear()
        // Don't replay evolution animations for bros that had already evolved.
        bros.forEach { prefs.setSeenStage(it.id, Evolution.info(it).stage.ordinal) }
        onChanged()
    }
}
