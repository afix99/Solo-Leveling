package com.ascend.app.data.cloud

import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.db.RewardEntity
import com.ascend.app.data.db.ShadowEntity
import com.ascend.app.data.db.StatProgressEntity
import com.ascend.app.domain.Stat
import org.json.JSONArray
import org.json.JSONObject

/**
 * Translation between the local database and the wire format.
 *
 * Kept apart from both the repository and the HTTP client because it is the
 * piece most likely to need care on a schema change: the snapshot is the thing
 * a future version has to be able to read back, so its shape is a contract and
 * deserves to be readable in one place.
 *
 * Habits are keyed by name rather than row id on restore. Ids are local and
 * meaningless on a fresh install, whereas names are what the user actually
 * recognises — and matching on them means restoring onto a device that already
 * has habits merges sensibly instead of duplicating everything.
 */
object CloudSnapshot {

    const val SNAPSHOT_VERSION = 1

    // ---- Building ----------------------------------------------------------

    fun build(
        profile: HunterProfileEntity,
        habits: List<HabitEntity>,
        logs: List<DailyLogEntity>,
        stats: List<StatProgressEntity>,
        rewards: List<RewardEntity>,
        shadows: List<ShadowEntity>,
        liesTruths: List<LieTruthEntity>,
    ): JSONObject {
        val habitsById = habits.associateBy { it.id }

        return JSONObject().apply {
            put("snapshotVersion", SNAPSHOT_VERSION)
            put("profile", profileJson(profile))
            put("habits", JSONArray(habits.map(::habitJson)))
            put(
                "logs",
                JSONArray(
                    logs.mapNotNull { log ->
                        val habit = habitsById[log.habitId] ?: return@mapNotNull null
                        logJson(log, habit.name)
                    },
                ),
            )
            put(
                "stats",
                JSONObject().apply { stats.forEach { put(it.stat.name, it.xp) } },
            )
            put("rewards", JSONArray(rewards.map { r ->
                JSONObject().apply {
                    put("name", r.name)
                    put("goldCost", r.goldCost)
                    put("timesPurchased", r.timesPurchased)
                    put("archived", r.archived)
                }
            }))
            put("shadows", JSONArray(shadows.mapNotNull { s ->
                val habit = habitsById[s.habitId] ?: return@mapNotNull null
                JSONObject().apply {
                    put("name", s.name)
                    put("habitName", habit.name)
                    put("stat", s.stat.name)
                    put("rank", s.rank)
                }
            }))
            put("liesTruths", JSONArray(liesTruths.map { lt ->
                JSONObject().apply {
                    put("lie", lt.lieText)
                    put("truth", lt.truthText)
                    put("active", lt.active)
                }
            }))
        }
    }

    private fun profileJson(p: HunterProfileEntity) = JSONObject().apply {
        put("hunterName", p.hunterName)
        put("gold", p.gold)
        put("goldEarnedTotal", p.goldEarnedTotal)
        put("perfectDays", p.perfectDays)
        put("perfectWeeks", p.perfectWeeks)
        put("equippedTitleId", p.equippedTitleId ?: JSONObject.NULL)
        put("hunterClass", p.hunterClass.name)
        put("lastPerfectDate", p.lastPerfectDate ?: JSONObject.NULL)
        put("lastRolloverDate", p.lastRolloverDate ?: JSONObject.NULL)
        put("allocatedPoints", JSONObject().apply {
            p.allocatedPoints.forEach { (stat, points) -> put(stat.name, points) }
        })
        put("unlockedSkills", JSONArray(p.unlockedSkills.map { it.name }))
    }

    private fun habitJson(h: HabitEntity) = JSONObject().apply {
        put("name", h.name)
        put("stat", h.stat.name)
        put("nonNegotiable", h.isNonNegotiable)
        put("focusEnabled", h.isFocusEnabled)
        put("targetDurationMinutes", h.targetDurationMinutes ?: JSONObject.NULL)
        put("reminderHour", h.reminderHour ?: JSONObject.NULL)
        put("reminderMinute", h.reminderMinute ?: JSONObject.NULL)
        put("archived", h.archived)
    }

    private fun logJson(l: DailyLogEntity, habitName: String) = JSONObject().apply {
        put("habitName", habitName)
        put("date", l.date)
        put("completed", l.completed)
        put("xp", l.xpAwarded)
        put("gold", l.goldAwarded)
        put("penaltyQuest", l.isPenaltyQuest)
        put("streak", l.streakAtCompletion)
    }

