package com.ascend.app.data.repo

import com.ascend.app.data.db.AscendDatabase
import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.EvidenceLogEntryEntity
import com.ascend.app.data.db.FocusSessionEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.data.db.WeeklyReviewEntity
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single entry point for all data access + the business rules that stitch
 * DAOs together with the pure [Leveling] math. ViewModels and WorkManager
 * workers both go through here — neither talks to a DAO directly.
 */
class AscendRepository(private val db: AscendDatabase) {

    private val habitDao = db.habitDao()
    private val dailyLogDao = db.dailyLogDao()
    private val statProgressDao = db.statProgressDao()
    private val hunterProfileDao = db.hunterProfileDao()
    private val lieTruthDao = db.lieTruthDao()
    private val weeklyReviewDao = db.weeklyReviewDao()
    private val focusSessionDao = db.focusSessionDao()
    private val evidenceLogDao = db.evidenceLogDao()

    // ---- Observing state -------------------------------------------------

    fun observeActiveHabits(): Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    fun observeHabit(habitId: Long): Flow<HabitEntity?> = habitDao.observeById(habitId)
    suspend fun getHabit(habitId: Long): HabitEntity? = habitDao.getById(habitId)
    fun observeLogsForDate(date: LocalDate): Flow<List<DailyLogEntity>> =
        dailyLogDao.observeForDate(date.toString())
    fun observeLogsForHabit(habitId: Long): Flow<List<DailyLogEntity>> =
        dailyLogDao.observeForHabit(habitId)
    fun observeStatProgress(): Flow<List<StatProgressEntity>> = statProgressDao.observeAll()
    fun observeTotalXp(): Flow<Int> = statProgressDao.observeTotalXp()
    fun observeHunterProfile(): Flow<HunterProfileEntity?> = hunterProfileDao.observe()
    fun observeLiesTruths(): Flow<List<LieTruthEntity>> = lieTruthDao.observeAll()
    fun observeWeeklyReviews(): Flow<List<WeeklyReviewEntity>> = weeklyReviewDao.observeAll()
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>> = focusSessionDao.observeAll()
    fun observeEvidenceLog(): Flow<List<EvidenceLogEntryEntity>> = evidenceLogDao.observeAll()

    suspend fun getHunterProfile(): HunterProfileEntity = hunterProfileDao.get() ?: HunterProfileEntity()

    /** Ensures a HunterProfile row exists so [observeHunterProfile] never emits
     * null forever on a fresh install — called once at app startup. */
    suspend fun ensureProfileExists() {
        if (hunterProfileDao.get() == null) hunterProfileDao.upsert(HunterProfileEntity())
    }

    // ---- Habit CRUD --------------------------------------------------------

