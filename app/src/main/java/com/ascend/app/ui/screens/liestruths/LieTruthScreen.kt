package com.ascend.app.ui.screens.liestruths

import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.db.LieTruthEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.theme.AscendColors

@Composable
fun LieTruthScreen(repository: AscendRepository) {
    val vm: LieTruthViewModel = viewModel(factory = SimpleViewModelFactory { LieTruthViewModel(repository) })
    val entries by vm.entries.collectAsStateWithLifecycle()
    var lie by remember { mutableStateOf("") }
    var truth by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Text(
            "LIES VS TRUTHS",
            color = AscendColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 4.dp),
        )
        Text(
            "The excuses you tell yourself, and the truth underneath them.",
            color = AscendColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GlassCard(accent = AscendColors.AccentViolet) {
                    Eyebrow("Add a pair")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = lie,
                        onValueChange = { lie = it },
                        label = { Text("The lie") },
                        placeholder = { Text("\"I'll start tomorrow\"") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = truth,
                        onValueChange = { truth = it },
                        label = { Text("The truth") },
                        placeholder = { Text("Action comes first, motivation comes after.") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            vm.add(lie, truth)
                            lie = ""
                            truth = ""
                        },
                        enabled = lie.isNotBlank() && truth.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentViolet),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add")
                    }
                }
            }

            items(entries, key = { it.id }) { entry ->
                LieTruthRow(entry = entry, onToggle = { active -> vm.setActive(entry, active) })
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun LieTruthRow(entry: LieTruthEntity, onToggle: (Boolean) -> Unit) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "“${entry.lieText}”",
                    color = AscendColors.TextTertiary,
                    fontSize = 13.sp,
                    textDecoration = if (!entry.active) TextDecoration.LineThrough else null,
                )
                Spacer(Modifier.height(4.dp))
                Text(entry.truthText, color = AscendColors.TextPrimary, fontSize = 14.sp)
            }
            Switch(
                checked = entry.active,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = AscendColors.AccentViolet),
            )
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AscendColors.SurfaceElevated,
    unfocusedContainerColor = AscendColors.SurfaceElevated,
    focusedTextColor = AscendColors.TextPrimary,
    unfocusedTextColor = AscendColors.TextPrimary,
    focusedIndicatorColor = AscendColors.AccentViolet,
    unfocusedIndicatorColor = AscendColors.Divider,
    cursorColor = AscendColors.AccentViolet,
)
