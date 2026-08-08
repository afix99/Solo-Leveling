package com.ascend.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.Stat
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color

/**
 * The full "create/edit a habit" field set — name, stat, non-negotiable
 * toggle, and optional Focus Session settings. Shared by Onboarding step 3
 * and the Habits screen's add/edit sheet so both stay in sync.
 */
@Composable
fun HabitFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    stat: Stat,
    onStatChange: (Stat) -> Unit,
    isNonNegotiable: Boolean,
    onNonNegotiableChange: (Boolean) -> Unit,
    isFocusEnabled: Boolean,
    onFocusEnabledChange: (Boolean) -> Unit,
    targetMinutes: Int,
    onTargetMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Habit name") },
            placeholder = { Text("e.g. Cold shower") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )

        Column {
            Eyebrow("Which stat does this build?")
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                Stat.entries.forEach { s ->
                    FilterChip(
                        selected = stat == s,
                        onClick = { onStatChange(s) },
                        label = { Text(s.plainName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = s.color().copy(alpha = 0.22f),
                            selectedLabelColor = s.color(),
                            containerColor = AscendColors.SurfaceElevated,
                            labelColor = AscendColors.TextSecondary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stat.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
        }

        ToggleRow(
            title = "Non-negotiable",
            subtitle = "Missing it costs XP and queues a Penalty Quest",
            checked = isNonNegotiable,
            onCheckedChange = onNonNegotiableChange,
        )

        ToggleRow(
            title = "Enable Focus Session",
            subtitle = "Launch a full-screen timer for this habit",
            checked = isFocusEnabled,
            onCheckedChange = onFocusEnabledChange,
        )

        if (isFocusEnabled) {
            Column {
                Eyebrow("Target duration: $targetMinutes min")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 45, 60).forEach { minutes ->
                        FilterChip(
                            selected = targetMinutes == minutes,
                            onClick = { onTargetMinutesChange(minutes) },
                            label = { Text("${minutes}m", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AscendColors.AccentBlue.copy(alpha = 0.22f),
                                selectedLabelColor = AscendColors.AccentBlue,
                                containerColor = AscendColors.SurfaceElevated,
                                labelColor = AscendColors.TextSecondary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AscendColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = AscendColors.TextTertiary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AscendColors.AccentBlue),
        )
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AscendColors.SurfaceElevated,
    unfocusedContainerColor = AscendColors.SurfaceElevated,
    focusedTextColor = AscendColors.TextPrimary,
    unfocusedTextColor = AscendColors.TextPrimary,
    focusedLabelColor = AscendColors.AccentBlue,
    unfocusedLabelColor = AscendColors.TextSecondary,
    focusedIndicatorColor = AscendColors.AccentBlue,
    unfocusedIndicatorColor = AscendColors.Divider,
    cursorColor = AscendColors.AccentBlue,
)
