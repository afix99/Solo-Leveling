package com.ascend.app.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.db.DailyLogEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import com.ascend.app.focus.FocusSessionActivity
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.CircularProgressRing
import com.ascend.app.ui.components.ProgressBar
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import java.time.LocalTime

@Composable
fun TodayScreen(repository: AscendRepository, onOpenHabits: () -> Unit) {
    val vm: HomeViewModel = viewModel(factory = SimpleViewModelFactory { HomeViewModel(repository) })
    val context = LocalContext.current

    val habits by vm.habits.collectAsStateWithLifecycle()
    val logs by vm.logsToday.collectAsStateWithLifecycle()
    val totalXp by vm.totalXp.collectAsStateWithLifecycle()
    val hunterProfile by vm.hunterProfile.collectAsStateWithLifecycle()

    val logsByHabit = remember(logs) { logs.associateBy { it.habitId } }
    val nonNegotiables = remember(habits) { habits.filter { it.isNonNegotiable } }
    val otherHabits = remember(habits) { habits.filterNot { it.isNonNegotiable } }
    val allNonNegDone = nonNegotiables.isNotEmpty() &&
        nonNegotiables.all { logsByHabit[it.id]?.completed == true }
    val penaltyQuests = remember(habits, logsByHabit) {
        habits.filter { h -> logsByHabit[h.id]?.let { it.isPenaltyQuest && !it.completed } == true }
    }

    var otherExpanded by remember { mutableStateOf(false) }
    val expanded = otherExpanded || allNonNegDone

    // --- Level-up / rank-up detection -------------------------------------
    var previousTotalXp by remember { mutableStateOf<Int?>(null) }
    var rankUpToShow by remember { mutableStateOf<Rank?>(null) }
    LaunchedEffect(totalXp) {
        previousTotalXp?.let { prev ->
            val prevRank = Leveling.rankForTotalXp(prev)
            val newRank = Leveling.rankForTotalXp(totalXp)
            if (newRank.ordinal > prevRank.ordinal) rankUpToShow = newRank
        }
        previousTotalXp = totalXp
    }

    val statProgress by vm.statProgress.collectAsStateWithLifecycle()
    var previousStatXp by remember { mutableStateOf<Map<Stat, Int>?>(null) }
    var statLevelUpToShow by remember { mutableStateOf<Pair<Stat, Int>?>(null) }
    LaunchedEffect(statProgress) {
        val current = statProgress.associate { it.stat to it.xp }
        previousStatXp?.let { prev ->
            for ((stat, xp) in current) {
                val prevXp = prev[stat] ?: xp
                val prevLevel = Leveling.levelForXp(prevXp)
                val newLevel = Leveling.levelForXp(xp)
                if (newLevel > prevLevel) {
                    statLevelUpToShow = stat to newLevel
                    break
                }
            }
        }
        previousStatXp = current
    }

    // Evening lie/truth nudge — after 18:00, if any non-negotiable still open.
    LaunchedEffect(habits, logsByHabit) {
        val afterEvening = LocalTime.now().isAfter(LocalTime.of(18, 0))
        val anyOpen = nonNegotiables.any { logsByHabit[it.id]?.completed != true }
        if (afterEvening && anyOpen) vm.requestLieTruthNudge()
    }

    Box(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HunterHeader(
                    hunterName = hunterProfile?.hunterName ?: "Hunter",
                    totalXp = totalXp,
                )
            }

            if (vm.showRecalibrationNudge && !vm.recalibrationDismissed) {
                item {
                    RecalibrationBanner(onReview = onOpenHabits, onDismiss = vm::dismissRecalibrationNudge)
                }
            }

            if (penaltyQuests.isNotEmpty()) {
                item { Eyebrow("Penalty quests", color = AscendColors.Amber) }
                items(penaltyQuests, key = { "penalty-${it.id}" }) { habit ->
                    HabitRow(
                        habit = habit,
                        log = logsByHabit[habit.id],
                        accent = AscendColors.Amber,
                        onToggle = { checked -> vm.toggleHabit(habit, checked) },
                        onFocus = { context.launchFocusSession(habit.id) },
                    )
                }
            }

            item { Eyebrow("Non-negotiables") }
            if (nonNegotiables.isEmpty()) {
                item {
                    EmptyRow("No non-negotiables yet — add some in Habits.", onOpenHabits)
                }
            }
            items(nonNegotiables, key = { "nn-${it.id}" }) { habit ->
                HabitRow(
                    habit = habit,
                    log = logsByHabit[habit.id],
                    accent = AscendColors.AccentBlue,
                    onToggle = { checked -> vm.toggleHabit(habit, checked) },
                    onFocus = { context.launchFocusSession(habit.id) },
                )
            }

            if (otherHabits.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { otherExpanded = !otherExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Eyebrow("Other habits (${otherHabits.size})")
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AscendColors.TextTertiary,
                        )
                    }
                }
                if (expanded) {
                    items(otherHabits, key = { "other-${it.id}" }) { habit ->
                        HabitRow(
                            habit = habit,
                            log = logsByHabit[habit.id],
                            accent = AscendColors.AccentBlue,
                            onToggle = { checked -> vm.toggleHabit(habit, checked) },
                            onFocus = { context.launchFocusSession(habit.id) },
                        )
                    }
                }
            }

            vm.lieTruthNudge?.let { lieTruth ->
                item {
                    LieTruthNudgeCard(lie = lieTruth.lieText, truth = lieTruth.truthText, onDismiss = vm::dismissLieTruthNudge)
                }
            }

            item { Spacer(Modifier.height(60.dp)) }
        }

        AnimatedVisibility(
            visible = statLevelUpToShow != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        ) {
            statLevelUpToShow?.let { (stat, level) ->
                StatLevelUpToast(stat = stat, level = level, onDismiss = { statLevelUpToShow = null })
            }
        }

        rankUpToShow?.let { rank ->
            RankUpOverlay(rank = rank, onDismiss = { rankUpToShow = null })
        }
    }
}

