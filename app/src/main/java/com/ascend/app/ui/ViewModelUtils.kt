package com.ascend.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Tiny factory for ViewModels that just need a constructor argument (our
 * [com.ascend.app.data.repo.AscendRepository]) — avoids pulling in a full DI
 * framework for a single-user, backend-free app. */
class SimpleViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
