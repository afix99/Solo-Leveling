package com.ascend.app.ui.screens.cloud

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.cloud.CloudClient
import com.ascend.app.data.cloud.CloudResult
import com.ascend.app.data.db.CloudSettingsEntity
import com.ascend.app.data.repo.AscendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the screen is currently doing, so the UI can show one spinner rather
 * than guessing from a pile of booleans. */
enum class CloudBusy { NONE, TESTING, BACKING_UP, RESTORING }

class CloudViewModel(private val repository: AscendRepository) : ViewModel() {

    private val client = CloudClient()

    /**
     * Read as a Flow rather than cached in init — the same mistake in the Coach
     * ViewModel meant a key saved on one screen was invisible to another,
     * because viewModel() scopes per NavBackStackEntry.
     */
    val settings: StateFlow<CloudSettingsEntity> = repository.observeCloudSettings()
        .map { it ?: CloudSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CloudSettingsEntity())

    var busy by mutableStateOf(CloudBusy.NONE)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var isError by mutableStateOf(false)
        private set

    fun clearMessage() {
        message = null
        isError = false
    }

    private fun report(text: String, error: Boolean = false) {
        message = text
        isError = error
    }

    fun saveUrl(url: String) {
        viewModelScope.launch {
            val current = repository.getCloudSettings()
            repository.saveCloudSettings(
                current.copy(baseUrl = CloudClient.normaliseBaseUrl(url)),
            )
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveCloudSettings(repository.getCloudSettings().copy(autoBackup = enabled))
        }
    }

    /** Creates a key if there isn't one. Never silently replaces an existing
     * key — that would orphan the backup it unlocks. */
    fun generateKeyIfAbsent() {
        viewModelScope.launch {
            val current = repository.getCloudSettings()
            if (current.hunterKey.isNotBlank()) {
                report("You already have a Hunter Key. Replacing it would orphan your backup.")
                return@launch
            }
            repository.saveCloudSettings(current.copy(hunterKey = CloudClient.generateHunterKey()))
            report("Hunter Key created. Write it down — it is the only way back to your backup.")
        }
    }

    fun setKey(key: String) {
        viewModelScope.launch {
            repository.saveCloudSettings(repository.getCloudSettings().copy(hunterKey = key.trim()))
        }
    }

    fun testConnection() {
        if (busy != CloudBusy.NONE) return
        busy = CloudBusy.TESTING
        viewModelScope.launch {
            val settings = repository.getCloudSettings()
            when (val result = client.health(settings.baseUrl)) {
                is CloudResult.Ok ->
                    report(
                        if (result.value) {
                            "Connected. Database is attached and ready."
                        } else {
                            "Server is up, but no database is attached to it yet. " +
                                "Add a Postgres store in the Vercel dashboard."
                        },
                        error = !result.value,
                    )
                is CloudResult.Failure -> report(result.message, error = true)
            }
            busy = CloudBusy.NONE
        }
    }

    fun backupNow() {
        if (busy != CloudBusy.NONE) return
        busy = CloudBusy.BACKING_UP
        viewModelScope.launch {
            val settings = repository.getCloudSettings()
            if (settings.hunterKey.isBlank()) {
                report("Create a Hunter Key first.", error = true)
                busy = CloudBusy.NONE
                return@launch
            }

            val body = repository.buildCloudBackupBody()
            when (val result = client.backup(settings.baseUrl, settings.hunterKey, body)) {
                is CloudResult.Ok -> {
                    repository.saveCloudSettings(
                        settings.copy(
                            lastBackupAtEpochMillis = System.currentTimeMillis(),
                            lastBackupStatus = "OK",
                        ),
                    )
                    report(
                        "Backed up ${result.value.daysWritten} days and " +
                            "${result.value.habitsWritten} habit records.",
                    )
                }
                is CloudResult.Failure -> {
                    repository.saveCloudSettings(settings.copy(lastBackupStatus = result.message))
                    report(result.message, error = true)
                }
            }
            busy = CloudBusy.NONE
        }
    }

    fun restoreNow() {
        if (busy != CloudBusy.NONE) return
        busy = CloudBusy.RESTORING
        viewModelScope.launch {
            val settings = repository.getCloudSettings()
            when (val result = client.latestSnapshot(settings.baseUrl, settings.hunterKey)) {
                is CloudResult.Ok -> {
                    val (habits, logs) = repository.restoreFromSnapshot(result.value.payload)
                    report("Restored $habits habits and $logs day records.")
                }
                is CloudResult.Failure -> report(result.message, error = true)
            }
            busy = CloudBusy.NONE
        }
    }
}
