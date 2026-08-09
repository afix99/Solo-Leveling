package com.ascend.app.ui.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.repo.AscendRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

/** How far back the screen looks. Kept small and named so the UI can offer
 * the choice without the ViewModel knowing about buttons. */
enum class HistoryRange(val label: String, val days: Int) {
    MONTH("30 days", 30),
    QUARTER("90 days", 90),
}

class HistoryViewModel(private val repository: AscendRepository) : ViewModel() {

    var range by mutableStateOf(HistoryRange.MONTH)
        private set

    var data by mutableStateOf<AscendRepository.HistoryData?>(null)
        private set

    var loading by mutableStateOf(true)
        private set

    init {
        load()
    }

    /** Named selectRange, not setRange: `range` already generates a setter
     * of that JVM signature, and the two would clash. */
    fun selectRange(next: HistoryRange) {
        if (next == range) return
        range = next
        load()
    }

    /** Re-read on resume as well as on range change: a habit completed on the
     * Today screen should be reflected here without a restart. */
    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            loading = true
            // The clock is read per load, not cached, so the screen doesn't
            // silently keep charting yesterday after midnight.
            data = repository.buildHistory(LocalDate.now(), range.days)
            loading = false
        }
    }
}
