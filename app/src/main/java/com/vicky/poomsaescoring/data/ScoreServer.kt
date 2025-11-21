package com.vicky.poomsaescoring.data

import android.util.Log
import com.vicky.poomsaescoring.viewModel.HostViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.BindException
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
    private var serverJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        if (running) {
            Log.d("ScoreServer", "Server already running, skipping start()")
            return
        }
        running = true

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("ScoreServer", "Server started on port $port")

                while (running) {
                    val client = try {
                        serverSocket?.accept() ?: break
                    } catch (e: SocketException) {
                        Log.d("ScoreServer", "Server socket closed")
                        break
                    }
                    handleClient(client)
                }
            } catch (e: BindException) {
                Log.e("ScoreServer", "PORT $port already in use (EADDRINUSE)", e)
            } catch (e: Exception) {
                Log.e("ScoreServer", "Server error: ${e.message}", e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverJob?.cancel()
        Log.d("ScoreServer", "Server stopped")
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
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

                    val p1Total = round3(player1Accuracy + player1Presentation)
                    val p2Total = round3(player2Accuracy + player2Presentation)

                    viewModel.addOrUpdateRefereeScore(
                        name = refereeName,
                        player1Accuracy = player1Accuracy,
                        player2Accuracy = player2Accuracy,
                        player1Presentation = player1Presentation,
                        player2Presentation = player2Presentation,
                        player1Total = p1Total,
                        player2Total = p2Total
                    )

                    // Acknowledge back to scoring app
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

    private fun round3(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }
}