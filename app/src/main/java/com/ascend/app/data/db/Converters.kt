package com.ascend.app.data.db

import androidx.room.TypeConverter
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.domain.EvidenceType
import com.ascend.app.domain.Stat

class Converters {
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