    // ---- Flattened facts, for the server's analytics tables ---------------

    /**
     * Per-day aggregates. Computed on the phone because it already holds every
     * log; the server stores them relationally so it can answer questions
     * across months without unpacking a JSON blob each time.
     */
    fun dayFacts(
        habits: List<HabitEntity>,
        logs: List<DailyLogEntity>,
        focusMinutesByDate: Map<String, Int>,
    ): JSONArray {
        val habitsById = habits.associateBy { it.id }
        val nonNegotiableCount = habits.count { it.isNonNegotiable && !it.archived }

        return JSONArray(
            logs.groupBy { it.date }.map { (date, dayLogs) ->
                val nonNegDone = dayLogs.count {
                    it.completed && habitsById[it.habitId]?.isNonNegotiable == true
                }
                JSONObject().apply {
                    put("date", date)
                    put("nonNegTotal", nonNegotiableCount)
                    put("nonNegDone", nonNegDone)
                    put("optionalDone", dayLogs.count {
                        it.completed && habitsById[it.habitId]?.isNonNegotiable == false
                    })
                    put("xpEarned", dayLogs.sumOf { it.xpAwarded })
                    put("goldEarned", dayLogs.sumOf { it.goldAwarded })
                    put("focusMinutes", focusMinutesByDate[date] ?: 0)
                    put("perfect", nonNegotiableCount > 0 && nonNegDone >= nonNegotiableCount)
                }
            },
        )
    }

    fun habitFacts(habits: List<HabitEntity>, logs: List<DailyLogEntity>): JSONArray {
        val habitsById = habits.associateBy { it.id }
        return JSONArray(
            logs.mapNotNull { log ->
                val habit = habitsById[log.habitId] ?: return@mapNotNull null
                JSONObject().apply {
                    put("date", log.date)
                    put("habitName", habit.name)
                    put("stat", habit.stat.name)
                    put("nonNegotiable", habit.isNonNegotiable)
                    put("completed", log.completed)
                    put("penaltyQuest", log.isPenaltyQuest)
                    put("streak", log.streakAtCompletion)
                }
            },
        )
    }

    // ---- Reading back ------------------------------------------------------

    data class RestoredHabit(
        val name: String,
        val stat: Stat,
        val nonNegotiable: Boolean,
        val focusEnabled: Boolean,
        val targetDurationMinutes: Int?,
    )

    data class RestoredLog(
        val habitName: String,
        val date: String,
        val completed: Boolean,
        val xp: Int,
        val gold: Int,
        val penaltyQuest: Boolean,
        val streak: Int,
    )

    fun readHabits(snapshot: JSONObject): List<RestoredHabit> {
        val array = snapshot.optJSONArray("habits") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val stat = runCatching { Stat.valueOf(o.optString("stat")) }.getOrNull()
                ?: return@mapNotNull null
            RestoredHabit(
                name = name,
                stat = stat,
                nonNegotiable = o.optBoolean("nonNegotiable"),
                focusEnabled = o.optBoolean("focusEnabled"),
                targetDurationMinutes = o.optInt("targetDurationMinutes").takeIf { it > 0 },
            )
        }
    }

    fun readLogs(snapshot: JSONObject): List<RestoredLog> {
        val array = snapshot.optJSONArray("logs") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val habitName = o.optString("habitName").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val date = o.optString("date").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            RestoredLog(
                habitName = habitName,
                date = date,
                completed = o.optBoolean("completed"),
                xp = o.optInt("xp"),
                gold = o.optInt("gold"),
                penaltyQuest = o.optBoolean("penaltyQuest"),
                streak = o.optInt("streak"),
            )
        }
    }

    fun readStats(snapshot: JSONObject): Map<Stat, Int> {
        val o = snapshot.optJSONObject("stats") ?: return emptyMap()
        return Stat.entries.mapNotNull { stat ->
            if (o.has(stat.name)) stat to o.optInt(stat.name) else null
        }.toMap()
    }

    fun readProfileBasics(snapshot: JSONObject): Triple<String?, Int, Int> {
        val o = snapshot.optJSONObject("profile") ?: return Triple(null, 0, 0)
        return Triple(
            o.optString("hunterName").takeIf { it.isNotBlank() },
            o.optInt("gold"),
            o.optInt("perfectDays"),
        )
    }
}
