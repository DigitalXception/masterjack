package com.strategy.blackjacktrainer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MistakeEntry(
    val key: String,
    val handLabel: String,
    val dealerLabel: String,
    val correctAction: String,
    val lastChosenAction: String,
    val count: Int,
    val lastSeenMillis: Long
)

data class SessionStats(val attempts: Int, val correct: Int) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else ((correct * 100.0) / attempts).toInt()
}

/** Aggregated correct/attempt counts for one strategy-chart cell, e.g. "Hard 16|10". */
data class ScenarioStat(val key: String, val attempts: Int, val correct: Int) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else ((correct * 100.0) / attempts).toInt()
}

/**
 * Lightweight local persistence — no external database dependency.
 * Everything lives in a single SharedPreferences file as JSON, scoped to this device.
 */
class MistakeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trainer_store", Context.MODE_PRIVATE)

    // ---- Session-wide stats ----

    fun stats(): SessionStats = SessionStats(
        attempts = prefs.getInt(KEY_ATTEMPTS, 0),
        correct = prefs.getInt(KEY_CORRECT, 0)
    )

    fun recordAttempt(wasCorrect: Boolean) {
        prefs.edit()
            .putInt(KEY_ATTEMPTS, prefs.getInt(KEY_ATTEMPTS, 0) + 1)
            .putInt(KEY_CORRECT, prefs.getInt(KEY_CORRECT, 0) + if (wasCorrect) 1 else 0)
            .apply()
    }

    fun resetStats() {
        prefs.edit().putInt(KEY_ATTEMPTS, 0).putInt(KEY_CORRECT, 0).putInt(KEY_HANDS_PLAYED, 0).apply()
    }

    // ---- Rounds played (a round = one full hand, however many decisions it took) ----

    fun handsPlayed(): Int = prefs.getInt(KEY_HANDS_PLAYED, 0)

    fun recordHandPlayed() {
        prefs.edit().putInt(KEY_HANDS_PLAYED, handsPlayed() + 1).apply()
    }

    // ---- Per-scenario accuracy (powers the strategy chart's color coding) ----

    fun recordScenario(handLabel: String, dealerLabel: String, wasCorrect: Boolean) {
        val key = "$handLabel|$dealerLabel"
        val all = loadScenarios().toMutableMap()
        val existing = all[key]
        all[key] = ScenarioStat(
            key = key,
            attempts = (existing?.attempts ?: 0) + 1,
            correct = (existing?.correct ?: 0) + if (wasCorrect) 1 else 0
        )
        saveScenarios(all.values.toList())
    }

    fun allScenarioStats(): Map<String, ScenarioStat> = loadScenarios()

    fun resetScenarioStats() {
        prefs.edit().putString(KEY_SCENARIOS, null).apply()
    }

    private fun loadScenarios(): Map<String, ScenarioStat> {
        val raw = prefs.getString(KEY_SCENARIOS, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = LinkedHashMap<String, ScenarioStat>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stat = ScenarioStat(
                    key = obj.getString("key"),
                    attempts = obj.getInt("attempts"),
                    correct = obj.getInt("correct")
                )
                map[stat.key] = stat
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveScenarios(stats: List<ScenarioStat>) {
        val arr = JSONArray()
        for (s in stats) {
            val obj = JSONObject()
            obj.put("key", s.key)
            obj.put("attempts", s.attempts)
            obj.put("correct", s.correct)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCENARIOS, arr.toString()).apply()
    }

    // ---- Mistake log (aggregated by scenario) ----

    fun recordMistake(
        handLabel: String,
        dealerLabel: String,
        correctAction: String,
        chosenAction: String
    ) {
        val key = "$handLabel|$dealerLabel"
        val all = loadAll().toMutableMap()
        val existing = all[key]
        all[key] = MistakeEntry(
            key = key,
            handLabel = handLabel,
            dealerLabel = dealerLabel,
            correctAction = correctAction,
            lastChosenAction = chosenAction,
            count = (existing?.count ?: 0) + 1,
            lastSeenMillis = System.currentTimeMillis()
        )
        saveAll(all.values.toList())
    }

    fun allMistakes(): List<MistakeEntry> =
        loadAll().values.sortedWith(compareByDescending<MistakeEntry> { it.count }.thenByDescending { it.lastSeenMillis })

    fun clearMistakes() {
        prefs.edit().putString(KEY_MISTAKES, null).apply()
    }

    fun clearEverything() {
        prefs.edit().clear().apply()
    }

    private fun loadAll(): Map<String, MistakeEntry> {
        val raw = prefs.getString(KEY_MISTAKES, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = LinkedHashMap<String, MistakeEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val entry = MistakeEntry(
                    key = obj.getString("key"),
                    handLabel = obj.getString("handLabel"),
                    dealerLabel = obj.getString("dealerLabel"),
                    correctAction = obj.getString("correctAction"),
                    lastChosenAction = obj.getString("lastChosenAction"),
                    count = obj.getInt("count"),
                    lastSeenMillis = obj.getLong("lastSeenMillis")
                )
                map[entry.key] = entry
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveAll(entries: List<MistakeEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            val obj = JSONObject()
            obj.put("key", e.key)
            obj.put("handLabel", e.handLabel)
            obj.put("dealerLabel", e.dealerLabel)
            obj.put("correctAction", e.correctAction)
            obj.put("lastChosenAction", e.lastChosenAction)
            obj.put("count", e.count)
            obj.put("lastSeenMillis", e.lastSeenMillis)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_MISTAKES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_ATTEMPTS = "attempts"
        private const val KEY_CORRECT = "correct"
        private const val KEY_MISTAKES = "mistakes_json"
        private const val KEY_HANDS_PLAYED = "hands_played"
        private const val KEY_SCENARIOS = "scenario_stats_json"
    }
}
