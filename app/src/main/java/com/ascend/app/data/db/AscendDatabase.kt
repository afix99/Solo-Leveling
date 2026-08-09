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
        CloudSettingsEntity::class,
    ],
    version = 7,
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
    abstract fun cloudSettingsDao(): CloudSettingsDao

    companion object {
        /** Mirrors the @Database version so other layers can record it. */
        const val SCHEMA_VERSION = 7

        /**
         * The migrations, built from [MigrationSql].
         *
         * Each is a thin executor over a published list of statements, so the
         * SQL that runs on a device is byte-for-byte the SQL a JVM test runs
         * against a real SQLite engine. Adding a version means appending a
         * list there and bumping the @Database version here — there is no
         * second place to keep in sync.
         */
        private val MIGRATIONS: Array<Migration> = MigrationSql.ALL
            .map { (from, to, statements) ->
                object : Migration(from, to) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        statements.forEach(db::execSQL)
                    }
                }
            }
            .toTypedArray()

        @Volatile private var instance: AscendDatabase? = null

        fun getInstance(context: Context): AscendDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AscendDatabase::class.java,
                    "ascend.db",
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}
