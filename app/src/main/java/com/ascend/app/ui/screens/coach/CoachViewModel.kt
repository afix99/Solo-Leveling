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
import com.ascend.app.data.ai.ModelListResult
import com.ascend.app.data.db.AiSettingsEntity
import com.ascend.app.data.db.ChatMessageEntity
import com.ascend.app.data.db.CoachAdviceEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.AdviceType
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoachViewModel(private val repository: AscendRepository) : ViewModel() {

    private val client = AiClient()

    val advice: StateFlow<List<CoachAdviceEntity>> =
        repository.observeCoachAdvice()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Observed rather than read once at construction.
     *
     * Coach and Coach Setup are separate nav destinations, so each gets its own
     * ViewModel instance. A one-shot read meant the Coach screen kept the empty
     * settings it loaded before setup ran, and then sent a blank key.
     */
    val settingsFlow: StateFlow<AiSettingsEntity> =
        repository.observeAiSettings()
            .map { it ?: AiSettingsEntity() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSettingsEntity())

    val settings: AiSettingsEntity get() = settingsFlow.value

    val isConfigured: Boolean get() = settings.apiKey.isNotBlank()

    val provider: AiProvider
        get() = runCatching { AiProvider.valueOf(settings.provider) }.getOrDefault(AiProvider.OPENROUTER)

    var generating by mutableStateOf<AdviceType?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var latest by mutableStateOf<Pair<AdviceType, String>?>(null)
        private set

    /** Result of the last connection test, so setup can be verified on the spot. */
    var testResult by mutableStateOf<String?>(null)
        private set

    var testing by mutableStateOf(false)
        private set

    /** Models the saved key can actually call, fetched from the provider. */
    var availableModels by mutableStateOf<List<String>>(emptyList())
        private set

    var loadingModels by mutableStateOf(false)
        private set

    var modelsMessage by mutableStateOf<String?>(null)
        private set

    /** Non-error information, e.g. that the model was swapped automatically. */
    var notice by mutableStateOf<String?>(null)
        private set

    val chat: StateFlow<List<ChatMessageEntity>> =
        repository.observeChat()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var sending by mutableStateOf(false)
        private set

    fun saveSettings(updated: AiSettingsEntity) {
        viewModelScope.launch { repository.saveAiSettings(updated) }
    }

    fun generate(type: AdviceType) {
        if (generating != null) return
        viewModelScope.launch {
            generating = type
            error = null
            latest = null

            // Read straight from storage rather than trusting cached state —
            // this is the call that has to be right.
            val current = repository.getAiSettings()
            val activeProvider = runCatching { AiProvider.valueOf(current.provider) }
                .getOrDefault(AiProvider.OPENROUTER)
            val model = current.model.ifBlank { activeProvider.defaultModel }

            if (current.apiKey.isBlank()) {
                error = "No API key saved. Open Setup, paste your key, and tap Save."
                generating = null
                return@launch
            }

            val ctx = repository.buildCoachContext(LocalDate.now())
            val (system, user) = CoachPrompts.build(type, ctx, current.systemVoice)

            val resolved = client.completeAutoRecovering(
                activeProvider, current.apiKey, model, system, user,
            )
            when (val result = resolved.result) {
                is AiResult.Success -> {
                    repository.saveAdvice(type.name, result.text, resolved.modelUsed)
                    latest = type to result.text
                    if (resolved.modelUsed != model) {
                        // Remember the model that worked so the next request
                        // doesn't repeat the failed round trip.
                        repository.saveAiSettings(current.copy(model = resolved.modelUsed))
                        notice = "Switched to ${resolved.modelUsed} — the saved model wasn't " +
                            "available on your key."
                    }
                }
                is AiResult.Failure ->
                    error = "${result.message}\n\n(${activeProvider.displayName} · $model)"
            }
            generating = null
        }
    }

    /** Cheapest possible round trip, so a bad key or model is caught immediately. */
    fun testConnection() {
        if (testing) return
        viewModelScope.launch {
            testing = true
            testResult = null

            val current = repository.getAiSettings()
            val activeProvider = runCatching { AiProvider.valueOf(current.provider) }
                .getOrDefault(AiProvider.OPENROUTER)
            val model = current.model.ifBlank { activeProvider.defaultModel }

            testResult = when {
                current.apiKey.isBlank() ->
                    "No key saved yet. Paste your key and tap Save first."

                else -> {
                    val resolved = client.completeAutoRecovering(
                        provider = activeProvider,
                        apiKey = current.apiKey,
                        model = model,
                        systemPrompt = "You are a connection test. Reply with exactly: OK",
                        userPrompt = "Reply with exactly: OK",
                    )
                    when (val result = resolved.result) {
                        is AiResult.Success -> {
                            if (resolved.modelUsed != model) {
                                repository.saveAiSettings(current.copy(model = resolved.modelUsed))
                            }
                            "Connected. ${activeProvider.displayName} replied using " +
                                "${resolved.modelUsed}." +
                                if (resolved.modelUsed != model) {
                                    " Your saved model wasn't available, so this one was " +
                                        "selected and saved."
                                } else {
                                    ""
                                }
                        }
                        is AiResult.Failure ->
                            "Failed: ${result.message}\n\n(${activeProvider.displayName} · $model)"
                    }
                }
            }
            testing = false
        }
    }

    fun loadModels() {
        if (loadingModels) return
        viewModelScope.launch {
            loadingModels = true
            modelsMessage = null

            val current = repository.getAiSettings()
            val activeProvider = runCatching { AiProvider.valueOf(current.provider) }
                .getOrDefault(AiProvider.OPENROUTER)

            when (val result = client.listModels(activeProvider, current.apiKey)) {
                is ModelListResult.Success -> {
                    availableModels = result.models
                    modelsMessage =
                        "${result.models.size} models available on your ${activeProvider.displayName} key."
                }
                is ModelListResult.Failure -> {
                    availableModels = emptyList()
                    modelsMessage = result.message
                }
            }
            loadingModels = false
        }
    }

    fun clearModelsMessage() {
        modelsMessage = null
    }

    /** Sends a chat turn. The reply is persisted so the thread survives restarts. */
    fun sendChat(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || sending) return
        viewModelScope.launch {
            sending = true
            error = null
            repository.addChatMessage("user", trimmed)

            val current = repository.getAiSettings()
            val activeProvider = runCatching { AiProvider.valueOf(current.provider) }
                .getOrDefault(AiProvider.OPENROUTER)
            val model = current.model.ifBlank { activeProvider.defaultModel }

            if (current.apiKey.isBlank()) {
                error = "No API key saved. Open Setup, paste your key, and tap Save."
                sending = false
                return@launch
            }

            val ctx = repository.buildCoachContext(LocalDate.now())
            val systemPrompt = CoachPrompts.chatSystemPrompt(ctx, current.systemVoice)
            val history = repository.recentChat().map { AiClient.ChatTurn(it.role, it.content) }

            when (val result = client.chat(activeProvider, current.apiKey, model, systemPrompt, history)) {
                is AiResult.Success -> repository.addChatMessage("assistant", result.text)
                is AiResult.Failure ->
                    error = "${result.message}\n\n(${activeProvider.displayName} · $model)"
            }
            sending = false
        }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChat() }
    }

    fun setSystemVoice(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAiSettings(repository.getAiSettings().copy(systemVoice = enabled))
        }
    }

    fun deleteAdvice(id: Long) {
        viewModelScope.launch { repository.deleteAdvice(id) }
    }

    fun clearError() {
        error = null
    }

    fun clearNotice() {
        notice = null
    }

    fun clearTestResult() {
        testResult = null
    }

    fun dismissLatest() {
        latest = null
    }
}
