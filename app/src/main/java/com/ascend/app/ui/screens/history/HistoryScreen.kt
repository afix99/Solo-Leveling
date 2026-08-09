package com.ascend.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.History
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.EmptyState
import com.ascend.app.ui.components.Footnote
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.HabitHeatmap
import com.ascend.app.ui.components.HeatmapLegend
import com.ascend.app.ui.components.LabelledBar
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.ScreenHeader
import com.ascend.app.ui.components.ScreenScaffold
import com.ascend.app.ui.components.SectionHeader
import com.ascend.app.ui.components.SegmentedToggle
import com.ascend.app.ui.components.TrendLine
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Space
import com.ascend.app.ui.theme.SurfaceLevel
import com.ascend.app.ui.theme.Type
import com.ascend.app.ui.theme.color
import java.time.format.TextStyle
import java.util.Locale

/**
 * History: what actually happened, as opposed to what the System says you are.
 *
 * The Stats screen answers "how strong am I"; this one answers "am I holding
 * up, and where am I leaking". Built from local logs only, so it works with no
 * backup configured and no connection.
 */
@Composable
fun HistoryScreen(repository: AscendRepository) {
    val vm: HistoryViewModel = viewModel(factory = SimpleViewModelFactory { HistoryViewModel(repository) })
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val data = vm.data
    if (data == null) {
        Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
            ScreenHeader("History", subtitle = "What actually happened")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (vm.loading) "Reading your history…" else "No history yet.",
                    color = AscendColors.TextTertiary,
                    fontSize = Type.bodySize,
                )
            }
        }
        return
    }

    val tracked = data.summaries.filterNot { it.isUntracked }

    ScreenScaffold(
        title = "History",
        subtitle = "${tracked.size} days on record",
        actions = {
            SegmentedToggle(
                options = HistoryRange.entries.toList(),
                selected = vm.range,
                label = { it.label },
                onSelect = vm::selectRange,
            )
        },
    ) {
        if (tracked.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing logged yet",
                    body = "Charts appear once you have a few days behind you. " +
                        "Tick some habits off on Today and come back.",
                    accent = AscendColors.Amber,
                )
            }
            return@ScreenScaffold
        }

        item { HeadlineCard(data, tracked.size) }
        item { HeatmapCard(data) }
        item { MomentumCard(data) }
        item { WeekdayCard(data) }

        item { SectionHeader("Per habit", trailing = "${data.trends.size} tracked") }
        items(data.trends.size, key = { data.trends[it].habitId }) { index ->
            HabitTrendCard(data.trends[index])
        }

        item {
            Footnote(
                "Built from this phone's own logs, so it works offline. Backing up gives " +
                    "the Coach a longer view than shown here.",
            )
        }
    }
}

/**
 * The headline: the live streak, then everything else.
 *
 * The current run is the one number a habit app exists to show, so it gets the
 * size and the colour and the rest arrange themselves around it. The previous
 * version gave seven numbers identical weight, which left the eye nowhere to
 * land — a grid of equals reads as a spreadsheet, not an answer.
 */
@Composable
private fun HeadlineCard(data: AscendRepository.HistoryData, trackedDays: Int) {
    val perfect = data.summaries.count { it.isPerfect }

    GlassCard(
        accent = AscendColors.AccentBlue,
        level = SurfaceLevel.Raised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${data.currentPerfectRun}",
                color = AscendColors.Success,
                fontSize = 52.sp,
                fontWeight = Type.displayWeight,
            )
            Spacer(Modifier.width(Space.md))
            Column {
                Text(
                    if (data.currentPerfectRun == 1) "perfect day" else "perfect days",
                    color = AscendColors.TextPrimary,
                    fontSize = Type.titleSize,
                    fontWeight = Type.titleWeight,
                )
                Text(
                    "in a row, right now",
                    color = AscendColors.TextTertiary,
                    fontSize = Type.captionSize,
                )
            }
        }

        Spacer(Modifier.height(Space.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MiniStat("$perfect", "perfect", AscendColors.Success)
            MiniStat("${data.longestPerfectRun}", "best run", AscendColors.Amber)
            MiniStat("$trackedDays", "logged", AscendColors.TextPrimary)
            MiniStat("${data.summaries.sumOf { it.xp }}", "XP", AscendColors.AccentBlue)
            MiniStat("${data.focusMinutesByDate.values.sum()}", "focus", AscendColors.StatPer)
        }
    }
}

