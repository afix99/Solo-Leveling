package com.ascend.app.ui.screens.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.DailyQuestEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Achievement
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A just-earned payout, surfaced as a brief confirmation on Today. */
data class XpGain(
    val amount: Int,
    val gold: Int,
    val stat: Stat,
    val streakBonusPercent: Int,
    val stamp: Long,
)

/** Drives the Today screen: today's checklist, XP/rank state, the daily quest,
 * and the nudges. See design spec §4.5, §5. */
class HomeViewModel(private val repository: AscendRepository) : ViewModel() {

    /**
     * The day the screen is currently showing.
     *
     * Deliberately not a cached `LocalDate.now()`: the app can sit open across
     * midnight — during a long focus session, or just left in recents — and a
     * captured date would keep writing completions to yesterday, out of step
     * with the rollover worker. Everything reads through [today], which
     * re-checks the clock and re-points the date-keyed flows when the day turns.
     */
    private val currentDate = MutableStateFlow(LocalDate.now())
    val todayDate: LocalDate get() = currentDate.value

    /** Reads the clock fresh, publishing the new day if it has turned over. */
    private fun today(): LocalDate {
        val now = LocalDate.now()
        if (now != currentDate.value) currentDate.value = now
        return now
    }

    private fun <T> flowState(initial: T, block: AscendRepository.() -> kotlinx.coroutines.flow.Flow<T>) =
        repository.block().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> dateFlowState(initial: T, block: AscendRepository.(LocalDate) -> kotlinx.coroutines.flow.Flow<T>) =
        currentDate.flatMapLatest { repository.block(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val habits: StateFlow<List<HabitEntity>> = flowState(emptyList()) { observeActiveHabits() }
    val logsToday: StateFlow<List<DailyLogEntity>> = dateFlowState(emptyList()) { observeLogsForDate(it) }
    val statProgress: StateFlow<List<StatProgressEntity>> = flowState(emptyList()) { observeStatProgress() }
    val totalXp: StateFlow<Int> = flowState(0) { observeTotalXp() }
    val hunterProfile: StateFlow<HunterProfileEntity?> = flowState(null) { observeHunterProfile() }
    val dailyQuest: StateFlow<DailyQuestEntity?> = dateFlowState(null) { observeDailyQuest(it) }

    var showRecalibrationNudge by mutableStateOf(false)
        private set

    var recalibrationDismissed by mutableStateOf(false)
        private set

    var lieTruthNudge by mutableStateOf<LieTruthEntity?>(null)
        private set

    var lastXpGain by mutableStateOf<XpGain?>(null)
        private set

    /** Achievements unlocked since the last time the UI announced them. */
    var pendingAchievements by mutableStateOf<List<Achievement>>(emptyList())
        private set

    /** Progress toward today's quest target. */
    var questProgress by mutableStateOf(0)
        private set

    /** Set once when today's quest is first issued, so the System window fires. */
    var newQuestAnnouncement by mutableStateOf<DailyQuestEntity?>(null)
        private set

    var questClaimed by mutableStateOf<DailyQuestEntity?>(null)
        private set

    init {
        viewModelScope.launch {
            showRecalibrationNudge = repository.shouldShowRecalibrationNudge(today())
            // dailyQuestProgress returns null only when no quest exists yet, so
            // this distinguishes "already issued" from "issuing right now".
            val hadQuestBefore = repository.dailyQuestProgress(today()) != null
            repository.ensureDailyQuest(today())?.let { quest ->
                if (!hadQuestBefore) newQuestAnnouncement = quest
            }
            refreshQuestProgress()
            checkAchievements()
        }
    }

    /**
     * Re-checks the clock and settles any day that ended while the app was
     * away. The worker is the primary path, but it can be deferred for days,
     * so opening the app is the reliable second chance to apply penalties and
     * advance a Gate. Cheap on a normal day — usually zero days to evaluate.
     */
    fun onResumed() {
        viewModelScope.launch {
            val date = today()
            repository.runRolloverCatchUp(date)
            showRecalibrationNudge = repository.shouldShowRecalibrationNudge(date)
            repository.ensureDailyQuest(date)
            refreshQuestProgress()
            checkAchievements()
        }
    }

    private suspend fun refreshQuestProgress() {
        questProgress = repository.dailyQuestProgress(today()) ?: 0
    }

    private suspend fun checkAchievements() {
        val unlocked = repository.checkAchievements()
        if (unlocked.isNotEmpty()) pendingAchievements = pendingAchievements + unlocked
    }

    fun toggleHabit(habit: HabitEntity, completed: Boolean) {
        viewModelScope.launch {
            val result = repository.setHabitCompleted(habit, today(), completed)
            lastXpGain = if (completed) {
                XpGain(
                    amount = result.xp,
                    gold = result.gold,
                    stat = habit.stat,
                    streakBonusPercent = result.streakBonusPercent,
                    stamp = System.currentTimeMillis(),
                )
            } else {
                null
            }
            refreshQuestProgress()
            checkAchievements()
        }
    }

    fun claimDailyQuest() {
        viewModelScope.launch {
            repository.claimDailyQuest(today())?.let { questClaimed = it }
            refreshQuestProgress()
            checkAchievements()
        }
    }

    fun dismissQuestAnnouncement() {
        newQuestAnnouncement = null
    }

    fun dismissQuestClaimed() {
        questClaimed = null
    }

    fun dismissFirstAchievement() {
        pendingAchievements = pendingAchievements.drop(1)
    }

    fun clearXpGain() {
        lastXpGain = null
    }

    fun dismissRecalibrationNudge() {
        recalibrationDismissed = true
    }

    /** Called by the UI once, late in the day, if non-negotiables remain unchecked. */
    fun requestLieTruthNudge() {
        if (lieTruthNudge != null) return
        viewModelScope.launch {
            lieTruthNudge = repository.randomActiveLieTruth()
        }
    }

    fun dismissLieTruthNudge() {
        lieTruthNudge = null
    }

    fun applyStarterPack(pack: StarterPack) {
        viewModelScope.launch {
            repository.applyStarterPack(pack)
            // Adding habits may make a quest generatable for the first time.
            // Only announce if one didn't already exist, or re-adding a pack
            // would re-open the System window on an already-issued quest.
            val hadQuestBefore = repository.dailyQuestProgress(today()) != null
            repository.ensureDailyQuest(today())?.let { quest ->
                if (!hadQuestBefore) newQuestAnnouncement = quest
            }
            refreshQuestProgress()
        }
    }
}
