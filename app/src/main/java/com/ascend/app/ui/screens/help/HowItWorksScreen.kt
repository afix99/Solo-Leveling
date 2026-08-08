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
import com.ascend.app.domain.Achievements
import com.ascend.app.domain.GateRank
import com.ascend.app.domain.HunterClass
import com.ascend.app.domain.Shadows
import com.ascend.app.domain.Skill
import com.ascend.app.domain.StatAllocation
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import com.ascend.app.domain.StatEffects
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
                Bullet("Tap the circle next to a habit to mark it done. You earn XP and Gold straight away.")
                Bullet("Non-negotiables pay 20 XP and 10 Gold. Optional habits pay 10 XP and 5 Gold.")
                Bullet("XP goes into whichever stat that habit is tagged to. Gold goes to your balance.")
                Bullet("Streaks add up to +2% XP per consecutive day on that habit.")
            }
        }

        item {
            GlassCard {
                Eyebrow("The 5 stats — and what they do")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Levelling a stat isn't cosmetic. Each one changes how the game plays:",
                    color = AscendColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
                Stat.entries.forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                            Text(
                                StatEffects.describeEffect(stat, 10).replace("+", "at Lv.10: +"),
                                color = stat.color(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Pill(stat.shortLabel, stat.color())
                    }
                }
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentBlue) {
                Eyebrow("Stat points — the build", color = AscendColors.AccentBlue)
                Spacer(Modifier.height(10.dp))
                Bullet("Every Hunter Level hands you ${StatAllocation.POINTS_PER_LEVEL} points to spend in System → Status.")
                Bullet("A point raises a stat exactly like levelling it naturally, and each stat does something different — so where they go is a real decision.")
                Bullet("Pour them into Mind to snowball XP, Body for Gold, Health to survive bad weeks, Focus for deep work, Movement for streaks.")
                Bullet("Changed your mind? Respec refunds everything for ${StatAllocation.RESPEC_GOLD_COST} Gold.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Eyebrow("Skills — rewriting the rules", color = AscendColors.AccentViolet)
                Spacer(Modifier.height(10.dp))
                Bullet("You earn a skill point every 2 levels, plus two every time your Rank rises.")
                Bullet("${Skill.entries.size} skills across 3 tiers. None of them add a side-game — each one changes a rule you already play by.")
                Bullet("Iron Body lets a streak survive a miss. Second Wind waives your first penalty each week. Ruler's Authority makes redemption pay 2.5x.")
                Bullet("They stack with your stats and your class, so builds compound.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Eyebrow("Shadows — failure becomes an asset", color = AscendColors.AccentViolet)
                Spacer(Modifier.height(10.dp))
                Bullet("Miss a non-negotiable, then complete its Penalty Quest the next day, and that habit joins you as a Shadow.")
                Bullet("Each Shadow permanently boosts its habit's stat by ${Shadows.PERCENT_PER_RANK}% per rank, capped at ${Shadows.MAX_PERCENT_PER_STAT}% per stat.")
                Bullet("Redeeming the same habit again promotes the Shadow instead of adding a duplicate.")
                Bullet("The point: coming back from a slip leaves you stronger than never slipping would have.")
            }
        }

        item {
            GlassCard(accent = AscendColors.Amber) {
                Eyebrow("Gates — optional risk", color = AscendColors.Amber)
                Spacer(Modifier.height(10.dp))
                Bullet("Stake Gold on holding every non-negotiable for a run of days.")
                Bullet("Clear it and the stake comes back multiplied, plus XP. Miss a single day and the stake is gone.")
                Bullet("${GateRank.entries.size} ranks, from a ${GateRank.E.days}-day E-Rank to a ${GateRank.S.days}-day S-Rank.")
                Bullet("Completely optional. Ignoring Gates costs you nothing — they exist so you can choose your own pressure, not so the app can apply it.")
            }
        }

        item {
            GlassCard(accent = AscendColors.Amber) {
                Eyebrow("Gold & the Shop", color = AscendColors.Amber)
                Spacer(Modifier.height(10.dp))
                Bullet("Every completed habit pays Gold as well as XP.")
                Bullet("In the Shop you define your own real-world rewards and set a Gold price.")
                Bullet("Buy them when you've earned them. That's the whole point — the reward is the payoff for the work, not a whim.")
                Bullet("Body (STR) raises how much Gold everything pays.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Eyebrow("Daily Quests", color = AscendColors.AccentViolet)
                Spacer(Modifier.height(10.dp))
                Bullet("The System issues one bonus quest each day, on top of your normal list.")
                Bullet("It changes daily — clear every non-negotiable, hit a specific habit, finish a focus session, and so on.")
                Bullet("Complete it and tap Claim for bonus XP spread across all stats, plus Gold.")
            }
        }

        item {
            GlassCard(accent = AscendColors.AccentViolet) {
                Eyebrow("Job Change", color = AscendColors.AccentViolet)
                Spacer(Modifier.height(10.dp))
                Bullet("At ${HunterClass.UNLOCK_RANK.displayName} you unlock a class choice.")
                HunterClass.entries.filter { it != HunterClass.NONE }.forEach {
                    Bullet("${it.displayName} — ${it.tagline}")
                }
                Bullet("Class perks stack on top of your stat bonuses. You can switch any time.")
            }
        }

        item {
            GlassCard(accent = AscendColors.Success) {
                Eyebrow("Achievements & Titles", color = AscendColors.Success)
                Spacer(Modifier.height(10.dp))
                Bullet("${Achievements.all.size} achievements to unlock, each paying Gold.")
                Bullet("Some grant Titles — equip one and it shows next to your name.")
                Bullet("Find them under More → Achievements & Titles.")
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
