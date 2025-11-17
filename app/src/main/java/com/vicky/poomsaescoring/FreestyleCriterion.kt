package com.vicky.poomsaescoring

data class FreestyleCriterion(
    val id: String,
    val label: String,
    val maxScore: Double,
    var score: Double = 0.0
)
