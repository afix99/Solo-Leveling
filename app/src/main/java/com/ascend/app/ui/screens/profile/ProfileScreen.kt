package com.ascend.app.ui.screens.profile

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.notifications.NotificationScheduler
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.theme.AscendColors
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: AscendRepository,
    onOpenWeeklyReview: () -> Unit,
    onOpenLiesTruths: () -> Unit,
) {
    val vm: ProfileViewModel = viewModel(factory = SimpleViewModelFactory { ProfileViewModel(repository) })
    val profile by vm.profile.collectAsStateWithLifecycle()
    val evidenceLog by vm.evidenceLog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportMessage by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val current = profile ?: HunterProfileEntity()
    var name by remember(current.hunterName) { mutableStateOf(current.hunterName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "PROFILE",
                color = AscendColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }

        item {
            GlassCard {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hunter name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Save name",
                    color = AscendColors.AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { vm.save(current.copy(hunterName = name)) },
                )
            }
        }

        item {
            GlassCard {
                Eyebrow("Notifications")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Daily reminders", color = AscendColors.TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = current.notificationsEnabled,
                        onCheckedChange = {
                            vm.save(current.copy(notificationsEnabled = it))
                            NotificationScheduler.scheduleAll(context)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = AscendColors.AccentBlue),
                    )
                }
                Spacer(Modifier.height(14.dp))
                TimeStepper(
                    label = "Morning reminder",
                    hour = current.morningReminderHour,
                    minute = current.morningReminderMinute,
                    onChange = { h, m ->
                        vm.save(current.copy(morningReminderHour = h, morningReminderMinute = m))
                        NotificationScheduler.scheduleAll(context)
                    },
                )
                Spacer(Modifier.height(10.dp))
                TimeStepper(
                    label = "Evening nudge",
                    hour = current.eveningReminderHour,
                    minute = current.eveningReminderMinute,
                    onChange = { h, m ->
                        vm.save(current.copy(eveningReminderHour = h, eveningReminderMinute = m))
                        NotificationScheduler.scheduleAll(context)
                    },
                )
            }
        }

        item {
            GlassCard {
                MenuRow("Weekly Review", onOpenWeeklyReview)
                Spacer(Modifier.height(4.dp))
                MenuRow("Lies vs Truths", onOpenLiesTruths)
            }
        }

        item {
            GlassCard {
                Eyebrow("Evidence Log")
                Spacer(Modifier.height(8.dp))
                if (evidenceLog.isEmpty()) {
                    Text("Nothing yet — it fills in as you redeem quests, hit streaks, and finish focus sessions.", color = AscendColors.TextTertiary, fontSize = 12.sp)
                } else {
                    evidenceLog.take(10).forEach { entry ->
                        val date = Instant.ofEpochMilli(entry.dateEpochMillis).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d"))
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.description, color = AscendColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(date, color = AscendColors.TextTertiary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Eyebrow("Data")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val json = vm.exportJson()
                            val file = File(context.getExternalFilesDir(null), "ascend_export.json")
                            file.writeText(json)
                            exportMessage = "Saved to ${file.path}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export data", color = AscendColors.TextPrimary)
                }
                exportMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = AscendColors.Success, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("Reset all data", color = AscendColors.Danger)
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all data?") },
            text = { Text("This deletes every habit, log, and stat. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    vm.resetAllData {}
                }) { Text("Reset", color = AscendColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AscendColors.TextPrimary, fontSize = 14.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AscendColors.TextTertiary)
    }
}

@Composable
private fun TimeStepper(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AscendColors.TextSecondary, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { val t = shiftMinutes(hour, minute, -15); onChange(t.first, t.second) }) {
                Text("−", color = AscendColors.AccentBlue, fontSize = 18.sp)
            }
            Text("%02d:%02d".format(hour, minute), color = AscendColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { val t = shiftMinutes(hour, minute, 15); onChange(t.first, t.second) }) {
                Text("+", color = AscendColors.AccentBlue, fontSize = 18.sp)
            }
        }
    }
}

private fun shiftMinutes(hour: Int, minute: Int, deltaMinutes: Int): Pair<Int, Int> {
    var total = (hour * 60 + minute + deltaMinutes) % (24 * 60)
    if (total < 0) total += 24 * 60
    return total / 60 to total % 60
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AscendColors.SurfaceElevated,
    unfocusedContainerColor = AscendColors.SurfaceElevated,
    focusedTextColor = AscendColors.TextPrimary,
    unfocusedTextColor = AscendColors.TextPrimary,
    focusedIndicatorColor = AscendColors.AccentBlue,
    unfocusedIndicatorColor = AscendColors.Divider,
    cursorColor = AscendColors.AccentBlue,
)
