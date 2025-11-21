package com.vicky.poomsaescoring.fragment

import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vicky.poomsaescoring.R
import com.vicky.poomsaescoring.databinding.FragmentCutoffBinding
import com.vicky.poomsaescoring.formatScore
import com.vicky.poomsaescoring.formatScoreThreeDecimals
import com.vicky.poomsaescoring.normalizeOneDecimal
import com.vicky.poomsaescoring.roundToThreeDecimals
import com.vicky.poomsaescoring.toast
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

class CutoffFragment : Fragment() {

    private var _binding: FragmentCutoffBinding? = null
    private val b get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCutoffBinding.inflate(inflater, container, false)
        return b.root
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupAccuracyButtons()
        setupPresentationButtons()
        setupSubmitButton()
        updateAllScores()
    }

    private fun setupAccuracyButtons() = with(b) {

        // -0.1 minor
        btnMinorError.setOnClickListener {
            applyAccuracyDeduction(0.1, isMinor = true)
        }

        tvConnectHost.setOnClickListener {
            b.clRefereeDetails.visibility = View.VISIBLE
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
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

        createCategoryRow(b.layoutC1, 0, values) { value ->
            c1Score = value
            updateAllScores()
        }

        createCategoryRow(b.layoutC2, 1, values) { value ->
            c2Score = value
            updateAllScores()
        }

        createCategoryRow(b.layoutC3, 2, values) { value ->
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
            val btn = Button(this.requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(40)
                ).apply {
                    rightMargin = dpToPx(4)
                    gravity = Gravity.CENTER_VERTICAL
                }
                text = formatScore(v)
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_dark_4))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.score_button_bg)

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
        val selectedBg = ContextCompat.getColor(this.requireContext(), R.color.score_blue)
        val unselectedBg = ContextCompat.getColor(this.requireContext(), R.color.neutral_light_2)
        val selectedText = ContextCompat.getColor(this.requireContext(), R.color.white)
        val unselectedText = ContextCompat.getColor(this.requireContext(), R.color.neutral_dark_4)

        categoryButtons[categoryIndex].forEach { btn ->
            val isSel = btn == selected
            btn.isSelected = isSel
            btn.backgroundTintList = ColorStateList.valueOf(if (isSel) selectedBg else unselectedBg)
            btn.setTextColor(if (isSel) selectedText else unselectedText)
        }
    }

    // ---------------- Submit to Host (Admin App) ----------------

    private fun setupSubmitButton() {
        b.btnSubmitScore.setOnClickListener {
            submitScoreToHost()
        }
    }

    private fun submitScoreToHost() {
        val refereeName = b.etRefereeName.text?.toString()?.trim().orEmpty()
        val hostIp = b.etHostIp.text?.toString()?.trim().orEmpty()
        val port = 5555

        // Simple validation with TextInputLayout error support
        var hasError = false

        if (refereeName.isEmpty()) {
            b.tilName.error = "Referee name required"
            hasError = true
        } else {
            b.tilName.error = null
        }

        if (hostIp.isEmpty()) {
            b.tilIp.error = "Host IP required"
            hasError = true
        } else {
            b.tilIp.error = null
        }

        if (hasError) return

        val presentationTotal = c1Score + c2Score + c3Score
        val total = accuracyScore + presentationTotal

        val payload = JSONObject().apply {
            put("refereeName", refereeName)
            put("player1Accuracy", roundToThreeDecimals(accuracyScore))
            put("player2Accuracy", "")
            put("player1Presentation", roundToThreeDecimals(presentationTotal))
            put("player2Presentation", "")
            put("player1Total", roundToThreeDecimals(total))
            put("player2Total", "")
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Socket().use { socket ->
                    // connect
                    socket.connect(InetSocketAddress(hostIp, port), 3000)

                    // send
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(payload)
                    writer.newLine()
                    writer.flush()

                    // wait for ACK
                    socket.soTimeout = 3000
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = reader.readLine()

                    if (response == "OK") {
                        withContext(Dispatchers.Main) {
                            setConnectionStatus(connected = true, hostIp = hostIp)
                            toast(requireContext(), "Score submitted")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            setConnectionStatus(connected = false, hostIp = hostIp)
                            toast(requireContext(), "Submit failed: no ACK from host")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setConnectionStatus(connected = false, hostIp = hostIp)
                    toast(requireContext(), "Submit failed: ${e.localizedMessage ?: "connection error"}")
                }
            }
        }
    }

    // ---------------- Scores & Reset ----------------

    private fun updateAllScores() {
        val presentationTotal = c1Score + c2Score + c3Score
        val total = accuracyScore + presentationTotal

        b.tvAccuracyHelp.text = "Minor -0.1 ($minorErrors)    Major -0.3 ($majorErrors)"

        b.tvAccuracyCurrent.text = formatScore(accuracyScore)
        b.tvPresentationCurrent.text = "Presentation: ${formatScore(presentationTotal)}"

        b.tvAccuracyBig.text = formatScore(accuracyScore)
        b.tvPresentationBig.text = formatScore(presentationTotal)
        b.tvTotalBig.text = formatScoreThreeDecimals(total)
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

    private fun setConnectionStatus(connected: Boolean, hostIp: String) {
        // If you didn't add tvConnectionStatus, you can skip this safely
        val tv = b.tvConnectionStatus ?: return

        if (connected) {
            tv.text = "Connected to $hostIp"
            tv.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.score_green))
            b.clRefereeDetails.visibility = View.GONE
            b.tvConnectHost.visibility = View.VISIBLE
        } else {
            tv.text = "Not connected"
            tv.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.error_500))
            b.clRefereeDetails.visibility = View.VISIBLE
            b.tvConnectHost.visibility = View.GONE

        }
    }
    override fun onResume() {
        super.onResume()
        requireActivity().requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }


    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }
}