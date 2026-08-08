package com.ascend.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.StarterHabits
import com.ascend.app.domain.StarterPack
import com.ascend.app.domain.Stat
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color
import kotlinx.coroutines.launch

/**
 * Two steps, not four. The last step *populates the app*, so nobody lands on
 * an empty Today screen wondering what to do.
 */
@Composable
fun OnboardingScreen(repository: AscendRepository, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var hunterName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun finish(pack: StarterPack?) {
        scope.launch {
            pack?.let { repository.applyStarterPack(it) }
            repository.completeOnboarding(hunterName.ifBlank { "Hunter" })
            onComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        AnimatedContent(targetState = step, label = "onboarding-step") { current ->
            when (current) {
                0 -> WelcomeStep(
                    name = hunterName,
                    onNameChange = { hunterName = it },
                    onNext = { step = 1 },
                )
                else -> ChoosePackStep(
                    onPick = { pack -> finish(pack) },
                    onSkip = { finish(null) },
                    onBack = { step = 0 },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "ASCEND",
            color = AscendColors.AccentBlue,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Here's how it works",
            color = AscendColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))

        ExplainerLine("1", "You pick daily habits.", "Some are non-negotiable, some optional.")
        ExplainerLine("2", "Every habit you complete earns XP.", "That XP levels up one of 5 stats.")
        ExplainerLine("3", "Enough XP raises your Rank.", "E → D → C → B → A → S.")
        ExplainerLine("4", "Miss a non-negotiable and you lose XP.", "Plus you get a redemption task tomorrow.")

        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AscendColors.SurfaceElevated,
                unfocusedContainerColor = AscendColors.SurfaceElevated,
                focusedTextColor = AscendColors.TextPrimary,
                unfocusedTextColor = AscendColors.TextPrimary,
                focusedLabelColor = AscendColors.AccentBlue,
                unfocusedLabelColor = AscendColors.TextSecondary,
                focusedIndicatorColor = AscendColors.AccentBlue,
                unfocusedIndicatorColor = AscendColors.Divider,
                cursorColor = AscendColors.AccentBlue,
            ),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next", modifier = Modifier.padding(vertical = 4.dp), fontSize = 15.sp)
        }
    }
}

@Composable
private fun ExplainerLine(number: String, title: String, detail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(AscendColors.AccentBlue.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = AscendColors.AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = AscendColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = AscendColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChoosePackStep(
    onPick: (StarterPack) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Pick a starting set",
                    color = AscendColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can edit, delete or add habits any time. This just gets you started with something real.",
                    color = AscendColors.TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        items(StarterHabits.all.size) { index ->
            val pack = StarterHabits.all[index]
            PackCard(pack = pack, onPick = { onPick(pack) })
        }

        item {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Start empty and add my own", color = AscendColors.TextSecondary, fontSize = 13.sp)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back", color = AscendColors.TextTertiary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PackCard(pack: StarterPack, onPick: () -> Unit) {
    GlassCard(accent = AscendColors.AccentBlue, modifier = Modifier.clickable(onClick = onPick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pack.title, color = AscendColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(pack.subtitle, color = AscendColors.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AscendColors.AccentBlue)
        }
        Spacer(Modifier.height(10.dp))
        pack.habits.take(4).forEach { template ->
            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(template.stat.color()),
                )
                Spacer(Modifier.width(8.dp))
                Text(template.name, color = AscendColors.TextSecondary, fontSize = 12.sp)
            }
        }
        if (pack.habits.size > 4) {
            Text(
                "+ ${pack.habits.size - 4} more",
                color = AscendColors.TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 14.dp),
            )
        }
    }
}
