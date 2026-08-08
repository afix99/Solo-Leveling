package com.ascend.app.ui.screens.shop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.db.HunterProfileEntity
import com.ascend.app.data.db.RewardEntity
import com.ascend.app.data.db.RewardPurchaseEntity
import com.ascend.app.data.repo.AscendRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(private val repository: AscendRepository) : ViewModel() {

    val rewards: StateFlow<List<RewardEntity>> =
        repository.observeRewards().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val purchases: StateFlow<List<RewardPurchaseEntity>> =
        repository.observePurchases().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<HunterProfileEntity?> =
        repository.observeHunterProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Set when a purchase is refused for lack of gold, so the UI can say why. */
    var lastPurchaseFailed by mutableStateOf<String?>(null)
        private set

    fun createReward(name: String, cost: Int) {
        if (name.isBlank() || cost <= 0) return
        viewModelScope.launch { repository.createReward(name, cost) }
    }

    fun purchase(reward: RewardEntity) {
        viewModelScope.launch {
            val ok = repository.purchaseReward(reward.id)
            lastPurchaseFailed = if (ok) null else "Not enough Gold for “${reward.name}”."
        }
    }

    fun clearPurchaseError() {
        lastPurchaseFailed = null
    }

    fun archive(rewardId: Long) {
        viewModelScope.launch { repository.archiveReward(rewardId) }
    }
}
