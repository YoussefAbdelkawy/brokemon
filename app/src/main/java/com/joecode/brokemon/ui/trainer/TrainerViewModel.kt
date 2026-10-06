package com.joecode.brokemon.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.Trainer
import com.joecode.brokemon.data.model.TrainerFrame
import com.joecode.brokemon.domain.Journal
import com.joecode.brokemon.domain.LevelInfo
import com.joecode.brokemon.domain.Quest
import com.joecode.brokemon.domain.QuestStatus
import com.joecode.brokemon.domain.Reward
import com.joecode.brokemon.domain.TrainerLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything about "you": card, level, Journal. Shared by Home and the Trainer screens. */
data class TrainerProgress(
    val trainer: Trainer? = null,
    val level: LevelInfo = TrainerLevel.info(0),
    val totals: TrainerTotals = TrainerTotals(0, 0, 0),
    val quests: List<QuestStatus> = emptyList(),
    val unlocked: Set<Reward> = emptySet(),
    val frames: List<TrainerFrame> = listOf(TrainerFrame.BASIC),
    val trophies: Int = 0,
    val battleWins: Int = 0,
) {
    val questsDone: Int get() = quests.count { it.done }
    val rewardsReady: Int get() = quests.count { it.claimable }
    val journalComplete: Boolean get() = quests.isNotEmpty() && quests.all { it.claimed }

    companion object {
        fun from(
            bros: List<Bro>,
            trainer: Trainer?,
            claimed: Set<String>,
            tradedQr: Boolean,
            shinyEarned: Boolean = false,
            champion: Boolean = false,
        ): TrainerProgress {
            val quests = Journal.status(trainer != null, bros, tradedQr, claimed)
            return TrainerProgress(
                trainer = trainer,
                level = TrainerLevel.info(TrainerLevel.xp(bros, trainer != null, quests.count { it.claimed })),
                totals = TrainerTotals(bros.size, bros.sumOf { it.checkInCount }, bros.sumOf { it.memories.size }),
                quests = quests,
                unlocked = Journal.unlocked(claimed, shinyEarned, champion),
                frames = Journal.unlockedFrames(claimed),
            )
        }
    }
}

fun trainerProgressFlow(repository: BroRepository, prefs: UserPrefs) =
    combine(
        combine(repository.bros, prefs.trainer, prefs.claimedQuests, prefs.tradedQr) { bros, trainer, claimed, traded ->
            TrainerProgress.from(bros, trainer, claimed, traded)
        },
        prefs.shinyEarned,
        prefs.tournamentWon,
        prefs.claimedQuests,
    ) { base, shiny, champ, claimed ->
        base.copy(unlocked = Journal.unlocked(claimed, shiny, champ))
    }.let { flow ->
        combine(flow, repository.bros, prefs.battleWins, prefs.dailyQuestsDone, prefs.trophies) { p, bros, wins, daily, trophies ->
            val xp = TrainerLevel.xp(bros, p.trainer != null, p.quests.count { it.claimed }, wins, daily)
            p.copy(level = TrainerLevel.info(xp), trophies = trophies, battleWins = wins)
        }
    }

class TrainerViewModel(repository: BroRepository, private val prefs: UserPrefs) : ViewModel() {

    val progress: StateFlow<TrainerProgress?> = trainerProgressFlow(repository, prefs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The reward just claimed, for the "reward get!" popup. */
    private val _justClaimed = MutableStateFlow<Reward?>(null)
    val justClaimed: StateFlow<Reward?> = _justClaimed.asStateFlow()

    fun saveTrainer(trainer: Trainer, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val existing = prefs.trainerOnce()
            prefs.setTrainer(
                trainer.copy(
                    name = trainer.name.trim().take(Trainer.MAX_NAME),
                    motto = trainer.motto.trim(),
                    createdAt = existing?.createdAt?.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    frame = existing?.frame ?: trainer.frame,
                ),
            )
            onSaved()
        }
    }

    fun setFrame(frame: TrainerFrame) {
        viewModelScope.launch {
            val current = prefs.trainerOnce() ?: return@launch
            prefs.setTrainer(current.copy(frame = frame.name))
        }
    }

    fun claim(quest: Quest) {
        viewModelScope.launch {
            val status = progress.first { it != null }!!.quests.firstOrNull { it.quest == quest } ?: return@launch
            if (!status.claimable) return@launch
            prefs.claimQuest(quest.name)
            _justClaimed.update { quest.reward }
        }
    }

    fun dismissClaimed() = _justClaimed.update { null }
}
