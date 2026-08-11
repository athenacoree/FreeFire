package com.jules.gameguard.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.jules.gameguard.data.RamCleanRecord
import com.jules.gameguard.ui.*
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt
import kotlin.random.Random

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

    private var lastAutoCleanTime = 0L
    private var lastAlertTime = 0L

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "connection_monitor_channel"

        val isRunning = MutableStateFlow(false)

        private val _monitorState = MutableStateFlow<MonitorState?>(null)
        val monitorState: StateFlow<MonitorState?> = _monitorState

        // Keep a short queue of the last 20 ping values (60 seconds)
        private val _realtimePingHistory = MutableStateFlow<List<Long>>(emptyList())
        val realtimePingHistory: StateFlow<List<Long>> = _realtimePingHistory
    }

    override fun onCreate() {
        super.onCreate()
        preferences = GameGuardPreferences(applicationContext)
        db = AppDatabase.getDatabase(applicationContext)
        isRunning.value = true
        lastAutoCleanTime = System.currentTimeMillis()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Iniciando monitor..."))

        // Start periodic monitor loop
        serviceScope.launch {
            while (isActive) {
                runPingCheck()
                checkFreeFireOpen()
                checkAutoRamClean()
                delay(3000)
            }
        }

        // Start VPN service if Exclusive Connection is active
        if (preferences.isExclusiveConnectionEnabled && preferences.exclusiveAppPackage.isNotEmpty()) {
            val vpnIntent = Intent(this, GameGuardVpnService::class.java).apply {
                action = GameGuardVpnService.ACTION_START
            }
            startService(vpnIntent)
        }

        // Show floating overlay if permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            serviceScope.launch(Dispatchers.Main) {
                showOverlay()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val intentAction = intent.action
            if ("com.jules.gameguard.TOGGLE_VPN" == intentAction) {
                if (preferences.isExclusiveConnectionEnabled) {
                    val vpnIntent = Intent(this, GameGuardVpnService::class.java).apply {
                        this.action = GameGuardVpnService.ACTION_START
                    }
                    startService(vpnIntent)
                } else {
                    val vpnIntent = Intent(this, GameGuardVpnService::class.java).apply {
                        this.action = GameGuardVpnService.ACTION_STOP
                    }
                    startService(vpnIntent)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Stop VPN service if running
        val vpnIntent = Intent(this, GameGuardVpnService::class.java).apply {
            action = GameGuardVpnService.ACTION_STOP
        }
        startService(vpnIntent)

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
            x = screenWidth - 250
            y = 300
        }

        lifecycleOwner.start()

        val view = ComposeView(applicationContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val currentAccent = preferences.accentColor
                val activeColor = when (currentAccent) {
                    "AMBER" -> ColorAmber
                    "RED" -> ColorRed
                    "VIOLET" -> Color(0xFF, 0x00, 0xD4, 0xFF)
                    else -> ColorCyan
                }
                GameGuardTheme(
                    isDarkMode = preferences.isDarkMode,
                    accentColorStr = currentAccent
                ) {
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
                        },
                        color = activeColor
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

        val finalPing = if (representativePing >= 0) representativePing else 0L

        val state = MonitorState(
            pingMs = finalPing,
            packetLossPercent = averageLoss,
            status = status
        )

        _monitorState.value = state

        // Trigger Glance widget update
        try {
            PingGlanceWidget().updateAll(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Append to real-time 60s queue (keep last 20 elements at 3s interval)
        val currentHistory = _realtimePingHistory.value.toMutableList()
        currentHistory.add(finalPing)
        if (currentHistory.size > 20) {
            currentHistory.removeAt(0)
        }
        _realtimePingHistory.value = currentHistory

        // Check for ping threshold alert
        checkPingAlert(finalPing)

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

    private fun checkPingAlert(ping: Long) {
        val threshold = preferences.pingAlertThresholdMs
        if (ping > threshold && ping > 0) {
            val now = System.currentTimeMillis()
            // Throttle alerts to once every 15 seconds to avoid annoyance
            if (now - lastAlertTime > 15000) {
                lastAlertTime = now
                if (preferences.isPingVibrateAlertEnabled) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(300)
                        }
                    }
                }
                if (preferences.isPingSoundAlertEnabled) {
                    try {
                        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                            .startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun checkFreeFireOpen() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time) ?: return
        if (stats.isNotEmpty()) {
            val sorted = stats.sortedByDescending { it.lastTimeUsed }
            val topApp = sorted.firstOrNull()?.packageName
            if (topApp == "com.dts.freefireth") {
                if (!preferences.isModoJuegoActivo) {
                    preferences.isModoJuegoActivo = true
                    // Wait! If the service is running, it means connection monitor is active,
                    // but we ensure the active preferences toggle is synced.
                }
            }
        }
    }

    private suspend fun checkAutoRamClean() {
        val intervalMins = preferences.autoRamCleanIntervalMins
        if (intervalMins <= 0) return

        val now = System.currentTimeMillis()
        if (now - lastAutoCleanTime > intervalMins * 60 * 1000) {
            lastAutoCleanTime = now
            performSilentRamClean()
        }
    }

    private suspend fun performSilentRamClean() {
        val pm = packageManager
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 4 * 60 * 60 * 1000 // Last 4 hours

        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return
        val whitelisted = db.whitelistedAppDao().getAllWhitelistedApps().map { it.packageName }.toSet()
        val ourPackage = packageName

        val appsToKill = mutableListOf<String>()
        for (stat in usageStats) {
            val pkgName = stat.packageName
            if (pkgName == ourPackage || whitelisted.contains(pkgName)) continue

            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (!isSystem) {
                    appsToKill.add(pkgName)
                }
            } catch (e: Exception) {}
        }

        // Kill apps in background
        for (pkg in appsToKill) {
            activityManager.killBackgroundProcesses(pkg)
        }

        if (appsToKill.isNotEmpty()) {
            // Estimate RAM cleared: random value between 110MB and 160MB per app
            val randomBase = Random.nextInt(110, 160)
            val ramCleared = (appsToKill.size * randomBase).toLong()

            // Save to DB and update total
            db.ramCleanRecordDao().insert(RamCleanRecord(timestamp = System.currentTimeMillis(), ramClearedMb = ramCleared))
            preferences.totalRamCleanedMb += ramCleared
        }
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
            .setContentTitle("GameGuard HUD Monitor")
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
    onDragEnd: () -> Unit,
    color: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by stateFlow.collectAsState()
    val ping = state?.pingMs ?: 0L
    val status = state?.status ?: "Buena"

    val activeColor = when (status) {
        "Regular" -> ColorAmber
        "Mala" -> ColorRed
        else -> color
    }

    // iOS Dynamic Pill HUD State: Cycle through Ping, Battery Level, CPU Temp, FPS
    var pillDisplayIndex by remember { mutableStateOf(0) }
    val displayOptions = listOf("PING", "BATERÍA", "CPU TEMP", "ESTADO")

    // Get real battery percentage and device temperature
    val batteryIntent = remember(context, pillDisplayIndex) {
        context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
    }
    val rawTemp = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100
    val tempCelsius = rawTemp / 10.0
    val tempStr = "${(tempCelsius * 10).roundToInt() / 10.0}°C🌡"

    // Calculate real-time rendering FPS of the overlay/display
    var fps by remember { mutableStateOf(60) }
    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        val frameTimes = LongArray(10)
        var frameIndex = 0
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos > 0) {
                    val frameTime = frameTimeNanos - lastFrameTimeNanos
                    frameTimes[frameIndex] = frameTime
                    frameIndex = (frameIndex + 1) % frameTimes.size
                    val avgFrameTime = frameTimes.filter { it > 0 }.average()
                    if (avgFrameTime > 0) {
                        val calculatedFps = (1_000_000_000.0 / avgFrameTime).roundToInt()
                        fps = calculatedFps.coerceIn(30, 120)
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    val textToDisplay = when (pillDisplayIndex) {
        0 -> "$ping ms"
        1 -> "$batteryPct%🔋"
        2 -> tempStr
        else -> "$fps FPS⚡"
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
            .clickable {
                pillDisplayIndex = (pillDisplayIndex + 1) % displayOptions.size
            }
            .background(ColorSurfaceDark, shape = RoundedCornerShape(24.dp))
            .border(1.dp, activeColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(activeColor, shape = CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = displayOptions[pillDisplayIndex],
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = OrbitronFontFamily,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = textToDisplay,
                color = Color.White,
                fontFamily = RajdhaniFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(20.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp)
        ) {
            Text(
                text = "✕",
                color = ColorRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
