package com.jules.gameguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jules.gameguard.MainActivity
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.data.PingRecord
import com.jules.gameguard.ui.*
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

    private var composeView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()

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

        // Show floating overlay if permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            // Must run on main thread
            serviceScope.launch(Dispatchers.Main) {
                showOverlay()
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
        lifecycleOwner.stop()

        if (composeView != null) {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                // ignore
            }
            composeView = null
        }
    }

    private fun showOverlay() {
        if (composeView != null) return

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 250 // start at the right side of the screen
            y = 300
        }

        lifecycleOwner.start()

        val view = ComposeView(applicationContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                GameGuardTheme {
                    OverlayView(
                        stateFlow = monitorState,
                        onClose = {
                            val serviceIntent = Intent(context, ConnectionMonitorService::class.java)
                            context.stopService(serviceIntent)
                        },
                        onDrag = { dx, dy ->
                            layoutParams.x = (layoutParams.x + dx.toInt()).coerceIn(0, displayMetrics.widthPixels - 120)
                            layoutParams.y = (layoutParams.y + dy.toInt()).coerceIn(0, displayMetrics.heightPixels - 80)
                            try {
                                windowManager.updateViewLayout(this, layoutParams)
                            } catch (e: Exception) {}
                        },
                        onDragEnd = {
                            val currentX = layoutParams.x
                            val bubbleWidth = 120
                            val targetX = if (currentX + (bubbleWidth / 2) < screenWidth / 2) {
                                0
                            } else {
                                screenWidth - bubbleWidth
                            }

                            serviceScope.launch(Dispatchers.Main) {
                                val startX = layoutParams.x
                                val steps = 15
                                val diff = targetX - startX
                                for (i in 1..steps) {
                                    layoutParams.x = startX + (diff * i / steps)
                                    try {
                                        windowManager.updateViewLayout(this@apply, layoutParams)
                                    } catch (e: Exception) {}
                                    delay(12)
                                }
                                layoutParams.x = targetX
                                try {
                                    windowManager.updateViewLayout(this@apply, layoutParams)
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(view, layoutParams)
            composeView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun runPingCheck() {
        val server = preferences.configurableServer

        val resultGoogle = pingHost("8.8.8.8")
        val resultConfig = pingHost(server)

        val latencyGoogle = resultGoogle.first
        val lossGoogle = resultGoogle.second

        val latencyConfig = resultConfig.first
        val lossConfig = resultConfig.second

        val averageLoss = ((lossGoogle + lossConfig) / 2.0).roundToInt()

        val successfulPings = listOf(latencyGoogle, latencyConfig).filter { it >= 0 }
        val representativePing = if (successfulPings.isNotEmpty()) {
            successfulPings.average().toLong()
        } else {
            -1L
        }

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

    private fun pingHost(host: String): Pair<Long, Double> {
        try {
            val process = Runtime.getRuntime().exec("ping -c 3 -W 1 $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var rttAvg = -1.0
            var packetLoss = 100.0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                if (currentLine.contains("packet loss")) {
                    val regex = "(\\d+)% packet loss".toRegex()
                    val match = regex.find(currentLine)
                    if (match != null) {
                        packetLoss = match.groupValues[1].toDouble()
                    }
                } else if (currentLine.contains("rtt min/avg/max/mdev")) {
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

class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val vmStore = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = vmStore
}

@Composable
fun OverlayView(
    stateFlow: StateFlow<MonitorState?>,
    onClose: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val state by stateFlow.collectAsState()
    val ping = state?.pingMs ?: 0L
    val status = state?.status ?: "Buena"

    val color = when (status) {
        "Regular" -> ColorAmber
        "Mala" -> ColorRed
        else -> ColorCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ping_pulse")
    val scale by if (status == "Buena") {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Row(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
            .scale(scale)
            .background(ColorGlassBg, shape = RoundedCornerShape(24.dp))
            .border(1.5.dp, color, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), shape = CircleShape)
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$ping",
                color = color,
                fontFamily = OrbitronFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "ms",
            color = Color.White.copy(alpha = 0.8f),
            fontFamily = RajdhaniFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                text = "✕",
                color = ColorRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
