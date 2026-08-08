package com.ascend.app.ui.screens.habits

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.db.HabitEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.StarterHabits
import com.ascend.app.domain.Stat
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.HabitFormFields
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color

@Composable
fun HabitsScreen(repository: AscendRepository) {
    val vm: HabitsViewModel = viewModel(factory = SimpleViewModelFactory { HabitsViewModel(repository) })
    val habits by vm.habits.collectAsStateWithLifecycle()
    var creatingNew by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("HABITS", color = AscendColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            TextButton(onClick = { creatingNew = !creatingNew; editingId = null }) {
                Text(if (creatingNew) "Cancel" else "+ New", color = AscendColors.AccentBlue)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (creatingNew) {
                item {
                    NewHabitCard(
                        onCreate = { name, stat, nonNeg, focus, minutes ->
                            vm.createHabit(name, stat, nonNeg, focus, minutes)
                            creatingNew = false
                        },
                    )
                }
            }

            if (habits.isEmpty() && !creatingNew) {
                item {
                    GlassCard(accent = AscendColors.AccentBlue) {
                        Text(
                            "No habits yet",
                            color = AscendColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + New above to write your own, or add a ready-made set:",
                            color = AscendColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        StarterHabits.all.forEach { pack ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.applyStarterPack(pack) }
                                    .padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.title, color = AscendColors.TextPrimary, fontSize = 14.sp)
                                    Text(pack.subtitle, color = AscendColors.TextTertiary, fontSize = 11.sp)
                                }
                                Text("Add", color = AscendColors.AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (habits.any { it.isNonNegotiable }) {
                item { Eyebrow("Non-negotiables · miss these and you lose XP") }
            }
            items(habits.filter { it.isNonNegotiable }, key = { it.id }) { habit ->
                HabitListRow(
                    habit = habit,
                    expanded = editingId == habit.id,
                    onClick = { editingId = if (editingId == habit.id) null else habit.id },
                    vm = vm,
                    onSaved = { editingId = null },
                )
            }

            if (habits.any { !it.isNonNegotiable }) {
                item { Spacer(Modifier.height(8.dp)) }
                item { Eyebrow("Other habits · bonus XP, no penalty") }
            }
            items(habits.filterNot { it.isNonNegotiable }, key = { it.id }) { habit ->
                HabitListRow(
                    habit = habit,
                    expanded = editingId == habit.id,
                    onClick = { editingId = if (editingId == habit.id) null else habit.id },
                    vm = vm,
                    onSaved = { editingId = null },
                )
            }

            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun NewHabitCard(onCreate: (String, Stat, Boolean, Boolean, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var stat by remember { mutableStateOf(Stat.VIT) }
    var statTouched by remember { mutableStateOf(false) }
    var nonNeg by remember { mutableStateOf(true) }
    var focus by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf(20) }

    GlassCard(accent = AscendColors.AccentBlue) {
        HabitFormFields(
            name = name,
            onNameChange = { newName ->
                name = newName
                // Auto-file the habit under a sensible stat as you type, until
                // the user picks one themselves.
                if (!statTouched) stat = StarterHabits.suggestStat(newName)
            },
            stat = stat,
            onStatChange = { stat = it; statTouched = true },
            isNonNegotiable = nonNeg,
            onNonNegotiableChange = { nonNeg = it },
            isFocusEnabled = focus,
            onFocusEnabledChange = { focus = it },
            targetMinutes = minutes,
            onTargetMinutesChange = { minutes = it },
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onCreate(name, stat, nonNeg, focus, minutes) },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create habit")
        }
    }
}

@Composable
private fun HabitListRow(
    habit: HabitEntity,
    expanded: Boolean,
    onClick: () -> Unit,
    vm: HabitsViewModel,
    onSaved: () -> Unit,
) {
    GlassCard(accent = habit.stat.color(), modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(habit.name, color = AscendColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (habit.isFocusEnabled) {
                    Text("${habit.targetDurationMinutes ?: 20} min focus session", color = AscendColors.TextTertiary, fontSize = 11.sp)
                }
            }
            Pill(habit.stat.shortLabel, habit.stat.color())
        }

        AnimatedVisibility(visible = expanded) {
            EditHabitPanel(habit = habit, vm = vm, onSaved = onSaved)
        }
    }
}

@Composable
private fun EditHabitPanel(habit: HabitEntity, vm: HabitsViewModel, onSaved: () -> Unit) {
    var name by remember(habit.id) { mutableStateOf(habit.name) }
    var stat by remember(habit.id) { mutableStateOf(habit.stat) }
    var nonNeg by remember(habit.id) { mutableStateOf(habit.isNonNegotiable) }
    var focus by remember(habit.id) { mutableStateOf(habit.isFocusEnabled) }
    var minutes by remember(habit.id) { mutableStateOf(habit.targetDurationMinutes ?: 20) }
    var suggestRaise by remember(habit.id) { mutableStateOf(false) }

    LaunchedEffect(habit.id) {
        suggestRaise = vm.shouldSuggestRaisingTarget(habit.id)
    }

    Column(modifier = Modifier.padding(top = 14.dp)) {
        if (suggestRaise) {
            Text(
                "Three easy sessions in a row — consider raising the target.",
                color = AscendColors.Amber,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        HabitFormFields(
            name = name,
            onNameChange = { name = it },
            stat = stat,
            onStatChange = { stat = it },
            isNonNegotiable = nonNeg,
            onNonNegotiableChange = { nonNeg = it },
            isFocusEnabled = focus,
            onFocusEnabledChange = { focus = it },
            targetMinutes = minutes,
            onTargetMinutesChange = { minutes = it },
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    vm.updateHabit(
                        habit.copy(
                            name = name,
                            stat = stat,
                            isNonNegotiable = nonNeg,
                            isFocusEnabled = focus,
                            targetDurationMinutes = if (focus) minutes else null,
                        ),
                    )
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
            ) {
                Text("Save")
            }
            TextButton(onClick = { vm.archiveHabit(habit.id); onSaved() }) {
                Text("Archive", color = AscendColors.Danger)
            }
        }
    }
}
