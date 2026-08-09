package com.ascend.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.History
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.HabitHeatmap
import com.ascend.app.ui.components.HeatmapLegend
import com.ascend.app.ui.components.LabelledBar
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.StatTile
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.components.TrendLine
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import java.time.format.TextStyle
import java.util.Locale

/**
 * History: what actually happened, as opposed to what the System says you are.
 *
 * The Stats screen answers "how strong am I"; this one answers "am I holding
 * up, and where am I leaking". It is built from local logs only, so it works
 * with no backup configured and no connection.
 */
@Composable
fun HistoryScreen(repository: AscendRepository) {
    val vm: HistoryViewModel = viewModel(factory = SimpleViewModelFactory { HistoryViewModel(repository) })
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val data = vm.data

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "HISTORY",
                color = AscendColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HistoryRange.entries.forEach { option ->
                    val selected = option == vm.range
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) AscendColors.AccentBlue.copy(alpha = 0.2f)
                                else AscendColors.SurfaceElevated,
                            )
                            .clickable { vm.selectRange(option) }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    ) {
                        Text(
                            option.label,
                            color = if (selected) AscendColors.AccentBlue else AscendColors.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (data == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (vm.loading) "Reading your history…" else "No history yet.",
                    color = AscendColors.TextTertiary,
                    fontSize = 13.sp,
                )
            }
            return@Column
        }

        val tracked = data.summaries.filterNot { it.isUntracked }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // A screen full of zeroes looks broken. Say plainly that the data
            // simply isn't there yet, and what will make it appear.
            if (tracked.isEmpty()) {
                item {
                    SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Nothing logged yet", color = AscendColors.Amber)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Charts appear once you have a few days of completions behind you. " +
                                "Tick some habits off on Today and come back.",
                            color = AscendColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
                return@LazyColumn
            }

            item { SummaryCard(data, tracked.size) }
            item { HeatmapCard(data) }
            item { MomentumCard(data) }
            item { WeekdayCard(data) }

            item { Eyebrow("Per habit", modifier = Modifier.padding(top = 6.dp)) }
            items(data.trends, key = { it.habitId }) { trend -> HabitTrendCard(trend) }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Built from this phone's own logs, so it works offline. Backing up gives " +
                        "the Coach a longer view than shown here.",
                    color = AscendColors.TextTertiary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(data: AscendRepository.HistoryData, trackedDays: Int) {
    val perfect = data.summaries.count { it.isPerfect }
    val totalXp = data.summaries.sumOf { it.xp }
    val focus = data.focusMinutesByDate.values.sum()

    SystemPanel(accent = AscendColors.AccentBlue, modifier = Modifier.fillMaxWidth()) {
        Eyebrow("The window", color = AscendColors.AccentBlue)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatTile("$perfect", "perfect days", AscendColors.Success)
            StatTile("${data.currentPerfectRun}", "current run", AscendColors.AccentViolet)
            StatTile("${data.longestPerfectRun}", "best run", AscendColors.Amber)
            StatTile("$trackedDays", "days logged", AscendColors.TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatTile("$totalXp", "XP earned", AscendColors.AccentBlue)
            StatTile("$focus", "focus minutes", AscendColors.StatPer)
            StatTile(
                "${data.summaries.sumOf { it.gold }}",
                "gold earned",
                AscendColors.Amber,
            )
        }
    }
}

@Composable
private fun HeatmapCard(data: AscendRepository.HistoryData) {
    GlassCard {
        Eyebrow("Every day in view")
        Spacer(Modifier.height(10.dp))
        HabitHeatmap(
            days = data.summaries.map {
                Triple(it.intensity, it.isPerfect, it.isUntracked)
            },
        )
        Spacer(Modifier.height(10.dp))
        HeatmapLegend()
        Spacer(Modifier.height(6.dp))
        Text(
            "Each square is a day, oldest at the left. Colour shows how much of " +
                "that day's non-negotiables you cleared.",
            color = AscendColors.TextTertiary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun MomentumCard(data: AscendRepository.HistoryData) {
    val change = data.momentum.changePoints
    val accent = when {
        change > 0 -> AscendColors.Success
        change < 0 -> AscendColors.Danger
        else -> AscendColors.TextSecondary
    }

    GlassCard(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Eyebrow("Momentum", color = accent)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        change > 0 -> "Up $change points"
                        change < 0 -> "Down ${-change} points"
                        else -> "Holding steady"
                    },
                    color = AscendColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    "Last ${History.TREND_WINDOW_DAYS} days ${data.momentum.recentRate}% " +
                        "vs ${data.momentum.priorRate}% before",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // XP per day carries the shape of effort even on days where the
        // non-negotiable count changed, so it makes a more honest trend line
        // than a raw completion count.
        TrendLine(values = data.summaries.map { it.xp }, color = accent)
        Text("XP per day", color = AscendColors.TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun WeekdayCard(data: AscendRepository.HistoryData) {
    val recorded = data.weekdays.filter { it.daysRecorded > 0 }
    if (recorded.isEmpty()) return
    val worst = recorded.minByOrNull { it.rate }

    GlassCard {
        Eyebrow("By day of week")
        Spacer(Modifier.height(8.dp))
        data.weekdays.forEach { day ->
            val hasData = day.daysRecorded > 0
            LabelledBar(
                label = day.day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                percent = day.rate,
                color = if (day.day == worst?.day) AscendColors.Danger else AscendColors.AccentBlue,
                trailing = if (hasData) "${day.rate}%" else "—",
                dimmed = !hasData,
            )
        }
        if (worst != null && recorded.size >= 3) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${worst.day.getDisplayName(TextStyle.FULL, Locale.getDefault())} is your " +
                    "weakest day so far, at ${worst.rate}%.",
                color = AscendColors.TextTertiary,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun HabitTrendCard(trend: History.HabitTrend) {
    GlassCard(accent = trend.stat.color()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    trend.name,
                    color = AscendColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    buildString {
                        append("${trend.completionRate}% overall")
                        if (trend.currentStreak > 0) append(" · ${trend.currentStreak}-day streak")
                        if (trend.bestStreak > trend.currentStreak) {
                            append(" · best ${trend.bestStreak}")
                        }
                    },
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
            when {
                trend.isSlipping -> Pill("▼ ${-trend.trendPoints}", AscendColors.Danger)
                trend.isRising -> Pill("▲ ${trend.trendPoints}", AscendColors.Success)
                else -> Pill(if (trend.isNonNegotiable) "CORE" else "EXTRA", AscendColors.Divider)
            }
        }
        Spacer(Modifier.height(6.dp))
        LabelledBar(
            label = "",
            percent = trend.completionRate,
            color = trend.stat.color(),
            trailing = "",
        )
        if (trend.isSlipping) {
            Text(
                "Fell from ${trend.priorRate}% to ${trend.recentRate}% in the last " +
                    "${History.TREND_WINDOW_DAYS} days.",
                color = AscendColors.Danger,
                fontSize = 10.sp,
            )
        }
    }
}
