package com.ascend.app.data.db

/**
 * Every migration statement, as data.
 *
 * The SQL lives here rather than inline in [AscendDatabase] so the statements
 * a device will run are the exact same objects a JVM test executes against a
 * real SQLite engine. Room's own migration testing requires an emulator; this
 * project is built without one, and seven untested migrations is the single
 * most effective way to destroy a user's streak.
 *
 * Rules for editing:
 * - Append only. A statement that has shipped has already run on real
 *   devices; reordering or rewriting it changes what a half-migrated
 *   database becomes.
 * - Each list runs in order, inside Room's migration transaction.
 */
internal object MigrationSql {

    /** v2 adds the economy layer: Gold, titles, classes, rewards,
     * achievements and daily quests. */
    val V1_TO_V2: List<String> = listOf(
        "ALTER TABLE hunter_profile ADD COLUMN gold INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE hunter_profile ADD COLUMN goldEarnedTotal INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE hunter_profile ADD COLUMN equippedTitleId TEXT",
        "ALTER TABLE hunter_profile ADD COLUMN hunterClass TEXT NOT NULL DEFAULT 'NONE'",
        "ALTER TABLE hunter_profile ADD COLUMN perfectDays INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE hunter_profile ADD COLUMN perfectWeeks INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE hunter_profile ADD COLUMN lastPerfectDate TEXT",
        "ALTER TABLE daily_logs ADD COLUMN goldAwarded INTEGER NOT NULL DEFAULT 0",
        "CREATE TABLE IF NOT EXISTS rewards ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, goldCost INTEGER NOT NULL, timesPurchased INTEGER NOT NULL DEFAULT 0, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL DEFAULT 0 )",
        "CREATE TABLE IF NOT EXISTS reward_purchases ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, rewardId INTEGER NOT NULL, rewardName TEXT NOT NULL, goldSpent INTEGER NOT NULL, purchasedAtEpochMillis INTEGER NOT NULL )",
        "CREATE TABLE IF NOT EXISTS unlocked_achievements ( achievementId TEXT NOT NULL PRIMARY KEY, unlockedAtEpochMillis INTEGER NOT NULL )",
        "CREATE TABLE IF NOT EXISTS daily_quests ( date TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, description TEXT NOT NULL, targetCount INTEGER NOT NULL, targetHabitId INTEGER, xpReward INTEGER NOT NULL, goldReward INTEGER NOT NULL, claimed INTEGER NOT NULL DEFAULT 0 )",
    )

    /** v3 adds the choice layer: manually allocated stat points, unlocked
     * skills, extracted Shadows and Gate runs. */
    val V2_TO_V3: List<String> = listOf(
        "ALTER TABLE hunter_profile ADD COLUMN allocatedPoints TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE hunter_profile ADD COLUMN unlockedSkills TEXT NOT NULL DEFAULT ''",
        "CREATE TABLE IF NOT EXISTS shadows ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, habitId INTEGER NOT NULL, stat TEXT NOT NULL, extractedAtEpochMillis INTEGER NOT NULL, rank INTEGER NOT NULL DEFAULT 1 )",
        "CREATE TABLE IF NOT EXISTS gate_runs ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, rank TEXT NOT NULL, startDate TEXT NOT NULL, daysCleared INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'ACTIVE', stakePaid INTEGER NOT NULL, lastEvaluatedDate TEXT )",
    )

    /** v4 adds the AI coach: provider settings, the optional athlete profile,
     * and cached advice so past answers stay readable offline. */
    val V3_TO_V4: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS ai_settings ( id INTEGER NOT NULL PRIMARY KEY, provider TEXT NOT NULL DEFAULT 'OPENROUTER', apiKey TEXT NOT NULL DEFAULT '', model TEXT NOT NULL DEFAULT '', age INTEGER, sex TEXT, heightCm INTEGER, weightKg INTEGER, goal TEXT, experience TEXT, equipment TEXT, dietaryNotes TEXT, injuries TEXT )",
        "CREATE TABLE IF NOT EXISTS coach_advice ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, adviceType TEXT NOT NULL, content TEXT NOT NULL, model TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL )",
    )

    /** v5 adds the System voice toggle and multi-turn chat history. */
    val V4_TO_V5: List<String> = listOf(
        "ALTER TABLE ai_settings ADD COLUMN systemVoice INTEGER NOT NULL DEFAULT 1",
        "CREATE TABLE IF NOT EXISTS chat_messages ( id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, content TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL )",
    )

    /** v6 records how far the midnight rollover has got, and the Mana
     * Conversion daily allowance. Existing rows start null, which the
     * catch-up treats as "evaluate yesterday only" - the old behaviour - so
     * upgrading never retroactively penalises days that already passed. */
    val V5_TO_V6: List<String> = listOf(
        "ALTER TABLE hunter_profile ADD COLUMN lastRolloverDate TEXT",
        "ALTER TABLE hunter_profile ADD COLUMN manaConvertedDate TEXT",
        "ALTER TABLE hunter_profile ADD COLUMN manaConvertedXpToday INTEGER NOT NULL DEFAULT 0",
    )

    /** v7 adds cloud backup settings and the sound/haptics toggles. Both
     * feedback flags default to 0 so upgrading never makes a silent app
     * start making noise. */
    val V6_TO_V7: List<String> = listOf(
        "ALTER TABLE hunter_profile ADD COLUMN soundEnabled INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE hunter_profile ADD COLUMN hapticsEnabled INTEGER NOT NULL DEFAULT 0",
        "CREATE TABLE IF NOT EXISTS cloud_settings ( id INTEGER NOT NULL PRIMARY KEY, baseUrl TEXT NOT NULL DEFAULT '', hunterKey TEXT NOT NULL DEFAULT '', autoBackup INTEGER NOT NULL DEFAULT 1, lastBackupAtEpochMillis INTEGER, lastBackupStatus TEXT )",
    )

    /** Oldest first, so a test can walk the entire chain in order. */
    val ALL: List<Triple<Int, Int, List<String>>> = listOf(
        Triple(1, 2, V1_TO_V2),
        Triple(2, 3, V2_TO_V3),
        Triple(3, 4, V3_TO_V4),
        Triple(4, 5, V4_TO_V5),
        Triple(5, 6, V5_TO_V6),
        Triple(6, 7, V6_TO_V7),
    )
}
