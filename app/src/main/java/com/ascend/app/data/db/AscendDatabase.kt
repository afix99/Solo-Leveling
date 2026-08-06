package com.ascend.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
    ],
    version = 1,
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

    companion object {
        @Volatile private var instance: AscendDatabase? = null

        fun getInstance(context: Context): AscendDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AscendDatabase::class.java,
                    "ascend.db",
                ).build().also { instance = it }
            }
    }
}
