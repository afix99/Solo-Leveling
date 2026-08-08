package com.ascend.app.ui.screens.system

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.Gates
import com.ascend.app.domain.Shadow
import com.ascend.app.domain.Shadows
import com.ascend.app.domain.ManaConversion
import com.ascend.app.domain.Skill
import com.ascend.app.domain.StatAllocation
import com.ascend.app.domain.Stat
import com.ascend.app.domain.StatEffects
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.ProgressBar
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color

private enum class Tab(val label: String) {
    STATUS("Status"), SKILLS("Skills"), SHADOWS("Shadows"), GATES("Gates")
}

@Composable
fun SystemScreen(repository: AscendRepository) {
    val vm: SystemViewModel = viewModel(factory = SimpleViewModelFactory { SystemViewModel(repository) })
    val shadows by vm.shadows.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.STATUS) }
    val state = vm.state

    // Shadows arrive via Flow; other counters are pulled, so re-sync on change.
    LaunchedEffect(shadows.size) { vm.refresh() }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Text(
            "SYSTEM",
            color = AscendColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 18.dp, top = 20.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tab.entries.forEach { t ->
                val selected = t == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) AscendColors.AccentBlue.copy(alpha = 0.2f)
                            else AscendColors.SurfaceElevated,
                        )
                        .clickable { tab = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        t.label,
                        color = if (selected) AscendColors.AccentBlue else AscendColors.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        vm.message?.let { msg ->
            GlassCard(
                accent = AscendColors.Amber,
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 4.dp)
                    .clickable { vm.clearMessage() },
            ) {
                Text(msg, color = AscendColors.Amber, fontSize = 12.sp)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (tab) {
                Tab.STATUS -> statusTab(vm)
                Tab.SKILLS -> skillsTab(vm)
                Tab.SHADOWS -> shadowsTab(shadows)
                Tab.GATES -> gatesTab(vm)
            }
        }
    }
}

