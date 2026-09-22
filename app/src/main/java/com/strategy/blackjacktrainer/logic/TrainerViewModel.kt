package com.strategy.blackjacktrainer.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.strategy.blackjacktrainer.data.MistakeEntry
import com.strategy.blackjacktrainer.data.MistakeStore
import com.strategy.blackjacktrainer.data.PlayingCard
import com.strategy.blackjacktrainer.data.Suit
import com.strategy.blackjacktrainer.data.ScenarioStat
import com.strategy.blackjacktrainer.data.SessionStats
import com.strategy.blackjacktrainer.data.dealNewHand
import com.strategy.blackjacktrainer.data.randomCard

data class Feedback(
    val wasCorrect: Boolean,
    val chosen: Action,
    val correct: Action,
    val note: String?
)

data class DecisionHistoryEntry(
    val handNumber: Int,
    val playerLabel: String,
    val dealerLabel: String,
    val chosen: Action,
    val correct: Action,
    val wasCorrect: Boolean
)

enum class TrainingMode { RANDOM, DIAGNOSTIC }
enum class DiagnosticPhase { FIRST_PASS, TRANSITION, CORRECTION, COMPLETE }

data class DiagnosticCase(
    val id: String,
    val playerCards: List<PlayingCard>,
    val dealerCard: PlayingCard,
    val label: String
)

data class DiagnosticDecisionCase(
    val id: String,
    val playerCards: List<PlayingCard>,
    val dealerCard: PlayingCard,
    val canDouble: Boolean,
    val canSplit: Boolean,
    val canSurrender: Boolean,
    val label: String
)

class TrainerViewModel(private val store: MistakeStore) : ViewModel() {

    // ---- Round state ----

    var dealer by mutableStateOf<PlayingCard>(dealNewHand().dealer)
        private set

    var hands by mutableStateOf<List<PlayerHand>>(emptyList())
        private set

    var activeHandIndex by mutableStateOf(0)
        private set

    var feedback by mutableStateOf<Feedback?>(null)
        private set

    var roundComplete by mutableStateOf(false)
        private set

    // ---- Session-wide stats ----

    var stats by mutableStateOf(store.stats())
        private set

    var handsPlayed by mutableStateOf(store.handsPlayed())
        private set

    var streak by mutableStateOf(0)
        private set

    var mistakes by mutableStateOf<List<MistakeEntry>>(store.allMistakes())
        private set

    var scenarioStats by mutableStateOf<Map<String, ScenarioStat>>(store.allScenarioStats())
        private set

    var decisionHistory by mutableStateOf<List<DecisionHistoryEntry>>(emptyList())
        private set

    // ---- Training mode ----

    var trainingMode by mutableStateOf(TrainingMode.RANDOM)
        private set

    var diagnosticPhase by mutableStateOf(DiagnosticPhase.FIRST_PASS)
        private set

    var diagnosticCases by mutableStateOf<List<DiagnosticCase>>(emptyList())
        private set

    var diagnosticIndex by mutableStateOf(0)
        private set

    var diagnosticFirstPassCorrect by mutableStateOf(0)
        private set

    var diagnosticFirstPassAttempts by mutableStateOf(0)
        private set

    /** Missed decision-level cases, keyed by the exact graph scenario/state. */
    var diagnosticMissCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    private var diagnosticDecisionCases by mutableStateOf<Map<String, DiagnosticDecisionCase>>(emptyMap())
    private var diagnosticCanDoubleOverride: Boolean? = null
    private var diagnosticCanSplitOverride: Boolean? = null
    private var diagnosticCanSurrenderOverride: Boolean? = null
    private var pendingDiagnosticAction: Action? = null
    private var currentCorrectionId: String? = null

    init {
        diagnosticCases = buildDiagnosticCases()
        startNewRound()
    }

    val activeHand: PlayerHand get() = hands[activeHandIndex]
    val activeHandRead get() = Strategy.readHand(activeHand.cards)
    val isMultiHand: Boolean get() = hands.size > 1

    val canDoubleNow: Boolean get() = diagnosticCanDoubleOverride ?: activeHand.canDouble
    val canSplitNow: Boolean
        get() = diagnosticCanSplitOverride ?: (activeHand.cards.size == 2 &&
            hands.size == 1 &&
            activeHandRead.category == HandCategory.PAIR)
    val canSurrenderNow: Boolean
        get() = diagnosticCanSurrenderOverride ?: (hands.size == 1 &&
            activeHand.cards.size == 2 &&
            dealer.strategyLabel != "A")

