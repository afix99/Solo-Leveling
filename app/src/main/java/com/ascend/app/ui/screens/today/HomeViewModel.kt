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
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun toggleHabit(habit: HabitEntity, completed: Boolean) {
        viewModelScope.launch {
            repository.setHabitCompleted(habit, today, completed)
        }
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
