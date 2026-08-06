package com.ascend.app.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import com.ascend.app.ui.theme.LabelEyebrow
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.DisplayNumeral
import com.ascend.app.ui.theme.color
import kotlinx.coroutines.delay

/**
 * Full-screen "System" overlay for a Hunter Rank crossing — the emotional
 * payoff beat, see design spec §6. Auto-dismisses after ~2.2s.
 */
@Composable
fun RankUpOverlay(rank: Rank, onDismiss: () -> Unit) {
    LaunchedEffect(rank) {
        delay(2200)
        onDismiss()
    }

    val infinite = rememberInfiniteTransition(label = "rankup-glow")
    val glow by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "rankup-scale",
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + scaleIn(tween(400), initialScale = 0.85f),
        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RANK UP", style = LabelEyebrow, color = AscendColors.AccentViolet)
                Spacer(Modifier.height(12.dp))
                Text(
                    rank.name,
                    style = DisplayNumeral,
                    color = rank.color(),
                    fontSize = 96.sp,
                    modifier = Modifier.scale(glow),
                )
                Spacer(Modifier.height(12.dp))
                Text(rank.displayName, color = AscendColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Small transient toast for an individual stat leveling up. */
@Composable
fun StatLevelUpToast(stat: Stat, level: Int, onDismiss: () -> Unit) {
    LaunchedEffect(stat, level) {
        delay(1800)
        onDismiss()
    }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.9f),
        exit = fadeOut(tween(250)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AscendColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stat.shortLabel, color = stat.color(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("  leveled up to  ", color = AscendColors.TextSecondary, fontSize = 13.sp)
                Text("Lv. $level", color = AscendColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
