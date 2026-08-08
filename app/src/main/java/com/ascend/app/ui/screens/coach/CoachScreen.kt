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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ascend.app.domain.AdviceType
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.AscendColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CoachScreen(repository: AscendRepository, onOpenSetup: () -> Unit) {
    val vm: CoachViewModel = viewModel(factory = SimpleViewModelFactory { CoachViewModel(repository) })
    val history by vm.advice.collectAsStateWithLifecycle()
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("COACH", color = AscendColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(
                "Setup",
                color = AscendColors.AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onOpenSetup),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!vm.isConfigured) {
                item {
                    SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Not connected", color = AscendColors.Amber)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The Coach reads your real stats and habits, then asks an AI model for " +
                                "specific advice. It needs an API key you provide — nothing is bundled " +
                                "with the app.",
                            color = AscendColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Open Setup →",
                            color = AscendColors.Amber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = onOpenSetup),
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

            item { Eyebrow("Ask for") }

            items(AdviceType.entries) { type ->
                AdviceButton(
                    type = type,
                    enabled = vm.isConfigured && vm.generating == null,
                    loading = vm.generating == type,
                    onClick = { vm.generate(type) },
                )
            }

            vm.latest?.let { (type, text) ->
                item {
                    SystemPanel(accent = AscendColors.Success, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Eyebrow(type.title, color = AscendColors.Success)
                            Text(
                                "Dismiss",
                                color = AscendColors.TextTertiary,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { vm.dismissLatest() },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        MarkdownText(text)
                    }
                }
            }

            if (history.isNotEmpty()) {
                item { Eyebrow("Saved advice", modifier = Modifier.padding(top = 8.dp)) }
                items(history, key = { it.id }) { entry ->
                    val type = runCatching { AdviceType.valueOf(entry.adviceType) }.getOrNull()
                    val date = Instant.ofEpochMilli(entry.createdAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
                    val expanded = expandedId == entry.id

                    GlassCard(
                        modifier = Modifier.clickable {
                            expandedId = if (expanded) null else entry.id
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    type?.title ?: entry.adviceType,
                                    color = AscendColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(date, color = AscendColors.TextTertiary, fontSize = 10.sp)
                            }
                            Text(
                                "Delete",
                                color = AscendColors.Danger,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { vm.deleteAdvice(entry.id) },
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(10.dp))
                            MarkdownText(entry.content)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Advice is generated by a third-party model and can be wrong. It is not " +
                        "medical advice — check with a professional before making big changes, " +
                        "especially with an existing condition or injury.",
                    color = AscendColors.TextTertiary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun AdviceButton(
    type: AdviceType,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(
        accent = if (enabled) AscendColors.AccentBlue else AscendColors.Divider,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    type.title,
                    color = if (enabled) AscendColors.TextPrimary else AscendColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(type.blurb, color = AscendColors.TextTertiary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            if (loading) {
                CircularProgressIndicator(
                    color = AscendColors.AccentBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else if (type.needsAthleteProfile) {
                Pill("BODY", AscendColors.StatStr)
            }
        }
    }
}

/**
 * Renders the subset of markdown these models actually emit — headings, bullets,
 * numbered lists and **bold**. Full markdown would mean a parser dependency for
 * very little gain.
 */
@Composable
internal fun MarkdownText(text: String) {
    Column {
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(6.dp))

                line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        line.trimStart('#', ' ').stripEmphasis(),
                        color = AscendColors.AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }

                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•", color = AscendColors.TextTertiary, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            line.trimStart().removeRange(0, 2).stripEmphasis(),
                            color = AscendColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }

                line.trimStart().firstOrNull()?.isDigit() == true && line.contains(". ") -> {
                    Text(
                        line.stripEmphasis(),
                        color = AscendColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }

                else -> Text(
                    line.stripEmphasis(),
                    color = AscendColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

/** Strips markdown emphasis markers rather than rendering them literally. */
private fun String.stripEmphasis(): String =
    replace("**", "").replace("__", "").replace("`", "")
