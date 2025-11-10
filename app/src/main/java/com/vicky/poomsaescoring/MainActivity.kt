package com.vicky.poomsaescoring

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vicky.poomsaescoring.databinding.ActivityMainBinding
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Accuracy
    private var accuracyScore = 4.0
    private var minorErrors = 0
    private var majorErrors = 0
    private val maxAccuracy = 4.0
    private val minAccuracy = 0.0

    // Presentation (3 criteria, each 2.0 -> 0.5)
    private var c1Score = 2.0
    private var c2Score = 2.0
    private var c3Score = 2.0

    // For highlighting selected buttons in each row
    private val categoryButtons: Array<MutableList<Button>> =
        Array(3) { mutableListOf<Button>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setupAccuracyButtons()
        setupPresentationButtons()
        updateAllScores()
    }

    // ---------------- Accuracy ----------------

    private fun setupAccuracyButtons() = with(binding) {

        // -0.1 minor
        btnMinorError.setOnClickListener {
            applyAccuracyDeduction(0.1, isMinor = true)
        }

        // -0.3 major
        btnMajorError.setOnClickListener {
            applyAccuracyDeduction(0.3, isMinor = false)
        }

        // +0.1 (undo minor)
        btnMinorAdd.setOnClickListener {
            applyAccuracyAddition(0.1, isMinor = true)
        }

        // +0.3 (undo major)
        btnMajorAdd.setOnClickListener {
            applyAccuracyAddition(0.3, isMinor = false)
        }

        // Reset only accuracy
        btnResetAccuracy.setOnClickListener {
            resetAccuracy()
        }

        // Reset all (accuracy + presentation) handled in layout button
        btnResetAll.setOnClickListener {
            resetAll()
        }
    }

    private fun applyAccuracyDeduction(amount: Double, isMinor: Boolean) {
        // Apply deduction
        accuracyScore -= amount
        if (accuracyScore < minAccuracy) accuracyScore = minAccuracy

        // Track counts
        if (isMinor) {
            minorErrors++
        } else {
            majorErrors++
        }

        updateAllScores()
    }

    private fun applyAccuracyAddition(amount: Double, isMinor: Boolean) {
        // Treat as undo of previous error of that type.
        if (isMinor) {
            if (minorErrors > 0) {
                minorErrors--
                accuracyScore += amount
            }
        } else {
            if (majorErrors > 0) {
                majorErrors--
                accuracyScore += amount
            }
        }

        if (accuracyScore > maxAccuracy) accuracyScore = maxAccuracy

        updateAllScores()
    }

    private fun resetAccuracy() {
        accuracyScore = maxAccuracy
        minorErrors = 0
        majorErrors = 0
        updateAllScores()
    }

    // ---------------- Presentation ----------------

    private fun setupPresentationButtons() {
        // Values 2.0 down to 0.5 step 0.1
        val values = generateSequence(2.0) { it - 0.1 }
            .takeWhile { it >= 0.5 - 1e-9 }
            .map { normalizeOneDecimal(it) }
            .toList()

        createCategoryRow(binding.layoutC1, 0, values) { value ->
            c1Score = value
            updateAllScores()
        }

        createCategoryRow(binding.layoutC2, 1, values) { value ->
            c2Score = value
            updateAllScores()
        }

        createCategoryRow(binding.layoutC3, 2, values) { value ->
            c3Score = value
            updateAllScores()
        }
    }

    private fun createCategoryRow(
        container: LinearLayout,
        categoryIndex: Int,
        values: List<Double>,
        onValueSelected: (Double) -> Unit
    ) {
        container.removeAllViews()
        categoryButtons[categoryIndex].clear()

        for (v in values) {
            val btn = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(40)
                ).apply {
                    rightMargin = dpToPx(4)
                    gravity = Gravity.CENTER_VERTICAL
                }
                text = formatScore(v)
                textSize = 16f
                setTextColor(resources.getColor(R.color.btn_text, theme))
                background = getDrawable(R.drawable.score_button_bg)

                setOnClickListener {
                    onValueSelected(v)
                    markSelected(categoryIndex, this)
                }
            }

            container.addView(btn)
            categoryButtons[categoryIndex].add(btn)
        }

        // Default select 2.0 (first button)
        if (categoryButtons[categoryIndex].isNotEmpty()) {
            markSelected(categoryIndex, categoryButtons[categoryIndex].first())
        }
    }

    private fun markSelected(categoryIndex: Int, selected: Button) {
        categoryButtons[categoryIndex].forEach { btn ->
            val isSel = btn == selected
            btn.isSelected = isSel
            btn.alpha = if (isSel) 1.0f else 0.5f
        }
    }

    // ---------------- Scores & Reset ----------------

    private fun updateAllScores() {
        val presentationTotal = c1Score + c2Score + c3Score
        val total = accuracyScore + presentationTotal

        // Small accuracy line with counts
        binding.tvAccuracyHelp.text =
            "Minor -0.1 ($minorErrors)    Major -0.3 ($majorErrors)"

        // Big numbers & labels
        binding.tvAccuracyCurrent.text = formatScore(accuracyScore)
        binding.tvPresentationCurrent.text =
            "Presentation: ${formatScore(presentationTotal)}"

        binding.tvAccuracyBig.text = formatScore(accuracyScore)
        binding.tvPresentationBig.text = formatScore(presentationTotal)
        binding.tvTotalBig.text = formatScoreThreeDecimals(total)
    }

    private fun resetAll() {
        accuracyScore = maxAccuracy
        minorErrors = 0
        majorErrors = 0

        c1Score = 2.0
        c2Score = 2.0
        c3Score = 2.0

        // Reset presentation button highlights: select first (2.0) in each row
        for (cat in 0..2) {
            categoryButtons[cat].forEachIndexed { index, button ->
                val isSel = index == 0
                button.isSelected = isSel
                button.alpha = if (isSel) 1.0f else 0.5f
            }
        }

        updateAllScores()
    }

    // ---------------- Helpers ----------------

    private fun formatScore(value: Double): String {
        return String.format("%.1f", normalizeOneDecimal(value))
    }

    private fun formatScoreThreeDecimals(value: Double): String {
        // Total always 3 decimals (e.g. 10.000)
        val rounded = (value * 1000.0).roundToInt() / 1000.0
        return String.format("%.3f", rounded)
    }

    private fun normalizeOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }
}