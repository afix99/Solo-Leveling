package com.ascend.app.data.db

import androidx.room.TypeConverter
import com.ascend.app.domain.DailyQuestKind
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.GateStatus
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.Skill
import com.ascend.app.domain.Stat

class Converters {
    @TypeConverter
    fun hunterClassToString(value: HunterClass): String = value.name

    @TypeConverter
    fun stringToHunterClass(value: String): HunterClass =
        runCatching { HunterClass.valueOf(value) }.getOrDefault(HunterClass.NONE)

    @TypeConverter
    fun gateRankToString(value: GateRank): String = value.name

    @TypeConverter
    fun stringToGateRank(value: String): GateRank = GateRank.valueOf(value)

    @TypeConverter
    fun gateStatusToString(value: GateStatus): String = value.name

    @TypeConverter
    fun stringToGateStatus(value: String): GateStatus = GateStatus.valueOf(value)

    /** Skills stored as "IRON_BODY,COIN_PURSE". Unknown names are dropped rather
     * than thrown on, so removing a skill in a future version can't brick a save. */
    @TypeConverter
    fun skillsToString(value: Set<Skill>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSkills(value: String): Set<Skill> {
        if (value.isBlank()) return emptySet()
        return value.split(",").mapNotNull { runCatching { Skill.valueOf(it) }.getOrNull() }.toSet()
    }

    @TypeConverter
    fun questKindToString(value: DailyQuestKind): String = value.name

    @TypeConverter
    fun stringToQuestKind(value: String): DailyQuestKind = DailyQuestKind.valueOf(value)

    @TypeConverter
    fun statToString(stat: Stat): String = stat.name

    @TypeConverter
    fun stringToStat(value: String): Stat = Stat.valueOf(value)

    @TypeConverter
    fun difficultyToString(value: DifficultyRating?): String? = value?.name

    @TypeConverter
    fun stringToDifficulty(value: String?): DifficultyRating? = value?.let { DifficultyRating.valueOf(it) }

    @TypeConverter
    fun evidenceTypeToString(value: EvidenceType): String = value.name

    @TypeConverter
    fun stringToEvidenceType(value: String): EvidenceType = EvidenceType.valueOf(value)

    /** Map<Stat, Int> stored as "STR:120,VIT:80" — avoids pulling in a JSON dependency
     * for one small, fully-controlled shape. */
    @TypeConverter
    fun statXpMapToString(value: Map<Stat, Int>): String =
        value.entries.joinToString(",") { "${it.key.name}:${it.value}" }

    @TypeConverter
    fun stringToStatXpMap(value: String): Map<Stat, Int> {
        if (value.isBlank()) return emptyMap()
        return value.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val stat = runCatching { Stat.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val xp = parts[1].toIntOrNull() ?: return@mapNotNull null
            stat to xp
        }.toMap()
    }
}
