package com.ascend.app.ui.screens.help

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.Pill
import com.ascend.app.ui.theme.AscendColors
import com.ascend.app.ui.theme.color

/** Plain-language reference for anything in the app that isn't self-evident. */
@Composable
fun HowItWorksScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "HOW IT WORKS",
                color = AscendColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item {
            GlassCard {
                Eyebrow("The basics")
                Spacer(Modifier.height(10.dp))
                Bullet("Tap the circle next to a habit to mark it done. You earn XP straight away.")
                Bullet("Non-negotiables are worth 20 XP. Optional habits are worth 10 XP.")
                Bullet("XP goes into whichever stat that habit is tagged to.")
            }
        }

        item {
            GlassCard {
                Eyebrow("The 5 stats")
                Spacer(Modifier.height(10.dp))
                Stat.entries.forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${stat.plainName} (${stat.shortLabel})",
                                color = AscendColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(stat.description, color = AscendColors.TextTertiary, fontSize = 11.sp)
                        }
                        Pill(stat.shortLabel, stat.color())
                    }
                }
            }
        }

        item {
            GlassCard {
                Eyebrow("Ranks")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your Hunter Level comes from all your stats combined. Cross these levels and your Rank goes up:",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Rank.entries.forEach { rank ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Pill(rank.displayName, rank.color())
                        Text(
                            "Level ${rank.minHunterLevel}+",
                            color = AscendColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(accent = AscendColors.Amber) {
                Eyebrow("Penalties", color = AscendColors.Amber)
                Spacer(Modifier.height(10.dp))
                Bullet("Leave a non-negotiable unchecked at midnight and you lose 15 XP from that stat.")
                Bullet("You can never drop below the level you've already reached — a bad day dents progress, it doesn't erase it.")
                Bullet("The next day you get a Penalty Quest: the same habit, pinned at the top, worth 1.5× XP if you do it.")
                Bullet("Miss non-negotiables 3+ times in a week and the app suggests you cut back rather than piling on more.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentBlue) {
                Eyebrow("Focus sessions", color = AscendColors.AccentBlue)
                Spacer(Modifier.height(10.dp))
                Bullet("Habits with a ▶ button open a full-screen timer.")
                Bullet("Notifications are muted while it runs. Finish the whole thing for bonus XP.")
                Bullet("Leaving early still logs the time, but no bonus.")
                Bullet("Rate it afterwards. Three 'too easy' ratings in a row and the app suggests raising the target.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Eyebrow("Streaks & evidence", color = AscendColors.AccentViolet)
                Spacer(Modifier.height(10.dp))
                Bullet("Streaks count consecutive days per habit and are tracked separately from XP.")
                Bullet("The Evidence Log fills in automatically when you redeem a penalty, hit a 7/30/100-day streak, or finish a focus session.")
                Bullet("It's a record of things you actually did — proof, not affirmations.")
                Spacer(Modifier.height(4.dp))
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("•", color = AscendColors.TextTertiary, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = AscendColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
