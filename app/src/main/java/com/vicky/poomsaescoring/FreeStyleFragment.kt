package com.vicky.poomsaescoring

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.vicky.poomsaescoring.databinding.FragmentFreeStyleBinding
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


class FreeStyleFragment : Fragment() {

    private var _binding: FragmentFreeStyleBinding? = null
    private val b get() = _binding!!

    private val technical = mutableListOf<FreestyleCriterion>()
    private val presentation = mutableListOf<FreestyleCriterion>()

    private lateinit var techAdapter: FreestyleCriterionAdapter
    private lateinit var presAdapter: FreestyleCriterionAdapter

    private var technicalSubtotal = 0.0
    private var presentationSubtotal = 0.0
    private var deductions = 0.0
    private var finalScore = 0.0

    // optional: base points for mandatory stance
    private var mandatoryBase = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFreeStyleBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCriteria()
        setupRecyclerViews()
        setupMandatoryStance()
        setupDeductionControls()
        setupSubmitButton()

        recomputeTotals()
    }

    // ---------- CRITERIA SETUP ----------

    private fun initCriteria() {
        technical.clear()
        presentation.clear()

        // TECHNICAL – based on WT sheet (max total ≈ 6.0)
        technical += listOf(
            FreestyleCriterion(
                id = "height_side_kick",
                label = "Height of Jumping Side Kick",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "multiple_kicks",
                label = "Multiple Kicks in the Air",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "spin_gradient",
                label = "Gradient of Spins in a Spin Kick",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "performance_sequence",
                label = "Performance Level of Sparring Kicks",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "acro_kicking",
                label = "Acrobatic Kicking Technique",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "basic_movement",
                label = "Basic Movements and Practicability",
                maxScore = 1.0
            )
        )

        // PRESENTATION – 4 criteria, max 4.0
        presentation += listOf(
            FreestyleCriterion(
                id = "creativity",
                label = "Creativity",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "harmony",
                label = "Harmony",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "expression",
                label = "Expression of Energy",
                maxScore = 1.0
            ),
            FreestyleCriterion(
                id = "music_choreo",
                label = "Music & Choreography",
                maxScore = 1.0
            )
        )
    }

    private fun setupRecyclerViews() {
        techAdapter = FreestyleCriterionAdapter(technical) { recomputeTotals() }
        presAdapter = FreestyleCriterionAdapter(presentation) { recomputeTotals() }

        b.rvTechnical.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = techAdapter
            setHasFixedSize(true)
        }

        b.rvPresentation.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = presAdapter
            setHasFixedSize(true)
        }
    }

    // ---------- MANDATORY STANCE & DEDUCTIONS ----------

    private fun setupMandatoryStance() {
        // Example: checked = full base (0.7), unchecked = 0
//        b.cbMandatoryStance.setOnCheckedChangeListener { _, isChecked ->
//            mandatoryBase = if (isChecked) 0.7 else 0.0
//            recomputeTotals()
//        }
    }

    private fun setupDeductionControls() {
        // Example plain EditText for total deductions (time, restarts, boundary, etc.)
        b.etDeductions.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                deductions = s?.toString()?.toDoubleOrNull() ?: 0.0
                recomputeTotals()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ---------- TOTALS & UI ----------

    private fun recomputeTotals() {
        // Base technical from mandatory stances + criteria
        val techCriteriaSum = technical.sumOf { it.score }
        technicalSubtotal = mandatoryBase + techCriteriaSum   // max ~6.0

        presentationSubtotal = presentation.sumOf { it.score }   // max 4.0

        val totalBefore = technicalSubtotal + presentationSubtotal // max 10.0
        val final = (totalBefore - deductions).coerceAtLeast(0.0)
        finalScore = round3(final)

        // Show subtotals clearly
        b.tvTechSubtotal.text = String.format("%.1f / 6.0", technicalSubtotal)
        b.tvPresentationSubtotal.text = String.format("%.1f / 4.0", presentationSubtotal)

        b.tvTotalBeforeDeduction.text = String.format(
            "Technical: %.1f / 6.0    Presentation: %.1f / 4.0    Total: %.1f / 10.0",
            technicalSubtotal,
            presentationSubtotal,
            totalBefore
        )

        b.tvFinalScore.text = String.format("Final Score: %.3f", finalScore)
    }

    private fun round3(v: Double): Double =
        (v * 1000.0).roundToInt() / 1000.0

    // ---------- SUBMIT TO HOST (if needed) ----------

    private fun setupSubmitButton() {
        b.btnSubmitScore.setOnClickListener {
            submitScoreToHost()
        }
    }

    private fun submitScoreToHost() {
        // You can extend this JSON however you like (e.g. technicalSubtotal, presentationSubtotal, finalScore)
        val refereeName = b.etRefereeName.text?.toString()?.trim().orEmpty()
        val hostIp = b.etHostIp.text?.toString()?.trim().orEmpty()
        val port = 5555

        if (refereeName.isEmpty() || hostIp.isEmpty()) {
            Toast.makeText(requireContext(), "Enter referee name & host IP", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val payload = JSONObject().apply {
            put("mode", "freestyle")
            put("refereeName", refereeName)
            put("technical", round3(technicalSubtotal))
            put("presentation", round3(presentationSubtotal))
            put("deductions", round3(deductions))
            put("finalScore", finalScore)
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(hostIp, port), 3000)

                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(payload)
                    writer.newLine()
                    writer.flush()

                    socket.soTimeout = 3000
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val response = reader.readLine()

                    withContext(Dispatchers.Main) {
                        if (response == "OK") {
                            Toast.makeText(
                                requireContext(),
                                "Freestyle score submitted",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Submit failed (no ACK)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Submit failed: ${e.localizedMessage ?: "connection error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}