    val diagnosticTotal: Int get() = diagnosticCases.size
    val diagnosticCurrentNumber: Int get() = (diagnosticIndex + 1).coerceAtMost(diagnosticTotal)
    val diagnosticScoreText: String get() = "$diagnosticFirstPassCorrect/$diagnosticFirstPassAttempts"
    val diagnosticRemainingCorrections: Int get() = diagnosticMissCounts.size
    val diagnosticDecisionCount: Int get() = diagnosticFirstPassAttempts
    val currentDiagnosticCase: DiagnosticCase? get() = diagnosticCases.getOrNull(diagnosticIndex)

    private fun startNewRound() {
        val dealt = dealNewHand()
        dealer = dealt.dealer
        hands = listOf(PlayerHand(cards = dealt.player))
        activeHandIndex = 0
        feedback = null
        roundComplete = false
    }

    fun changeTrainingMode(mode: TrainingMode) {
        if (mode == trainingMode) return
        trainingMode = mode
        feedback = null
        if (mode == TrainingMode.RANDOM) {
            diagnosticPhase = DiagnosticPhase.FIRST_PASS
            diagnosticMissCounts = emptyMap()
            diagnosticDecisionCases = emptyMap()
            clearDiagnosticOverrides()
            startNewRound()
        } else {
            startDiagnostic()
        }
    }

    fun restartDiagnostic() {
        trainingMode = TrainingMode.DIAGNOSTIC
        startDiagnostic()
    }

    private fun startDiagnostic() {
        diagnosticCases = buildDiagnosticCases()
        diagnosticIndex = 0
        diagnosticFirstPassCorrect = 0
        diagnosticFirstPassAttempts = 0
        diagnosticMissCounts = emptyMap()
        diagnosticDecisionCases = emptyMap()
        clearDiagnosticOverrides()
        pendingDiagnosticAction = null
        diagnosticPhase = DiagnosticPhase.FIRST_PASS
        loadDiagnosticCase(0)
    }

    private fun loadDiagnosticCase(index: Int) {
        val testCase = diagnosticCases.getOrNull(index) ?: return
        diagnosticIndex = index
        dealer = testCase.dealerCard
        hands = listOf(PlayerHand(cards = testCase.playerCards))
        activeHandIndex = 0
        feedback = null
        roundComplete = false
        clearDiagnosticOverrides()
        pendingDiagnosticAction = null
        currentCorrectionId = null
    }

    private fun clearDiagnosticOverrides() {
        diagnosticCanDoubleOverride = null
        diagnosticCanSplitOverride = null
        diagnosticCanSurrenderOverride = null
    }

    private fun loadDiagnosticCorrectionCase() {
        val candidateId = diagnosticMissCounts.entries
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            ?.key

        val candidate = candidateId?.let { diagnosticDecisionCases[it] } ?: run {
            diagnosticPhase = DiagnosticPhase.COMPLETE
            feedback = null
            clearDiagnosticOverrides()
            return
        }

        currentCorrectionId = candidate.id
        dealer = candidate.dealerCard
        hands = listOf(PlayerHand(cards = candidate.playerCards))
        activeHandIndex = 0
        diagnosticCanDoubleOverride = candidate.canDouble
        diagnosticCanSplitOverride = candidate.canSplit
        diagnosticCanSurrenderOverride = candidate.canSurrender
        feedback = null
        roundComplete = false
        pendingDiagnosticAction = null
    }

    fun choose(action: Action) {
        if (feedback?.wasCorrect == false) return

        if (trainingMode == TrainingMode.DIAGNOSTIC) {
            chooseDiagnostic(action)
        } else {
            chooseRandom(action)
        }
    }

    private fun chooseRandom(action: Action) {
        if (roundComplete) return

        val hand = activeHand
        val handRead = Strategy.readHand(hand.cards)
        val decision = Strategy.correctDecision(
            playerCards = hand.cards,
            dealerCard = dealer,
            canDouble = canDoubleNow,
            canSplit = canSplitNow,
            canSurrender = canSurrenderNow
        )
        val correct = action == decision.action
        streak = if (correct) streak + 1 else 0

        decisionHistory = (decisionHistory + DecisionHistoryEntry(
            handNumber = handsPlayed + 1,
            playerLabel = handRead.label,
            dealerLabel = dealer.strategyLabel,
            chosen = action,
            correct = decision.action,
            wasCorrect = correct
        )).takeLast(30)

        recordAttempt(handRead.label, action, decision, correct)

        if (correct) {
            applyAction(action)
            feedback = Feedback(
                wasCorrect = true,
                chosen = action,
                correct = decision.action,
                note = decision.note
            )
            continueRound(clearFeedback = false)
        } else {
            // Do not apply a wrong action until the user dismisses the blocking popup.
            feedback = Feedback(
                wasCorrect = false,
                chosen = action,
                correct = decision.action,
                note = decision.note
            )
        }
    }

