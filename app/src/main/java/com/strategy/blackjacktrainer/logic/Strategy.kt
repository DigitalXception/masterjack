package com.strategy.blackjacktrainer.logic

import com.strategy.blackjacktrainer.data.PlayingCard

/**
 * Basic strategy table, transcribed from:
 * Blackjack Basic Strategy — 4 to 8 Decks, European style,
 * Dealer Hits Soft 17, Double After Split Allowed, Surrender against 2-10.
 * Source: WizardOfOdds.com
 *
 * Columns for every row (in order): dealer 2,3,4,5,6,7,8,9,10,A
 * Codes: H=Hit  S=Stand  P=Split  Dh=Double(else Hit)  Ds=Double(else Stand)  Rh=Surrender(else Hit)
 */
private object Table {

    val hard: Map<Int, List<String>> = mapOf(
        5 to listOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"),
        6 to listOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"),
        7 to listOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"),
        8 to listOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"),
        9 to listOf("H", "Dh", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"),
        10 to listOf("Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "H", "H"),
        11 to listOf("Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "H", "H"),
        12 to listOf("H", "H", "S", "S", "S", "H", "H", "H", "H", "H"),
        13 to listOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"),
        14 to listOf("S", "S", "S", "S", "S", "H", "H", "H", "Rh", "H"),
        15 to listOf("S", "S", "S", "S", "S", "H", "H", "H", "Rh", "H"),
        16 to listOf("S", "S", "S", "S", "S", "H", "H", "Rh", "Rh", "H"),
        17 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        18 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        19 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        20 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        21 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")
    )

    val soft: Map<Int, List<String>> = mapOf(
        13 to listOf("H", "H", "H", "Dh", "Dh", "H", "H", "H", "H", "H"),
        14 to listOf("H", "H", "H", "Dh", "Dh", "H", "H", "H", "H", "H"),
        15 to listOf("H", "H", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"),
        16 to listOf("H", "H", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"),
        17 to listOf("H", "Dh", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"),
        18 to listOf("Ds", "Ds", "Ds", "Ds", "Ds", "S", "S", "H", "H", "H"),
        19 to listOf("S", "S", "S", "S", "Ds", "S", "S", "S", "S", "S"),
        20 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        21 to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")
    )

    val pair: Map<String, List<String>> = mapOf(
        "2" to listOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"),
        "3" to listOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"),
        "4" to listOf("H", "H", "H", "P", "P", "H", "H", "H", "H", "H"),
        "5" to listOf("Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "H", "H"),
        "6" to listOf("P", "P", "P", "P", "P", "H", "H", "H", "H", "H"),
        "7" to listOf("P", "P", "P", "P", "P", "P", "H", "H", "Rh", "H"),
        "8" to listOf("P", "P", "P", "P", "P", "P", "P", "P", "Rh", "H"),
        "9" to listOf("P", "P", "P", "P", "P", "S", "P", "P", "S", "S"),
        "10" to listOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"),
        "A" to listOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "H")
    )

    // Column order matches the table exactly: 2,3,4,5,6,7,8,9,10,A
    val columns = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "A")

    fun columnIndex(dealerLabel: String): Int = columns.indexOf(dealerLabel)
}

enum class HandCategory { HARD, SOFT, PAIR }

data class HandRead(
    val category: HandCategory,
    val label: String,       // e.g. "Hard 16", "Soft 18", "Pair of 8s"
    val total: Int
)

data class Decision(
    val rawCode: String,
    val action: Action,
    val note: String?         // e.g. why the "if allowed" action wasn't available here
)

object Strategy {

    /** Best hand value (aces count as 11 unless that would bust), and whether it's currently soft. */
    fun handValue(cards: List<PlayingCard>): Pair<Int, Boolean> {
        var total = cards.sumOf { it.pointValue }
        var softAces = cards.count { it.rank == "A" }
        while (total > 21 && softAces > 0) {
            total -= 10
            softAces--
        }
        return total to (softAces > 0)
    }

    fun isBust(cards: List<PlayingCard>): Boolean = handValue(cards).first > 21

    /**
     * Reads a hand for display + table lookup. A hand only counts as a "pair" while it
     * still has exactly its original two cards — the moment a card is added it's read as
     * a plain hard/soft total, matching how the strategy chart treats it.
     */
    fun readHand(cards: List<PlayingCard>): HandRead {
        if (cards.size == 2) {
            val c1 = cards[0]
            val c2 = cards[1]
            if (c1.splitGroup == c2.splitGroup) {
                val label = if (c1.splitGroup == "A") "Pair of Aces" else "Pair of ${c1.splitGroup}s"
                return HandRead(HandCategory.PAIR, label, c1.pointValue * 2)
            }
        }
        val (total, isSoft) = handValue(cards)
        return if (isSoft) {
            HandRead(HandCategory.SOFT, "Soft $total", total)
        } else {
            HandRead(HandCategory.HARD, "Hard $total", total)
        }
    }

    /**
     * The textbook-correct decision for this hand, given what's actually legal right now.
     * @param canDouble    true only on a hand's first two cards
     * @param canSplit     true only when this exact pair is still splittable (this trainer allows one split)
     * @param canSurrender true only on the original hand's very first decision
     */
    fun correctDecision(
        playerCards: List<PlayingCard>,
        dealerCard: PlayingCard,
        canDouble: Boolean = true,
        canSplit: Boolean = true,
        canSurrender: Boolean = true
    ): Decision {
        val hand = readHand(playerCards)
        val col = Table.columnIndex(dealerCard.strategyLabel)

        val code = when {
            hand.category == HandCategory.PAIR && canSplit -> {
                val rowKey = playerCards[0].splitGroup
                Table.pair.getValue(rowKey)[col]
            }
            hand.category == HandCategory.SOFT -> {
                val row = hand.total.coerceIn(13, 21)
                Table.soft.getValue(row)[col]
            }
            else -> {
                // HARD, or a pair that can no longer be split — read off the hard total.
                val row = hand.total.coerceIn(5, 21)
                Table.hard.getValue(row)[col]
            }
        }

        return decodeAction(code, canDouble, canSurrender)
    }

    private fun decodeAction(code: String, canDouble: Boolean, canSurrender: Boolean): Decision {
        return when (code) {
            "H" -> Decision(code, Action.HIT, null)
            "S" -> Decision(code, Action.STAND, null)
            "P" -> Decision(code, Action.SPLIT, null)
            "Dh" -> {
                if (canDouble) Decision(code, Action.DOUBLE, null)
                else Decision(code, Action.HIT, "Double isn't on the table right now, so it falls back to Hit")
            }
            "Ds" -> {
                if (canDouble) Decision(code, Action.DOUBLE, null)
                else Decision(code, Action.STAND, "Double isn't on the table right now, so it falls back to Stand")
            }
            "Rh" -> {
                if (canSurrender) Decision(code, Action.SURRENDER, null)
                else Decision(code, Action.HIT, "Surrender isn't on the table right now, so it falls back to Hit")
            }
            else -> throw IllegalStateException("Unknown strategy code: $code")
        }
    }

    /** The action the chart displays for a cell, assuming that action is always legal there. */
    fun primaryActionFor(code: String): Action = when (code) {
        "H" -> Action.HIT
        "S" -> Action.STAND
        "P" -> Action.SPLIT
        "Dh", "Ds" -> Action.DOUBLE
        "Rh" -> Action.SURRENDER
        else -> Action.HIT
    }

    // ---- Data for the full strategy chart screen ----

    val dealerColumns: List<String> = Table.columns

    data class ChartCell(val scenarioLabel: String, val dealerLabel: String, val code: String)
    data class ChartRow(val rowLabel: String, val cells: List<ChartCell>)
    data class ChartSection(val title: String, val rows: List<ChartRow>)

    fun buildChartSections(): List<ChartSection> {
        val hardRows = Table.hard.keys.sorted().map { total ->
            val label = "Hard $total"
            ChartRow(
                rowLabel = total.toString(),
                cells = dealerColumns.mapIndexed { i, d -> ChartCell(label, d, Table.hard.getValue(total)[i]) }
            )
        }
        val softRows = Table.soft.keys.sorted().map { total ->
            val label = "Soft $total"
            ChartRow(
                rowLabel = total.toString(),
                cells = dealerColumns.mapIndexed { i, d -> ChartCell(label, d, Table.soft.getValue(total)[i]) }
            )
        }
        val pairOrder = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "A")
        val pairRows = pairOrder.map { key ->
            val label = if (key == "A") "Pair of Aces" else "Pair of ${key}s"
            ChartRow(
                rowLabel = if (key == "A") "A,A" else "$key,$key",
                cells = dealerColumns.mapIndexed { i, d -> ChartCell(label, d, Table.pair.getValue(key)[i]) }
            )
        }
        return listOf(
            ChartSection("Hard Totals", hardRows),
            ChartSection("Soft Totals", softRows),
            ChartSection("Pairs", pairRows)
        )
    }
}
