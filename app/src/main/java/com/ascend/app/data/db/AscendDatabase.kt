package com.ascend.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        DailyLogEntity::class,
        StatProgressEntity::class,
        HunterProfileEntity::class,
        LieTruthEntity::class,
        WeeklyReviewEntity::class,
        FocusSessionEntity::class,
        EvidenceLogEntryEntity::class,
        RewardEntity::class,
        RewardPurchaseEntity::class,
        UnlockedAchievementEntity::class,
        DailyQuestEntity::class,
        ShadowEntity::class,
        GateRunEntity::class,
        AiSettingsEntity::class,
        CoachAdviceEntity::class,
        ChatMessageEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun statProgressDao(): StatProgressDao
    abstract fun hunterProfileDao(): HunterProfileDao
    abstract fun lieTruthDao(): LieTruthDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun evidenceLogDao(): EvidenceLogDao
    abstract fun rewardDao(): RewardDao
    abstract fun rewardPurchaseDao(): RewardPurchaseDao
    abstract fun unlockedAchievementDao(): UnlockedAchievementDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun shadowDao(): ShadowDao
    abstract fun gateRunDao(): GateRunDao
    abstract fun aiSettingsDao(): AiSettingsDao
    abstract fun coachAdviceDao(): CoachAdviceDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        /** Mirrors the @Database version so other layers can record it. */
        const val SCHEMA_VERSION = 6

        /**
         * v2 adds the economy layer: Gold, titles, classes, rewards,
         * achievements and daily quests. Written as a real migration rather
         * than a destructive fallback so nobody loses their streaks.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN gold INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN goldEarnedTotal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN equippedTitleId TEXT")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN hunterClass TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN perfectDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN perfectWeeks INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN lastPerfectDate TEXT")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN goldAwarded INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rewards (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        goldCost INTEGER NOT NULL,
                        timesPurchased INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        archived INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_purchases (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        rewardId INTEGER NOT NULL,
                        rewardName TEXT NOT NULL,
                        goldSpent INTEGER NOT NULL,
                        purchasedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS unlocked_achievements (
                        achievementId TEXT NOT NULL PRIMARY KEY,
                        unlockedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_quests (
                        date TEXT NOT NULL PRIMARY KEY,
                        kind TEXT NOT NULL,
                        description TEXT NOT NULL,
                        targetCount INTEGER NOT NULL,
                        targetHabitId INTEGER,
                        xpReward INTEGER NOT NULL,
                        goldReward INTEGER NOT NULL,
                        claimed INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v3 adds the choice layer: manually allocated stat points, unlocked
         * skills, extracted Shadows and Gate runs. Written as a real migration
         * so nobody loses streaks, gold or achievements.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN allocatedPoints TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN unlockedSkills TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shadows (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        habitId INTEGER NOT NULL,
                        stat TEXT NOT NULL,
                        extractedAtEpochMillis INTEGER NOT NULL,
                        rank INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gate_runs (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        rank TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        daysCleared INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        stakePaid INTEGER NOT NULL,
                        lastEvaluatedDate TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v4 adds the AI coach: provider settings, the optional athlete profile,
         * and cached advice so past answers stay readable offline.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ai_settings (
                        id INTEGER NOT NULL PRIMARY KEY,
                        provider TEXT NOT NULL DEFAULT 'OPENROUTER',
                        apiKey TEXT NOT NULL DEFAULT '',
                        model TEXT NOT NULL DEFAULT '',
                        age INTEGER,
                        sex TEXT,
                        heightCm INTEGER,
                        weightKg INTEGER,
                        goal TEXT,
                        experience TEXT,
                        equipment TEXT,
                        dietaryNotes TEXT,
                        injuries TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS coach_advice (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        adviceType TEXT NOT NULL,
                        content TEXT NOT NULL,
                        model TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** v5 adds the System chat transcript. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_settings ADD COLUMN systemVoice INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Records how far the midnight rollover has actually got. Existing
         * users start at null, which the catch-up treats as "only evaluate
         * yesterday" — the old behaviour — so upgrading never retroactively
         * penalises days that passed before this column existed.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN lastRolloverDate TEXT")
                db.execSQL("ALTER TABLE hunter_profile ADD COLUMN manaConvertedDate TEXT")
                db.execSQL(
                    "ALTER TABLE hunter_profile ADD COLUMN manaConvertedXpToday INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile private var instance: AscendDatabase? = null

        fun getInstance(context: Context): AscendDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AscendDatabase::class.java,
                    "ascend.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { instance = it }
            }
    }
}
