package com.ascend.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.Radius
import com.ascend.app.ui.theme.Space
import com.ascend.app.ui.theme.SurfaceLevel
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.LabelEyebrow

/**
 * The default surface. Almost every card in the app is one of these.
 *
 * The visual weight now comes from [SurfaceLevel] rather than from each caller
 * inventing a border alpha. Three levels is enough to build a hierarchy and
 * few enough that "which one" is never a coin flip:
 *
 *   Resting     - sits in a list, barely separated from the background
 *   Raised      - the card carrying the screen's main answer
 *   Highlighted - something that wants reading now
 *
 * The top edge catches a little more light than the bottom, which is what
 * makes a flat rectangle read as a raised surface rather than a coloured box.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = AscendColors.Divider,
    level: SurfaceLevel = SurfaceLevel.Resting,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderAlpha = when (level) {
        SurfaceLevel.Resting -> 0.22f
        SurfaceLevel.Raised -> 0.4f
        SurfaceLevel.Highlighted -> 0.7f
    }
    val surface = when (level) {
        SurfaceLevel.Resting -> AscendColors.Surface
        SurfaceLevel.Raised -> AscendColors.SurfaceElevated
        SurfaceLevel.Highlighted -> AscendColors.SurfaceElevated
    }
    val tint = if (level == SurfaceLevel.Resting) 0f else 0.06f

    Column(
        modifier = modifier
            .clip(Radius.lg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        surface.lightenBy(0.035f + tint, accent),
                        surface,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = borderAlpha), Radius.lg)
            .padding(Space.lg),
        content = content,
    )
}

/** Blends a hint of [accent] into a surface, for the lit top edge of a card. */
private fun Color.lightenBy(amount: Float, accent: Color): Color = Color(
    red = red + (accent.red - red) * amount,
    green = green + (accent.green - green) * amount,
    blue = blue + (accent.blue - blue) * amount,
    alpha = alpha,
)

/** Section label — small caps, wide tracking, dim — used above every group of content. */
@Composable
fun Eyebrow(text: String, color: Color = AscendColors.TextSecondary, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = LabelEyebrow, color = color, modifier = modifier)
}

/** Animated horizontal XP/progress bar with a soft glow-capable accent color. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = AscendColors.SurfaceElevated2,
    fillColor: Color = AscendColors.AccentBlue,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0.02f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(listOf(fillColor.copy(alpha = 0.7f), fillColor)),
                ),
        )
    }
}

/** Circular XP ring with centered content — used for the Hunter level display. */
@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 148.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = AscendColors.SurfaceElevated2,
    progressColor: Color = AscendColors.AccentBlue,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(700),
        label = "ring-progress",
    )
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = stroke,
            )
        }
        content()
    }
}

/** Small rounded pill, used for rank badges / tags. */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = LabelEyebrow, color = color)
    }
}
