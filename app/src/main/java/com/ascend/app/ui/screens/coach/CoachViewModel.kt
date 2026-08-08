package com.ascend.app.ui.screens.coach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.ai.AiClient
import com.ascend.app.data.ai.AiProvider
import com.ascend.app.data.ai.AiResult
import com.ascend.app.data.ai.CoachPrompts
import com.ascend.app.data.db.AiSettingsEntity
import com.ascend.app.data.db.CoachAdviceEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.AdviceType
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoachViewModel(private val repository: AscendRepository) : ViewModel() {

    private val client = AiClient()

    val advice: StateFlow<List<CoachAdviceEntity>> =
        repository.observeCoachAdvice()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var settings by mutableStateOf(AiSettingsEntity())
        private set

    var isConfigured by mutableStateOf(false)
        private set

    /** Which advice type is currently generating, if any. */
    var generating by mutableStateOf<AdviceType?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** The freshly generated answer, shown before it drops into the history list. */
    var latest by mutableStateOf<Pair<AdviceType, String>?>(null)
        private set

    init {
        viewModelScope.launch { reloadSettings() }
    }

    private suspend fun reloadSettings() {
        settings = repository.getAiSettings()
        isConfigured = settings.apiKey.isNotBlank()
    }

    val provider: AiProvider
        get() = runCatching { AiProvider.valueOf(settings.provider) }.getOrDefault(AiProvider.OPENROUTER)

    fun saveSettings(updated: AiSettingsEntity) {
        viewModelScope.launch {
            repository.saveAiSettings(updated)
            reloadSettings()
        }
    }

    fun generate(type: AdviceType) {
        if (generating != null) return
        viewModelScope.launch {
            generating = type
            error = null
            latest = null

            val ctx = repository.buildCoachContext(LocalDate.now())
            val (system, user) = CoachPrompts.build(type, ctx)
            val model = settings.model.ifBlank { provider.defaultModel }

            when (val result = client.complete(provider, settings.apiKey, model, system, user)) {
                is AiResult.Success -> {
                    repository.saveAdvice(type.name, result.text, model)
                    latest = type to result.text
                }
                is AiResult.Failure -> error = result.message
            }
            generating = null
        }
    }

    fun deleteAdvice(id: Long) {
        viewModelScope.launch { repository.deleteAdvice(id) }
    }

    fun clearError() {
        error = null
    }

    fun dismissLatest() {
        latest = null
    }
}
