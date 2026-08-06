package com.ascend.app.domain

/**
 * The 5 fixed stat categories every habit is tagged to. See design spec §3.
 *
 * [plainName] exists because "PER" means nothing on its own — the UI always
 * shows the plain word alongside the RPG label.
 */
enum class Stat(
    val displayName: String,
    val shortLabel: String,
    val plainName: String,
    val description: String,
) {
    STR("Strength", "STR", "Body", "Training, cold showers, physical effort"),
    VIT("Vitality", "VIT", "Health", "Sleep, food, water — how you maintain yourself"),
    INT("Intellect", "INT", "Mind", "Reading, deep work, learning"),
    PER("Perception", "PER", "Focus", "Meditation, journaling, staying off your phone"),
    AGI("Agility", "AGI", "Movement", "Steps, cardio, getting your body moving"),
}

/** Hunter rank, derived from overall Hunter Level. See design spec §4. */
enum class Rank(val displayName: String, val minHunterLevel: Int) {
    E("E-Rank", 1),
    D("D-Rank", 5),
    C("C-Rank", 10),
    B("B-Rank", 20),
    A("A-Rank", 35),
    S("S-Rank", 55),
    ;

    companion object {
        fun forHunterLevel(level: Int): Rank =
            entries.lastOrNull { level >= it.minHunterLevel } ?: E
    }
}

/** Post-Focus-Session self-rating, feeds the deliberate-practice target-bump suggestion. */
enum class DifficultyRating { TOO_EASY, JUST_RIGHT, TOO_HARD }

/** The three kinds of proof events that populate the Evidence Log. See spec §4.5. */
enum class EvidenceType { PENALTY_REDEMPTION, STREAK_MILESTONE, HARD_FOCUS_SESSION }
