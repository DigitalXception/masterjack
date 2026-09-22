package com.strategy.blackjacktrainer.logic

import com.strategy.blackjacktrainer.data.PlayingCard

/**
 * One hand of cards being played out within a round. A round starts with a single
 * PlayerHand; splitting turns it into two, each played to resolution independently.
 */
data class PlayerHand(
    val cards: List<PlayingCard>,
    val doubled: Boolean = false,
    val surrendered: Boolean = false,
    val finished: Boolean = false,
    val isFromSplit: Boolean = false,
    val isSplitAceHand: Boolean = false
) {
    val total: Int get() = Strategy.handValue(cards).first
    val isSoft: Boolean get() = Strategy.handValue(cards).second
    val isBust: Boolean get() = total > 21

    /** True only while this hand still has just its original two cards. */
    val canDouble: Boolean get() = cards.size == 2 && !doubled

    val resultLabel: String
        get() = when {
            surrendered -> "Surrendered"
            isBust -> "Bust ($total)"
            doubled -> "Doubled to $total"
            else -> "Stood on $total"
        }
}
