package com.ascend.app.data.db

import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Executes the real migration SQL against a real SQLite engine.
 *
 * Room's own MigrationTestHelper needs an instrumented test and therefore an
 * emulator, which this project is built without. Rather than leave seven
 * migrations completely unexecuted — the single most effective way to destroy
 * a user's streak — these run the exact statement lists from [MigrationSql]
 * through sqlite-jdbc.
 *
 * What this does prove: the SQL is valid, the statements apply in order
 * without conflicting, new columns land with the right defaults, and existing
 * rows survive with their values intact.
 *
 * What it does not prove: that the final schema matches what Room's generated
 * code expects. Room validates that on the device at open time; catching a
 * mismatch here would need the schema JSON that only an instrumented build
 * produces. So this closes the "does the SQL work and keep my data" gap, not
 * the "does Room agree with the result" one.
 *
 * The v1 schema below is reconstructed from what the v1→v2 migration assumes
 * already exists. If it were wrong, the first migration would fail loudly here
 * rather than silently pass.
 */
class MigrationSqlTest {

    private lateinit var db: Connection

    /** The schema as it shipped at version 1, before any migration existed. */
    private val v1Schema = listOf(
        """
        CREATE TABLE habits (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            stat TEXT NOT NULL,
            isNonNegotiable INTEGER NOT NULL,
            reminderHour INTEGER,
            reminderMinute INTEGER,
            isFocusEnabled INTEGER NOT NULL DEFAULT 0,
            targetDurationMinutes INTEGER,
            createdAt INTEGER NOT NULL,
            archived INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE daily_logs (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            habitId INTEGER NOT NULL,
            date TEXT NOT NULL,
            completed INTEGER NOT NULL,
            xpAwarded INTEGER NOT NULL DEFAULT 0,
            isPenaltyQuest INTEGER NOT NULL DEFAULT 0,
            streakAtCompletion INTEGER NOT NULL DEFAULT 0
        )
        """,
        "CREATE UNIQUE INDEX index_daily_logs_habitId_date ON daily_logs (habitId, date)",
        "CREATE TABLE stat_progress (stat TEXT NOT NULL PRIMARY KEY, xp INTEGER NOT NULL DEFAULT 0)",
        """
        CREATE TABLE hunter_profile (
            id INTEGER NOT NULL PRIMARY KEY,
            hunterName TEXT NOT NULL,
            morningReminderHour INTEGER NOT NULL,
            morningReminderMinute INTEGER NOT NULL,
            eveningReminderHour INTEGER NOT NULL,
            eveningReminderMinute INTEGER NOT NULL,
            notificationsEnabled INTEGER NOT NULL,
            onboardingComplete INTEGER NOT NULL
        )
        """,
        """
        CREATE TABLE lies_truths (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            lieText TEXT NOT NULL,
            truthText TEXT NOT NULL,
            active INTEGER NOT NULL DEFAULT 1,
            createdAt INTEGER NOT NULL
        )
        """,
        """
        CREATE TABLE weekly_reviews (
            weekStart TEXT NOT NULL PRIMARY KEY,
            weekEnd TEXT NOT NULL,
            completionPercent REAL NOT NULL,
            xpByStat TEXT NOT NULL,
            bestDay TEXT,
            worstDay TEXT,
            userReflectionNote TEXT NOT NULL DEFAULT ''
        )
        """,
        """
        CREATE TABLE focus_sessions (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            habitId INTEGER NOT NULL,
            startTimeEpochMillis INTEGER NOT NULL,
            plannedDurationMinutes INTEGER NOT NULL,
            actualDurationSeconds INTEGER NOT NULL,
            completed INTEGER NOT NULL,
            difficultyRating TEXT
        )
        """,
        """
        CREATE TABLE evidence_log (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            type TEXT NOT NULL,
            description TEXT NOT NULL,
            dateEpochMillis INTEGER NOT NULL,
            sourceId INTEGER
        )
        """,
    )

