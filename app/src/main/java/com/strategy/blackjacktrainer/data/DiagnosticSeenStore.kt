package com.strategy.blackjacktrainer.data

import android.content.Context

/**
 * Tracks diagnostic-test-mode decision keys the user has already seen,
 * persisted across sessions via SharedPreferences so the trainer never
 * repeats a scenario until the user resets progress.
 *
 * Key format matches [com.strategy.blackjacktrainer.logic.TrainerViewModel.diagnosticDecisionKey].
 */
class DiagnosticSeenStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(
        "trainer_seen_prefs", Context.MODE_PRIVATE
    )

    fun getSeenKeys(): MutableSet<String> =
        (prefs.getStringSet("seen_keys", emptySet()) ?: emptySet()).toMutableSet()

    fun addKey(key: String) {
        val seen = getSeenKeys()
        if (seen.add(key)) {
            prefs.edit().putStringSet("seen_keys", seen).apply()
        }
    }

    fun clear() {
        prefs.edit().remove("seen_keys").apply()
        return
    }
}
