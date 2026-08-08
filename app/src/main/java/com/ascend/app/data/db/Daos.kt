package com.ascend.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ascend.app.domain.Stat
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    suspend fun getActiveHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: Long): Flow<HabitEntity?>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getForHabitAndDate(habitId: Long, date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun observeForHabit(habitId: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :start AND :end")
    suspend fun getForRange(start: String, end: String): List<DailyLogEntity>

    @Query(
        "SELECT COUNT(*) FROM daily_logs WHERE habitId = :habitId AND completed = 0 " +
            "AND date BETWEEN :start AND :end",
    )
    suspend fun countMissesInRange(habitId: Long, start: String, end: String): Int

    @Query(
        "SELECT COUNT(*) FROM daily_logs dl JOIN habits h ON dl.habitId = h.id " +
            "WHERE h.isNonNegotiable = 1 AND dl.completed = 0 AND dl.date BETWEEN :start AND :end",
    )
    suspend fun countNonNegotiableMissesInRange(start: String, end: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyLogEntity): Long

    @Query("SELECT COUNT(*) FROM daily_logs WHERE completed = 1")
    suspend fun totalCompletions(): Int

    @Query("SELECT COALESCE(MAX(streakAtCompletion), 0) FROM daily_logs WHERE completed = 1")
    suspend fun longestStreak(): Int

    @Query("SELECT COALESCE(MAX(streakAtCompletion), 0) FROM daily_logs WHERE habitId = :habitId AND completed = 1")
    suspend fun longestStreakForHabit(habitId: Long): Int

    @Query("SELECT COUNT(*) FROM daily_logs WHERE completed = 1 AND isPenaltyQuest = 1")
    suspend fun penaltyQuestsRedeemed(): Int

    @Query("SELECT habitId FROM daily_logs WHERE date = :date AND completed = 1")
    suspend fun completedHabitIdsOn(date: String): List<Long>
}

@Dao
interface StatProgressDao {
    @Query("SELECT * FROM stat_progress")
    fun observeAll(): Flow<List<StatProgressEntity>>

    @Query("SELECT * FROM stat_progress")
    suspend fun getAll(): List<StatProgressEntity>

    @Query("SELECT * FROM stat_progress WHERE stat = :stat")
    suspend fun get(stat: Stat): StatProgressEntity?

    @Query("SELECT COALESCE(SUM(xp), 0) FROM stat_progress")
    fun observeTotalXp(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StatProgressEntity)
}

@Dao
interface HunterProfileDao {
    @Query("SELECT * FROM hunter_profile WHERE id = ${HunterProfileEntity.SINGLETON_ID}")
    fun observe(): Flow<HunterProfileEntity?>

    @Query("SELECT * FROM hunter_profile WHERE id = ${HunterProfileEntity.SINGLETON_ID}")
    suspend fun get(): HunterProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HunterProfileEntity)
}

@Dao
interface LieTruthDao {
    @Query("SELECT * FROM lies_truths ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LieTruthEntity>>

    @Query("SELECT * FROM lies_truths WHERE active = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun randomActive(): LieTruthEntity?

    @Insert
    suspend fun insert(entity: LieTruthEntity): Long

    @Update
    suspend fun update(entity: LieTruthEntity)
}

@Dao
interface WeeklyReviewDao {
    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_reviews WHERE weekStart = :weekStart LIMIT 1")
    suspend fun get(weekStart: String): WeeklyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeeklyReviewEntity)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE habitId = :habitId ORDER BY startTimeEpochMillis DESC LIMIT 3")
    suspend fun lastThreeForHabit(habitId: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeEpochMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    suspend fun completedCount(): Int

    @Query("SELECT COALESCE(SUM(actualDurationSeconds), 0) / 60 FROM focus_sessions WHERE completed = 1")
    suspend fun completedMinutesTotal(): Int

    @Query(
        "SELECT COUNT(*) FROM focus_sessions WHERE completed = 1 " +
            "AND startTimeEpochMillis BETWEEN :start AND :end",
    )
    suspend fun completedCountBetween(start: Long, end: Long): Int

    @Insert
    suspend fun insert(entity: FocusSessionEntity): Long

    @Update
    suspend fun update(entity: FocusSessionEntity)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE archived = 0 ORDER BY goldCost ASC")
    fun observeActive(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getById(id: Long): RewardEntity?

    @Insert
    suspend fun insert(entity: RewardEntity): Long

    @Update
    suspend fun update(entity: RewardEntity)
}

@Dao
interface RewardPurchaseDao {
    @Query("SELECT * FROM reward_purchases ORDER BY purchasedAtEpochMillis DESC")
    fun observeAll(): Flow<List<RewardPurchaseEntity>>

    @Insert
    suspend fun insert(entity: RewardPurchaseEntity): Long
}

@Dao
interface UnlockedAchievementDao {
    @Query("SELECT * FROM unlocked_achievements ORDER BY unlockedAtEpochMillis DESC")
    fun observeAll(): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT achievementId FROM unlocked_achievements")
    suspend fun unlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: UnlockedAchievementEntity)
}

@Dao
interface DailyQuestDao {
    @Query("SELECT * FROM daily_quests WHERE date = :date LIMIT 1")
    fun observeForDate(date: String): Flow<DailyQuestEntity?>

    @Query("SELECT * FROM daily_quests WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): DailyQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyQuestEntity)
}

@Dao
interface EvidenceLogDao {
    @Query("SELECT * FROM evidence_log ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<EvidenceLogEntryEntity>>

    @Query("SELECT * FROM evidence_log WHERE dateEpochMillis BETWEEN :start AND :end ORDER BY dateEpochMillis DESC")
    suspend fun getForRange(start: Long, end: Long): List<EvidenceLogEntryEntity>

    @Insert
    suspend fun insert(entity: EvidenceLogEntryEntity): Long
}

@Dao
interface ShadowDao {
    @Query("SELECT * FROM shadows ORDER BY rank DESC, extractedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ShadowEntity>>

    @Query("SELECT * FROM shadows")
    suspend fun getAll(): List<ShadowEntity>

    @Query("SELECT * FROM shadows WHERE habitId = :habitId LIMIT 1")
    suspend fun forHabit(habitId: Long): ShadowEntity?

    @Insert
    suspend fun insert(entity: ShadowEntity): Long

    @Update
    suspend fun update(entity: ShadowEntity)
}

@Dao
interface GateRunDao {
    @Query("SELECT * FROM gate_runs WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<GateRunEntity?>

    @Query("SELECT * FROM gate_runs WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): GateRunEntity?

    @Query("SELECT * FROM gate_runs ORDER BY id DESC LIMIT 20")
    fun observeHistory(): Flow<List<GateRunEntity>>

    @Insert
    suspend fun insert(entity: GateRunEntity): Long

    @Update
    suspend fun update(entity: GateRunEntity)
}
