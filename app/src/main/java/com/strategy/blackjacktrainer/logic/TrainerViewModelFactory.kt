package com.strategy.blackjacktrainer.logic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.strategy.blackjacktrainer.data.DiagnosticSeenStore
import com.strategy.blackjacktrainer.data.MistakeStore

class TrainerViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TrainerViewModel(
            store = MistakeStore(appContext),
            seenStore = DiagnosticSeenStore(appContext)
        ) as T
    }
}
