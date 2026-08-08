package com.ascend.app.ui.screens.weeklyreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.db.WeeklyReviewEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Stat
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.ProgressBar
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WeeklyReviewScreen(repository: AscendRepository) {
    val vm: WeeklyReviewViewModel = viewModel(factory = SimpleViewModelFactory { WeeklyReviewViewModel(repository) })
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val evidenceLog by vm.evidenceLog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refreshCurrentWeek() }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Text(
            "WEEKLY REVIEW",
            color = AscendColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 4.dp),
        )
        if (reviews.isEmpty()) {
            Text(
                "Your first weekly report appears once you've logged a few days.",
                color = AscendColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(reviews, key = { it.weekStart }) { review ->
                val weekEvidence = remember(review.weekStart, evidenceLog) {
                    val startMillis = LocalDate.parse(review.weekStart).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = LocalDate.parse(review.weekEnd).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    evidenceLog.filter { it.dateEpochMillis in startMillis until endMillis }
                }
                WeekCard(
                    review = review,
                    evidence = weekEvidence.map { it.description },
                    isCurrentWeek = review.weekStart == WeeklyReviewViewModel.currentWeekStart().toString(),
                    onSaveReflection = { note -> vm.saveReflection(review.weekStart, note) },
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun WeekCard(
    review: WeeklyReviewEntity,
    evidence: List<String>,
    isCurrentWeek: Boolean,
    onSaveReflection: (String) -> Unit,
) {
    var note by remember(review.weekStart) { mutableStateOf(review.userReflectionNote) }

    GlassCard(accent = if (isCurrentWeek) AscendColors.AccentBlue else AscendColors.Divider) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow(if (isCurrentWeek) "This week" else "${review.weekStart} → ${review.weekEnd}")
            Text("${review.completionPercent.toInt()}%", color = AscendColors.AccentBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(progress = review.completionPercent / 100f, fillColor = AscendColors.AccentBlue)
        Spacer(Modifier.height(14.dp))

        if (review.xpByStat.isNotEmpty()) {
            Stat.entries.forEach { stat ->
                val xp = review.xpByStat[stat] ?: 0
                if (xp > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stat.shortLabel, color = stat.color(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("+$xp XP", color = AscendColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        review.bestDay?.let { Text("Best day: $it", color = AscendColors.Success, fontSize = 12.sp) }
        review.worstDay?.let { Text("Toughest day: $it", color = AscendColors.TextTertiary, fontSize = 12.sp) }

        if (evidence.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Eyebrow("Evidence")
            evidence.forEach { line ->
                Text("• $line", color = AscendColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Reflection") },
            placeholder = { Text("How did this week go?") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AscendColors.SurfaceElevated,
                unfocusedContainerColor = AscendColors.SurfaceElevated,
                focusedTextColor = AscendColors.TextPrimary,
                unfocusedTextColor = AscendColors.TextPrimary,
                focusedIndicatorColor = AscendColors.AccentBlue,
                unfocusedIndicatorColor = AscendColors.Divider,
                cursorColor = AscendColors.AccentBlue,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Save reflection",
            color = AscendColors.AccentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp).clickable { onSaveReflection(note) },
        )
    }
}
