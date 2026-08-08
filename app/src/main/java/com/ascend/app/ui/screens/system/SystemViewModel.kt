package com.ascend.app.ui.screens.system

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.GateRun
import com.ascend.app.domain.HunterLoadout
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Shadow
import com.ascend.app.domain.Skill
import com.ascend.app.domain.StatAllocation
import com.ascend.app.domain.Stat
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the System screen needs, recomputed whenever the DB changes. */
data class SystemState(
    val hunterLevel: Int = 1,
    val rank: Rank = Rank.E,
    val gold: Int = 0,
    val loadout: HunterLoadout = HunterLoadout(),
    val statPointsAvailable: Int = 0,
    val skillPointsAvailable: Int = 0,
    val shadows: List<Shadow> = emptyList(),
    val activeGate: GateRun? = null,
)

class SystemViewModel(private val repository: AscendRepository) : ViewModel() {

    val shadows: StateFlow<List<Shadow>> = repository.observeShadows()
        .map { list ->
            list.map {
                Shadow(it.id, it.name, it.habitId, it.stat, it.extractedAtEpochMillis, it.rank)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(SystemState())
        private set

    /** Set when an action is refused, so the UI can explain why. */
    var message by mutableStateOf<String?>(null)
        private set

    init {
        // Re-read after any change that can alter points, gold or the gate.
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        val profile = repository.getHunterProfile()
        val level = repository.hunterLevel()
        val rank = repository.currentRank()
        val loadout = repository.currentLoadout()
        state = SystemState(
            hunterLevel = level,
            rank = rank,
            gold = profile.gold,
            loadout = loadout,
            statPointsAvailable = StatAllocation.pointsAvailable(level, profile.allocatedPoints),
            skillPointsAvailable = Skill.pointsAvailable(level, rank, profile.unlockedSkills),
            shadows = loadout.shadows,
            activeGate = repository.activeGate(),
        )
    }

    fun allocate(stat: Stat) {
        viewModelScope.launch {
            if (!repository.allocateStatPoint(stat)) message = "No stat points available."
            refresh()
        }
    }

    fun respec() {
        viewModelScope.launch {
            message = if (repository.respecStatPoints()) {
                "Points refunded."
            } else {
                "Respec costs ${StatAllocation.RESPEC_GOLD_COST} Gold, and needs points to refund."
            }
            refresh()
        }
    }

    fun unlock(skill: Skill) {
        viewModelScope.launch {
            if (!repository.unlockSkill(skill)) {
                message = "Not enough skill points, or your level is too low."
            }
            refresh()
        }
    }

    fun enterGate(rank: GateRank) {
        viewModelScope.launch {
            if (!repository.enterGate(rank, LocalDate.now())) {
                message = "Need ${rank.stake} Gold, level ${rank.requiredHunterLevel}, and no active Gate."
            }
            refresh()
        }
    }

    fun abandonGate() {
        viewModelScope.launch {
            repository.abandonGate()
            message = "Gate abandoned. The stake is forfeit."
            refresh()
        }
    }

    fun clearMessage() {
        message = null
    }
}