    @Before
    fun setUp() {
        db = DriverManager.getConnection("jdbc:sqlite::memory:")
        exec(v1Schema)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exec(statements: List<String>) {
        db.createStatement().use { st -> statements.forEach { st.executeUpdate(it) } }
    }

    private fun migrateTo(target: Int) {
        MigrationSql.ALL.filter { (_, to, _) -> to <= target }.forEach { (_, _, sql) -> exec(sql) }
    }

    private fun columnsOf(table: String): Set<String> {
        val names = mutableSetOf<String>()
        db.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) names += rs.getString("name")
            }
        }
        return names
    }

    private fun tables(): Set<String> {
        val names = mutableSetOf<String>()
        db.createStatement().use { st ->
            st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                while (rs.next()) names += rs.getString("name")
            }
        }
        return names
    }

    private fun <T> queryOne(sql: String, read: (java.sql.ResultSet) -> T): T =
        db.createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                assertTrue("expected a row from: $sql", rs.next())
                read(rs)
            }
        }

    private fun seedV1Data() {
        exec(
            listOf(
                "INSERT INTO habits (id, name, stat, isNonNegotiable, createdAt) " +
                    "VALUES (1, 'Cold shower', 'STR', 1, 1700000000000)",
                "INSERT INTO habits (id, name, stat, isNonNegotiable, createdAt) " +
                    "VALUES (2, 'Read 20 minutes', 'INT', 1, 1700000000000)",
                "INSERT INTO daily_logs (habitId, date, completed, xpAwarded, streakAtCompletion) " +
                    "VALUES (1, '2026-01-05', 1, 25, 7)",
                "INSERT INTO daily_logs (habitId, date, completed, xpAwarded, streakAtCompletion) " +
                    "VALUES (2, '2026-01-05', 0, 0, 0)",
                "INSERT INTO stat_progress (stat, xp) VALUES ('STR', 4050)",
                "INSERT INTO stat_progress (stat, xp) VALUES ('INT', 1200)",
                "INSERT INTO hunter_profile (id, hunterName, morningReminderHour, " +
                    "morningReminderMinute, eveningReminderHour, eveningReminderMinute, " +
                    "notificationsEnabled, onboardingComplete) " +
                    "VALUES (0, 'Afiq', 7, 0, 20, 0, 1, 1)",
                "INSERT INTO lies_truths (lieText, truthText, createdAt) " +
                    "VALUES ('I will start tomorrow', 'Tomorrow is a story I tell myself', 1700000000000)",
            ),
        )
    }

    // ---- The whole chain ---------------------------------------------------

    @Test
    fun `the full v1 to v7 chain applies without error`() {
        seedV1Data()
        migrateTo(7)
        // Reaching here means all 26 statements were valid SQL in order.
        assertTrue(tables().containsAll(listOf("habits", "hunter_profile", "cloud_settings")))
    }

    @Test
    fun `a populated database keeps every row through the whole chain`() {
        seedV1Data()
        migrateTo(7)

        assertEquals(2, queryOne("SELECT COUNT(*) c FROM habits") { it.getInt("c") })
        assertEquals(2, queryOne("SELECT COUNT(*) c FROM daily_logs") { it.getInt("c") })
        assertEquals(2, queryOne("SELECT COUNT(*) c FROM stat_progress") { it.getInt("c") })
        assertEquals(1, queryOne("SELECT COUNT(*) c FROM lies_truths") { it.getInt("c") })
    }

    @Test
    fun `hard-won values survive the upgrade unchanged`() {
        // This is the test that matters. A migration that drops or rewrites
        // these is one that erases someone's months of work.
        seedV1Data()
        migrateTo(7)

        assertEquals(4050, queryOne("SELECT xp FROM stat_progress WHERE stat='STR'") { it.getInt("xp") })
        assertEquals(
            7,
            queryOne("SELECT streakAtCompletion s FROM daily_logs WHERE habitId=1") { it.getInt("s") },
        )
        assertEquals(
            "Afiq",
            queryOne("SELECT hunterName n FROM hunter_profile WHERE id=0") { it.getString("n") },
        )
        assertEquals(
            "Cold shower",
            queryOne("SELECT name FROM habits WHERE id=1") { it.getString("name") },
        )
    }

    @Test
    fun `migrating an empty database also works`() {
        // A user who upgrades before ever opening the app has no rows at all,
        // and ALTER TABLE on an empty table must still succeed.
        migrateTo(7)
        assertEquals(0, queryOne("SELECT COUNT(*) c FROM habits") { it.getInt("c") })
        assertTrue(columnsOf("hunter_profile").contains("soundEnabled"))
    }

    // ---- Per-version arrivals ---------------------------------------------

    @Test
    fun `v2 adds the economy without disturbing existing rows`() {
        seedV1Data()
        migrateTo(2)

        assertTrue(columnsOf("hunter_profile").containsAll(listOf("gold", "goldEarnedTotal", "hunterClass")))
        assertTrue(tables().containsAll(listOf("rewards", "reward_purchases", "unlocked_achievements", "daily_quests")))
        // Existing profile picks up the declared defaults rather than nulls.
        assertEquals(0, queryOne("SELECT gold FROM hunter_profile WHERE id=0") { it.getInt("gold") })
        assertEquals(
            "NONE",
            queryOne("SELECT hunterClass c FROM hunter_profile WHERE id=0") { it.getString("c") },
        )
    }

    @Test
    fun `v3 adds the choice layer with empty-string defaults the converters can read`() {
        seedV1Data()
        migrateTo(3)

        // Converters turn "" into an empty map/set. A null here would crash on
        // first read for every upgrading user.
        assertEquals(
            "",
            queryOne("SELECT allocatedPoints a FROM hunter_profile WHERE id=0") { it.getString("a") },
        )
        assertEquals(
            "",
            queryOne("SELECT unlockedSkills u FROM hunter_profile WHERE id=0") { it.getString("u") },
        )
        assertTrue(tables().containsAll(listOf("shadows", "gate_runs")))
    }

    @Test
    fun `v4 and v5 build the coach tables`() {
        migrateTo(5)
        assertTrue(tables().containsAll(listOf("ai_settings", "coach_advice", "chat_messages")))
        assertTrue(columnsOf("ai_settings").contains("systemVoice"))
    }

    @Test
    fun `v6 leaves the rollover marker null so upgrading invents no penalties`() {
        seedV1Data()
        migrateTo(6)

        // Null means "only evaluate yesterday", which is the pre-v6 behaviour.
        // A non-null default here would make the catch-up walk backwards
        // through days the user was never warned about and penalise them.
        assertNull(
            queryOne("SELECT lastRolloverDate r FROM hunter_profile WHERE id=0") { it.getString("r") },
        )
        assertEquals(
            0,
            queryOne("SELECT manaConvertedXpToday m FROM hunter_profile WHERE id=0") { it.getInt("m") },
        )
    }

    @Test
    fun `v7 leaves sound and haptics off for existing users`() {
        seedV1Data()
        migrateTo(7)

        assertEquals(
            "a silent app must not start making noise on upgrade",
            0,
            queryOne("SELECT soundEnabled s FROM hunter_profile WHERE id=0") { it.getInt("s") },
        )
        assertEquals(
            0,
            queryOne("SELECT hapticsEnabled h FROM hunter_profile WHERE id=0") { it.getInt("h") },
        )
        assertTrue(tables().contains("cloud_settings"))
    }

    // ---- Structural guards -------------------------------------------------

    @Test
    fun `the migration chain is contiguous and ends at the declared version`() {
        // Catches the classic release-day mistake: bumping @Database version
        // without adding the migration, which makes Room throw on every
        // existing install.
        val chain = MigrationSql.ALL
        chain.forEachIndexed { index, (from, to, _) ->
            assertEquals("migration $index starts at the wrong version", index + 1, from)
            assertEquals("migration $index must move exactly one version", from + 1, to)
        }
        assertEquals(
            "the last migration must reach the declared schema version",
            AscendDatabase.SCHEMA_VERSION,
            chain.last().second,
        )
    }

    @Test
    fun `every migration actually does something`() {
        assertTrue(MigrationSql.ALL.all { (_, _, statements) -> statements.isNotEmpty() })
    }

    @Test
    fun `applying the chain twice fails loudly rather than corrupting silently`() {
        // ALTER TABLE ADD COLUMN is not idempotent in SQLite. Room guarantees
        // each migration runs once, so this documents the dependency rather
        // than asking the SQL to defend itself: if a future change ever runs
        // a migration twice, it will throw here rather than half-apply.
        seedV1Data()
        migrateTo(7)

        val threw = runCatching { migrateTo(7) }.isFailure
        assertTrue("a second application should be rejected by SQLite", threw)
    }
}
