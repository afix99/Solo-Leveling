package com.ascend.app.ui.screens.liestruths

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.repo.AscendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LieTruthViewModel(private val repository: AscendRepository) : ViewModel() {

    val entries: StateFlow<List<LieTruthEntity>> =
        repository.observeLiesTruths().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(lie: String, truth: String) {
        if (lie.isBlank() || truth.isBlank()) return
        viewModelScope.launch { repository.addLieTruth(lie, truth) }
    }

    fun setActive(entry: LieTruthEntity, active: Boolean) {
        viewModelScope.launch { repository.updateLieTruth(entry.copy(active = active)) }
    }
}
