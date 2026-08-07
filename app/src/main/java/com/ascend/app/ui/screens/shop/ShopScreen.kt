package com.ascend.app.ui.screens.shop

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.ascend.app.data.db.RewardEntity
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Economy
import com.ascend.app.ui.SimpleViewModelFactory
import com.ascend.app.ui.components.Eyebrow
import com.ascend.app.ui.components.GlassCard
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.AscendColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ShopScreen(repository: AscendRepository) {
    val vm: ShopViewModel = viewModel(factory = SimpleViewModelFactory { ShopViewModel(repository) })
    val rewards by vm.rewards.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val gold = profile?.gold ?: 0

    var creating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SHOP", color = AscendColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            TextButton(onClick = { creating = !creating }) {
                Text(if (creating) "Cancel" else "+ New reward", color = AscendColors.AccentBlue)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SystemPanel(accent = AscendColors.Amber, modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Balance", color = AscendColors.Amber)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$gold Gold",
                        color = AscendColors.Amber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Earned by completing habits. Spend it on rewards you set for yourself.",
                        color = AscendColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            vm.lastPurchaseFailed?.let { error ->
                item {
                    GlassCard(accent = AscendColors.Danger, modifier = Modifier.clickable { vm.clearPurchaseError() }) {
                        Text(error, color = AscendColors.Danger, fontSize = 13.sp)
                        Text("Tap to dismiss", color = AscendColors.TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            if (creating) {
                item { NewRewardCard(onCreate = { name, cost -> vm.createReward(name, cost); creating = false }) }
            }

            if (rewards.isEmpty() && !creating) {
                item {
                    GlassCard(accent = AscendColors.AccentBlue) {
                        Text(
                            "No rewards yet",
                            color = AscendColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Set things you actually want — a takeaway, a night off, a new game — " +
                                "and a Gold price. Earning them beats buying them on a whim.",
                            color = AscendColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { creating = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Add your first reward") }
                    }
                }
            }

            items(rewards, key = { it.id }) { reward ->
                RewardRow(
                    reward = reward,
                    affordable = gold >= reward.goldCost,
                    onBuy = { vm.purchase(reward) },
                    onArchive = { vm.archive(reward.id) },
                )
            }

            if (purchases.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Eyebrow("Purchase history")
                }
                items(purchases.take(20), key = { it.id }) { purchase ->
                    val date = Instant.ofEpochMilli(purchase.purchasedAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d"))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(purchase.rewardName, color = AscendColors.TextSecondary, fontSize = 12.sp)
                        Text("−${purchase.goldSpent}g · $date", color = AscendColors.TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun NewRewardCard(onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf(100) }

    GlassCard(accent = AscendColors.AccentBlue) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Reward") },
            placeholder = { Text("e.g. Takeaway night") },
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
        Spacer(Modifier.height(14.dp))
        Eyebrow("Cost: $cost Gold")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Economy.suggestedRewardCosts.forEach { suggested ->
                FilterChip(
                    selected = cost == suggested,
                    onClick = { cost = suggested },
                    label = { Text("${suggested}g", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AscendColors.Amber.copy(alpha = 0.22f),
                        selectedLabelColor = AscendColors.Amber,
                        containerColor = AscendColors.SurfaceElevated,
                        labelColor = AscendColors.TextSecondary,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onCreate(name, cost) },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.AccentBlue),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add reward") }
    }
}

@Composable
private fun RewardRow(
    reward: RewardEntity,
    affordable: Boolean,
    onBuy: () -> Unit,
    onArchive: () -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }

    GlassCard(
        accent = if (affordable) AscendColors.Amber else AscendColors.Divider,
        modifier = Modifier.clickable { showActions = !showActions },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reward.name,
                    color = if (affordable) AscendColors.TextPrimary else AscendColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                if (reward.timesPurchased > 0) {
                    Text(
                        "Claimed ${reward.timesPurchased}×",
                        color = AscendColors.TextTertiary,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (affordable) AscendColors.Amber.copy(alpha = 0.2f) else AscendColors.SurfaceElevated2,
                    )
                    .clickable(enabled = affordable, onClick = onBuy)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    if (affordable) "Buy · ${reward.goldCost}g" else "${reward.goldCost}g",
                    color = if (affordable) AscendColors.Amber else AscendColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (showActions) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Remove reward",
                color = AscendColors.Danger,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onArchive),
            )
        }
    }
}
