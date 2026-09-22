package com.strategy.blackjacktrainer.logic

enum class Action(val label: String, val shortCode: String) {
    HIT("Hit", "H"),
    STAND("Stand", "S"),
    DOUBLE("Double", "D"),
    SPLIT("Split", "P"),
    SURRENDER("Surrender", "R")
}
