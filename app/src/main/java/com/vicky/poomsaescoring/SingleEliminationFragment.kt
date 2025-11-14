package com.vicky.poomsaescoring

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vicky.poomsaescoring.databinding.FragmentSingleEliminationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt

class SingleEliminationFragment : Fragment() {

    private var _binding: FragmentSingleEliminationBinding? = null
    private val binding get() = _binding!!

    // Player 1 presentation scores (3 criteria)
    private var p1C1Score = 2.0
    private var p1C2Score = 2.0
    private var p1C3Score = 2.0

    // Player 2 presentation scores (3 criteria)
    private var p2C1Score = 2.0
    private var p2C2Score = 2.0
    private var p2C3Score = 2.0

    // Buttons for highlighting (3 criteria per player)
    private val p1CategoryButtons: Array<MutableList<Button>> = Array(3) { mutableListOf() }
    private val p2CategoryButtons: Array<MutableList<Button>> = Array(3) { mutableListOf() }

    private var player1Accuracy = 4.0
    private var player2Accuracy = 4.0

    private var player1MinorErrors = 0
    private var player2MinorErrors = 0
    private var player1MajorErrors = 0
    private var player2MajorErrors = 0

    var p1TotalScore: Double = 0.0
    var p2TotalScore: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSingleEliminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewListener()
        setupScoringButtons()
        setupUndoButtons()
        setupPresentationButtons()
    }

    private fun viewListener() {
        binding.apply {
            btnNext.setOnClickListener {
                if (btnNext.text == "Next") {
                    clPlayer2Presentation.visibility = View.VISIBLE
                    clPlayer1Presentation.visibility = View.VISIBLE
                    btnSubmitScore.visibility = View.VISIBLE
                    clPlayer1Accuracy.visibility = View.GONE
                    clPlayer2Accuracy.visibility = View.GONE
                    tvAccuracyLabel.text = getString(R.string.presentation_3_criteria_2_0)
                    tvConnectHost.visibility = View.GONE
                    tvAccuracyHelp.text = getString(R.string.tap_value_2_0_0_5_for_each_criterion)
                    btnNext.text = "Back"

                } else {
                    clPlayer2Presentation.visibility = View.GONE
                    clPlayer1Presentation.visibility = View.GONE
                    btnSubmitScore.visibility = View.GONE
                    clPlayer2Accuracy.visibility = View.VISIBLE
                    clPlayer1Accuracy.visibility = View.VISIBLE
                    tvAccuracyLabel.text = getString(R.string.accuracy_start_4_0)
                    tvConnectHost.visibility = View.VISIBLE
                    tvAccuracyHelp.text = getString(R.string.minor_0_1_major_0_3)
                    btnNext.text = "Next"
                }

            }

            btnSubmitScore.setOnClickListener {
                submitScoreToHost()
            }

            tvConnectHost.setOnClickListener {
                clRefereeDetails.visibility = View.VISIBLE
            }
        }

    }

    private fun submitScoreToHost() {
        val refereeName = binding.etRefereeName.text?.toString()?.trim().orEmpty()
        val hostIp = binding.etHostIp.text?.toString()?.trim().orEmpty()
        val port = 5555

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

        // Calculate Player 1 and Player 2 Total Scores
        val p1PresentationTotal = p1C1Score + p1C2Score + p1C3Score
        val p2PresentationTotal = p2C1Score + p2C2Score + p2C3Score

        val p1Total = player1Accuracy + p1PresentationTotal
        val p2Total = player2Accuracy + p2PresentationTotal

        // Create the payload to send both players' scores to the host
        val payload = JSONObject().apply {
            put("refereeName", refereeName)
            put("player1Accuracy", roundToThreeDecimals(player1Accuracy))
            put("player2Accuracy", roundToThreeDecimals(player2Accuracy))
            put("player1Presentation", roundToThreeDecimals(p1Total))
            put("player2Presentation", roundToThreeDecimals(p2Total))
            put("player1Total", roundToThreeDecimals(player1Accuracy + p1Total))
            put("player2Total", roundToThreeDecimals(player2Accuracy + p2Total))
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Socket().use { socket ->
                    // Connect to the host
                    socket.connect(InetSocketAddress(hostIp, port), 3000)

                    // Send the payload to the host
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(payload)
                    writer.newLine()
                    writer.flush()

                    // Wait for ACK from the host
                    socket.soTimeout = 3000
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = reader.readLine()

                    if (response == "OK") {
                        withContext(Dispatchers.Main) {
                            setConnectionStatus(connected = true, hostIp = hostIp)
                            toast("Scores submitted successfully!")
                            resetScores()
                            binding.clPlayer2Presentation.visibility = View.GONE
                            binding.clPlayer1Presentation.visibility = View.GONE
                            binding.btnSubmitScore.visibility = View.GONE
                            binding.clPlayer2Accuracy.visibility = View.VISIBLE
                            binding.clPlayer1Accuracy.visibility = View.VISIBLE
                            binding.tvAccuracyLabel.text = getString(R.string.accuracy_start_4_0)
                            binding.tvConnectHost.visibility = View.VISIBLE
                            binding.tvAccuracyHelp.text = getString(R.string.minor_0_1_major_0_3)
                            binding.btnNext.text = "Next"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            setConnectionStatus(connected = false, hostIp = hostIp)
                            toast("Submit failed: no ACK from host")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setConnectionStatus(connected = false, hostIp = hostIp)
                    toast("Submit failed: ${e.localizedMessage ?: "connection error"}")
                }
            }
        }
    }

    private fun resetScores() {
        // Reset Player 1 scores
        p1C1Score = 2.0
        p1C2Score = 2.0
        p1C3Score = 2.0
        player1Accuracy = 4.0
        player1MinorErrors = 0
        player1MajorErrors = 0

        // Reset Player 2 scores
        p2C1Score = 2.0
        p2C2Score = 2.0
        p2C3Score = 2.0
        player2Accuracy = 4.0
        player2MinorErrors = 0
        player2MajorErrors = 0

        // Update UI
        updateAllScores()
    }

    private fun updateAllScores() {
        // Calculate the total score for Player 1 and Player 2
        val p1Total = p1C1Score + p1C2Score + p1C3Score
        val p2Total = p2C1Score + p2C2Score + p2C3Score

        // Update UI with Player 1 and Player 2 total scores
        binding.apply {
            // Player 1 Accuracy and Presentation
            tvPlayer1Accuracy.text = formatScore(player1Accuracy)
            tvPlayer1AccuracyBig.text = formatScore(player1Accuracy)

            // Player 1 Presentation (C1 + C2 + C3)
//            tvPlayer1Presentation.text = formatScore(p1Total)
            tvPlayer1PresentationBig.text = formatScore(p1Total)

            // Player 1 Total (Accuracy + Presentation)
            val p1TotalScore = roundToThreeDecimals(player1Accuracy + p1Total)
            tvPlayer1TotalBig.text = p1TotalScore.toString()

            // Player 2 Accuracy and Presentation
            tvPlayer2Accuracy.text = formatScore(player2Accuracy)
            tvPlayer2AccuracyBig.text = formatScore(player2Accuracy)

            // Player 2 Presentation (C1 + C2 + C3)
//            tvPlayer2Presentation.text = formatScore(p2Total)
            tvPlayer2PresentationBig.text = formatScore(p2Total)

            // Player 2 Total (Accuracy + Presentation)
            val p2TotalScore = roundToThreeDecimals(player2Accuracy + p2Total)
            tvPlayer2TotalBig.text = p2TotalScore.toString()

        }

        // Call this function to reset the category buttons after resetting the scores
        setupPresentationButtons()
    }





    private fun setConnectionStatus(connected: Boolean, hostIp: String) {
        // If you didn't add tvConnectionStatus, you can skip this safely
        val tv = binding.tvConnectionStatus ?: return

        if (connected) {
            tv.text = "Connected to $hostIp"
            tv.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.score_green))
            binding.clRefereeDetails.visibility = View.GONE
            binding.tvConnectHost.visibility = View.VISIBLE
        } else {
            tv.text = "Not connected"
            tv.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.error_500))
            binding.clRefereeDetails.visibility = View.VISIBLE
            binding.tvConnectHost.visibility = View.GONE

        }
    }


    // Handle deduction buttons (for -0.1 and -0.3)
    private fun setupScoringButtons() {
        binding.apply {
            // Player 1 - Minor Error (-0.1)
            btnPlayer1MinorError.setOnClickListener {
                applyDeduction(0.1, player = 1, isMinor = true)
            }
            // Player 1 - Major Error (-0.3)
            btnPlayer1MajorError.setOnClickListener {
                applyDeduction(0.3, player = 1, isMinor = false)
            }
            // Player 2 - Minor Error (-0.1)
            btnPlayer2MinorError.setOnClickListener {
                applyDeduction(0.1, player = 2, isMinor = true)
            }
            // Player 2 - Major Error (-0.3)
            btnPlayer2MajorError.setOnClickListener {
                applyDeduction(0.3, player = 2, isMinor = false)
            }
        }
    }

    private fun setupPresentationButtons() {
        // Values 2.0 down to 0.5 step 0.1
        val values = generateSequence(2.0) { it - 0.1 }
            .takeWhile { it >= 0.5 - 1e-9 }
            .map { normalizeOneDecimal(it) }
            .toList()

        // ---------- PLAYER 1 ----------
        createCategoryRow(
            container = binding.Player1layoutC1,
            buttonsStore = p1CategoryButtons[0],
            values = values
        ) { value ->
            p1C1Score = value
            updatePresentationTotals()
        }

        createCategoryRow(
            container = binding.Player1layoutC2,
            buttonsStore = p1CategoryButtons[1],
            values = values
        ) { value ->
            p1C2Score = value
            updatePresentationTotals()
        }

        createCategoryRow(
            container = binding.Player1layoutC3,
            buttonsStore = p1CategoryButtons[2],
            values = values
        ) { value ->
            p1C3Score = value
            updatePresentationTotals()
        }

        // ---------- PLAYER 2 ----------
        createCategoryRow(
            container = binding.Player2layoutC1,
            buttonsStore = p2CategoryButtons[0],
            values = values
        ) { value ->
            p2C1Score = value
            updatePresentationTotals()
        }

        createCategoryRow(
            container = binding.Player2layoutC2,
            buttonsStore = p2CategoryButtons[1],
            values = values
        ) { value ->
            p2C2Score = value
            updatePresentationTotals()
        }

        createCategoryRow(
            container = binding.Player2layoutC3,
            buttonsStore = p2CategoryButtons[2],
            values = values
        ) { value ->
            p2C3Score = value
            updatePresentationTotals()
        }
    }

    private fun createCategoryRow(
        container: LinearLayout,
        buttonsStore: MutableList<Button>,
        values: List<Double>,
        onValueSelected: (Double) -> Unit
    ) {
        container.removeAllViews()  // Clear the container before adding buttons
        buttonsStore.clear()         // Clear the store for this category

        // Create a button for each value in the category
        for (v in values) {
            val btn = Button(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(40)
                ).apply {
                    rightMargin = dpToPx(4)
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Set the button text (formatted to one decimal point)
                text = formatScoreOneDecimal(v)
                textSize = 16f
                setTextColor(resources.getColor(R.color.neutral_dark_4, requireContext().theme))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.score_button_bg)

                setOnClickListener {
                    onValueSelected(v)
                    markSelected(buttonsStore, this)  // Mark the button as selected
                }
            }

            // Add the button to the container and store it for future reference
            container.addView(btn)
            buttonsStore.add(btn)
        }

        // After all buttons are created, select the first one by default
        if (buttonsStore.isNotEmpty()) {
            markSelected(buttonsStore, buttonsStore.first()) // Select the first button by default
        }
    }


    private fun markSelected(buttonsStore: MutableList<Button>, selected: Button) {
        buttonsStore.forEach { btn ->
            val isSelected = btn == selected
            btn.isSelected = isSelected

            // Change the background color of the selected button
            btn.backgroundTintList = ColorStateList.valueOf(
                resources.getColor(
                    if (isSelected) R.color.score_blue else R.color.neutral_light_2,  // Highlight selected
                    requireContext().theme
                )
            )

            // Change the text color of the selected button
            btn.setTextColor(
                resources.getColor(
                    if (isSelected) R.color.white_black else R.color.neutral_dark_4,  // Change text color for selected
                    requireContext().theme
                )
            )
        }
    }


    private fun normalizeOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }

    private fun formatScoreOneDecimal(value: Double): String {
        return String.format("%.1f", value)
    }

    private fun roundToThreeDecimals(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    /**
     * Here you decide how to show the presentation totals for each player.
     * Example: sum C1 + C2 + C3 and maybe update some TextViews.
     */
    private fun updatePresentationTotals() {
        val p1Total = p1C1Score + p1C2Score + p1C3Score
        val p2Total = p2C1Score + p2C2Score + p2C3Score

        binding.apply {
            tvPlayer1PresentationBig.text = formatScoreOneDecimal(p1Total)
            tvPlayer2PresentationBig.text = formatScoreOneDecimal(p2Total)

            p1TotalScore = roundToThreeDecimals(player1Accuracy + p1Total)
            p2TotalScore = roundToThreeDecimals(player2Accuracy + p2Total)

            tvPlayer1TotalBig.text = p1TotalScore.toString()
            tvPlayer2TotalBig.text = p2TotalScore.toString()
        }
    }


    // Handle undo buttons (for +0.1 and +0.3)
    private fun setupUndoButtons() {
        binding.apply {
            // Player 1 - Undo Minor Error (+0.1)
            btnPlayer1MinorAdd.setOnClickListener {
                undoDeduction(0.1, player = 1, isMinor = true)
            }
            // Player 1 - Undo Major Error (+0.3)
            btnPlayer1MajorAdd.setOnClickListener {
                undoDeduction(0.3, player = 1, isMinor = false)
            }
            // Player 2 - Undo Minor Error (+0.1)
            btnPlayer2MinorAdd.setOnClickListener {
                undoDeduction(0.1, player = 2, isMinor = true)
            }
            // Player 2 - Undo Major Error (+0.3)
            btnPlayer2MajorAdd.setOnClickListener {
                undoDeduction(0.3, player = 2, isMinor = false)
            }
        }
    }

    // Apply deductions for both players
    private fun applyDeduction(amount: Double, player: Int, isMinor: Boolean) {
        when (player) {
            1 -> {
                if (isMinor) {
                    player1Accuracy = (player1Accuracy - amount).coerceAtLeast(0.0)  // Ensure score doesn't go below 0
                    player1MinorErrors++
                } else {
                    player1Accuracy = (player1Accuracy - amount).coerceAtLeast(0.0)
                    player1MajorErrors++
                }
            }

            2 -> {
                if (isMinor) {
                    player2Accuracy = (player2Accuracy - amount).coerceAtLeast(0.0)
                    player2MinorErrors++
                } else {
                    player2Accuracy = (player2Accuracy - amount).coerceAtLeast(0.0)
                    player2MajorErrors++
                }
            }
        }

        updateScores()  // Update the UI with the latest scores
    }

    // Undo deductions for both players
    private fun undoDeduction(amount: Double, player: Int, isMinor: Boolean) {
        when (player) {
            1 -> {
                if (isMinor && player1MinorErrors > 0) {
                    player1Accuracy = (player1Accuracy + amount).coerceAtMost(4.0)  // Max is 4.0 for accuracy
                    player1MinorErrors--
                } else if (!isMinor && player1MajorErrors > 0) {
                    player1Accuracy = (player1Accuracy + amount).coerceAtMost(4.0)
                    player1MajorErrors--
                }
            }

            2 -> {
                if (isMinor && player2MinorErrors > 0) {
                    player2Accuracy = (player2Accuracy + amount).coerceAtMost(4.0)
                    player2MinorErrors--
                } else if (!isMinor && player2MajorErrors > 0) {
                    player2Accuracy = (player2Accuracy + amount).coerceAtMost(4.0)
                    player2MajorErrors--
                }
            }
        }

        updateScores()  // Update the UI with the latest scores
    }

    // Update the UI after each score modification
    private fun updateScores() {
        binding.apply {
            tvPlayer1Accuracy.text = formatScore(player1Accuracy)
            tvPlayer1AccuracyBig.text = formatScore(player1Accuracy)
            tvPlayer2Accuracy.text = formatScore(player2Accuracy)
            tvPlayer2AccuracyBig.text = formatScore(player2Accuracy)
        }
    }

    // Format score to show with one decimal place
    private fun formatScore(score: Double): String {
        return String.format("%.1f", score)
    }
    private fun toast(msg: String) {
        Toast.makeText(this.requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
