package com.ascend.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.HabitFormFields
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(repository: AscendRepository, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var hunterName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        AnimatedContent(targetState = step, label = "onboarding-step") { current ->
            when (current) {
                0 -> WelcomeStep(onNext = { step = 1 })
                1 -> NameStep(
                    name = hunterName,
                    onNameChange = { hunterName = it },
                    onNext = { step = 2 },
                    onBack = { step = 0 },
                )
                2 -> ExplainerStep(onNext = { step = 3 }, onBack = { step = 1 })
                else -> FirstHabitsStep(
                    repository = repository,
                    onFinish = {
                        scope.launch {
                            repository.completeOnboarding(hunterName.ifBlank { "Hunter" })
                            onComplete()
                        }
                    },
                    onBack = { step = 2 },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ASCEND",
            color = AscendColors.AccentBlue,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "A discipline system that levels you up for real.\n" +
                "Every habit you keep raises a stat. Every one you break costs you something.",
            color = AscendColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(48.dp))
        PrimaryButton("Get started", onNext)
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Eyebrow("Step 1 of 3")
        Spacer(Modifier.height(12.dp))
        Text("What should we call you?", color = AscendColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Hunter name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        StepNav(onBack = onBack, onNext = onNext, nextEnabled = true)
    }
}

@Composable
private fun ExplainerStep(onNext: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow("Step 2 of 3")
        Spacer(Modifier.height(12.dp))
        Text("Stats & Rank", color = AscendColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Every habit you create is tagged to one of 5 stats. Keep them up and your Hunter Rank climbs from E to S.",
            color = AscendColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(20.dp))
        GlassCard {
            Stat.entries.forEach { stat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stat.displayName, color = AscendColors.TextPrimary, fontSize = 14.sp)
                    Pill(stat.shortLabel, stat.color())
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Rank.entries.forEach { rank -> Pill(rank.name, rank.color()) }
            }
        }
        Spacer(Modifier.weight(1f))
        StepNav(onBack = onBack, onNext = onNext, nextEnabled = true)
    }
}

@Composable
private fun FirstHabitsStep(repository: AscendRepository, onFinish: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var stat by remember { mutableStateOf(Stat.STR) }
    var isNonNegotiable by remember { mutableStateOf(true) }
    var isFocusEnabled by remember { mutableStateOf(false) }
    var targetMinutes by remember { mutableStateOf(20) }
    var addedNames by remember { mutableStateOf(listOf<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow("Step 3 of 3")
        Spacer(Modifier.height(12.dp))
        Text("Add your first habits", color = AscendColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add a few now, or skip — you can always add more later.",
            color = AscendColors.TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                HabitFormFields(
                    name = name,
                    onNameChange = { name = it },
                    stat = stat,
                    onStatChange = { stat = it },
                    isNonNegotiable = isNonNegotiable,
                    onNonNegotiableChange = { isNonNegotiable = it },
                    isFocusEnabled = isFocusEnabled,
                    onFocusEnabledChange = { isFocusEnabled = it },
                    targetMinutes = targetMinutes,
                    onTargetMinutesChange = { targetMinutes = it },
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val habitName = name
                            scope.launch {
                                repository.createHabit(
                                    name = habitName,
                                    stat = stat,
                                    isNonNegotiable = isNonNegotiable,
                                    reminderHour = null,
                                    reminderMinute = null,
                                    isFocusEnabled = isFocusEnabled,
                                    targetDurationMinutes = if (isFocusEnabled) targetMinutes else null,
                                )
                            }
                            addedNames = addedNames + habitName
                            name = ""
                            isFocusEnabled = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.SurfaceElevated2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ Add habit", color = AscendColors.AccentBlue)
                }
                Spacer(Modifier.height(16.dp))
            }
            items(addedNames) { added ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AscendColors.Success)
                    Spacer(Modifier.width(8.dp))
                    Text(added, color = AscendColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }

        StepNav(onBack = onBack, onNext = onFinish, nextEnabled = true, nextLabel = "Enter the System")
    }
}

@Composable
private fun StepNav(onBack: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean, nextLabel: String = "Continue") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Back", color = AscendColors.TextSecondary) }
        PrimaryButton(nextLabel, onNext, enabled = nextEnabled)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
    }
}
