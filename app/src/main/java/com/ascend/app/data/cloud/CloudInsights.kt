package com.ascend.app.data.cloud

import org.json.JSONObject

/**
 * Turns the server's analytics JSON into prose lines for the coach prompt.
 *
 * Deliberately a translation and not an interpretation: each line states a
 * measured fact and stops. Writing "you always fail on Sundays" here would put
 * a verdict in the model's mouth before it has seen the rest of the data, and
 * would be wrong the first week the pattern changed. The model gets the
 * numbers and does the judging.
 *
 * Lines are also budgeted — a report over months could otherwise crowd out the
 * actual question with per-habit rows the model does not need.
 */
object CloudInsights {

    private const val MAX_HABIT_LINES = 6

    fun fromReport(report: JSONObject): List<String> {
        if (!report.optBoolean("hasData", false)) return emptyList()
        val lines = mutableListOf<String>()

        report.optJSONObject("coverage")?.let { c ->
            val days = c.optInt("daysRecorded")
            if (days > 0) {
                lines += "History covers $days recorded days, " +
                    "${c.optString("firstDay")} to ${c.optString("lastDay")}."
            }
        }

        report.optJSONArray("windows")?.let { windows ->
            for (i in 0 until windows.length()) {
                val w = windows.optJSONObject(i) ?: continue
                if (w.isNull("nonNegotiableRate")) continue
                lines += "Last ${w.optString("window")}: " +
                    "${w.optInt("nonNegotiableRate")}% of non-negotiables done, " +
                    "${w.optInt("perfectDays")} perfect days, " +
                    "${w.optInt("focusMinutes")} focus minutes."
            }
        }

        report.optJSONObject("momentum")?.let { m ->
            if (!m.isNull("changePoints")) {
                val change = m.optInt("changePoints")
                val direction = when {
                    change > 0 -> "up $change points"
                    change < 0 -> "down ${-change} points"
                    else -> "flat"
                }
                lines += "Momentum: last 14 days are $direction against the 14 before " +
                    "(${m.optInt("recentRate14")}% vs ${m.optInt("priorRate14")}%)."
            }
        }

        // Weekday pattern is stated in full rather than reduced to a "worst
        // day", because a single low day means nothing without its neighbours.
        report.optJSONArray("weekdayPattern")?.let { days ->
            val parts = (0 until days.length()).mapNotNull { i ->
                val d = days.optJSONObject(i) ?: return@mapNotNull null
                if (d.isNull("nonNegotiableRate")) return@mapNotNull null
                "${d.optString("weekday")} ${d.optInt("nonNegotiableRate")}%"
            }
            if (parts.isNotEmpty()) lines += "By weekday: ${parts.joinToString(", ")}."
        }

        report.optJSONArray("slippingHabits")?.let { slipping ->
            for (i in 0 until minOf(slipping.length(), MAX_HABIT_LINES)) {
                val h = slipping.optJSONObject(i) ?: continue
                lines += "Slipping: \"${h.optString("habit")}\" fell from " +
                    "${h.optInt("priorRate14")}% to ${h.optInt("recentRate14")}% " +
                    "over the last two weeks."
            }
        }

        report.optJSONArray("strongestHabits")?.let { strong ->
            val parts = (0 until minOf(strong.length(), 3)).mapNotNull { i ->
                val h = strong.optJSONObject(i) ?: return@mapNotNull null
                "\"${h.optString("habit")}\" ${h.optInt("completionRate30")}%"
            }
            if (parts.isNotEmpty()) lines += "Most reliable (30d): ${parts.joinToString(", ")}."
        }

        report.optJSONArray("weakestHabits")?.let { weak ->
            val parts = (0 until minOf(weak.length(), 3)).mapNotNull { i ->
                val h = weak.optJSONObject(i) ?: return@mapNotNull null
                "\"${h.optString("habit")}\" ${h.optInt("completionRate30")}%"
            }
            if (parts.isNotEmpty()) lines += "Least reliable (30d): ${parts.joinToString(", ")}."
        }

        report.optJSONArray("longestPerfectRuns")?.optJSONObject(0)?.let { run ->
            val length = run.optInt("lengthDays")
            if (length > 0) {
                lines += "Longest perfect run: $length days, ending ${run.optString("ended")}."
            }
        }

        return lines
    }
}
