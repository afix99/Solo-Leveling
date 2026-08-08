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
import com.ascend.app.data.db.GateRunEntity
import com.ascend.app.data.db.RewardPurchaseEntity
import com.ascend.app.data.db.AiSettingsEntity
import com.ascend.app.data.db.ChatMessageEntity
import com.ascend.app.data.db.CoachAdviceEntity
import com.ascend.app.data.db.ShadowEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.data.db.UnlockedAchievementEntity
import com.ascend.app.data.db.WeeklyReviewEntity
import com.ascend.app.data.ai.CoachContext
import com.ascend.app.data.ai.HabitSnapshot
import com.ascend.app.domain.Achievement
import com.ascend.app.domain.AthleteProfile
import com.ascend.app.domain.Achievements
import com.ascend.app.domain.DailyQuests
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.Economy
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.GateRun
import com.ascend.app.domain.GateStatus
import com.ascend.app.domain.Gates
import com.ascend.app.domain.HunterLoadout
import com.ascend.app.domain.Shadow
import com.ascend.app.domain.Shadows
import com.ascend.app.domain.Skill
import com.ascend.app.domain.StatAllocation
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.HunterStats
import com.ascend.app.domain.ManaConversion
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import com.ascend.app.domain.StatEffects
import androidx.room.withTransaction
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
    private val shadowDao = db.shadowDao()
    private val gateRunDao = db.gateRunDao()
    private val aiSettingsDao = db.aiSettingsDao()
    private val coachAdviceDao = db.coachAdviceDao()
    private val chatMessageDao = db.chatMessageDao()

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
    fun observeShadows(): Flow<List<ShadowEntity>> = shadowDao.observeAll()
    fun observeActiveGate(): Flow<GateRunEntity?> = gateRunDao.observeActive()
    fun observeGateHistory(): Flow<List<GateRunEntity>> = gateRunDao.observeHistory()
    fun observeAiSettings(): Flow<AiSettingsEntity?> = aiSettingsDao.observe()
    fun observeCoachAdvice(): Flow<List<CoachAdviceEntity>> = coachAdviceDao.observeAll()
    fun observeChat(): Flow<List<ChatMessageEntity>> = chatMessageDao.observeAll()

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

    /**
     * Awards XP, gold, evidence and any shadow for a completion, or takes them
     * back on un-completion.
     *
     * Transactional because it writes the log, the stat, the profile balance
     * and possibly a shadow. A crash between those writes would otherwise
     * leave gold awarded for a completion that was never recorded — or, worse,
     * a log with no payout that a re-toggle would then pay out twice.
     */
    suspend fun setHabitCompleted(
        habit: HabitEntity,
        date: LocalDate,
        completed: Boolean,
    ): CompletionResult = db.withTransaction {
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

        val loadout = currentLoadout()

        val newXp = if (completed) {
            loadout.finalXp(habit.isNonNegotiable, isPenaltyQuest, newStreak)
        } else {
            0
        }
        val newGold = if (completed) {
            loadout.finalGold(habit.isNonNegotiable, isPenaltyQuest)
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
            extractShadow(habit)
        }

        if (completed) recordPerfectDayIfEarned(date)

        CompletionResult(
            xp = newXp,
            gold = newGold,
            streak = newStreak,
            streakBonusPercent = loadout.streakBonusPercent(newStreak),
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
    suspend fun runMidnightRollover(rolledDate: LocalDate) = db.withTransaction {
        val dateStr = rolledDate.toString()
        val nextDateStr = rolledDate.plusDays(1).toString()
        val loadout = currentLoadout()

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
            // Second Wind can waive the first miss of a week, so the count of
            // prior misses this week decides whether the penalty lands at all.
            val missesThisWeek = dailyLogDao.countNonNegotiableMissesInRange(
                rolledDate.minusDays(6).toString(),
                rolledDate.minusDays(1).toString(),
            )
            val penalty = loadout.finalPenalty(isFirstMissThisWeek = missesThisWeek == 0)
            if (penalty > 0) {
                statProgressDao.upsert(
                    StatProgressEntity(habit.stat, Leveling.applyPenaltyOf(currentXp, penalty)),
                )
            }

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

    /**
     * Evaluates every day that has ended but never been rolled over, oldest
     * first, and advances any active Gate through the same days.
     *
     * The worker fires just after local midnight, but the OS is free to defer
     * it — Doze, battery optimisation, or the app being force-stopped can all
     * swallow a day entirely. Without this, those days are silently skipped:
     * no penalty, no gate progress, and a Gate that should have failed keeps
     * looking alive. Running from [lastRolloverDate] instead of a hardcoded
     * "yesterday" makes the rollover eventually consistent.
     *
     * Safe to call often — on a normal day it evaluates one date, and on a day
     * already evaluated it does nothing.
     */
    suspend fun runRolloverCatchUp(today: LocalDate) {
        val profile = getHunterProfile()
        val lastRolled = profile.lastRolloverDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val lastDayToEvaluate = today.minusDays(1)

        // A null marker means this install predates the column. Evaluating
        // every day since the user's first log would invent penalties for days
        // they were never warned about, so start from yesterday as before.
        var date = lastRolled?.plusDays(1) ?: lastDayToEvaluate
        if (date.isAfter(lastDayToEvaluate)) return

        // A device clock pushed far forward would otherwise walk years of
        // empty days. Nothing is lost by capping: days with no logs produce
        // penalties the user can do nothing about anyway.
        val earliest = lastDayToEvaluate.minusDays(MAX_CATCH_UP_DAYS - 1)
        if (date.isBefore(earliest)) date = earliest

        while (!date.isAfter(lastDayToEvaluate)) {
            runMidnightRollover(date)
            advanceGateForDay(date)
            date = date.plusDays(1)
        }

        hunterProfileDao.upsert(
            getHunterProfile().copy(lastRolloverDate = lastDayToEvaluate.toString()),
        )
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
            adjustStatXp(habit.stat, currentLoadout().finalFocusBonus())
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
    /** Transactional: gold must never leave the balance without a purchase
     * recorded against it, nor a purchase be recorded unpaid. */
    suspend fun purchaseReward(rewardId: Long): Boolean = db.withTransaction {
        val reward = rewardDao.getById(rewardId) ?: return@withTransaction false
        val profile = getHunterProfile()
        if (profile.gold < reward.goldCost) return@withTransaction false

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
        true
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
    /** Transactional: marking the quest claimed and paying it out must happen
     * together, or a crash between them silently eats the reward. */
    suspend fun claimDailyQuest(date: LocalDate): DailyQuestEntity? = db.withTransaction {
        val quest = dailyQuestDao.getForDate(date.toString()) ?: return@withTransaction null
        if (quest.claimed) return@withTransaction null
        val progress = dailyQuestProgress(date) ?: return@withTransaction null
        if (progress < quest.targetCount) return@withTransaction null

        dailyQuestDao.upsert(quest.copy(claimed = true))
        val multiplier = currentLoadout().dailyQuestMultiplier()
        // Quest XP is split evenly across all five stats so it advances the
        // Hunter Level without distorting any single stat's meaning.
        val perStat = ((quest.xpReward * multiplier) / Stat.entries.size).coerceAtLeast(1)
        Stat.entries.forEach { adjustStatXp(it, perStat) }
        addGold(quest.goldReward * multiplier)
        quest
    }

    // ---- Loadout: the single place every payout modifier is resolved -------

    /** Assembles the current stat levels, allocations, skills, shadows and
     * class into one value. Every payout calculation goes through this. */
    suspend fun currentLoadout(): HunterLoadout {
        val profile = getHunterProfile()
        return HunterLoadout(
            statLevels = statLevels(),
            allocatedPoints = profile.allocatedPoints,
            unlockedSkills = profile.unlockedSkills,
            shadows = shadowDao.getAll().map { it.toDomain() },
            hunterClass = profile.hunterClass,
        )
    }

    private fun ShadowEntity.toDomain() = Shadow(
        id = id,
        name = name,
        habitId = habitId,
        stat = stat,
        extractedAtEpochMillis = extractedAtEpochMillis,
        rank = rank,
    )

    suspend fun hunterLevel(): Int =
        Leveling.hunterLevelForTotalXp(statProgressDao.getAll().sumOf { it.xp })

    suspend fun currentRank(): com.ascend.app.domain.Rank =
        Leveling.rankForTotalXp(statProgressDao.getAll().sumOf { it.xp })

    // ---- Stat point allocation --------------------------------------------

    suspend fun allocateStatPoint(stat: Stat): Boolean {
        val profile = getHunterProfile()
        val level = hunterLevel()
        if (!StatAllocation.canAllocate(level, profile.allocatedPoints)) return false
        hunterProfileDao.upsert(
            profile.copy(
                allocatedPoints = StatAllocation.allocate(stat, level, profile.allocatedPoints),
            ),
        )
        return true
    }

    /** Refunds every allocated point for a gold fee, so a build is a decision
     * you can revise but not one you can churn for free. */
    suspend fun respecStatPoints(): Boolean {
        val profile = getHunterProfile()
        if (profile.allocatedPoints.isEmpty()) return false
        if (profile.gold < StatAllocation.RESPEC_GOLD_COST) return false
        hunterProfileDao.upsert(
            profile.copy(
                allocatedPoints = StatAllocation.respec(),
                gold = profile.gold - StatAllocation.RESPEC_GOLD_COST,
            ),
        )
        return true
    }

    // ---- Skills ------------------------------------------------------------

    suspend fun unlockSkill(skill: Skill): Boolean {
        val profile = getHunterProfile()
        val level = hunterLevel()
        val rank = currentRank()
        if (!Skill.canUnlock(skill, level, rank, profile.unlockedSkills)) return false
        hunterProfileDao.upsert(profile.copy(unlockedSkills = profile.unlockedSkills + skill))
        return true
    }

    // ---- Mana Conversion ----------------------------------------------------

    /** XP already converted today, resetting when the date rolls over. */
    suspend fun manaXpConvertedToday(today: LocalDate): Int {
        val profile = getHunterProfile()
        return if (profile.manaConvertedDate == today.toString()) profile.manaConvertedXpToday else 0
    }

    /**
     * Burns gold for XP in one stat, the trade [Skill.MANA_CONVERSION] unlocks.
     *
     * Charges only for the XP actually granted: the daily cap can cut a stake
     * short, and billing the full amount would take gold for XP never given.
     * Returns the XP granted, or 0 if the trade could not happen at all.
     */
    suspend fun convertGoldToXp(stat: Stat, goldOffered: Int, today: LocalDate): Int =
        db.withTransaction {
            val profile = getHunterProfile()
            if (Skill.MANA_CONVERSION !in profile.unlockedSkills) return@withTransaction 0

            val convertedToday =
                if (profile.manaConvertedDate == today.toString()) profile.manaConvertedXpToday else 0
            val affordable = goldOffered.coerceAtMost(profile.gold)
            val xp = ManaConversion.xpFor(affordable, convertedToday)
            if (xp <= 0) return@withTransaction 0

            val goldSpent = ManaConversion.goldFor(xp)
            hunterProfileDao.upsert(
                profile.copy(
                    gold = profile.gold - goldSpent,
                    manaConvertedDate = today.toString(),
                    manaConvertedXpToday = convertedToday + xp,
                ),
            )
            adjustStatXp(stat, xp)
            // Deliberately not logged to the Evidence Log: that log exists to
            // show the user proof they did the work, and spending gold is not
            // evidence of anything they did.
            xp
        }

    suspend fun skillPointsAvailable(): Int =
        Skill.pointsAvailable(hunterLevel(), currentRank(), getHunterProfile().unlockedSkills)

    // ---- Shadows -----------------------------------------------------------

    /** Extracting from a habit you already command promotes that Shadow rather
     * than adding a duplicate, so the army stays as short as your habit list. */
    private suspend fun extractShadow(habit: HabitEntity) {
        val existing = shadowDao.forHabit(habit.id)
        if (existing != null) {
            shadowDao.update(existing.copy(rank = existing.rank + 1))
        } else {
            shadowDao.insert(
                ShadowEntity(
                    name = Shadows.nameFor(habit.name),
                    habitId = habit.id,
                    stat = habit.stat,
                    extractedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    // ---- Gates --------------------------------------------------------------

    private fun GateRunEntity.toDomain() = GateRun(
        id = id,
        rank = rank,
        startDate = LocalDate.parse(startDate),
        daysCleared = daysCleared,
        status = status,
    )

    suspend fun activeGate(): GateRun? = gateRunDao.getActive()?.toDomain()

    /**
     * Pays the stake up front — that is what gives the run its weight.
     *
     * Transactional: this is the costliest write in the app. Deducting the
     * stake without inserting the run would take the user's gold and give
     * them nothing to show for it.
     */
    suspend fun enterGate(rank: GateRank, today: LocalDate): Boolean = db.withTransaction {
        val profile = getHunterProfile()
        if (!Gates.canEnter(activeGate(), rank, hunterLevel(), profile.gold)) {
            return@withTransaction false
        }

        hunterProfileDao.upsert(profile.copy(gold = profile.gold - rank.stake))
        gateRunDao.insert(
            GateRunEntity(
                rank = rank,
                startDate = today.toString(),
                stakePaid = rank.stake,
                lastEvaluatedDate = today.minusDays(1).toString(),
            ),
        )
        true
    }

    /** Abandoning forfeits the stake; the gold is already spent either way. */
    suspend fun abandonGate(): Boolean {
        val active = gateRunDao.getActive() ?: return false
        gateRunDao.update(active.copy(status = GateStatus.FAILED))
        return true
    }

    /**
     * Advances the active run for a finished day. Guarded by lastEvaluatedDate
     * so a rollover that fires twice can't double-count or wrongly fail a run.
     */
    suspend fun advanceGateForDay(rolledDate: LocalDate): GateRun? = db.withTransaction {
        val entity = gateRunDao.getActive() ?: return@withTransaction null
        if (entity.lastEvaluatedDate == rolledDate.toString()) {
            return@withTransaction entity.toDomain()
        }
        if (LocalDate.parse(entity.startDate).isAfter(rolledDate)) {
            return@withTransaction entity.toDomain()
        }

        val nonNegotiables = habitDao.getActiveHabits().filter { it.isNonNegotiable }
        val completed = dailyLogDao.completedHabitIdsOn(rolledDate.toString()).toSet()
        val perfect = nonNegotiables.isNotEmpty() && nonNegotiables.all { it.id in completed }

        val advanced = Gates.advance(entity.toDomain(), perfect)
        gateRunDao.update(
            entity.copy(
                daysCleared = advanced.daysCleared,
                status = advanced.status,
                lastEvaluatedDate = rolledDate.toString(),
            ),
        )

        if (advanced.status == GateStatus.CLEARED) {
            addGold(Gates.goldPayout(advanced))
            val perStat = (Gates.xpPayout(advanced) / Stat.entries.size).coerceAtLeast(1)
            Stat.entries.forEach { adjustStatXp(it, perStat) }
            logEvidence(
                EvidenceType.STREAK_MILESTONE,
                "Cleared a ${advanced.rank.displayName}",
                advanced.id,
            )
        }
        advanced
    }

    /** Wipes every table — used by Settings' "Reset data". Irreversible. */
    // ---- AI coach -----------------------------------------------------------

    suspend fun getAiSettings(): AiSettingsEntity = aiSettingsDao.get() ?: AiSettingsEntity()

    suspend fun saveAiSettings(settings: AiSettingsEntity) = aiSettingsDao.upsert(settings)

    suspend fun saveAdvice(type: String, content: String, model: String): Long =
        coachAdviceDao.insert(
            CoachAdviceEntity(
                adviceType = type,
                content = content,
                model = model,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )

    suspend fun deleteAdvice(id: Long) = coachAdviceDao.delete(id)

    suspend fun addChatMessage(role: String, content: String): Long =
        chatMessageDao.insert(
            ChatMessageEntity(
                role = role,
                content = content,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )

    /** Recent turns, oldest first. Capped so a long conversation can't grow
     * past the model's context window or quietly inflate cost. */
    suspend fun recentChat(limit: Int = 20): List<ChatMessageEntity> =
        chatMessageDao.recent(limit).sortedBy { it.createdAtEpochMillis }

    suspend fun clearChat() = chatMessageDao.clear()

    /**
     * Assembles the real numbers the coach reasons about. Nothing here is
     * invented — every field comes from what has actually been logged, which is
     * the difference between advice and a horoscope.
     */
    suspend fun buildCoachContext(today: LocalDate): CoachContext {
        val profile = getHunterProfile()
        val habits = habitDao.getActiveHabits()
        val windowStart = today.minusDays(29)
        val logs = dailyLogDao.getForRange(windowStart.toString(), today.toString())
        val settings = getAiSettings()

        val logsByHabit = logs.groupBy { it.habitId }
        val snapshots = habits.map { habit ->
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val completed = habitLogs.count { it.completed }
            // Measured against days the habit actually has entries for, so one
            // added yesterday doesn't read as 3% adherence.
            val trackedDays = maxOf(habitLogs.size, 1)
            HabitSnapshot(
                name = habit.name,
                stat = habit.stat,
                isNonNegotiable = habit.isNonNegotiable,
                completionRate = (completed * 100) / trackedDays,
                currentStreak = dailyLogDao.longestStreakForHabit(habit.id),
                focusMinutes = habit.targetDurationMinutes.takeIf { habit.isFocusEnabled },
            )
        }

        val nonNegLogs = logs.filter { log -> habits.find { it.id == log.habitId }?.isNonNegotiable == true }
        val completionRate = if (nonNegLogs.isNotEmpty()) {
            (nonNegLogs.count { it.completed } * 100) / nonNegLogs.size
        } else {
            0
        }

        val totalXp = statProgressDao.getAll().sumOf { it.xp }
        return CoachContext(
            hunterName = profile.hunterName,
            hunterLevel = Leveling.hunterLevelForTotalXp(totalXp),
            rank = Leveling.rankForTotalXp(totalXp),
            statLevels = statLevels(),
            habits = snapshots,
            perfectDays = profile.perfectDays,
            longestStreak = dailyLogDao.longestStreak(),
            focusSessions = focusSessionDao.completedCount(),
            focusMinutes = focusSessionDao.completedMinutesTotal(),
            missesLast30Days = nonNegLogs.count { !it.completed },
            completionRateLast30 = completionRate,
            athlete = AthleteProfile(
                age = settings.age,
                sex = settings.sex,
                heightCm = settings.heightCm,
                weightKg = settings.weightKg,
                goal = settings.goal,
                experience = settings.experience,
                equipment = settings.equipment,
                dietaryNotes = settings.dietaryNotes,
                injuries = settings.injuries,
            ),
        )
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) { db.clearAllTables() }

    // ---- Export -------------------------------------------------------------

    /**
     * A minimal, dependency-free JSON summary of the setup. Hand-built rather
     * than pulled from a serialization library, since the shape is small and
     * fully under our control.
     *
     * Note what this is not: it carries the hunter name and habit list, but no
     * XP, gold, streaks or logs, and nothing reads it back in. It is a record
     * of how the app was configured, not a backup that can restore progress.
     * The envelope carries the schema version so that if an import is ever
     * built, it can tell which shape it is looking at instead of guessing.
     */
    suspend fun exportAllDataAsJson(): String = withContext(Dispatchers.IO) {
        val habits = habitDao.getActiveHabits()
        val profile = getHunterProfile()
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"ascendExportVersion\": $EXPORT_VERSION,\n")
        sb.append("  \"schemaVersion\": ${AscendDatabase.SCHEMA_VERSION},\n")
        sb.append("  \"exportedAt\": ${quote(java.time.Instant.now().toString())},\n")
        sb.append("  \"contains\": \"setup only — no progress, streaks or history\",\n")
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

    companion object {
        /** Upper bound on how many days one catch-up will walk. */
        const val MAX_CATCH_UP_DAYS = 30L

        /** Shape of the JSON export. Bump when the fields change. */
        const val EXPORT_VERSION = 1
    }
}
