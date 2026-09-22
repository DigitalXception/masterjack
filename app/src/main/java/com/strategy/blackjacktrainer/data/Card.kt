package com.strategy.blackjacktrainer.data

import kotlin.random.Random

enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("\u2660", false),
    HEARTS("\u2665", true),
    DIAMONDS("\u2666", true),
    CLUBS("\u2663", false)
}

data class PlayingCard(val rank: String, val suit: Suit) {

    /** Blackjack point value: face cards = 10, Ace = 11 (soft), numerics = themselves. */
    val pointValue: Int
        get() = when (rank) {
            "A" -> 11
            "J", "Q", "K", "10" -> 10
            else -> rank.toInt()
        }

    /** Group used for split eligibility: all 10-value cards share a group. */
    val splitGroup: String
        get() = if (rank in listOf("10", "J", "Q", "K")) "10" else rank

    /** Label used to look up the strategy table column for the dealer's card. */
    val strategyLabel: String
        get() = if (rank in listOf("J", "Q", "K")) "10" else rank
}

private val RANKS = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

fun randomCard(): PlayingCard {
    val rank = RANKS[Random.nextInt(RANKS.size)]
    val suit = Suit.entries[Random.nextInt(Suit.entries.size)]
    return PlayingCard(rank, suit)
}

data class DealtHand(
    val player: List<PlayingCard>,
    val dealer: PlayingCard
)

/** Deals a fresh two-card player hand + dealer up-card, excluding natural blackjacks
 *  (a made blackjack has no decision to train). */
fun dealNewHand(): DealtHand {
    while (true) {
        val c1 = randomCard()
        val c2 = randomCard()
        val dealer = randomCard()
        val isBlackjack = (c1.rank == "A" && c2.pointValue == 10) || (c2.rank == "A" && c1.pointValue == 10)
        if (!isBlackjack) {
            return DealtHand(listOf(c1, c2), dealer)
        }
    }
}
