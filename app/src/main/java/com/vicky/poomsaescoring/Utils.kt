package com.vicky.poomsaescoring

import android.content.Context
import android.widget.Toast
import kotlin.math.roundToInt


fun formatScore(score: Double): String {
    return String.format("%.1f", score)
}
fun toast(context: Context?, message: String? = "") {
    context?.let { cxt ->
        Toast.makeText(cxt, message, Toast.LENGTH_SHORT).show()
    }
}

fun formatScoreThreeDecimals(value: Double): String {
    val rounded = roundToThreeDecimals(value)
    return String.format("%.3f", rounded)
}

fun roundToThreeDecimals(value: Double): Double {
    return (value * 1000.0).roundToInt() / 1000.0
}

fun normalizeOneDecimal(value: Double): Double {
    return (value * 10.0).roundToInt() / 10.0
}
