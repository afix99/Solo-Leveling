package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.AscendColors

data class DockItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Replaces the stock Material navigation bar, which looked like every other
 * app's. This is a floating cut-corner panel matching the System window
 * language: angular corners, a hairline accent border, and a glowing marker
 * that sits *above* the active item rather than a filled pill behind it.
 */
@Composable
fun SystemDock(
    items: List<DockItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 4.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AscendColors.SurfaceElevated,
                            AscendColors.Surface,
                        ),
                    ),
                )
                .border(1.dp, AscendColors.AccentBlue.copy(alpha = 0.22f), shape)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                DockTab(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DockTab(
    item: DockItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (selected) AscendColors.AccentBlue else AscendColors.TextTertiary,
        animationSpec = tween(220),
        label = "dock-tint",
    )
    val markerAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "dock-marker",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = tween(220),
        label = "dock-scale",
    )

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interaction,
                // No ripple — the marker and tint carry the state, and a
                // rectangular ripple would fight the angular panel.
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Glowing marker above the active tab.
        Box(
            modifier = Modifier
                .alpha(markerAlpha)
                .width(20.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AscendColors.AccentBlue.copy(alpha = 0.1f),
                            AscendColors.AccentBlue,
                            AscendColors.AccentBlue.copy(alpha = 0.1f),
                        ),
                    ),
                ),
        )
        Spacer(Modifier.height(7.dp))
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(21.dp).scale(iconScale),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label.uppercase(),
            color = tint,
            fontSize = 9.sp,
            letterSpacing = 0.9.sp,
        )
    }
}
