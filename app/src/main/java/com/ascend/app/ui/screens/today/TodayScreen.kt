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
import com.ascend.app.data.db.DailyQuestEntity
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Achievements
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.Leveling
import com.ascend.app.domain.Rank
import com.ascend.app.domain.StarterHabits
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import com.ascend.app.focus.FocusSessionActivity
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.CircularProgressRing
import com.ascend.app.ui.components.ProgressBar
import com.ascend.app.ui.components.SystemMessage
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.components.SystemWindow
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
    val dailyQuest by vm.dailyQuest.collectAsStateWithLifecycle()

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
                    gold = hunterProfile?.gold ?: 0,
                    titleText = Achievements.titleById(hunterProfile?.equippedTitleId)?.text,
                    hunterClass = hunterProfile?.hunterClass ?: HunterClass.NONE,
                )
            }

            dailyQuest?.let { quest ->
                item {
                    DailyQuestCard(
                        quest = quest,
                        progress = vm.questProgress,
                        onClaim = vm::claimDailyQuest,
                    )
                }
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

            if (habits.isEmpty()) {
                item { EmptyStateCard(onAddPack = { vm.applyStarterPack(it) }, onOpenHabits = onOpenHabits) }
            }

            if (nonNegotiables.isNotEmpty()) {
                item { Eyebrow("Non-negotiables · must do today") }
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
                        Eyebrow(
                            if (expanded) "Other habits · optional" else "Other habits · optional (${otherHabits.size})",
                        )
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

        // XP confirmation sits above the level-up toast slot so both can appear
        // in sequence without fighting for the same position.
        vm.lastXpGain?.let { gain ->
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp)) {
                XpGainToast(gain = gain, onDismiss = vm::clearXpGain)
            }
        }

        AnimatedVisibility(
            visible = statLevelUpToShow != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        ) {
            statLevelUpToShow?.let { (stat, level) ->
                StatLevelUpToast(stat = stat, level = level, onDismiss = { statLevelUpToShow = null })
            }
        }

        // System windows, in priority order — only one is ever on screen.
        val questClaimed = vm.questClaimed
        val newQuest = vm.newQuestAnnouncement
        val achievement = vm.pendingAchievements.firstOrNull()
        when {
            rankUpToShow != null -> RankUpOverlay(
                rank = rankUpToShow!!,
                onDismiss = { rankUpToShow = null },
            )

            achievement != null -> SystemWindow(
                message = SystemMessage(
                    heading = "Achievement unlocked",
                    title = achievement.name,
                    body = achievement.description,
                    lines = buildList {
                        add("+${achievement.goldReward} Gold")
                        Achievements.titleById(achievement.titleId)?.let { add("Title earned: ${it.text}") }
                    },
                    accent = AscendColors.Success,
                ),
                onDismiss = vm::dismissFirstAchievement,
            )

            questClaimed != null -> SystemWindow(
                message = SystemMessage(
                    heading = "Quest complete",
                    title = "Daily Quest cleared",
                    body = questClaimed.description,
                    lines = listOf(
                        "+${questClaimed.xpReward} XP",
                        "+${questClaimed.goldReward} Gold",
                    ),
                    accent = AscendColors.Amber,
                ),
                onDismiss = vm::dismissQuestClaimed,
            )

            newQuest != null -> SystemWindow(
                message = SystemMessage(
                    heading = "Notification",
                    title = "Daily Quest has arrived",
                    body = newQuest.description,
                    lines = listOf(
                        "Reward: ${newQuest.xpReward} XP",
                        "Reward: ${newQuest.goldReward} Gold",
                    ),
                    accent = AscendColors.AccentViolet,
                    confirmLabel = "Accept",
                ),
                onDismiss = vm::dismissQuestAnnouncement,
            )
        }
    }
}

private fun android.content.Context.launchFocusSession(habitId: Long) {
    val intent = android.content.Intent(this, FocusSessionActivity::class.java)
        .putExtra(FocusSessionActivity.EXTRA_HABIT_ID, habitId)
    startActivity(intent)
}