private fun android.content.Context.launchFocusSession(habitId: Long) {
    val intent = android.content.Intent(this, FocusSessionActivity::class.java)
        .putExtra(FocusSessionActivity.EXTRA_HABIT_ID, habitId)
    startActivity(intent)
}

@Composable
private fun HunterHeader(hunterName: String, totalXp: Int) {
    val rank = Leveling.rankForTotalXp(totalXp)
    val hunterLevel = Leveling.hunterLevelForTotalXp(totalXp)
    val progress = Leveling.hunterLevelProgress(totalXp)

    GlassCard(accent = rank.color()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CircularProgressRing(
                progress = progress.fraction,
                diameter = 92.dp,
                strokeWidth = 8.dp,
                progressColor = rank.color(),
            ) {
                Text("$hunterLevel", color = AscendColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(hunterName, color = AscendColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Pill(rank.displayName, rank.color())
                Spacer(Modifier.height(6.dp))
                Text(
                    "${progress.xpIntoLevel} / ${progress.xpSpanForLevel} XP to next level",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun RecalibrationBanner(onReview: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(accent = AscendColors.Amber) {
        Text("This might be too much right now", color = AscendColors.Amber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "You've missed a few non-negotiables this week. Consider reviewing them.",
            color = AscendColors.TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                "Review habits",
                color = AscendColors.Amber,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onReview),
            )
            Spacer(Modifier.width(20.dp))
            Text(
                "Dismiss",
                color = AscendColors.TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun LieTruthNudgeCard(lie: String, truth: String, onDismiss: () -> Unit) {
    GlassCard(accent = AscendColors.AccentViolet) {
        Text("“$lie”", color = AscendColors.TextTertiary, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
        Spacer(Modifier.height(6.dp))
        Text(truth, color = AscendColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Text(
            "Got it",
            color = AscendColors.AccentViolet,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onDismiss),
        )
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    log: DailyLogEntity?,
    accent: Color,
    onToggle: (Boolean) -> Unit,
    onFocus: () -> Unit,
) {
    val completed = log?.completed == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AscendColors.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (completed) accent else AscendColors.SurfaceElevated2)
                .clickable { onToggle(!completed) },
            contentAlignment = Alignment.Center,
        ) {
            if (completed) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                habit.name,
                color = if (completed) AscendColors.TextTertiary else AscendColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
            )
            if (log?.isPenaltyQuest == true) {
                Text("Redemption · 1.5× XP", color = AscendColors.Amber, fontSize = 11.sp)
            }
        }
        Pill(habit.stat.shortLabel, habit.stat.color())
        if (habit.isFocusEnabled) {
            IconButton(onClick = onFocus) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start focus session", tint = AscendColors.AccentBlue)
            }
        }
    }
}

@Composable
private fun EmptyRow(text: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick)) {
        Text(text, color = AscendColors.TextSecondary, fontSize = 13.sp)
    }
}
