package com.ascend.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ascend.app.domain.DailyQuestKind
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.GateStatus
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.Skill
import com.ascend.app.domain.Stat

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val stat: Stat,
    val isNonNegotiable: Boolean,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isFocusEnabled: Boolean = false,
    val targetDurationMinutes: Int? = null,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "daily_logs",
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** ISO date string, e.g. "2026-08-06" — one row per (habit, date). */
    val date: String,
    val completed: Boolean,
    val xpAwarded: Int = 0,
    val goldAwarded: Int = 0,
    val isPenaltyQuest: Boolean = false,
    val streakAtCompletion: Int = 0,
)

@Entity(tableName = "stat_progress")
data class StatProgressEntity(
    @PrimaryKey val stat: Stat,
    val xp: Int = 0,
)

@Entity(tableName = "hunter_profile")
data class HunterProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val hunterName: String = "Hunter",
    val morningReminderHour: Int = 7,
    val morningReminderMinute: Int = 0,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val gold: Int = 0,
    val goldEarnedTotal: Int = 0,
    val equippedTitleId: String? = null,
    val hunterClass: HunterClass = HunterClass.NONE,
    val perfectDays: Int = 0,
    val perfectWeeks: Int = 0,
    /** ISO date of the most recent fully-cleared day; stops the counter being
     * farmed by unchecking and rechecking a habit. */
    val lastPerfectDate: String? = null,
    /** ISO date of the last day the midnight rollover actually evaluated.
     * Null until the first rollover runs. Lets the app catch up on days the
     * OS deferred the worker through, instead of silently skipping them. */
    val lastRolloverDate: String? = null,
    /** Date the Mana Conversion daily allowance was last drawn against, with
     * the XP taken on it. Stored as a pair so the cap resets by date rather
     * than needing a scheduled job to clear it. */
    val manaConvertedDate: String? = null,
    val manaConvertedXpToday: Int = 0,
    /** Manually spent stat points, on top of levels earned from XP. */
    val allocatedPoints: Map<Stat, Int> = emptyMap(),
    val unlockedSkills: Set<Skill> = emptySet(),
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goldCost: Int,
    val timesPurchased: Int = 0,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(tableName = "reward_purchases")
data class RewardPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val rewardName: String,
    val goldSpent: Int,
    val purchasedAtEpochMillis: Long,
)

@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAtEpochMillis: Long,
)

@Entity(tableName = "daily_quests")
data class DailyQuestEntity(
    @PrimaryKey val date: String,
    val kind: DailyQuestKind,
    val description: String,
    val targetCount: Int,
    val targetHabitId: Long? = null,
    val xpReward: Int,
    val goldReward: Int,
    val claimed: Boolean = false,
)

@Entity(tableName = "lies_truths")
data class LieTruthEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lieText: String,
    val truthText: String,
    val active: Boolean = true,
    val createdAt: Long,
)

@Entity(tableName = "weekly_reviews")
data class WeeklyReviewEntity(
    @PrimaryKey val weekStart: String, // ISO date of the Monday starting the week
    val weekEnd: String,
    val completionPercent: Float,
    val xpByStat: Map<Stat, Int>,
    val bestDay: String? = null,
    val worstDay: String? = null,
    val userReflectionNote: String = "",
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val startTimeEpochMillis: Long,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Int,
    val completed: Boolean,
    val difficultyRating: DifficultyRating? = null,
)

@Entity(tableName = "evidence_log")
data class EvidenceLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EvidenceType,
    val description: String,
    val dateEpochMillis: Long,
    val sourceId: Long? = null,
)

@Entity(tableName = "shadows")
data class ShadowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val habitId: Long,
    val stat: Stat,
    val extractedAtEpochMillis: Long,
    /** Rises each time the same habit is missed and redeemed again. */
    val rank: Int = 1,
)

@Entity(tableName = "gate_runs")
data class GateRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: GateRank,
    /** ISO date the run was entered. */
    val startDate: String,
    val daysCleared: Int = 0,
    val status: GateStatus = GateStatus.ACTIVE,
    val stakePaid: Int,
    /** ISO date of the last day already counted, so a run advances once per day. */
    val lastEvaluatedDate: String? = null,
)

/**
 * AI coach configuration and the optional body profile that makes training and
 * nutrition advice specific. Single row, like the hunter profile.
 *
 * The API key lives in app-private storage. That is sandboxed from other apps
 * on a non-rooted device, but it is not encrypted at rest — worth knowing
 * before pasting in a key that bills you.
 */
@Entity(tableName = "ai_settings")
data class AiSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val provider: String = "OPENROUTER",
    val apiKey: String = "",
    val model: String = "",
    // Optional athlete profile — all nullable, advice degrades gracefully.
    val age: Int? = null,
    val sex: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val goal: String? = null,
    val experience: String? = null,
    val equipment: String? = null,
    val dietaryNotes: String? = null,
    val injuries: String? = null,
    /** In-character System voice vs plain coaching prose. */
    val systemVoice: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

/** Advice is cached so it stays readable with no connection. */
@Entity(tableName = "coach_advice")
data class CoachAdviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adviceType: String,
    val content: String,
    val model: String,
    val createdAtEpochMillis: Long,
)

/** One turn of the System chat. Kept so a conversation survives app restarts. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "user" or "assistant", matching the wire format. */
    val role: String,
    val content: String,
    val createdAtEpochMillis: Long,
)
