package com.ascend.app.ui.screens.cloud

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.components.ScreenHeader
import com.ascend.app.ui.theme.AscendColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Cloud backup settings.
 *
 * The screen is written to make the one irreversible thing — losing the Hunter
 * Key — impossible to miss, and to keep the two destructive-adjacent actions
 * (restore, key replacement) behind explicit intent rather than a stray tap.
 */
@Composable
fun CloudScreen(repository: AscendRepository) {
    val vm: CloudViewModel = viewModel(factory = SimpleViewModelFactory { CloudViewModel(repository) })
    val settings by vm.settings.collectAsStateWithLifecycle()

    var urlDraft by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var keyDraft by remember(settings.hunterKey) { mutableStateOf(settings.hunterKey) }
    var confirmRestore by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        ScreenHeader("Cloud")

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SystemPanel(accent = AscendColors.AccentBlue, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Backup", color = AscendColors.AccentBlue)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your progress lives on this phone. A backup copies it to your own " +
                            "server so a lost or wiped device doesn't erase your streak, and " +
                            "gives the Coach a longer history to reason about.",
                        color = AscendColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            vm.message?.let { text ->
                item {
                    GlassCard(
                        accent = if (vm.isError) AscendColors.Danger else AscendColors.Success,
                        modifier = Modifier.clickable { vm.clearMessage() },
                    ) {
                        Text(
                            text,
                            color = if (vm.isError) AscendColors.Danger else AscendColors.Success,
                            fontSize = 12.sp,
                        )
                        Text("Tap to dismiss", color = AscendColors.TextTertiary, fontSize = 10.sp)
                    }
                }
            }

            item {
                GlassCard {
                    OutlinedTextField(
                        value = urlDraft,
                        onValueChange = { urlDraft = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://am-system-five.vercel.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = cloudFieldColors(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Save",
                            color = AscendColors.AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { vm.saveUrl(urlDraft) },
                        )
                        Text(
                            "Test connection",
                            color = AscendColors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                vm.saveUrl(urlDraft)
                                vm.testConnection()
                            },
                        )
                        if (vm.busy == CloudBusy.TESTING) {
                            CircularProgressIndicator(
                                color = AscendColors.AccentBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            item {
                SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Hunter Key", color = AscendColors.Amber)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "This key is the only thing that unlocks your backup. The server " +
                            "stores just a hash of it, so nobody — including whoever runs the " +
                            "server — can recover it for you. Write it down somewhere safe.",
                        color = AscendColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    if (settings.hunterKey.isBlank()) {
                        Text(
                            "Generate a Hunter Key",
                            color = AscendColors.Amber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { vm.generateKeyIfAbsent() },
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("…or paste one from another device:", color = AscendColors.TextTertiary, fontSize = 11.sp)
                    } else {
                        Text(
                            settings.hunterKey,
                            color = AscendColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Restoring on another phone? Enter this exact key there.",
                            color = AscendColors.TextTertiary,
                            fontSize = 11.sp,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text("Hunter Key") },
                        placeholder = { Text("XXXXXX-XXXXXX-XXXXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = cloudFieldColors(),
                    )
                    Text(
                        "Save key",
                        color = AscendColors.AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { vm.setKey(keyDraft) },
                    )
                }
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Back up automatically",
                                color = AscendColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                            Text(
                                "Once a day, after the midnight rollover.",
                                color = AscendColors.TextTertiary,
                                fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = settings.autoBackup,
                            onCheckedChange = { vm.setAutoBackup(it) },
                        )
                    }
                }
            }

            item {
                GlassCard(accent = AscendColors.Success) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vm.backupNow() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Back up now",
                                color = AscendColors.Success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                            val last = settings.lastBackupAtEpochMillis
                            Text(
                                if (last == null) {
                                    "Never backed up."
                                } else {
                                    "Last: " + Instant.ofEpochMilli(last)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) +
                                        (settings.lastBackupStatus
                                            ?.takeIf { it != "OK" }
                                            ?.let { " — $it" } ?: "")
                                },
                                color = AscendColors.TextTertiary,
                                fontSize = 11.sp,
                            )
                        }
                        if (vm.busy == CloudBusy.BACKING_UP) {
                            CircularProgressIndicator(
                                color = AscendColors.Success,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(accent = AscendColors.Amber) {
                    Text(
                        "Restore from backup",
                        color = AscendColors.Amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "Merges the backup into what is on this phone. Habits are matched by " +
                            "name, and stat XP only ever moves up — restoring cannot cost you " +
                            "progress made since the backup.",
                        color = AscendColors.TextTertiary,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (confirmRestore) {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text(
                                "Yes, restore",
                                color = AscendColors.Danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    confirmRestore = false
                                    vm.restoreNow()
                                },
                            )
                            Text(
                                "Cancel",
                                color = AscendColors.TextTertiary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { confirmRestore = false },
                            )
                        }
                    } else {
                        Text(
                            "Restore…",
                            color = AscendColors.Amber,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { confirmRestore = true },
                        )
                    }
                    if (vm.busy == CloudBusy.RESTORING) {
                        Spacer(Modifier.height(6.dp))
                        CircularProgressIndicator(
                            color = AscendColors.Amber,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun cloudFieldColors() = TextFieldDefaults.colors(
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
