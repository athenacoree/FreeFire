package com.jules.gameguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jules.gameguard.MainActivity
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.data.PingRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

data class MonitorState(
    val pingMs: Long,
    val packetLossPercent: Int,
    val status: String // "Buena", "Regular", "Mala"
)

class ConnectionMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var preferences: GameGuardPreferences
    private lateinit var db: AppDatabase

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "connection_monitor_channel"

        val isRunning = MutableStateFlow(false)

        private val _monitorState = MutableStateFlow<MonitorState?>(null)
        val monitorState: StateFlow<MonitorState?> = _monitorState
    }

    override fun onCreate() {
        super.onCreate()
        preferences = GameGuardPreferences(applicationContext)
        db = AppDatabase.getDatabase(applicationContext)
        isRunning.value = true

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Iniciando monitor..."))

        // Start periodic monitor loop
        serviceScope.launch {
            while (isActive) {
                runPingCheck()
                delay(3000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        serviceJob.cancel()
    }

    private suspend fun runPingCheck() {
        val server = preferences.configurableServer
        // We ping both 8.8.8.8 and configured server, or do configured server pinging.
        // Let's perform a dual ping and combine/calculate packet loss & latency.
        // Alternatively, the spec says: "Cada 3 segundos haga ping a 8.8.8.8 y a un servidor configurable, midiendo latencia y pérdida de paquetes"

        val resultGoogle = pingHost("8.8.8.8")
        val resultConfig = pingHost(server)

        // Combine latencies (e.g. average or worst, let's take average if both succeed, or whichever succeeds).
        // Standard ping output extraction gives us latency and success/loss.
        val latencyGoogle = resultGoogle.first
        val lossGoogle = resultGoogle.second

        val latencyConfig = resultConfig.first
        val lossConfig = resultConfig.second

        // Calculate packet loss (average of both or configured)
        val averageLoss = ((lossGoogle + lossConfig) / 2.0).roundToInt()

        // Choose a representative ping: average of successful ones
        val successfulPings = listOf(latencyGoogle, latencyConfig).filter { it >= 0 }
        val representativePing = if (successfulPings.isNotEmpty()) {
            successfulPings.average().toLong()
        } else {
            -1L
        }

        // State determination
        // Buena: Ping < 80ms and Loss < 5%
        // Regular: Ping between 80ms and 150ms or Loss between 5% and 15%
        // Mala: Ping > 150ms or Loss > 15% or Ping is -1 (error)
        val status = when {
            representativePing == -1L || averageLoss > 15 || representativePing > 150 -> "Mala"
            averageLoss >= 5 || representativePing >= 80 -> "Regular"
            else -> "Buena"
        }

        val state = MonitorState(
            pingMs = if (representativePing >= 0) representativePing else 0,
            packetLossPercent = averageLoss,
            status = status
        )

        _monitorState.value = state

        // Save to Room and prune old records
        val record = PingRecord(
            timestamp = System.currentTimeMillis(),
            pingMs = state.pingMs,
            packetLossPercent = state.packetLossPercent,
            status = state.status
        )
        db.pingRecordDao().insertAndPrune(record)

        // Update persistent notification
        val text = "Ping: ${state.pingMs} ms | Pérdida: ${state.packetLossPercent}% | Conexión: ${state.status}"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    /**
     * Executes android ping command. Returns Pair(latencyMs, packetLossPercent)
     */
    private fun pingHost(host: String): Pair<Long, Double> {
        try {
            // Run 3 pings
            val process = Runtime.getRuntime().exec("ping -c 3 -W 1 $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var rttAvg = -1.0
            var packetLoss = 100.0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                if (currentLine.contains("packet loss")) {
                    // Extract packet loss
                    // E.g., "3 packets transmitted, 3 received, 0% packet loss, time 2003ms"
                    val regex = "(\\d+)% packet loss".toRegex()
                    val match = regex.find(currentLine)
                    if (match != null) {
                        packetLoss = match.groupValues[1].toDouble()
                    }
                } else if (currentLine.contains("rtt min/avg/max/mdev")) {
                    // Extract avg rtt
                    // E.g., "rtt min/avg/max/mdev = 12.345/14.567/16.789/1.234 ms"
                    val parts = currentLine.split(" = ")
                    if (parts.size > 1) {
                        val stats = parts[1].split("/")
                        if (stats.size > 1) {
                            rttAvg = stats[1].toDouble()
                        }
                    }
                }
            }
            process.destroy()

            return Pair(rttAvg.toLong(), packetLoss)
        } catch (e: Exception) {
            return Pair(-1L, 100.0)
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor de Conexión GameGuard")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Monitor de Conexión de GameGuard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra de forma persistente la latencia y calidad de la conexión actual"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
