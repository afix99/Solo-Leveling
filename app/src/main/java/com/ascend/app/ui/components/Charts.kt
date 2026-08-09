package com.ascend.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors

/**
 * Charts drawn directly on a Compose Canvas.
 *
 * No charting library: the shapes here are a grid, some bars and a polyline,
 * and every library capable of drawing them brings a theming system that would
 * fight the app's own. Drawing them by hand is less code than configuring
 * someone else's defaults away, and keeps the visual language consistent.
 */

/**
 * Calendar heatmap, newest column on the right.
 *
 * Colour encodes *how much* of the day was cleared, not merely done/not-done,
 * because a day at 3-of-4 and a day at 0-of-4 are different kinds of day and
 * flattening them loses the thing the user most wants to see. Untracked days
 * are drawn as an empty outline so "no data" never reads as "failed".
 */
@Composable
fun HabitHeatmap(
    /** Oldest first. Each entry: intensity 0f..1f, perfect flag, untracked flag. */
    days: List<Triple<Float, Boolean, Boolean>>,
    modifier: Modifier = Modifier,
    accent: Color = AscendColors.AccentBlue,
    perfectColor: Color = AscendColors.Success,
) {
    if (days.isEmpty()) return
    val rows = 7
    val columns = (days.size + rows - 1) / rows

    Canvas(modifier = modifier.fillMaxWidth().height((rows * 15).dp)) {
        val gap = 3.dp.toPx()
        val cell = ((size.width - gap * (columns - 1)) / columns).coerceAtLeast(1f)
        val radius = CornerRadius(cell * 0.28f, cell * 0.28f)

        days.forEachIndexed { index, (intensity, perfect, untracked) ->
            val column = index / rows
            val row = index % rows
            val topLeft = Offset(column * (cell + gap), row * (cell + gap))

            when {
                untracked -> drawRoundRect(
                    color = AscendColors.Divider.copy(alpha = 0.35f),
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = radius,
                    style = Stroke(width = 1.dp.toPx()),
                )

                perfect -> drawRoundRect(
                    color = perfectColor,
                    topLeft = topLeft,
                    size = Size(cell, cell),
                    cornerRadius = radius,
                )

                else -> {
                    // Floor the alpha so a genuinely-logged miss is still
                    // visible as a filled cell rather than vanishing.
                    val alpha = if (intensity <= 0f) 0.14f else 0.2f + intensity * 0.65f
                    drawRoundRect(
                        color = accent.copy(alpha = alpha),
                        topLeft = topLeft,
                        size = Size(cell, cell),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

/** Key for the heatmap, so the colours mean something without guessing. */
@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LegendSwatch(AscendColors.Success, "perfect")
        LegendSwatch(AscendColors.AccentBlue.copy(alpha = 0.6f), "partial")
        LegendSwatch(AscendColors.AccentBlue.copy(alpha = 0.14f), "missed")
        LegendSwatch(AscendColors.Divider.copy(alpha = 0.5f), "no data")
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(9.dp)) {
            drawRoundRect(color = color, cornerRadius = CornerRadius(3f, 3f))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, color = AscendColors.TextTertiary, fontSize = 9.sp)
    }
}

/**
 * Line chart with a soft fill under it.
 *
 * The y-axis is scaled to the data's own range rather than pinned to zero:
 * over a month of similar days a zero-based axis renders as a flat line and
 * shows nothing. A floor is applied so a completely flat series still draws
 * a sensible mid-height line instead of dividing by zero.
 */
@Composable
fun TrendLine(
    values: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = AscendColors.AccentBlue,
) {
    if (values.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val max = values.max()
        val min = values.min()
        val span = (max - min).coerceAtLeast(1)
        val stepX = size.width / (values.size - 1)

        fun pointAt(index: Int): Offset {
            val normalised = (values[index] - min).toFloat() / span
            return Offset(index * stepX, size.height - normalised * size.height * 0.86f - 4f)
        }

        val line = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until values.size) lineTo(pointAt(i).x, pointAt(i).y)
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(fill, color = color.copy(alpha = 0.13f))
        drawPath(line, color = color, style = Stroke(width = 2.dp.toPx()))

        val last = pointAt(values.size - 1)
        drawCircle(color = color, radius = 3.5.dp.toPx(), center = last)
    }
}

/**
 * Horizontal bar with a label and value — used for weekday adherence.
 *
 * Bars are drawn against a track so a low value still occupies visible space;
 * a bare 8%-wide bar on a dark background reads as an empty row.
 */
@Composable
fun LabelledBar(
    label: String,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: String = "$percent%",
    dimmed: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (dimmed) AscendColors.TextTertiary else AscendColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(38.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(9.dp)) {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(
                    color = AscendColors.SurfaceElevated2,
                    size = size,
                    cornerRadius = radius,
                )
                val width = (size.width * (percent / 100f)).coerceIn(0f, size.width)
                if (width > 0f) {
                    drawRoundRect(
                        color = if (dimmed) color.copy(alpha = 0.35f) else color,
                        size = Size(width.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            trailing,
            color = if (dimmed) AscendColors.TextTertiary else AscendColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp),
        )
    }
}

/** Big number with a caption, for the summary row. */
@Composable
fun StatTile(value: String, caption: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        Text(caption, color = AscendColors.TextTertiary, fontSize = 10.sp)
    }
}
