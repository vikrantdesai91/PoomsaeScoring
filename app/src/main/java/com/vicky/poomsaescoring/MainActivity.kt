package com.vicky.poomsaescoring

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vicky.poomsaescoring.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
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

    // Dynamic buttons for presentation rows
    private val categoryButtons: Array<MutableList<Button>> =
        Array(3) { mutableListOf<Button>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setupAccuracyButtons()
        setupPresentationButtons()
        setupSubmitButton()
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

        // Reset all (accuracy + presentation)
        btnResetAll.setOnClickListener {
            resetAll()
        }
    }

    private fun applyAccuracyDeduction(amount: Double, isMinor: Boolean) {
        accuracyScore -= amount
        if (accuracyScore < minAccuracy) accuracyScore = minAccuracy

        if (isMinor) {
            minorErrors++
        } else {
            majorErrors++
        }

        updateAllScores()
    }

    private fun applyAccuracyAddition(amount: Double, isMinor: Boolean) {
        if (isMinor && minorErrors > 0) {
            minorErrors--
            accuracyScore += amount
        } else if (!isMinor && majorErrors > 0) {
            majorErrors--
            accuracyScore += amount
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
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.neutral_dark_4))
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.score_button_bg)

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

    /**
     * Visually mark one button as selected for a category using your color system.
     */
    private fun markSelected(categoryIndex: Int, selected: Button) {
        val selectedBg = ContextCompat.getColor(this, R.color.score_blue)
        val unselectedBg = ContextCompat.getColor(this, R.color.neutral_light_2)
        val selectedText = ContextCompat.getColor(this, R.color.white)
        val unselectedText = ContextCompat.getColor(this, R.color.neutral_dark_4)

        categoryButtons[categoryIndex].forEach { btn ->
            val isSel = btn == selected
            btn.isSelected = isSel
            btn.backgroundTintList = ColorStateList.valueOf(if (isSel) selectedBg else unselectedBg)
            btn.setTextColor(if (isSel) selectedText else unselectedText)
        }
    }

    // ---------------- Submit to Host (Admin App) ----------------

    private fun setupSubmitButton() {
        binding.btnSubmitScore.setOnClickListener {
            submitScoreToHost()
        }
    }

    private fun submitScoreToHost() {
        val refereeName = binding.etRefereeName.text?.toString()?.trim().orEmpty()
        val hostIp = binding.etHostIp.text?.toString()?.trim().orEmpty()
        val port = 5555 // same as host app

        // Simple validation with TextInputLayout error support
        var hasError = false

        if (refereeName.isEmpty()) {
            binding.tilName.error = "Referee name required"
            hasError = true
        } else {
            binding.tilName.error = null
        }

        if (hostIp.isEmpty()) {
            binding.tilIp.error = "Host IP required"
            hasError = true
        } else {
            binding.tilIp.error = null
        }

        if (hasError) return

        val presentationTotal = c1Score + c2Score + c3Score
        val total = accuracyScore + presentationTotal

        val accuracyRounded = roundToThreeDecimals(accuracyScore)
        val presRounded = roundToThreeDecimals(presentationTotal)
        val totalRounded = roundToThreeDecimals(total)

        val payload = JSONObject().apply {
            put("refereeName", refereeName)
            put("accuracy", accuracyRounded)
            put("presentation", presRounded)
            put("total", totalRounded)
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(hostIp, port), 3000)
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(payload)
                    writer.newLine()
                    writer.flush()
                }
                withContext(Dispatchers.Main) {
                    toast("Score submitted")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("Submit failed: ${e.localizedMessage ?: "Connection error"}")
                }
            }
        }
    }

    // ---------------- Scores & Reset ----------------

    private fun updateAllScores() {
        val presentationTotal = c1Score + c2Score + c3Score
        val total = accuracyScore + presentationTotal

        binding.tvAccuracyHelp.text =
            "Minor -0.1 ($minorErrors)    Major -0.3 ($majorErrors)"

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

        // Re-select default (2.0) for each category row
        for (cat in 0..2) {
            val buttons = categoryButtons[cat]
            if (buttons.isNotEmpty()) {
                markSelected(cat, buttons.first())
            }
        }

        updateAllScores()
    }

    // ---------------- Helpers ----------------

    private fun formatScore(value: Double): String {
        return String.format("%.1f", normalizeOneDecimal(value))
    }

    private fun formatScoreThreeDecimals(value: Double): String {
        val rounded = roundToThreeDecimals(value)
        return String.format("%.3f", rounded)
    }

    private fun roundToThreeDecimals(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }

    private fun normalizeOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}