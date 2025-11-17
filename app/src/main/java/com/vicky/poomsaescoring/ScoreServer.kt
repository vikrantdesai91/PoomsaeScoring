package com.vicky.poomsaescoring

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.math.roundToInt

class ScoreServer(
    private val viewModel: HostViewModel,
    private val port: Int = 5555
) {
    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (running) return
        running = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)

                while (running) {
                    val client = try {
                        serverSocket?.accept() ?: break
                    } catch (e: SocketException) {
                        break
                    }
                    handleClient(client)
                }
            } finally {
                try {
                    serverSocket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
    }

    private fun handleClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            socket.use { s ->
                try {
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val line = reader.readLine() ?: return@use

                    val json = JSONObject(line)

                    val refereeName = json.optString("refereeName", "Unknown")
                    val player1Accuracy = json.optDouble("player1Accuracy", 0.0)
                    val player2Accuracy = json.optDouble("player2Accuracy", 0.0)
                    val player1Presentation = json.optDouble("player1Presentation", 0.0)
                    val player2Presentation = json.optDouble("player2Presentation", 0.0)

                    // Calculate the total score for both players (accuracy + presentation)
                    val player1Total = round3(player1Accuracy + player1Presentation)
                    val player2Total = round3(player2Accuracy + player2Presentation)

                    // Update scores in the ViewModel
                    viewModel.addOrUpdateRefereeScore(
                        name = refereeName,
                        player1Accuracy = player1Accuracy,
                        player2Accuracy = player2Accuracy,
                        player1Presentation = player1Presentation,
                        player2Presentation = player2Presentation,
                        player1Total = player1Total,
                        player2Total = player2Total
                    )

                    // Send ACK back to the client (scoring app)
                    val writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                    writer.write("OK")
                    writer.newLine()
                    writer.flush()

                } catch (e: Exception) {
                    Log.e("ScoreServer", "Client error: ${e.message}", e)
                }
            }
        }
    }


    // Helper function to round values to 3 decimal places
    private fun round3(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }
}