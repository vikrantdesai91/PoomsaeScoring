package com.vicky.poomsaescoring

data class RefereeScore(
    val name: String,
    val player1Accuracy: Double,
    val player2Accuracy: Double,
    val player1Presentation: Double,
    val player2Presentation: Double,
    val player1Total: Double,    // Player 1 total (accuracy + presentation)
    val player2Total: Double     // Player 2 total (accuracy + presentation)
)