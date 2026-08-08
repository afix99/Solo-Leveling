package com.ascend.app.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.DifficultyRating
import com.ascend.app.notifications.FocusSessionState
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.DisplayNumeral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { IDLE, RUNNING, RATING }

@Composable
fun FocusSessionScreen(
    habitId: Long,
    repository: AscendRepository,
    onFinish: () -> Unit,
) {
    DisposableEffect(Unit) {
        FocusSessionState.isActive = true
        onDispose { FocusSessionState.isActive = false }
    }

    val habit by repository.observeHabit(habitId).collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(Phase.IDLE) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var completedFully by remember { mutableStateOf(false) }
    var startTimeMillis by remember { mutableStateOf(0L) }
    var saved by remember { mutableStateOf(false) }

    val plannedMinutes = habit?.targetDurationMinutes ?: 20
    val plannedSeconds = plannedMinutes * 60

    suspend fun saveSession(rating: DifficultyRating?) {
        if (saved) return
        saved = true
        habit?.let {
            repository.recordFocusSession(
                habit = it,
                startTimeEpochMillis = startTimeMillis,
                plannedDurationMinutes = plannedMinutes,
                actualDurationSeconds = elapsedSeconds,
                completedFully = completedFully,
                rating = rating,
            )
        }
    }

    LaunchedEffect(phase) {
        if (phase == Phase.RUNNING) {
            while (elapsedSeconds < plannedSeconds) {
                delay(1000)
                elapsedSeconds++
            }
            completedFully = true
            phase = Phase.RATING
        }
    }

    BackHandler(enabled = phase == Phase.RUNNING) {
        completedFully = false
        phase = Phase.RATING
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AscendColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = phase, label = "focus-phase") { current ->
            when (current) {
                Phase.IDLE -> IdleContent(
                    habitName = habit?.name ?: "Focus",
                    plannedMinutes = plannedMinutes,
                    onStart = {
                        startTimeMillis = System.currentTimeMillis()
                        elapsedSeconds = 0
                        phase = Phase.RUNNING
                    },
                    onCancel = onFinish,
                )
                Phase.RUNNING -> RunningContent(
                    habitName = habit?.name ?: "Focus",
                    elapsedSeconds = elapsedSeconds,
                    plannedSeconds = plannedSeconds,
                    onEndEarly = {
                        completedFully = false
                        phase = Phase.RATING
                    },
                )
                Phase.RATING -> RatingContent(
                    completedFully = completedFully,
                    onRate = { rating ->
                        scope.launch {
                            saveSession(rating)
                            onFinish()
                        }
                    },
                    onSkip = {
                        scope.launch {
                            saveSession(null)
                            onFinish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun IdleContent(habitName: String, plannedMinutes: Int, onStart: () -> Unit, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "FOCUS SESSION",
            color = AscendColors.AccentBlue,
            style = com.ascend.app.ui.theme.LabelEyebrow,
        )
        Spacer(Modifier.height(12.dp))
        Text(habitName, color = AscendColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "$plannedMinutes minutes, uninterrupted. Notifications are paused for the session.",
            color = AscendColors.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
            shape = RoundedCornerShape(50),
        ) {
            Text("Begin", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onCancel) {
            Text("Not now", color = AscendColors.TextSecondary)
        }
    }
}

@Composable
private fun RunningContent(
    habitName: String,
    elapsedSeconds: Int,
    plannedSeconds: Int,
    onEndEarly: () -> Unit,
) {
    val progress = if (plannedSeconds > 0) elapsedSeconds.toFloat() / plannedSeconds else 0f
    val remaining = (plannedSeconds - elapsedSeconds).coerceAtLeast(0)
    val minutes = remaining / 60
    val seconds = remaining % 60

    val infinite = rememberInfiniteTransition(label = "pulse")
    val glow by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow-scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(habitName.uppercase(), color = AscendColors.TextSecondary, style = com.ascend.app.ui.theme.LabelEyebrow)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(glow)
                .clip(CircleShape)
                .background(AscendColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "%d:%02d".format(minutes, seconds),
                style = DisplayNumeral,
                color = AscendColors.AccentBlue,
            )
        }
        Spacer(Modifier.height(28.dp))
        com.ascend.app.ui.components.ProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            fillColor = AscendColors.AccentBlue,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onEndEarly) {
            Text("End session early", color = AscendColors.TextSecondary)
        }
    }
}

@Composable
private fun RatingContent(
    completedFully: Boolean,
    onRate: (DifficultyRating) -> Unit,
    onSkip: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (completedFully) "Session complete" else "Session ended early",
            color = AscendColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "How did that feel?",
            color = AscendColors.TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RatingChip("Too easy", AscendColors.Success) { onRate(DifficultyRating.TOO_EASY) }
            RatingChip("Just right", AscendColors.AccentBlue) { onRate(DifficultyRating.JUST_RIGHT) }
            RatingChip("Too hard", AscendColors.Amber) { onRate(DifficultyRating.TOO_HARD) }
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onSkip) {
            Text("Skip", color = AscendColors.TextTertiary)
        }
    }
}

@Composable
private fun RatingChip(label: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 13.sp)
    }
}
