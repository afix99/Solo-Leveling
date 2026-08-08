package com.ascend.app.domain

/** A habit template used by the starter packs and the "suggest a stat" helper. */
data class HabitTemplate(
    val name: String,
    val stat: Stat,
    val isNonNegotiable: Boolean,
    val focusMinutes: Int? = null,
)

data class StarterPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val habits: List<HabitTemplate>,
)

/**
 * Pre-built habit sets so the app is usable the moment it opens, instead of
 * making someone type in seven habits before anything works.
 *
 * The "Full discipline system" pack mirrors the daily non-negotiables and
 * other-habits lists from the journal this app was designed around.
 */
object StarterHabits {

    val EASE_IN = StarterPack(
        id = "ease_in",
        title = "Start small",
        subtitle = "3 habits. Build the streak first, add more later.",
        habits = listOf(
            HabitTemplate("Wake up before 7am", Stat.VIT, isNonNegotiable = true),
            HabitTemplate("Read 20 minutes", Stat.INT, isNonNegotiable = true, focusMinutes = 20),
            HabitTemplate("Bed before 11:30", Stat.VIT, isNonNegotiable = true),
        ),
    )

    val FULL_SYSTEM = StarterPack(
        id = "full_system",
        title = "Full discipline system",
        subtitle = "7 non-negotiables + 5 extras. The complete setup.",
        habits = listOf(
            HabitTemplate("Wake up before 7am", Stat.VIT, isNonNegotiable = true),
            HabitTemplate("Cold shower", Stat.STR, isNonNegotiable = true),
            HabitTemplate("No phone until 11am", Stat.PER, isNonNegotiable = true),
            HabitTemplate("2+ hr deep work", Stat.INT, isNonNegotiable = true, focusMinutes = 60),
            HabitTemplate("10k steps", Stat.AGI, isNonNegotiable = true),
            HabitTemplate("Read 20 minutes", Stat.INT, isNonNegotiable = true, focusMinutes = 20),
            HabitTemplate("Bed before 11:30", Stat.VIT, isNonNegotiable = true),
            HabitTemplate("Stretch / mobility", Stat.STR, isNonNegotiable = false, focusMinutes = 10),
            HabitTemplate("Journal", Stat.PER, isNonNegotiable = false, focusMinutes = 10),
            HabitTemplate("Drink 2L water", Stat.VIT, isNonNegotiable = false),
            HabitTemplate("No sugar", Stat.VIT, isNonNegotiable = false),
            HabitTemplate("Meditate", Stat.PER, isNonNegotiable = false, focusMinutes = 10),
        ),
    )

    val BODY = StarterPack(
        id = "body",
        title = "Body focus",
        subtitle = "Training, sleep and movement.",
        habits = listOf(
            HabitTemplate("Workout", Stat.STR, isNonNegotiable = true, focusMinutes = 45),
            HabitTemplate("10k steps", Stat.AGI, isNonNegotiable = true),
            HabitTemplate("8 hours sleep", Stat.VIT, isNonNegotiable = true),
            HabitTemplate("Stretch / mobility", Stat.STR, isNonNegotiable = false, focusMinutes = 10),
            HabitTemplate("Drink 2L water", Stat.VIT, isNonNegotiable = false),
        ),
    )

    val MIND = StarterPack(
        id = "mind",
        title = "Mind focus",
        subtitle = "Deep work, reading and attention.",
        habits = listOf(
            HabitTemplate("2+ hr deep work", Stat.INT, isNonNegotiable = true, focusMinutes = 60),
            HabitTemplate("No phone until 11am", Stat.PER, isNonNegotiable = true),
            HabitTemplate("Read 20 minutes", Stat.INT, isNonNegotiable = true, focusMinutes = 20),
            HabitTemplate("Meditate", Stat.PER, isNonNegotiable = false, focusMinutes = 10),
            HabitTemplate("Journal", Stat.PER, isNonNegotiable = false, focusMinutes = 10),
        ),
    )

    val all = listOf(EASE_IN, FULL_SYSTEM, BODY, MIND)

    /**
     * Guesses which stat a habit belongs to from its name, so people don't
     * silently file "Read 20 min" under Strength. Falls back to [Stat.VIT]
     * (general maintenance) when nothing matches.
     */
    fun suggestStat(habitName: String): Stat {
        val name = habitName.lowercase()
        val matches = listOf(
            Stat.STR to listOf(
                "workout", "gym", "train", "lift", "weights", "push", "pull", "run",
                "exercise", "cold shower", "stretch", "mobility", "yoga", "sport",
            ),
            // Deliberately no bare "work" here — it swallows things like
            // "cycle to work" that belong to another stat.
            Stat.INT to listOf(
                "read", "study", "deep work", "learn", "write", "code", "course",
                "book", "practice", "language", "revise",
            ),
            Stat.PER to listOf(
                "meditate", "meditation", "journal", "reflect", "breathe", "no phone",
                "phone", "screen", "social media", "scroll", "mindful", "gratitude",
            ),
            Stat.AGI to listOf(
                "steps", "walk", "cardio", "bike", "cycle", "swim", "move", "sprint",
            ),
            Stat.VIT to listOf(
                "sleep", "bed", "wake", "water", "hydrate", "eat", "meal", "diet",
                "sugar", "vitamin", "rest", "nap", "clean",
            ),
        )
        for ((stat, keywords) in matches) {
            if (keywords.any { name.contains(it) }) return stat
        }
        return Stat.VIT
    }
}
