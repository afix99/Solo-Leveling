package com.ascend.app.ui.screens.coach

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.ai.AiProvider
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.AthleteProfile
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.AscendColors

@Composable
fun CoachSetupScreen(repository: AscendRepository) {
    val vm: CoachViewModel = viewModel(factory = SimpleViewModelFactory { CoachViewModel(repository) })
    val uriHandler = LocalUriHandler.current
    val settings = vm.settings

    var provider by remember(settings.provider) {
        mutableStateOf(runCatching { AiProvider.valueOf(settings.provider) }.getOrDefault(AiProvider.OPENROUTER))
    }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by remember(settings.model) { mutableStateOf(settings.model) }

    var age by remember(settings.age) { mutableStateOf(settings.age?.toString() ?: "") }
    var sex by remember(settings.sex) { mutableStateOf(settings.sex ?: "") }
    var height by remember(settings.heightCm) { mutableStateOf(settings.heightCm?.toString() ?: "") }
    var weight by remember(settings.weightKg) { mutableStateOf(settings.weightKg?.toString() ?: "") }
    var goal by remember(settings.goal) { mutableStateOf(settings.goal ?: "") }
    var experience by remember(settings.experience) { mutableStateOf(settings.experience ?: "") }
    var equipment by remember(settings.equipment) { mutableStateOf(settings.equipment ?: "") }
    var dietary by remember(settings.dietaryNotes) { mutableStateOf(settings.dietaryNotes ?: "") }
    var injuries by remember(settings.injuries) { mutableStateOf(settings.injuries ?: "") }

    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) {
        if (saved) {
            kotlinx.coroutines.delay(1800)
            saved = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("COACH SETUP", color = AscendColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }

        item {
            SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
                Eyebrow("Before you connect", color = AscendColors.Amber)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Everything else in ASCEND works with no connection. The Coach is the one " +
                        "exception: when you ask for advice, your habit names, completion rates, " +
                        "stats and any body details you enter are sent to the provider you pick.",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your key is stored in this app's private storage on the device. That's " +
                        "sandboxed from other apps, but it isn't encrypted — use a free-tier key " +
                        "rather than one attached to a big balance.",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }

        item { Eyebrow("Provider") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProvider.entries.forEach { p ->
                    val selected = p == provider
                    GlassCard(
                        accent = if (selected) AscendColors.AccentBlue else AscendColors.Divider,
                        modifier = Modifier.clickable {
                            provider = p
                            if (model.isBlank()) model = p.defaultModel
                        },
                    ) {
                        Text(
                            p.displayName,
                            color = if (selected) AscendColors.AccentBlue else AscendColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(p.notes, color = AscendColors.TextTertiary, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Get a key: ${p.signupUrl}",
                            color = AscendColors.AccentBlue,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                runCatching { uriHandler.openUri(p.signupUrl) }
                            },
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    placeholder = { Text(provider.defaultModel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Leave blank to use ${provider.defaultModel}",
                    color = AscendColors.TextTertiary,
                    fontSize = 10.sp,
                )
            }
        }

        item {
            Eyebrow("Body profile — optional", modifier = Modifier.padding(top = 6.dp))
        }
        item {
            Text(
                "Only used for training, nutrition and recovery advice. Leave blank and those " +
                    "answers stay general.",
                color = AscendColors.TextTertiary,
                fontSize = 11.sp,
            )
        }

        item {
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Age", age, Modifier.weight(1f)) { age = it }
                    NumberField("Height (cm)", height, Modifier.weight(1f)) { height = it }
                    NumberField("Weight (kg)", weight, Modifier.weight(1f)) { weight = it }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = sex,
                    onValueChange = { sex = it },
                    label = { Text("Sex") },
                    placeholder = { Text("for calorie maths") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )

                Spacer(Modifier.height(14.dp))
                ChipRow("Goal", AthleteProfile.goals, goal) { goal = it }
                Spacer(Modifier.height(12.dp))
                ChipRow("Experience", AthleteProfile.experienceLevels, experience) { experience = it }
                Spacer(Modifier.height(12.dp))
                ChipRow("Equipment", AthleteProfile.equipmentOptions, equipment) { equipment = it }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = dietary,
                    onValueChange = { dietary = it },
                    label = { Text("Dietary notes") },
                    placeholder = { Text("vegetarian, halal, allergies...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = injuries,
                    onValueChange = { injuries = it },
                    label = { Text("Injuries or limitations") },
                    placeholder = { Text("bad knee, lower back...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
        }

        item {
            Button(
                onClick = {
                    vm.saveSettings(
                        settings.copy(
                            provider = provider.name,
                            apiKey = apiKey.trim(),
                            model = model.trim(),
                            age = age.toIntOrNull(),
                            sex = sex.ifBlank { null },
                            heightCm = height.toIntOrNull(),
                            weightKg = weight.toIntOrNull(),
                            goal = goal.ifBlank { null },
                            experience = experience.ifBlank { null },
                            equipment = equipment.ifBlank { null },
                            dietaryNotes = dietary.ifBlank { null },
                            injuries = injuries.ifBlank { null },
                        ),
                    )
                    saved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saved) "Saved" else "Save", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() }.take(3)) },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = fieldColors(),
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    // Tapping the active choice clears it, so a field can be
                    // deliberately left blank after being set.
                    onClick = { onSelect(if (selected == option) "" else option) },
                    label = { Text(option, fontSize = 11.sp) },
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