    private fun chooseDiagnostic(action: Action) {
        if (diagnosticPhase != DiagnosticPhase.FIRST_PASS && diagnosticPhase != DiagnosticPhase.CORRECTION) return
        if (activeHand.finished) return

        val hand = activeHand
        val handRead = Strategy.readHand(hand.cards)
        val canDouble = canDoubleNow
        val canSplit = canSplitNow
        val canSurrender = canSurrenderNow
        val decision = Strategy.correctDecision(
            playerCards = hand.cards,
            dealerCard = dealer,
            canDouble = canDouble,
            canSplit = canSplit,
            canSurrender = canSurrender
        )
        val correct = action == decision.action

        decisionHistory = (decisionHistory + DecisionHistoryEntry(
            handNumber = handsPlayed + 1,
            playerLabel = handRead.label,
            dealerLabel = dealer.strategyLabel,
            chosen = action,
            correct = decision.action,
            wasCorrect = correct
        )).takeLast(30)

        recordAttempt(handRead.label, action, decision, correct)

        if (diagnosticPhase == DiagnosticPhase.FIRST_PASS) {
            diagnosticFirstPassAttempts++
            if (correct) diagnosticFirstPassCorrect++

            if (!correct) {
                val key = diagnosticDecisionKey(handRead, dealer.strategyLabel, canDouble, canSplit, canSurrender)
                val updated = diagnosticMissCounts.toMutableMap()
                updated[key] = (updated[key] ?: 0) + 1
                diagnosticMissCounts = updated
                diagnosticDecisionCases = diagnosticDecisionCases + (key to DiagnosticDecisionCase(
                    id = key,
                    playerCards = hand.cards,
                    dealerCard = dealer,
                    canDouble = canDouble,
                    canSplit = canSplit,
                    canSurrender = canSurrender,
                    label = handRead.label
                ))
            }

            feedback = Feedback(
                wasCorrect = correct,
                chosen = action,
                correct = decision.action,
                note = decision.note
            )
            if (correct) {
                applyAction(action)
                advanceDiagnosticHandOrCase()
            } else {
                pendingDiagnosticAction = action
            }
            return
        }

        // Correction is decision-level: answer only this missed graph case.
        val correctionId = currentCorrectionId ?: return
        if (!correct) {
            diagnosticMissCounts = diagnosticMissCounts.toMutableMap().also {
                it[correctionId] = (it[correctionId] ?: 0) + 1
            }
            feedback = Feedback(false, action, decision.action, decision.note)
        } else {
            diagnosticMissCounts = diagnosticMissCounts.toMutableMap().also { it.remove(correctionId) }
            feedback = Feedback(true, action, decision.action, decision.note)
            currentCorrectionId = null
            if (diagnosticMissCounts.isEmpty()) {
                diagnosticPhase = DiagnosticPhase.COMPLETE
                clearDiagnosticOverrides()
                return
            }
            loadDiagnosticCorrectionCase()
        }
    }

    /**
     * After an incorrect first-pass answer, Continue applies that choice and the hand
     * keeps going. The next decision is graded independently.
     */
    private fun continueDiagnosticWrongDecision() {
        if (diagnosticPhase == DiagnosticPhase.CORRECTION) {
            feedback = null
            return
        }

        val action = pendingDiagnosticAction ?: return
        pendingDiagnosticAction = null
        feedback = null
        applyAction(action)
        advanceDiagnosticHandOrCase()
    }

    private fun diagnosticDecisionKey(
        handRead: HandRead,
        dealerLabel: String,
        canDouble: Boolean,
        canSplit: Boolean,
        canSurrender: Boolean
    ): String {
        return "${handRead.label}|$dealerLabel|D$canDouble|P$canSplit|R$canSurrender"
    }