    suspend fun createHabit(
        name: String,
        stat: Stat,
        isNonNegotiable: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        isFocusEnabled: Boolean,
        targetDurationMinutes: Int?,
    ): Long {
        val habit = HabitEntity(
            name = name,
            stat = stat,
            isNonNegotiable = isNonNegotiable,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            isFocusEnabled = isFocusEnabled,
            targetDurationMinutes = targetDurationMinutes,
            createdAt = System.currentTimeMillis(),
        )
        return habitDao.insert(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) = habitDao.update(habit)

    /** Bulk-creates a starter pack's habits, skipping any name already present
     * so applying a pack twice doesn't produce duplicates. */
    suspend fun applyStarterPack(pack: StarterPack) {
        val existingNames = habitDao.getActiveHabits().map { it.name.lowercase() }.toSet()
        for (template in pack.habits) {
            if (template.name.lowercase() in existingNames) continue
            createHabit(
                name = template.name,
                stat = template.stat,
                isNonNegotiable = template.isNonNegotiable,
                reminderHour = null,
                reminderMinute = null,
                isFocusEnabled = template.focusMinutes != null,
                targetDurationMinutes = template.focusMinutes,
            )
        }
    }

    suspend fun archiveHabit(habitId: Long) {
        habitDao.getById(habitId)?.let { habitDao.update(it.copy(archived = true)) }
    }

    // ---- Daily completion / penalty ----------------------------------------

    /**
     * Toggle a habit's completion for [date]. Awards or reverses XP, updates
     * the streak, and drops Evidence Log entries for streak milestones and
     * penalty-quest redemptions. See design spec §4.
     */
    suspend fun setHabitCompleted(habit: HabitEntity, date: LocalDate, completed: Boolean) {
        val dateStr = date.toString()
        val existing = dailyLogDao.getForHabitAndDate(habit.id, dateStr)
        val isPenaltyQuest = existing?.isPenaltyQuest ?: false
        val previousXp = existing?.xpAwarded ?: 0

        val newStreak = if (completed) {
            val yesterday = dailyLogDao.getForHabitAndDate(habit.id, date.minusDays(1).toString())
            val prevStreak = if (yesterday?.completed == true) yesterday.streakAtCompletion else 0
            prevStreak + 1
        } else {
            0
        }

        val newXp = if (completed) Leveling.xpForCompletion(habit.isNonNegotiable, isPenaltyQuest) else 0

        dailyLogDao.upsert(
            DailyLogEntity(
                id = existing?.id ?: 0,
                habitId = habit.id,
                date = dateStr,
                completed = completed,
                xpAwarded = newXp,
                isPenaltyQuest = isPenaltyQuest,
                streakAtCompletion = newStreak,
            ),
        )

        val xpDelta = newXp - previousXp
        if (xpDelta != 0) adjustStatXp(habit.stat, xpDelta)

        if (completed && Leveling.isStreakMilestone(newStreak)) {
            logEvidence(
                EvidenceType.STREAK_MILESTONE,
                "$newStreak-day streak on “${habit.name}”",
                habit.id,
            )
        }
        if (completed && isPenaltyQuest) {
            logEvidence(
                EvidenceType.PENALTY_REDEMPTION,
                "Redeemed “${habit.name}” after missing it",
                habit.id,
            )
        }
    }

    /**
     * Run once per day (via WorkManager, just after local midnight) for the
     * day that just ended: penalizes missed non-negotiables and queues a
     * Penalty Quest for the following day. See design spec §4.
     */
    suspend fun runMidnightRollover(rolledDate: LocalDate) {
        val dateStr = rolledDate.toString()
        val nextDateStr = rolledDate.plusDays(1).toString()

        for (habit in habitDao.getActiveHabits()) {
            if (!habit.isNonNegotiable) continue
            val log = dailyLogDao.getForHabitAndDate(habit.id, dateStr)
            val wasCompleted = log?.completed == true
            if (wasCompleted) continue

            if (log == null) {
                dailyLogDao.upsert(
                    DailyLogEntity(habitId = habit.id, date = dateStr, completed = false),
                )
            }

            val currentXp = statProgressDao.get(habit.stat)?.xp ?: 0
            statProgressDao.upsert(StatProgressEntity(habit.stat, Leveling.applyPenalty(currentXp)))

            val nextExisting = dailyLogDao.getForHabitAndDate(habit.id, nextDateStr)
            when {
                nextExisting == null -> dailyLogDao.upsert(
                    DailyLogEntity(
                        habitId = habit.id,
                        date = nextDateStr,
                        completed = false,
                        isPenaltyQuest = true,
                    ),
                )
                !nextExisting.isPenaltyQuest -> dailyLogDao.upsert(nextExisting.copy(isPenaltyQuest = true))
                // else: a penalty quest is already queued for tomorrow — capped at one, don't stack.
            }
        }
    }

    suspend fun getLogForHabitAndDate(habitId: Long, date: LocalDate): DailyLogEntity? =
        dailyLogDao.getForHabitAndDate(habitId, date.toString())

    suspend fun getLogsForRange(start: LocalDate, end: LocalDate): List<DailyLogEntity> =
        dailyLogDao.getForRange(start.toString(), end.toString())

    suspend fun countNonNegotiableMissesInLast7Days(today: LocalDate): Int =
        dailyLogDao.countNonNegotiableMissesInRange(today.minusDays(6).toString(), today.toString())

    suspend fun shouldShowRecalibrationNudge(today: LocalDate): Boolean =
        Leveling.shouldShowRecalibrationNudge(countNonNegotiableMissesInLast7Days(today))

    private suspend fun adjustStatXp(stat: Stat, delta: Int) {
        val current = statProgressDao.get(stat)?.xp ?: 0
        statProgressDao.upsert(StatProgressEntity(stat, (current + delta).coerceAtLeast(0)))
    }

    private suspend fun logEvidence(type: EvidenceType, description: String, sourceId: Long?) {
        evidenceLogDao.insert(
            EvidenceLogEntryEntity(
                type = type,
                description = description,
                dateEpochMillis = System.currentTimeMillis(),
                sourceId = sourceId,
            ),
        )
    }

    // ---- Focus sessions -----------------------------------------------------

    suspend fun recordFocusSession(
        habit: HabitEntity,
        startTimeEpochMillis: Long,
        plannedDurationMinutes: Int,
        actualDurationSeconds: Int,
        completedFully: Boolean,
        rating: DifficultyRating?,
    ): Long {
        val id = focusSessionDao.insert(
            FocusSessionEntity(
                habitId = habit.id,
                startTimeEpochMillis = startTimeEpochMillis,
                plannedDurationMinutes = plannedDurationMinutes,
                actualDurationSeconds = actualDurationSeconds,
                completed = completedFully,
                difficultyRating = rating,
            ),
        )
        if (completedFully) {
            adjustStatXp(habit.stat, Leveling.FOCUS_SESSION_BONUS_XP)
            logEvidence(
                EvidenceType.HARD_FOCUS_SESSION,
                "Completed a $plannedDurationMinutes-min focus session on “${habit.name}”",
                id,
            )
        }
        return id
    }

    suspend fun shouldSuggestRaisingTarget(habitId: Long): Boolean {
        val lastThree = focusSessionDao.lastThreeForHabit(habitId).mapNotNull { it.difficultyRating }
        return Leveling.shouldSuggestRaisingTarget(lastThree)
    }

    // ---- Lies vs Truths -------------------------------------------------------

    suspend fun addLieTruth(lie: String, truth: String): Long =
        lieTruthDao.insert(LieTruthEntity(lieText = lie, truthText = truth, createdAt = System.currentTimeMillis()))

    suspend fun updateLieTruth(entity: LieTruthEntity) = lieTruthDao.update(entity)

    suspend fun randomActiveLieTruth(): LieTruthEntity? = lieTruthDao.randomActive()

    // ---- Weekly review ----------------------------------------------------

    suspend fun generateWeeklyReview(weekStart: LocalDate): WeeklyReviewEntity {
        val weekEnd = weekStart.plusDays(6)
        val logs = dailyLogDao.getForRange(weekStart.toString(), weekEnd.toString())
        val habits = habitDao.getActiveHabits().associateBy { it.id }

        val nonNegLogs = logs.filter { habits[it.habitId]?.isNonNegotiable == true }
        val completionPercent = if (nonNegLogs.isNotEmpty()) {
            nonNegLogs.count { it.completed } * 100f / nonNegLogs.size
        } else {
            0f
        }

        val xpByStat = mutableMapOf<Stat, Int>()
        for (log in logs.filter { it.completed }) {
            val stat = habits[log.habitId]?.stat ?: continue
            xpByStat[stat] = (xpByStat[stat] ?: 0) + log.xpAwarded
        }

        val byDate = nonNegLogs.groupBy { it.date }
        val bestDay = byDate.maxByOrNull { (_, dayLogs) -> dayLogs.count { it.completed } }?.key
        val worstDay = byDate.minByOrNull { (_, dayLogs) -> dayLogs.count { it.completed } }?.key
        val existingNote = weeklyReviewDao.get(weekStart.toString())?.userReflectionNote ?: ""

        val review = WeeklyReviewEntity(
            weekStart = weekStart.toString(),
            weekEnd = weekEnd.toString(),
            completionPercent = completionPercent,
            xpByStat = xpByStat,
            bestDay = bestDay,
            worstDay = worstDay,
            userReflectionNote = existingNote,
        )
        weeklyReviewDao.upsert(review)
        return review
    }

    suspend fun saveWeeklyReflection(weekStart: String, note: String) {
        weeklyReviewDao.get(weekStart)?.let { weeklyReviewDao.upsert(it.copy(userReflectionNote = note)) }
    }

    // ---- Profile / onboarding -----------------------------------------------

    suspend fun saveHunterProfile(profile: HunterProfileEntity) = hunterProfileDao.upsert(profile)

    suspend fun completeOnboarding(hunterName: String) {
        val current = getHunterProfile()
        hunterProfileDao.upsert(current.copy(hunterName = hunterName, onboardingComplete = true))
    }

    /** Wipes every table — used by Settings' "Reset data". Irreversible. */
    suspend fun resetAllData() = withContext(Dispatchers.IO) { db.clearAllTables() }

    // ---- Export -------------------------------------------------------------

    /** A minimal, dependency-free JSON export of everything the app knows.
     * Hand-built rather than pulled from a serialization library, since the
     * shape is small and fully under our control. */
    suspend fun exportAllDataAsJson(): String = withContext(Dispatchers.IO) {
        val habits = habitDao.getActiveHabits()
        val profile = getHunterProfile()
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"hunterName\": ${quote(profile.hunterName)},\n")
        sb.append("  \"habits\": [\n")
        sb.append(
            habits.joinToString(",\n") { h ->
                "    {\"name\": ${quote(h.name)}, \"stat\": \"${h.stat}\", " +
                    "\"nonNegotiable\": ${h.isNonNegotiable}}"
            },
        )
        sb.append("\n  ]\n")
        sb.append("}\n")
        sb.toString()
    }

    private fun quote(value: String): String = "\"" + value.replace("\"", "\\\"") + "\""
}
