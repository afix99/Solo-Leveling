package com.ascend.app.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Stat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitsViewModel(private val repository: AscendRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> =
        repository.observeActiveHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createHabit(
        name: String,
        stat: Stat,
        isNonNegotiable: Boolean,
        isFocusEnabled: Boolean,
        targetMinutes: Int,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createHabit(
                name = name,
                stat = stat,
                isNonNegotiable = isNonNegotiable,
                reminderHour = null,
                reminderMinute = null,
                isFocusEnabled = isFocusEnabled,
                targetDurationMinutes = if (isFocusEnabled) targetMinutes else null,
            )
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.updateHabit(habit) }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch { repository.archiveHabit(habitId) }
    }

    suspend fun shouldSuggestRaisingTarget(habitId: Long): Boolean =
        repository.shouldSuggestRaisingTarget(habitId)
}
