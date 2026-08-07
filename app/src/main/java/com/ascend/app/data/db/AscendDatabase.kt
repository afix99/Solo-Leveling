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
    ],
    version = 2,
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

    companion object {
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

        @Volatile private var instance: AscendDatabase? = null

        fun getInstance(context: Context): AscendDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AscendDatabase::class.java,
                    "ascend.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
