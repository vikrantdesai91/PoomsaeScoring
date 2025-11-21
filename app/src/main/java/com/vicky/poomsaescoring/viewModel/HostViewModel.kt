package com.vicky.poomsaescoring.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vicky.poomsaescoring.data.RefereeScore
import kotlin.math.roundToInt

class HostViewModel : ViewModel() {

    private val _scores = MutableLiveData<List<RefereeScore>>(emptyList())
    val scores: LiveData<List<RefereeScore>> = _scores

    // Add or update referee score (accuracy, presentation, total for both players)
    fun addOrUpdateRefereeScore(
        name: String,
        player1Accuracy: Double,
        player2Accuracy: Double,
        player1Presentation: Double,
        player2Presentation: Double,
        player1Total: Double,
        player2Total: Double
    ) {
        val list = _scores.value?.toMutableList() ?: mutableListOf()
        val idx = list.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        val score = RefereeScore(
            name = name,
            player1Accuracy = round3(player1Accuracy),
            player2Accuracy = round3(player2Accuracy),
            player1Presentation = round3(player1Presentation),
            player2Presentation = round3(player2Presentation),
            player1Total = round3(player1Total),
            player2Total = round3(player2Total)
        )

        if (idx >= 0) {
            list[idx] = score
        } else {
            list.add(score)
        }

        _scores.postValue(list)
    }

    // Reset all scores
    fun resetScores() {
        _scores.postValue(emptyList())
    }

    // Calculate average of Player 1's and Player 2's scores separately
    fun player1Average(): Double {
        val list = _scores.value.orEmpty()
        if (list.isEmpty()) return 0.0

        val totalScores = list.map { it.player1Total }
        val willDrop = totalScores.size >= 5 // Drop highest and lowest if we have 5 or more judges

        return calculateAverageExcludingExtremes(totalScores, willDrop)
    }

    fun player2Average(): Double {
        val list = _scores.value.orEmpty()
        if (list.isEmpty()) return 0.0

        val totalScores = list.map { it.player2Total }
        val willDrop = totalScores.size >= 5 // Drop highest and lowest if we have 5 or more judges

        return calculateAverageExcludingExtremes(totalScores, willDrop)
    }

    private fun calculateAverageExcludingExtremes(scores: List<Double>, willDrop: Boolean): Double {
        if (!willDrop) return round3(scores.average())  // No drop, return regular average

        // Drop the highest and lowest scores
        val sortedScores = scores.sorted()
        if (sortedScores.size <= 2) return round3(sortedScores.average())  // If there are 2 or fewer, we can't drop anything

        // Remove the highest and lowest
        val trimmedScores = sortedScores.subList(1, sortedScores.size - 1)
        return round3(trimmedScores.average())  // Return the average of the remaining scores
    }

    // Method to calculate the max total (accuracy + presentation) for both players
    fun getMaxTotal(): Double {
        return _scores.value?.maxOfOrNull { maxOf(it.player1Total, it.player2Total) } ?: 0.0
    }

    // Method to calculate the min total (accuracy + presentation) for both players
    fun getMinTotal(): Double {
        return _scores.value?.minOfOrNull { minOf(it.player1Total, it.player2Total) } ?: 0.0
    }

    // Compute WT metrics (final score based on totals)
    fun computeWtMetrics(expectedJudges: Int): WtMetrics {
        val list = _scores.value.orEmpty()
        val received = list.size

        val rawAvgTotals = if (received == 0) 0.0
        else round3(list.sumOf { it.player1Total + it.player2Total } / (received * 2))

        val willDrop = expectedJudges >= 5 && received >= 5

        // Helper method to calculate average and drop highest/lowest for total score
        fun avg(values: List<Double>): Double {
            if (values.isEmpty()) return 0.0
            if (!willDrop) return round3(values.average())
            // Drop the highest and lowest
            val sorted = values.sorted()
            if (sorted.size <= 2) return round3(values.average())
            val trimmed = sorted.subList(1, sorted.lastIndex) // Remove highest and lowest
            return round3(trimmed.average())
        }

        // Calculate the final score (wtFinal) based on total scores
        val totalScores = list.flatMap { listOf(it.player1Total, it.player2Total) }
        val wtFinal = round3(avg(totalScores))

        // Find highest/lowest for accuracy and presentation (for UI highlighting)
        val accuracyScores = list.flatMap { listOf(it.player1Accuracy, it.player2Accuracy) }
        val presentationScores =
            list.flatMap { listOf(it.player1Presentation, it.player2Presentation) }

        val maxAccuracy = accuracyScores.maxOrNull() ?: 0.0
        val minAccuracy = accuracyScores.minOrNull() ?: 0.0

        val maxPresentation = presentationScores.maxOrNull() ?: 0.0
        val minPresentation = presentationScores.minOrNull() ?: 0.0

        return WtMetrics(
            received = received,
            expected = expectedJudges,
            rawAvgTotals = rawAvgTotals,
            wtFinal = wtFinal,
            droppedExtremes = willDrop,
            maxAccuracy = maxAccuracy,
            minAccuracy = minAccuracy,
            maxPresentation = maxPresentation,
            minPresentation = minPresentation
        )
    }

    private fun round3(v: Double): Double =
        (v * 1000.0).roundToInt() / 1000.0

    data class WtMetrics(
        val received: Int,
        val expected: Int,
        val rawAvgTotals: Double,
        val wtFinal: Double,
        val droppedExtremes: Boolean,
        val maxAccuracy: Double,
        val minAccuracy: Double,
        val maxPresentation: Double,
        val minPresentation: Double
    )
}