// ---- Status: spend your stat points -------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.statusTab(vm: SystemViewModel) {
    val state = vm.state
    item {
        SystemPanel(accent = AscendColors.AccentBlue, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Eyebrow("Stat points", color = AscendColors.AccentBlue)
                    Text(
                        "${state.statPointsAvailable} unspent",
                        color = AscendColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
                Pill("LV ${state.hunterLevel}", AscendColors.AccentBlue)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "You earn ${StatAllocation.POINTS_PER_LEVEL} per level. Each stat does something " +
                    "different, so where they go is the build.",
                color = AscendColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }

    items(Stat.entries) { stat ->
        val effective = vm.state.loadout.effectiveLevel(stat)
        val allocated = vm.state.loadout.allocatedPoints[stat] ?: 0
        val canAdd = vm.state.statPointsAvailable > 0

        GlassCard(accent = stat.color()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stat.plainName,
                            color = AscendColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LV $effective",
                            color = stat.color(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (allocated > 0) {
                            Text(
                                "  (+$allocated)",
                                color = AscendColors.TextTertiary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Text(
                        StatEffects.describeEffect(stat, effective),
                        color = stat.color().copy(alpha = 0.9f),
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (canAdd) stat.color().copy(alpha = 0.22f)
                            else AscendColors.SurfaceElevated2,
                        )
                        .clickable(enabled = canAdd) { vm.allocate(stat) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Allocate a point to ${stat.plainName}",
                        tint = if (canAdd) stat.color() else AscendColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    // Only shown once the skill is unlocked — the skill's whole promise is
    // that it puts this control on the System screen.
    if (vm.state.loadout.has(Skill.MANA_CONVERSION)) {
        item { ManaConversionPanel(vm) }
    }

    item {
        GlassCard {
            Text(
                "Respec — refund every point for ${StatAllocation.RESPEC_GOLD_COST} Gold",
                color = AscendColors.Danger,
                fontSize = 12.sp,
                modifier = Modifier.clickable { vm.respec() },
            )
        }
    }
}

/**
 * Gold → XP, gated behind Mana Conversion.
 *
 * The rate is shown plainly and the remaining daily allowance is always
 * visible, because the trade is meant to be an obvious, slightly bad deal that
 * you take on purpose — not a shortcut that quietly outpaces doing the work.
 */
@Composable
private fun ManaConversionPanel(vm: SystemViewModel) {
    val state = vm.state
    val remainingXp = ManaConversion.remainingToday(state.manaXpConvertedToday)
    val spendable = ManaConversion.maxSpendableNow(state.gold, state.manaXpConvertedToday)
    var target by remember { mutableStateOf(Stat.entries.first()) }

    SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("Mana Conversion", color = AscendColors.Amber)
            Pill("$remainingXp XP LEFT TODAY", AscendColors.Amber)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Burn ${ManaConversion.GOLD_PER_XP} Gold for 1 XP, up to " +
                "${ManaConversion.DAILY_XP_CAP} XP a day. A deliberately poor rate — it is a " +
                "use for idle Gold, not a way around the work.",
            color = AscendColors.TextSecondary,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(10.dp))
        Text("Into which stat", color = AscendColors.TextTertiary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Stat.entries.forEach { stat ->
                val selected = stat == target
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (selected) stat.color().copy(alpha = 0.25f)
                            else AscendColors.SurfaceElevated2,
                        )
                        .clickable { target = stat }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        stat.shortLabel,
                        color = if (selected) stat.color() else AscendColors.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val canConvert = spendable >= ManaConversion.GOLD_PER_XP
        Text(
            if (canConvert) {
                "Convert $spendable Gold → ${ManaConversion.xpFor(spendable, state.manaXpConvertedToday)} " +
                    "${target.shortLabel} XP"
            } else if (remainingXp == 0) {
                "Today's allowance is spent. It resets tomorrow."
            } else {
                "You need at least ${ManaConversion.GOLD_PER_XP} Gold to convert."
            },
            color = if (canConvert) AscendColors.Amber else AscendColors.TextTertiary,
            fontWeight = if (canConvert) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canConvert) { vm.convertGold(target, spendable) },
        )
    }
}

// ---- Skills: rewrite the rules -------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.skillsTab(vm: SystemViewModel) {
    item {
        SystemPanel(accent = AscendColors.AccentViolet, modifier = Modifier.fillMaxWidth()) {
            Eyebrow("Skill points", color = AscendColors.AccentViolet)
            Text(
                "${vm.state.skillPointsAvailable} available",
                color = AscendColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "One per 2 levels, plus two per rank. Every skill changes a rule you already play by.",
                color = AscendColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }

    Skill.byTier.toSortedMap().forEach { (tier, skills) ->
        item { Eyebrow("Tier $tier", modifier = Modifier.padding(top = 6.dp)) }
        items(skills) { skill ->
            val unlocked = vm.state.loadout.has(skill)
            val affordable = Skill.canUnlock(
                skill, vm.state.hunterLevel, vm.state.rank, vm.state.loadout.unlockedSkills,
            )
            GlassCard(
                accent = when {
                    unlocked -> AscendColors.Success
                    affordable -> AscendColors.AccentViolet
                    else -> AscendColors.Divider
                },
                modifier = Modifier.clickable(enabled = affordable) { vm.unlock(skill) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (unlocked) Icons.Default.Verified else Icons.Default.Lock,
                        contentDescription = null,
                        tint = when {
                            unlocked -> AscendColors.Success
                            affordable -> AscendColors.AccentViolet
                            else -> AscendColors.TextTertiary
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            skill.displayName,
                            color = if (unlocked) AscendColors.TextPrimary else AscendColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(skill.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
                        if (!unlocked && vm.state.hunterLevel < skill.requiredHunterLevel) {
                            Text(
                                "Unlocks at level ${skill.requiredHunterLevel}",
                                color = AscendColors.Amber,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Text(
                        if (unlocked) "OWNED" else "${skill.cost}p",
                        color = if (unlocked) AscendColors.Success else AscendColors.AccentViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ---- Shadows: failures turned into assets ---------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.shadowsTab(shadows: List<Shadow>) {
    item {
        SystemPanel(accent = AscendColors.AccentViolet, modifier = Modifier.fillMaxWidth()) {
            Eyebrow("Shadow army", color = AscendColors.AccentViolet)
            Text(
                "${shadows.size} commanded",
                color = AscendColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Miss a non-negotiable, then redeem it the next day, and it joins you as a " +
                    "Shadow — a permanent bonus to that habit's stat. Recovering from a slip " +
                    "leaves you stronger than never slipping would have.",
                color = AscendColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }

    if (shadows.isEmpty()) {
        item {
            GlassCard {
                Text(
                    "No Shadows yet. They only come from failures you come back from.",
                    color = AscendColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
        }
    }

    items(shadows) { shadow ->
        GlassCard(accent = shadow.stat.color()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        shadow.name,
                        color = AscendColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "${Shadows.rankTitle(shadow.rank)} · rank ${shadow.rank} · " +
                            "+${shadow.rank * Shadows.PERCENT_PER_RANK}% ${shadow.stat.plainName}",
                        color = shadow.stat.color(),
                        fontSize = 11.sp,
                    )
                }
                Pill(shadow.stat.shortLabel, shadow.stat.color())
            }
        }
    }
}

// ---- Gates: opt-in risk ----------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.gatesTab(vm: SystemViewModel) {
    val active = vm.state.activeGate

    item {
        SystemPanel(
            accent = if (active != null) AscendColors.Amber else AscendColors.AccentBlue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (active == null) {
                Eyebrow("No active Gate", color = AscendColors.AccentBlue)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stake Gold on holding every non-negotiable for a run of days. Clear it and " +
                        "the stake comes back multiplied. Miss one day and it's gone. Entirely " +
                        "optional — skipping Gates costs you nothing.",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
            } else {
                Eyebrow("${active.rank.displayName} in progress", color = AscendColors.Amber)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Day ${active.daysCleared} of ${active.rank.days}",
                    color = AscendColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress = active.progressFraction, fillColor = AscendColors.Amber)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${active.daysRemaining} days left · clears for ${active.rank.payout} Gold " +
                        "and ${active.rank.xpReward} XP",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Abandon Gate (forfeits the stake)",
                    color = AscendColors.Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { vm.abandonGate() },
                )
            }
        }
    }

    if (active == null) {
        item { Eyebrow("Available", modifier = Modifier.padding(top = 6.dp)) }
        items(GateRank.entries.toList()) { rank ->
            val unlocked = vm.state.hunterLevel >= rank.requiredHunterLevel
            val affordable = vm.state.gold >= rank.stake
            val canEnter = unlocked && affordable

            GlassCard(
                accent = if (canEnter) AscendColors.Amber else AscendColors.Divider,
                modifier = Modifier.clickable(enabled = canEnter) { vm.enterGate(rank) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rank.displayName,
                            color = if (canEnter) AscendColors.TextPrimary else AscendColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "${rank.days} perfect days · stake ${rank.stake}g · " +
                                "pays ${rank.payout}g + ${rank.xpReward} XP",
                            color = AscendColors.TextTertiary,
                            fontSize = 11.sp,
                        )
                        if (!unlocked) {
                            Text(
                                "Unlocks at level ${rank.requiredHunterLevel}",
                                color = AscendColors.Amber,
                                fontSize = 10.sp,
                            )
                        } else if (!affordable) {
                            Text(
                                "Need ${rank.stake - vm.state.gold} more Gold",
                                color = AscendColors.Amber,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    if (canEnter) Pill("ENTER", AscendColors.Amber)
                }
            }
        }
    }
}
