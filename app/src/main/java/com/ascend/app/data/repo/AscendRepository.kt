package com.ascend.app.data.repo

import com.ascend.app.data.db.AscendDatabase
import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.DailyQuestEntity
import com.ascend.app.data.db.EvidenceLogEntryEntity
import com.ascend.app.data.db.FocusSessionEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.db.RewardEntity
import com.ascend.app.data.db.RewardPurchaseEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.data.db.UnlockedAchievementEntity
import com.ascend.app.data.db.WeeklyReviewEntity
import com.ascend.app.domain.Achievement
import com.ascend.app.domain.Achievements
import com.ascend.app.domain.DailyQuests
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.Economy
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.HunterStats
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import com.ascend.app.domain.StatEffects
import java.time.LocalDate
import java.time.ZoneId
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
    private val rewardDao = db.rewardDao()
    private val rewardPurchaseDao = db.rewardPurchaseDao()
    private val unlockedAchievementDao = db.unlockedAchievementDao()
    private val dailyQuestDao = db.dailyQuestDao()

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
    fun observeRewards(): Flow<List<RewardEntity>> = rewardDao.observeActive()
    fun observePurchases(): Flow<List<RewardPurchaseEntity>> = rewardPurchaseDao.observeAll()
    fun observeUnlockedAchievements(): Flow<List<UnlockedAchievementEntity>> =
        unlockedAchievementDao.observeAll()
    fun observeDailyQuest(date: LocalDate): Flow<DailyQuestEntity?> =
        dailyQuestDao.observeForDate(date.toString())

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
    /** What a completion actually paid out, so the UI can show it. */
    data class CompletionResult(val xp: Int, val gold: Int, val streak: Int, val streakBonusPercent: Int)

    suspend fun setHabitCompleted(
        habit: HabitEntity,
        date: LocalDate,
        completed: Boolean,
    ): CompletionResult {
        val dateStr = date.toString()
        val existing = dailyLogDao.getForHabitAndDate(habit.id, dateStr)
        val isPenaltyQuest = existing?.isPenaltyQuest ?: false
        val previousXp = existing?.xpAwarded ?: 0
        val previousGold = existing?.goldAwarded ?: 0

        val newStreak = if (completed) {
            val yesterday = dailyLogDao.getForHabitAndDate(habit.id, date.minusDays(1).toString())
            val prevStreak = if (yesterday?.completed == true) yesterday.streakAtCompletion else 0
            prevStreak + 1
        } else {
            0
        }

        val profile = getHunterProfile()
        val levels = statLevels()
        val baseXp = Leveling.xpForCompletion(habit.isNonNegotiable, isPenaltyQuest)
        val baseGold = Economy.goldForCompletion(habit.isNonNegotiable, isPenaltyQuest)

        val newXp = if (completed) {
            StatEffects.finalXp(
                baseXp = baseXp,
                mindLevel = levels.getValue(Stat.INT),
                streakDays = newStreak,
                movementLevel = levels.getValue(Stat.AGI),
                classXpBonusPercent = profile.hunterClass.xpBonusPercent,
            )
        } else {
            0
        }
        val newGold = if (completed) {
            StatEffects.finalGold(
                baseGold = baseGold,
                bodyLevel = levels.getValue(Stat.STR),
                classGoldBonusPercent = profile.hunterClass.goldBonusPercent,
            )
        } else {
            0
        }

        dailyLogDao.upsert(
            DailyLogEntity(
                id = existing?.id ?: 0,
                habitId = habit.id,
                date = dateStr,
                completed = completed,
                xpAwarded = newXp,
                goldAwarded = newGold,
                isPenaltyQuest = isPenaltyQuest,
                streakAtCompletion = newStreak,
            ),
        )

        val xpDelta = newXp - previousXp
        if (xpDelta != 0) adjustStatXp(habit.stat, xpDelta)
        val goldDelta = newGold - previousGold
        if (goldDelta != 0) addGold(goldDelta)

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

        if (completed) recordPerfectDayIfEarned(date)

        return CompletionResult(
            xp = newXp,
            gold = newGold,
            streak = newStreak,
            streakBonusPercent = StatEffects.streakBonusPercent(newStreak, levels.getValue(Stat.AGI)),
        )
    }

    /** Current level of every stat, defaulting to 1 for untouched stats. */
    suspend fun statLevels(): Map<Stat, Int> {
        val rows = statProgressDao.getAll().associate { it.stat to Leveling.levelForXp(it.xp) }
        return Stat.entries.associateWith { rows[it] ?: 1 }
    }

    private suspend fun addGold(delta: Int) {
        val profile = getHunterProfile()
        hunterProfileDao.upsert(
            profile.copy(
                gold = (profile.gold + delta).coerceAtLeast(0),
                goldEarnedTotal = profile.goldEarnedTotal + delta.coerceAtLeast(0),
            ),
        )
    }

    /**
     * Bumps the perfect-day counter the first time a day is fully cleared.
     * [HunterProfileEntity.lastPerfectDate] guards against a toggle-off/on
     * farming the counter, without polluting the Evidence Log.
     */
    private suspend fun recordPerfectDayIfEarned(date: LocalDate) {
        val nonNegotiables = habitDao.getActiveHabits().filter { it.isNonNegotiable }
        if (nonNegotiables.isEmpty()) return
        val completed = dailyLogDao.completedHabitIdsOn(date.toString()).toSet()
        if (!nonNegotiables.all { it.id in completed }) return

        val profile = getHunterProfile()
        if (profile.lastPerfectDate == date.toString()) return

        hunterProfileDao.upsert(
            profile.copy(
                perfectDays = profile.perfectDays + 1,
                lastPerfectDate = date.toString(),
            ),
        )
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
            val penalty = StatEffects.finalPenalty(
                basePenalty = Leveling.PENALTY_XP_LOSS,
                healthLevel = statLevels().getValue(Stat.VIT),
                classPenaltyResistPercent = getHunterProfile().hunterClass.penaltyResistPercent,
            )
            statProgressDao.upsert(
                StatProgressEntity(habit.stat, Leveling.applyPenaltyOf(currentXp, penalty)),
            )

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
            val bonus = StatEffects.finalFocusBonus(
                baseBonus = Leveling.FOCUS_SESSION_BONUS_XP,
                focusLevel = statLevels().getValue(Stat.PER),
                classFocusBonusPercent = getHunterProfile().hunterClass.focusBonusPercent,
            )
            adjustStatXp(habit.stat, bonus)
            addGold(Economy.FOCUS_SESSION_GOLD)
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

    // ---- Rewards shop -------------------------------------------------------

    suspend fun createReward(name: String, goldCost: Int): Long =
        rewardDao.insert(RewardEntity(name = name, goldCost = goldCost, createdAt = System.currentTimeMillis()))

    suspend fun archiveReward(rewardId: Long) {
        rewardDao.getById(rewardId)?.let { rewardDao.update(it.copy(archived = true)) }
    }

    /** Spends gold on a reward. Returns false (and changes nothing) if the
     * balance can't cover it. */
    suspend fun purchaseReward(rewardId: Long): Boolean {
        val reward = rewardDao.getById(rewardId) ?: return false
        val profile = getHunterProfile()
        if (profile.gold < reward.goldCost) return false

        hunterProfileDao.upsert(profile.copy(gold = profile.gold - reward.goldCost))
        rewardDao.update(reward.copy(timesPurchased = reward.timesPurchased + 1))
        rewardPurchaseDao.insert(
            RewardPurchaseEntity(
                rewardId = reward.id,
                rewardName = reward.name,
                goldSpent = reward.goldCost,
                purchasedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return true
    }

    // ---- Achievements & titles ------------------------------------------------

    /** Gathers everything the achievement conditions need to evaluate. */
    suspend fun currentHunterStats(): HunterStats {
        val profile = getHunterProfile()
        val totalXp = statProgressDao.getAll().sumOf { it.xp }
        return HunterStats(
            totalCompletions = dailyLogDao.totalCompletions(),
            longestStreak = dailyLogDao.longestStreak(),
            penaltyQuestsRedeemed = dailyLogDao.penaltyQuestsRedeemed(),
            focusSessionsCompleted = focusSessionDao.completedCount(),
            focusMinutesTotal = focusSessionDao.completedMinutesTotal(),
            hunterLevel = Leveling.hunterLevelForTotalXp(totalXp),
            rank = Leveling.rankForTotalXp(totalXp),
            perfectDays = profile.perfectDays,
            perfectWeeks = profile.perfectWeeks,
            goldEarnedTotal = profile.goldEarnedTotal,
            maxStatLevel = statLevels().values.maxOrNull() ?: 1,
        )
    }

    /**
     * Evaluates every achievement, banking gold and recording unlocks for any
     * newly satisfied ones. Returns them so the UI can announce them.
     */
    suspend fun checkAchievements(): List<Achievement> {
        val unlocked = unlockedAchievementDao.unlockedIds().toSet()
        val newly = Achievements.newlyUnlocked(currentHunterStats(), unlocked)
        if (newly.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        for (achievement in newly) {
            unlockedAchievementDao.insert(UnlockedAchievementEntity(achievement.id, now))
            addGold(achievement.goldReward)
        }
        return newly
    }

    suspend fun equipTitle(titleId: String?) {
        hunterProfileDao.upsert(getHunterProfile().copy(equippedTitleId = titleId))
    }

    suspend fun setHunterClass(hunterClass: HunterClass) {
        hunterProfileDao.upsert(getHunterProfile().copy(hunterClass = hunterClass))
    }

    // ---- Daily quest ------------------------------------------------------------

    /** Issues today's quest if one hasn't been generated yet. */
    suspend fun ensureDailyQuest(date: LocalDate): DailyQuestEntity? {
        dailyQuestDao.getForDate(date.toString())?.let { return it }

        val habits = habitDao.getActiveHabits()
        val byId = habits.associateBy { it.id }
        val generated = DailyQuests.generate(
            dateSeed = date.toEpochDay(),
            nonNegotiables = habits.filter { it.isNonNegotiable }.map { it.id },
            optionalHabits = habits.filterNot { it.isNonNegotiable }.map { it.id },
            focusHabits = habits.filter { it.isFocusEnabled }.map { it.id },
            habitNameOf = { id -> byId[id]?.name ?: "a habit" },
        ) ?: return null

        val entity = DailyQuestEntity(
            date = date.toString(),
            kind = generated.kind,
            description = generated.description,
            targetCount = generated.targetCount,
            targetHabitId = generated.targetHabitId,
            xpReward = generated.xpReward,
            goldReward = generated.goldReward,
        )
        dailyQuestDao.upsert(entity)
        return entity
    }

    /** Current progress on today's quest, or null if there isn't one. */
    suspend fun dailyQuestProgress(date: LocalDate): Int? {
        val quest = dailyQuestDao.getForDate(date.toString()) ?: return null
        val habits = habitDao.getActiveHabits()
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return DailyQuests.progress(
            kind = quest.kind,
            targetHabitId = quest.targetHabitId,
            targetCount = quest.targetCount,
            completedHabitIds = dailyLogDao.completedHabitIdsOn(date.toString()).toSet(),
            nonNegotiables = habits.filter { it.isNonNegotiable }.map { it.id },
            optionalHabits = habits.filterNot { it.isNonNegotiable }.map { it.id },
            focusSessionsToday = focusSessionDao.completedCountBetween(startOfDay, endOfDay),
        )
    }

    /** Pays out today's quest if it's complete and hasn't been claimed. */
    suspend fun claimDailyQuest(date: LocalDate): DailyQuestEntity? {
        val quest = dailyQuestDao.getForDate(date.toString()) ?: return null
        if (quest.claimed) return null
        val progress = dailyQuestProgress(date) ?: return null
        if (progress < quest.targetCount) return null

        dailyQuestDao.upsert(quest.copy(claimed = true))
        // Quest XP is split evenly across all five stats so it advances the
        // Hunter Level without distorting any single stat's meaning.
        val perStat = (quest.xpReward / Stat.entries.size).coerceAtLeast(1)
        Stat.entries.forEach { adjustStatXp(it, perStat) }
        addGold(quest.goldReward)
        return quest
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
