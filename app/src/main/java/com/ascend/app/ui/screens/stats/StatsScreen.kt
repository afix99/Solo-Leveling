package com.ascend.app.ui.screens.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.ProgressBar
import com.ascend.app.domain.StatEffects
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color

@Composable
fun StatsScreen(repository: AscendRepository) {
    val vm: StatsViewModel = viewModel(factory = SimpleViewModelFactory { StatsViewModel(repository) })
    val cards by vm.statCards.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Text(
            "STATS",
            color = AscendColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 2.dp),
        )
        Text(
            "Each habit feeds one of these. Complete habits to level them up.",
            color = AscendColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(cards, key = { it.stat.name }) { card ->
                StatCard(card)
            }
        }
    }
}

@Composable
private fun StatCard(card: StatCardState) {
    var expanded by remember { mutableStateOf(false) }
    val color = card.stat.color()

    GlassCard(accent = color, modifier = Modifier.clickable { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Eyebrow("${card.stat.plainName} · ${card.stat.shortLabel}", color = color)
                Spacer(Modifier.height(4.dp))
                Text("Level ${card.level}", color = AscendColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(card.stat.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${card.completionsThisWeek}", color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("this week", color = AscendColors.TextTertiary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(
            progress = if (card.xpSpanForLevel > 0) card.xpIntoLevel.toFloat() / card.xpSpanForLevel else 0f,
            fillColor = color,
        )
        Spacer(Modifier.height(8.dp))
        // What this stat currently *does* — the thing v1 was missing entirely.
        Text(
            StatEffects.describeEffect(card.stat, card.level),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${card.xpIntoLevel} / ${card.xpSpanForLevel} XP", color = AscendColors.TextTertiary, fontSize = 11.sp)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AscendColors.TextTertiary,
                modifier = Modifier.height(16.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                if (card.habits.isEmpty()) {
                    Text("No habits assigned to this stat yet.", color = AscendColors.TextTertiary, fontSize = 12.sp)
                } else {
                    card.habits.forEach { habit ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(habit.name, color = AscendColors.TextSecondary, fontSize = 13.sp)
                            if (habit.isNonNegotiable) {
                                Text("non-negotiable", color = AscendColors.TextTertiary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
