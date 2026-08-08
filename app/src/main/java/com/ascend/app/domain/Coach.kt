package com.ascend.app.domain

/**
 * The kinds of advice the coach can produce. Each one is answered with the
 * user's *real* logged data, not a generic template — that's the whole reason
 * this lives inside the app rather than being a chatbot you paste into.
 */
enum class AdviceType(
    val title: String,
    val blurb: String,
    val needsAthleteProfile: Boolean,
) {
    DATA_REVIEW(
        title = "Review my data",
        blurb = "Reads your completion rates, streaks and misses, then says what to change.",
        needsAthleteProfile = false,
    ),
    TRAINING_PLAN(
        title = "Training plan",
        blurb = "A weekly lifting split for muscle growth, matched to your equipment and experience.",
        needsAthleteProfile = true,
    ),
    NUTRITION(
        title = "Eating schedule",
        blurb = "Daily calorie and protein targets with a meal timing structure for muscle gain.",
        needsAthleteProfile = true,
    ),
    READING(
        title = "Reading technique",
        blurb = "How to read faster and retain more, based on how much you actually read.",
        needsAthleteProfile = false,
    ),
    DEEP_WORK(
        title = "Deep work",
        blurb = "Concrete ways to extend your focus sessions, using your real session history.",
        needsAthleteProfile = false,
    ),
    HABIT_DESIGN(
        title = "Redesign my habits",
        blurb = "Which habits to keep, cut, or add — argued from your own numbers.",
        needsAthleteProfile = false,
    ),
    SLEEP(
        title = "Sleep & recovery",
        blurb = "Recovery advice built around your wake/sleep habits and training load.",
        needsAthleteProfile = true,
    ),
}

/** Optional body context. Everything is nullable — advice degrades gracefully. */
data class AthleteProfile(
    val age: Int? = null,
    val sex: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val goal: String? = null,
    val experience: String? = null,
    val equipment: String? = null,
    val dietaryNotes: String? = null,
    val injuries: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(age, sex, heightCm, weightKg, goal, experience, equipment, dietaryNotes, injuries)
            .all { it == null || (it is String && it.isBlank()) }

    companion object {
        val goals = listOf("Build muscle", "Lose fat", "Recomposition", "Strength", "General health")
        val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
        val equipmentOptions = listOf("Full gym", "Home gym", "Dumbbells only", "Bodyweight only")
    }
}
