package com.ascend.app.ui.screens.coach

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ascend.app.ui.theme.AscendColors

/** Questions worth asking that a blank box never suggests. */
private val PROMPTS = listOf(
    "Why do I keep breaking my streak?",
    "What should I cut to make this sustainable?",
    "Design my week around my worst habit.",
    "Am I progressing fast enough?",
    "What's the one thing I should fix first?",
)

@Composable
fun ChatScreen(repository: AscendRepository) {
    val vm: CoachViewModel = viewModel(factory = SimpleViewModelFactory { CoachViewModel(repository) })
    val messages by vm.chat.collectAsStateWithLifecycle()
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Follow the conversation as it grows, including while a reply lands.
    LaunchedEffect(messages.size, vm.sending) {
        val target = messages.size + if (vm.sending) 1 else 0
        if (target > 0) listState.animateScrollToItem(target)
    }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    if (settings.systemVoice) "THE SYSTEM" else "ASK THE COACH",
                    color = AscendColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Text(
                    "It already knows your stats, habits and misses.",
                    color = AscendColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
            if (messages.isNotEmpty()) {
                Text(
                    "Clear",
                    color = AscendColors.Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { vm.clearChat() },
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    SystemPanel(accent = AscendColors.AccentViolet, modifier = Modifier.fillMaxWidth()) {
                        Eyebrow(
                            if (settings.systemVoice) "Connection established" else "Ready",
                            color = AscendColors.AccentViolet,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ask anything. Unlike a normal chatbot, this one is handed your " +
                                "actual completion rates, streaks and stat levels before it " +
                                "answers — so \"why am I stalling?\" gets a real answer.",
                            color = AscendColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
                item { Eyebrow("Try asking", modifier = Modifier.padding(top = 6.dp)) }
                items(PROMPTS) { prompt ->
                    GlassCard(modifier = Modifier.clickable { vm.sendChat(prompt) }) {
                        Text(prompt, color = AscendColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    text = message.content,
                    fromUser = message.role == "user",
                    systemVoice = settings.systemVoice,
                )
            }

            if (vm.sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = AscendColors.AccentViolet,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (settings.systemVoice) "Processing…" else "Thinking…",
                            color = AscendColors.TextTertiary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            vm.error?.let { message ->
                item {
                    GlassCard(
                        accent = AscendColors.Danger,
                        modifier = Modifier.clickable { vm.clearError() },
                    ) {
                        Text(message, color = AscendColors.Danger, fontSize = 12.sp)
                        Text("Tap to dismiss", color = AscendColors.TextTertiary, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Ask something…", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AscendColors.SurfaceElevated,
                    unfocusedContainerColor = AscendColors.SurfaceElevated,
                    focusedTextColor = AscendColors.TextPrimary,
                    unfocusedTextColor = AscendColors.TextPrimary,
                    focusedIndicatorColor = AscendColors.AccentViolet,
                    unfocusedIndicatorColor = AscendColors.Divider,
                    cursorColor = AscendColors.AccentViolet,
                ),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank() && !vm.sending && settings.apiKey.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (canSend) AscendColors.AccentViolet else AscendColors.SurfaceElevated2,
                    )
                    .clickable(enabled = canSend) {
                        vm.sendChat(draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) AscendColors.Background else AscendColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean, systemVoice: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (fromUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp))
                    .background(AscendColors.AccentBlue.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text, color = AscendColors.TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        } else {
            // The System's replies get the angular panel; the user's stay round,
            // so the two voices are distinguishable at a glance.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(
                        if (systemVoice) {
                            CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                        } else {
                            RoundedCornerShape(16.dp)
                        },
                    )
                    .background(AscendColors.Surface)
                    .padding(14.dp),
            ) {
                MarkdownText(text)
            }
        }
    }
}
