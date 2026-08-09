package com.ascend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.Radius
import com.ascend.app.ui.theme.Space
import com.ascend.app.ui.theme.Type

/**
 * The frame every screen sits in.
 *
 * Sixteen screens previously hand-rolled a title Row, each with its own
 * padding and font size. That is why the app felt like a set of pages rather
 * than one product: the header shifted a few pixels every time you navigated.
 * One scaffold means the title never moves, and a new screen inherits the
 * layout instead of re-deciding it.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(AscendColors.Background)) {
        ScreenHeader(title = title, subtitle = subtitle, actions = actions)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.xl,
                end = Space.xl,
                top = Space.md,
                bottom = Space.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.md),
            content = content,
        )
    }
}

/** Header for screens that manage their own scrolling (chat, focus, forms). */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Space.xl, end = Space.lg, top = Space.xl, bottom = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title.uppercase(),
                color = AscendColors.TextPrimary,
                fontSize = Type.displaySize,
                fontWeight = Type.displayWeight,
                letterSpacing = Type.displayTracking,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = AscendColors.TextTertiary,
                    fontSize = Type.captionSize,
                    lineHeight = Type.captionLineHeight,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * Divides a long screen into named sections.
 *
 * Carries its own space above rather than relying on the caller to remember,
 * which is how section gaps drift apart across screens.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AscendColors.TextTertiary,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Space.sm, bottom = Space.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text.uppercase(),
            color = color,
            fontSize = Type.labelSize,
            fontWeight = Type.labelWeight,
            letterSpacing = Type.labelTracking,
        )
        if (trailing != null) {
            Text(trailing, color = AscendColors.TextTertiary, fontSize = Type.captionSize)
        }
    }
}

/**
 * Segmented control for a small set of choices — ranges, tabs, filters.
 *
 * Replaces the three different hand-built pill rows that had drifted into
 * three different heights.
 */
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AscendColors.AccentBlue,
    fillWidth: Boolean = false,
) {
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(Radius.pill)
            .background(AscendColors.Surface)
            .padding(Space.xxs),
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                    .clip(Radius.pill)
                    .background(if (isSelected) accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = Space.md, vertical = Space.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = if (isSelected) accent else AscendColors.TextTertiary,
                    fontSize = Type.captionSize,
                    fontWeight = Type.titleWeight,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * What a screen shows when it has nothing to show.
 *
 * A blank area reads as a bug; a screen of zeroes reads as failure. Both are
 * worse than saying plainly that there is no data yet and what produces some.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    accent: Color = AscendColors.AccentBlue,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A soft disc behind the title keeps the block from reading as an
        // error message, which a bare centred paragraph tends to.
        Box(
            modifier = Modifier
                .clip(Radius.pill)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.06f)),
                    ),
                )
                .padding(horizontal = Space.lg, vertical = Space.sm),
        ) {
            Text(
                title.uppercase(),
                color = accent,
                fontSize = Type.labelSize,
                fontWeight = Type.labelWeight,
                letterSpacing = Type.labelTracking,
            )
        }
        Spacer(Modifier.height(Space.md))
        Text(
            body,
            color = AscendColors.TextSecondary,
            fontSize = Type.bodySize,
            lineHeight = Type.bodyLineHeight,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(Space.lg))
            Box(
                modifier = Modifier
                    .clip(Radius.pill)
                    .background(accent.copy(alpha = 0.18f))
                    .clickable(onClick = action.second)
                    .padding(horizontal = Space.xl, vertical = Space.md),
            ) {
                Text(
                    action.first,
                    color = accent,
                    fontSize = Type.titleSize,
                    fontWeight = Type.titleWeight,
                )
            }
        }
    }
}

/** Footnote text — the quiet explanation at the bottom of a screen. */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = AscendColors.TextTertiary,
        fontSize = Type.captionSize,
        lineHeight = Type.captionLineHeight,
        modifier = modifier.padding(top = Space.sm),
    )
}
