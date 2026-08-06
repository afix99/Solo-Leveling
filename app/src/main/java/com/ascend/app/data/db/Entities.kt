package com.ascend.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.EvidenceType
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
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

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