    /** Diagnostic first pass continues the entire hand, grading each decision independently. */
    private fun advanceDiagnosticHandOrCase() {
        if (diagnosticPhase != DiagnosticPhase.FIRST_PASS) return

        while (activeHand.finished) {
            val nextIdx = hands.indexOfFirst { !it.finished }
            if (nextIdx == -1) {
                completeDiagnosticCase()
                return
            }
            activeHandIndex = nextIdx
            var next = hands[nextIdx]
            if (next.cards.size == 1) {
                next = next.copy(cards = next.cards + randomCard())
                if (next.isSplitAceHand || Strategy.handValue(next.cards).first == 21) {
                    next = next.copy(finished = true)
                }
                updateHand(nextIdx, next)
            }
        }
    }

    private fun completeDiagnosticCase() {
        store.recordHandPlayed()
        handsPlayed = store.handsPlayed()
        roundComplete = false
        feedback = null
        pendingDiagnosticAction = null
        clearDiagnosticOverrides()

        if (diagnosticIndex == diagnosticCases.lastIndex) {
            diagnosticPhase = if (diagnosticMissCounts.isEmpty()) {
                DiagnosticPhase.COMPLETE
            } else {
                DiagnosticPhase.TRANSITION
            }
            return
        }
        loadDiagnosticCase(diagnosticIndex + 1)
    }

    private fun recordAttempt(
        handLabel: String,
        action: Action,
        decision: Decision,
        correct: Boolean
    ) {
        store.recordAttempt(correct)
        store.recordScenario(handLabel, dealer.strategyLabel, correct)
        stats = store.stats()
        scenarioStats = store.allScenarioStats()

        if (!correct) {
            store.recordMistake(
                handLabel = handLabel,
                dealerLabel = dealer.strategyLabel,
                correctAction = decision.action.label,
                chosenAction = action.label
            )
            mistakes = store.allMistakes()
        }
    }

    /** Mutates the hand/round state to reflect the action just graded. */
    private fun applyAction(action: Action) {
        val idx = activeHandIndex
        val hand = hands[idx]
        when (action) {
            Action.STAND -> updateHand(idx, hand.copy(finished = true))

            Action.SURRENDER -> updateHand(idx, hand.copy(finished = true, surrendered = true))

            Action.HIT -> {
                val newCards = hand.cards + randomCard()
                val autoStop = Strategy.handValue(newCards).first >= 21
                updateHand(idx, hand.copy(cards = newCards, finished = autoStop))
            }

            Action.DOUBLE -> {
                val newCards = hand.cards + randomCard()
                updateHand(idx, hand.copy(cards = newCards, doubled = true, finished = true))
            }

            Action.SPLIT -> {
                val c1 = hand.cards[0]
                val c2 = hand.cards[1]
                val isAcePair = c1.rank == "A"
                val firstHandCards = listOf(c1, randomCard())
                val firstHandTotal21 = Strategy.handValue(firstHandCards).first == 21

                val handA = PlayerHand(
                    cards = firstHandCards,
                    isFromSplit = true,
                    isSplitAceHand = isAcePair,
                    finished = isAcePair || firstHandTotal21
                )
                val handB = PlayerHand(
                    cards = listOf(c2),
                    isFromSplit = true,
                    isSplitAceHand = isAcePair
                )
                hands = listOf(handA, handB)
                activeHandIndex = 0
            }
        }
    }

    private fun updateHand(index: Int, newHand: PlayerHand) {
        hands = hands.toMutableList().also { it[index] = newHand }
    }

    /** Called when the user taps Continue after an incorrect decision. */
    fun continueRound(clearFeedback: Boolean = true) {
        if (trainingMode == TrainingMode.DIAGNOSTIC) {
            if (feedback?.wasCorrect == false) {
                continueDiagnosticWrongDecision()
            }
            return
        }

        // A wrong decision is still played out so the trainer can continue the hand.
        // This also prevents the wrong-answer popup from trapping the user on a live hand.
        if (feedback?.wasCorrect == false) {
            val wrongAction = feedback?.chosen ?: return
            feedback = null
            applyAction(wrongAction)
        } else if (clearFeedback) {
            feedback = null
        }

        val current = hands[activeHandIndex]
        if (!current.finished) return

        val nextIdx = hands.indexOfFirst { !it.finished }
        if (nextIdx == -1) {
            store.recordHandPlayed()
            handsPlayed = store.handsPlayed()
            roundComplete = false
            startNewRound()
            return
        }

        activeHandIndex = nextIdx
        var next = hands[nextIdx]
        if (next.cards.size == 1) {
            next = next.copy(cards = next.cards + randomCard())
            if (next.isSplitAceHand || Strategy.handValue(next.cards).first == 21) {
                next = next.copy(finished = true)
            }
            updateHand(nextIdx, next)
        }
    }

