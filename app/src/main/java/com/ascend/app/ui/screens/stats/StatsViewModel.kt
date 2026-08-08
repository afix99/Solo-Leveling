package com.ascend.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Stat
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatCardState(
    val stat: Stat,
    val xp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpSpanForLevel: Int,
    val habits: List<HabitEntity>,
    val completionsThisWeek: Int,
)

class StatsViewModel(private val repository: AscendRepository) : ViewModel() {

    val statCards: StateFlow<List<StatCardState>> = combine(
        repository.observeStatProgress(),
        repository.observeActiveHabits(),
    ) { progress, habits ->
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val weekLogs = repository.getLogsForRange(weekStart, today)
        val completionsByStat = weekLogs.filter { it.completed }
            .groupingBy { log -> habits.find { it.id == log.habitId }?.stat }
            .eachCount()

        Stat.entries.map { stat ->
            val entry = progress.find { it.stat == stat }
            val xp = entry?.xp ?: 0
            val p = com.ascend.app.domain.Leveling.progressForXp(xp)
            StatCardState(
                stat = stat,
                xp = xp,
                level = p.level,
                xpIntoLevel = p.xpIntoLevel,
                xpSpanForLevel = p.xpSpanForLevel,
                habits = habits.filter { it.stat == stat },
                completionsThisWeek = completionsByStat[stat] ?: 0,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
