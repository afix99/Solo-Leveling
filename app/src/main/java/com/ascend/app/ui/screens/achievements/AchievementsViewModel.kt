package com.ascend.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.UnlockedAchievementEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.HunterClass
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementsViewModel(private val repository: AscendRepository) : ViewModel() {

    val unlocked: StateFlow<List<UnlockedAchievementEntity>> =
        repository.observeUnlockedAchievements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<HunterProfileEntity?> =
        repository.observeHunterProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun equipTitle(titleId: String?) {
        viewModelScope.launch { repository.equipTitle(titleId) }
    }

    fun chooseClass(hunterClass: HunterClass) {
        viewModelScope.launch { repository.setHunterClass(hunterClass) }
    }
}