    fun advanceDiagnosticTransition() {
        if (diagnosticPhase == DiagnosticPhase.TRANSITION) {
            diagnosticPhase = DiagnosticPhase.CORRECTION
            loadDiagnosticCorrectionCase()
        }
    }

    fun refreshMistakes() {
        mistakes = store.allMistakes()
    }

    fun clearMistakes() {
        store.clearMistakes()
        mistakes = store.allMistakes()
    }

    fun refreshScenarioStats() {
        scenarioStats = store.allScenarioStats()
    }

    fun resetScenarioStats() {
        store.resetScenarioStats()
        scenarioStats = store.allScenarioStats()
    }

    fun resetStats() {
        store.resetStats()
        stats = store.stats()
        handsPlayed = store.handsPlayed()
        streak = 0
    }

    private fun buildDiagnosticCases(): List<DiagnosticCase> {
        val result = mutableListOf<DiagnosticCase>()

        // 20/21 are deliberately excluded because the correct decision is always Stand.
        // 10s are also excluded from the pair suite because they represent a 20-total hand
        // and therefore add no decision value for this diagnostic.
        for (total in 5..19) {
            for (dealerLabel in Strategy.dealerColumns) {
                result += DiagnosticCase(
                    id = "H:$total:$dealerLabel",
                    playerCards = hardRepresentative(total),
                    dealerCard = dealerCard(dealerLabel),
                    label = "Hard $total vs $dealerLabel"
                )
            }
        }

        for (total in 13..19) {
            for (dealerLabel in Strategy.dealerColumns) {
                result += DiagnosticCase(
                    id = "S:$total:$dealerLabel",
                    playerCards = softRepresentative(total),
                    dealerCard = dealerCard(dealerLabel),
                    label = "Soft $total vs $dealerLabel"
                )
            }
        }

        for (pair in listOf("2", "3", "4", "5", "6", "7", "8", "9", "A")) {
            for (dealerLabel in Strategy.dealerColumns) {
                result += DiagnosticCase(
                    id = "P:$pair:$dealerLabel",
                    playerCards = pairRepresentative(pair),
                    dealerCard = dealerCard(dealerLabel),
                    label = if (pair == "A") "Pair of Aces vs $dealerLabel" else "Pair of ${pair}s vs $dealerLabel"
                )
            }
        }

        // Each case appears exactly once in a pass, but every new diagnosis uses a fresh random order.
        return result.shuffled()
    }

    private fun card(rank: String, suit: Suit = Suit.SPADES): PlayingCard = PlayingCard(rank, suit)

    private fun dealerCard(label: String): PlayingCard = card(label)

    private fun hardRepresentative(total: Int): List<PlayingCard> = when (total) {
        5 -> listOf(card("2"), card("3"))
        6 -> listOf(card("2"), card("4"))
        7 -> listOf(card("2"), card("5"))
        8 -> listOf(card("3"), card("5"))
        9 -> listOf(card("4"), card("5"))
        10 -> listOf(card("2"), card("8"))
        11 -> listOf(card("2"), card("9"))
        12 -> listOf(card("2"), card("10"))
        13 -> listOf(card("3"), card("10"))
        14 -> listOf(card("4"), card("10"))
        15 -> listOf(card("5"), card("10"))
        16 -> listOf(card("6"), card("10"))
        17 -> listOf(card("7"), card("10"))
        18 -> listOf(card("8"), card("10"))
        19 -> listOf(card("9"), card("10"))
        20 -> listOf(card("5"), card("5", Suit.HEARTS), card("10"))
        21 -> listOf(card("6"), card("5", Suit.HEARTS), card("10"))
        else -> error("Unsupported hard total $total")
    }

    private fun softRepresentative(total: Int): List<PlayingCard> = when (total) {
        13 -> listOf(card("A"), card("2"))
        14 -> listOf(card("A"), card("3"))
        15 -> listOf(card("A"), card("4"))
        16 -> listOf(card("A"), card("5"))
        17 -> listOf(card("A"), card("6"))
        18 -> listOf(card("A"), card("7"))
        19 -> listOf(card("A"), card("8"))
        20 -> listOf(card("A"), card("9"))
        21 -> listOf(card("A"), card("5"), card("5", Suit.HEARTS))
        else -> error("Unsupported soft total $total")
    }

    private fun pairRepresentative(pair: String): List<PlayingCard> = when (pair) {
        "10" -> listOf(card("10"), card("J", Suit.HEARTS))
        else -> listOf(card(pair), card(pair, Suit.HEARTS))
    }
}