@Composable
private fun MiniStat(value: String, caption: String, color: Color) {
    Column {
        Text(value, color = color, fontSize = Type.titleSize, fontWeight = Type.statWeight)
        Text(caption, color = AscendColors.TextTertiary, fontSize = Type.labelSize)
    }
}

@Composable
private fun HeatmapCard(data: AscendRepository.HistoryData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Every day in view")
        Spacer(Modifier.height(Space.md))
        HabitHeatmap(days = data.summaries.map { Triple(it.intensity, it.isPerfect, it.isUntracked) })
        Spacer(Modifier.height(Space.md))
        HeatmapLegend()
        Spacer(Modifier.height(Space.sm))
        Text(
            "Oldest at the left. Colour shows how much of that day you cleared.",
            color = AscendColors.TextTertiary,
            fontSize = Type.captionSize,
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

    GlassCard(
        accent = accent,
        level = if (change != 0) SurfaceLevel.Raised else SurfaceLevel.Resting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader("Momentum", color = accent)
                Text(
                    when {
                        change > 0 -> "Up $change points"
                        change < 0 -> "Down ${-change} points"
                        else -> "Holding steady"
                    },
                    color = AscendColors.TextPrimary,
                    fontSize = Type.statSize,
                    fontWeight = Type.statWeight,
                )
                Text(
                    "${data.momentum.recentRate}% this fortnight, " +
                        "${data.momentum.priorRate}% the one before",
                    color = AscendColors.TextTertiary,
                    fontSize = Type.captionSize,
                )
            }
            if (change != 0) Pill(if (change > 0) "▲" else "▼", accent)
        }
        Spacer(Modifier.height(Space.md))
        // XP per day carries the shape of effort even when the number of
        // non-negotiables changed, so it is a more honest line than a count.
        TrendLine(values = data.summaries.map { it.xp }, color = accent)
        Text("XP per day", color = AscendColors.TextTertiary, fontSize = Type.labelSize)
    }
}

@Composable
private fun WeekdayCard(data: AscendRepository.HistoryData) {
    val recorded = data.weekdays.filter { it.daysRecorded > 0 }
    if (recorded.isEmpty()) return
    val worst = recorded.minByOrNull { it.rate }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("By day of week")
        Spacer(Modifier.height(Space.sm))
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
            Spacer(Modifier.height(Space.sm))
            Text(
                "${worst.day.getDisplayName(TextStyle.FULL, Locale.getDefault())} is your " +
                    "weakest day so far, at ${worst.rate}%.",
                color = AscendColors.TextTertiary,
                fontSize = Type.captionSize,
            )
        }
    }
}

@Composable
private fun HabitTrendCard(trend: History.HabitTrend) {
    GlassCard(
        accent = trend.stat.color(),
        level = if (trend.isSlipping) SurfaceLevel.Highlighted else SurfaceLevel.Resting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    trend.name,
                    color = AscendColors.TextPrimary,
                    fontSize = Type.titleSize,
                    fontWeight = Type.titleWeight,
                )
                Text(
                    buildString {
                        append("${trend.completionRate}% overall")
                        if (trend.currentStreak > 0) append(" · ${trend.currentStreak}-day streak")
                        if (trend.bestStreak > trend.currentStreak) append(" · best ${trend.bestStreak}")
                    },
                    color = AscendColors.TextTertiary,
                    fontSize = Type.captionSize,
                )
            }
            when {
                trend.isSlipping -> Pill("▼ ${-trend.trendPoints}", AscendColors.Danger)
                trend.isRising -> Pill("▲ ${trend.trendPoints}", AscendColors.Success)
                else -> Pill(if (trend.isNonNegotiable) "CORE" else "EXTRA", AscendColors.Divider)
            }
        }
        Spacer(Modifier.height(Space.sm))
        LabelledBar(label = "", percent = trend.completionRate, color = trend.stat.color(), trailing = "")
        if (trend.isSlipping) {
            Text(
                "Fell from ${trend.priorRate}% to ${trend.recentRate}% in the last " +
                    "${History.TREND_WINDOW_DAYS} days.",
                color = AscendColors.Danger,
                fontSize = Type.captionSize,
            )
        }
    }
}
