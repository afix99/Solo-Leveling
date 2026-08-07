package com.ascend.app.ui.screens.achievements

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Achievement
import com.ascend.app.domain.Achievements
import com.ascend.app.domain.HunterClass
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.AscendColors

@Composable
fun AchievementsScreen(repository: AscendRepository, currentRank: com.ascend.app.domain.Rank) {
    val vm: AchievementsViewModel = viewModel(factory = SimpleViewModelFactory { AchievementsViewModel(repository) })
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val unlockedIds = remember(unlocked) { unlocked.map { it.achievementId }.toSet() }

    val unlockedTitleIds = remember(unlockedIds) {
        Achievements.all.filter { it.id in unlockedIds }.mapNotNull { it.titleId }.toSet()
    }
    val selectableClasses = remember(currentRank) { HunterClass.selectableAt(currentRank) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "ACHIEVEMENTS",
                    color = AscendColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "${unlockedIds.size} of ${Achievements.all.size} unlocked",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        // ---- Job Change --------------------------------------------------
        item {
            if (selectableClasses.isEmpty()) {
                SystemPanel(accent = AscendColors.TextTertiary, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Job Change · locked", color = AscendColors.TextTertiary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reach ${HunterClass.UNLOCK_RANK.displayName} to choose a class.",
                        color = AscendColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            } else {
                SystemPanel(accent = AscendColors.AccentViolet, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Job Change", color = AscendColors.AccentViolet)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pick a class. Its passive stacks on top of your stat bonuses.",
                        color = AscendColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    selectableClasses.forEach { klass ->
                        val selected = profile?.hunterClass == klass
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) {
                                        AscendColors.AccentViolet.copy(alpha = 0.2f)
                                    } else {
                                        AscendColors.SurfaceElevated
                                    },
                                )
                                .clickable { vm.chooseClass(klass) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    klass.displayName,
                                    color = if (selected) AscendColors.AccentViolet else AscendColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(klass.tagline, color = AscendColors.TextTertiary, fontSize = 11.sp)
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = AscendColors.AccentViolet,
                                    modifier = Modifier.width(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ---- Titles ------------------------------------------------------
        item {
            GlassCard(accent = AscendColors.Amber) {
                Eyebrow("Titles", color = AscendColors.Amber)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Shown next to your name. Tap to equip.",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                if (unlockedTitleIds.isEmpty()) {
                    Text(
                        "No titles yet — they come from achievements.",
                        color = AscendColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                } else {
                    Achievements.titles.filter { it.id in unlockedTitleIds }.forEach { title ->
                        val equipped = profile?.equippedTitleId == title.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.equipTitle(if (equipped) null else title.id) }
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    title.text,
                                    color = if (equipped) AscendColors.Amber else AscendColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(title.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
                            }
                            if (equipped) Pill("Equipped", AscendColors.Amber)
                        }
                    }
                }
            }
        }

        // ---- Achievement list --------------------------------------------
        item { Eyebrow("All achievements") }
        items(Achievements.all, key = { it.id }) { achievement ->
            AchievementRow(achievement = achievement, unlocked = achievement.id in unlockedIds)
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean) {
    GlassCard(accent = if (unlocked) AscendColors.Success else AscendColors.Divider) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (unlocked) Icons.Default.Verified else Icons.Default.Lock,
                contentDescription = null,
                tint = if (unlocked) AscendColors.Success else AscendColors.TextTertiary,
                modifier = Modifier.width(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.name,
                    color = if (unlocked) AscendColors.TextPrimary else AscendColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(achievement.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "+${achievement.goldReward}g",
                color = if (unlocked) AscendColors.Amber else AscendColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
