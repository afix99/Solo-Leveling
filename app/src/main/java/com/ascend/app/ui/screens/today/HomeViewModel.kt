package com.ascend.app.ui.screens.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A just-earned XP award, surfaced as a brief confirmation on Today. */
data class XpGain(val amount: Int, val stat: Stat, val stamp: Long)

/** Drives the Today screen: today's checklist, XP/rank state, and the two
 * gentle nudges (recalibration + lie/truth). See design spec §4.5, §5. */
class HomeViewModel(private val repository: AscendRepository) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    val todayDate: LocalDate get() = today

    private fun <T> flowState(initial: T, block: AscendRepository.() -> kotlinx.coroutines.flow.Flow<T>) =
        repository.block().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val habits: StateFlow<List<HabitEntity>> = flowState(emptyList()) { observeActiveHabits() }
    val logsToday: StateFlow<List<DailyLogEntity>> = flowState(emptyList()) { observeLogsForDate(today) }
    val statProgress: StateFlow<List<StatProgressEntity>> = flowState(emptyList()) { observeStatProgress() }
    val totalXp: StateFlow<Int> = flowState(0) { observeTotalXp() }
    val hunterProfile: StateFlow<HunterProfileEntity?> = flowState(null) { observeHunterProfile() }

    var showRecalibrationNudge by mutableStateOf(false)
        private set

    var recalibrationDismissed by mutableStateOf(false)
        private set

    var lieTruthNudge by mutableStateOf<LieTruthEntity?>(null)
        private set

    init {
        viewModelScope.launch {
            showRecalibrationNudge = repository.shouldShowRecalibrationNudge(today)
        }
    }

    /** Set briefly after a completion so the UI can confirm "+20 XP" — without
     * it, checking a habit looks like nothing happened. */
    var lastXpGain by mutableStateOf<XpGain?>(null)
        private set

    fun toggleHabit(habit: HabitEntity, completed: Boolean) {
        viewModelScope.launch {
            val log = repository.getLogForHabitAndDate(habit.id, today)
            val isPenaltyQuest = log?.isPenaltyQuest == true
            repository.setHabitCompleted(habit, today, completed)
            lastXpGain = if (completed) {
                XpGain(
                    amount = Leveling.xpForCompletion(habit.isNonNegotiable, isPenaltyQuest),
                    stat = habit.stat,
                    stamp = System.currentTimeMillis(),
                )
            } else {
                null
            }
        }
    }

    fun clearXpGain() {
        lastXpGain = null
    }

    /** Applies a starter pack from the Today empty state. */
    fun applyStarterPack(pack: StarterPack) {
        viewModelScope.launch { repository.applyStarterPack(pack) }
    }

    fun dismissRecalibrationNudge() {
        recalibrationDismissed = true
    }

    /** Called by the UI once, late in the day, if non-negotiables remain unchecked. */
    fun requestLieTruthNudge() {
        if (lieTruthNudge != null) return
        viewModelScope.launch {
            lieTruthNudge = repository.randomActiveLieTruth()
        }
    }

    fun dismissLieTruthNudge() {
        lieTruthNudge = null
    }
}
