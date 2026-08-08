package com.ascend.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.LabelEyebrow

/** One announcement to show in the System window. */
data class SystemMessage(
    val heading: String,
    val title: String,
    val body: String? = null,
    val lines: List<String> = emptyList(),
    val accent: Color = AscendColors.AccentBlue,
    val confirmLabel: String = "Confirm",
)

/**
 * The manhwa's blue notification window — cut corners, glowing border, all-caps
 * heading. This is the app's signature visual moment, used for level-ups,
 * rank-ups, quest arrivals and achievement unlocks.
 */
@Composable
fun SystemWindow(
    message: SystemMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "system-glow")
    val glow by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) + scaleIn(tween(280), initialScale = 0.9f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clip(CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                message.accent.copy(alpha = 0.16f),
                                AscendColors.Surface.copy(alpha = 0.97f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.5.dp,
                        color = message.accent.copy(alpha = glow),
                        shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "[ ${message.heading.uppercase()} ]",
                    style = LabelEyebrow,
                    color = message.accent.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    message.title,
                    color = AscendColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                message.body?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        color = AscendColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }

                if (message.lines.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(message.accent.copy(alpha = 0.3f)),
                    )
                    Spacer(Modifier.height(12.dp))
                    message.lines.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                line,
                                color = message.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .background(message.accent.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            message.accent.copy(alpha = 0.6f),
                            CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                ) {
                    Text(
                        message.confirmLabel,
                        color = message.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** A compact inline version used for the Daily Quest card on Today. */
@Composable
fun SystemPanel(
    accent: Color = AscendColors.AccentBlue,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.14f), AscendColors.Surface),
                ),
            )
            .border(
                1.dp,
                accent.copy(alpha = 0.45f),
                CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
            )
            .padding(16.dp),
        content = content,
    )
}
