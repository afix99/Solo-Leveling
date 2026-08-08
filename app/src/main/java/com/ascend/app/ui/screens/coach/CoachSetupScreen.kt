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
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()

    var provider by remember(settings.provider) {
        mutableStateOf(runCatching { AiProvider.valueOf(settings.provider) }.getOrDefault(AiProvider.OPENROUTER))
    }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    // Older builds let the model be typed freely, so a stored value that isn't a
    // real model id for this provider is repaired rather than left to 401.
    var model by remember(settings.model, settings.provider) {
        val stored = settings.model
        val current = runCatching { AiProvider.valueOf(settings.provider) }
            .getOrDefault(AiProvider.OPENROUTER)
        // Only repair values that clearly aren't model ids (old builds allowed
        // free text). A real id that's merely missing from the suggestions is kept.
        val looksLikeModelId = stored.isNotBlank() && !stored.contains(" ")
        mutableStateOf(if (looksLikeModelId) stored else current.defaultModel)
    }

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

        item {
            Column {
                Eyebrow("Step 1 — pick where your key came from")
                Spacer(Modifier.height(4.dp))
                Text(
                    "These are separate companies. A key from one will not work on another.",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProvider.entries.forEach { p ->
                    val selected = p == provider
                    GlassCard(
                        accent = if (selected) AscendColors.AccentBlue else AscendColors.Divider,
                        modifier = Modifier.clickable {
                            // Carry the model across only if it was hand-typed;
                            // otherwise a provider swap would send the previous
                            // provider's model id and fail with a 404.
                            val wasDefault = model.isBlank() ||
                                AiProvider.entries.any { it.defaultModel == model }
                            provider = p
                            if (wasDefault) model = p.defaultModel
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                p.displayName,
                                color = if (selected) AscendColors.AccentBlue else AscendColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                if (selected) "SELECTED" else "TAP TO USE",
                                color = if (selected) AscendColors.AccentBlue else AscendColors.TextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
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
                    label = { Text("Step 2 — paste your API key") },
                    placeholder = { Text("paste the key from your provider") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )

                // Keys are recognisable by prefix, so a key pasted under the
                // wrong provider is caught here instead of coming back as a
                // baffling 401.
                val detected = AiProvider.detectFromKey(apiKey)
                if (detected != null && detected != provider) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "That looks like a ${detected.displayName} key, but " +
                            "${provider.displayName} is selected. They are different " +
                            "services — a key from one will not work on the other.",
                        color = AscendColors.Amber,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Switch to ${detected.displayName}",
                        color = AscendColors.Amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            provider = detected
                            model = detected.defaultModel
                        },
                    )
                }

                Spacer(Modifier.height(16.dp))
                Eyebrow("Model")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Which AI model answers you — not a name for your key.",
                    color = AscendColors.TextTertiary,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(8.dp))

                // Suggestions until the real catalogue is fetched; provider
                // model lists change often enough that baked-in ids go stale.
                val choices = vm.availableModels.ifEmpty { provider.commonModels }
                ModelChips(
                    models = choices,
                    selected = model,
                    onSelect = { model = it },
                )

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { vm.loadModels() },
                    enabled = !vm.loadingModels && settings.apiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (vm.loadingModels) "Loading…" else "Load my models",
                        color = AscendColors.AccentBlue,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (vm.availableModels.isEmpty()) {
                        "Save your key, then load the exact models it can use. " +
                            "The chips above are only suggestions and may be out of date."
                    } else {
                        "Showing the real models your key can call."
                    },
                    color = AscendColors.TextTertiary,
                    fontSize = 10.sp,
                )
                vm.modelsMessage?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg,
                        color = if (vm.availableModels.isEmpty()) AscendColors.Danger else AscendColors.Success,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { vm.clearModelsMessage() },
                    )
                }
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "System voice",
                            color = AscendColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Answers arrive as clipped System notifications rather than plain " +
                                "coaching prose. The advice underneath is identical either way.",
                            color = AscendColors.TextTertiary,
                            fontSize = 11.sp,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = settings.systemVoice,
                        onCheckedChange = { vm.setSystemVoice(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedTrackColor = AscendColors.AccentViolet,
                        ),
                    )
                }
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

        vm.testResult?.let { result ->
            item {
                GlassCard(
                    accent = if (result.startsWith("Connected")) AscendColors.Success else AscendColors.Danger,
                    modifier = Modifier.clickable { vm.clearTestResult() },
                ) {
                    Text(
                        result,
                        color = if (result.startsWith("Connected")) AscendColors.Success else AscendColors.Danger,
                        fontSize = 12.sp,
                    )
                    Text("Tap to dismiss", color = AscendColors.TextTertiary, fontSize = 10.sp)
                }
            }
        }

        item {
            if (settings.apiKey.isNotBlank()) {
                Text(
                    "Saved key ends in \u2026${settings.apiKey.takeLast(4)}",
                    color = AscendColors.Success,
                    fontSize = 11.sp,
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

        item {
            OutlinedButton(
                onClick = { vm.testConnection() },
                enabled = !vm.testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (vm.testing) "Testing\u2026" else "Test connection",
                    color = AscendColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Save first, then test. This sends one tiny request so a wrong key or " +
                    "model shows up here instead of failing later.",
                color = AscendColors.TextTertiary,
                fontSize = 10.sp,
            )
        }
    }
}

/** Model IDs are exact strings the provider must recognise, so they're picked
 * from a list rather than typed. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ModelChips(models: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        models.forEach { candidate ->
            FilterChip(
                selected = selected == candidate,
                onClick = { onSelect(candidate) },
                label = { Text(candidate, fontSize = 10.sp) },
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
