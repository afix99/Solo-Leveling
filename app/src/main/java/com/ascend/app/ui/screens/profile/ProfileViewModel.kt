package com.ascend.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.EvidenceLogEntryEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.repo.AscendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: AscendRepository) : ViewModel() {

    val profile: StateFlow<HunterProfileEntity?> =
        repository.observeHunterProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val evidenceLog: StateFlow<List<EvidenceLogEntryEntity>> =
        repository.observeEvidenceLog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(profile: HunterProfileEntity) {
        viewModelScope.launch { repository.saveHunterProfile(profile) }
    }

    suspend fun exportJson(): String = repository.exportAllDataAsJson()

    fun resetAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resetAllData()
            onDone()
        }
    }
}