@Composable
private fun HunterHeader(
    hunterName: String,
    totalXp: Int,
    gold: Int,
    titleText: String?,
    hunterClass: HunterClass,
) {
    val rank = Leveling.rankForTotalXp(totalXp)
    val hunterLevel = Leveling.hunterLevelForTotalXp(totalXp)
    val progress = Leveling.hunterLevelProgress(totalXp)

    SystemPanel(accent = rank.color(), modifier = Modifier.fillMaxWidth()) {
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
            Column(modifier = Modifier.weight(1f)) {
                titleText?.let {
                    Text(it, color = AscendColors.Amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(hunterName, color = AscendColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(rank.displayName, rank.color())
                    if (hunterClass != HunterClass.NONE) {
                        Spacer(Modifier.width(6.dp))
                        Pill(hunterClass.displayName, AscendColors.AccentViolet)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${progress.xpSpanForLevel - progress.xpIntoLevel} XP to level ${hunterLevel + 1}",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$gold", color = AscendColors.Amber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("GOLD", color = AscendColors.TextTertiary, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }
}

/** The System's bonus quest for today, in the cut-corner panel style. */
@Composable
private fun DailyQuestCard(quest: DailyQuestEntity, progress: Int, onClaim: () -> Unit) {
    val complete = progress >= quest.targetCount
    val accent = when {
        quest.claimed -> AscendColors.Success
        complete -> AscendColors.Amber
        else -> AscendColors.AccentViolet
    }

    SystemPanel(accent = accent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("Daily Quest", color = accent)
            Text(
                "$progress / ${quest.targetCount}",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(quest.description, color = AscendColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        ProgressBar(
            progress = progress.toFloat() / quest.targetCount.coerceAtLeast(1),
            fillColor = accent,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Reward: ${quest.xpReward} XP · ${quest.goldReward} Gold",
                color = AscendColors.TextSecondary,
                fontSize = 12.sp,
            )
            when {
                quest.claimed -> Text("Claimed", color = AscendColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                complete -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AscendColors.Amber.copy(alpha = 0.22f))
                        .clickable(onClick = onClaim)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text("Claim", color = AscendColors.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                else -> Text("In progress", color = AscendColors.TextTertiary, fontSize = 12.sp)
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
        Pill(habit.stat.plainName, habit.stat.color())
        if (habit.isFocusEnabled) {
            Spacer(Modifier.width(6.dp))
            // Labelled rather than a bare icon — a lone ▶ gave no clue that a
            // timer existed at all.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AscendColors.AccentBlue.copy(alpha = 0.16f))
                    .clickable(onClick = onFocus)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AscendColors.AccentBlue,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "${habit.targetDurationMinutes ?: 20}m",
                    color = AscendColors.AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Shown when there are no habits at all — the state the first build dumped
 * people into with no way forward. Now it offers one-tap starter sets. */
@Composable
private fun EmptyStateCard(onAddPack: (StarterPack) -> Unit, onOpenHabits: () -> Unit) {
    GlassCard(accent = AscendColors.AccentBlue) {
        Text(
            "You have no habits yet",
            color = AscendColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Add a starter set to get going in one tap, or build your own in the Habits tab.",
            color = AscendColors.TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(14.dp))
        StarterHabits.all.forEach { pack ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AscendColors.SurfaceElevated)
                    .clickable { onAddPack(pack) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pack.title, color = AscendColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(pack.subtitle, color = AscendColors.TextTertiary, fontSize = 11.sp)
                }
                Text("Add", color = AscendColors.AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            "Build my own instead",
            color = AscendColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onOpenHabits).padding(top = 4.dp),
        )
    }
}

/** Brief "+20 XP · Mind" confirmation so completing a habit visibly does something. */
@Composable
private fun XpGainToast(gain: XpGain, onDismiss: () -> Unit) {
    LaunchedEffect(gain.stamp) {
        kotlinx.coroutines.delay(1500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(gain.stat.color().copy(alpha = 0.9f))
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            buildString {
                append("+${gain.amount} XP")
                append(" · +${gain.gold}g")
                if (gain.streakBonusPercent > 0) append(" · streak +${gain.streakBonusPercent}%")
            },
            color = AscendColors.Background,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
