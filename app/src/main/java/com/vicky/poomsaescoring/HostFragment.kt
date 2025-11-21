package com.vicky.poomsaescoring

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vicky.poomsaescoring.databinding.FragmentHostBinding


class HostFragment : Fragment() {

    private var _binding: FragmentHostBinding? = null
    private val b get() = _binding!!

    private val vm: HostViewModel by viewModels()
    private var server: ScoreServer? = null
    private val adapter = RefereeScoreAdapter()

    private var expectedJudges = 3
    private var serverStarted = false   // << Prevent double start

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        // RecyclerView
        b.rvScores.layoutManager = LinearLayoutManager(requireContext())
        b.rvScores.adapter = adapter

        // Spinner setup
        val judgeOptions = listOf(3, 5, 7)
        b.spJudgeCount.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            judgeOptions
        )
        b.spJudgeCount.setSelection(0)
        b.spJudgeCount.setOnItemSelectedListenerCompat { value ->
            expectedJudges = value
            renderMetrics()
        }

        // Show host IP
        val ip = getLocalIp()
        b.tvHostIp.text = "Host IP: $ip:5555"

        // START SERVER ONLY ONCE
        if (!serverStarted) {
            serverStarted = true
            server = ScoreServer(vm).also {
                try {
                    it.start()   // safe start
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Observe scores
        vm.scores.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            renderMetrics()
        }

        // Reset button
        b.btnResetHost.setOnClickListener {
            vm.resetScores()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Stop server SAFELY
        try {
            server?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        server = null
        serverStarted = false  // allow restarting when re-entering fragment
        _binding = null
    }

    private fun renderMetrics() {
        val m = vm.computeWtMetrics(expectedJudges)

        val p1 = vm.player1Average()
        val p2 = vm.player2Average()

        b.tvAveragePlayer1.text = String.format("Player 1 Average: %.3f", p1)
        b.tvAveragePlayer2.text = String.format("Player 2 Average: %.3f", p2)

        b.tvReceivedOutOf.text = "Received: ${m.received} / ${m.expected}"
    }

    private fun getLocalIp(): String {
        return try {
            val wm = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }
}

private inline fun Spinner.setOnItemSelectedListenerCompat(
    crossinline onSelected: (Int) -> Unit
) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>,
            view: View?,
            position: Int,
            id: Long
        ) {
            val value = parent.getItemAtPosition(position).toString().toInt()
            onSelected(value)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
}