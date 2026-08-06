package com.ascend.app.ui.screens.weeklyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.EvidenceLogEntryEntity
import com.ascend.app.data.db.WeeklyReviewEntity
import com.ascend.app.data.repo.AscendRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeeklyReviewViewModel(private val repository: AscendRepository) : ViewModel() {

    val reviews: StateFlow<List<WeeklyReviewEntity>> =
        repository.observeWeeklyReviews().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val evidenceLog: StateFlow<List<EvidenceLogEntryEntity>> =
        repository.observeEvidenceLog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recomputes (and upserts) the in-progress current week so it shows live data,
     * not just weeks finalized by the Monday rollover. */
    fun refreshCurrentWeek() {
        viewModelScope.launch { repository.generateWeeklyReview(currentWeekStart()) }
    }

    fun saveReflection(weekStart: String, note: String) {
        viewModelScope.launch { repository.saveWeeklyReflection(weekStart, note) }
    }

    companion object {
        fun currentWeekStart(): LocalDate {
            val today = LocalDate.now()
            return today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        }
    }
}
