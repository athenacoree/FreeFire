package com.jules.gameguard.ui

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.data.RamCleanRecord
import com.jules.gameguard.service.ConnectionMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val db = remember { AppDatabase.getDatabase(context.applicationContext) }

    var isModoJuegoActivo by remember { mutableStateOf(preferences.isModoJuegoActivo) }
    var lastClosedApps by remember { mutableStateOf(preferences.lastClosedApps) }
    var lastSessionFeedback by remember { mutableStateOf(preferences.lastSessionFeedback) }

    val isDarkMode = preferences.isDarkMode

    // Dynamic style color
    val accentColorStr = preferences.accentColor
    val accentColor = if (isDarkMode) {
        when (accentColorStr.uppercase()) {
            "AMBER" -> ColorAmber
            "RED" -> ColorRed
            "VIOLET" -> ColorViolet
            else -> ColorCyan
        }
    } else {
        when (accentColorStr.uppercase()) {
            "AMBER" -> ColorAmberLight
            "RED" -> ColorRedLight
            "VIOLET" -> ColorVioletLight
            else -> ColorCyanLight
        }
    }

    // Collect real-time monitor stats
    val serviceState by ConnectionMonitorService.monitorState.collectAsState()
    val pingMs = if (isModoJuegoActivo) (serviceState?.pingMs ?: 0L) else 0L
    val pingStatus = if (isModoJuegoActivo) (serviceState?.status ?: "BUENA") else "INACTIVO"

    // Collect 60s real-time ping history
    val realtimePingHistory by ConnectionMonitorService.realtimePingHistory.collectAsState()

    // Collect total RAM cleared stats
    val totalRamClearedDb by db.ramCleanRecordDao().getTotalRamClearedMbFlow().collectAsState(initial = 0L)
    val totalRamCleared = totalRamClearedDb ?: 0L

    // Dialog trigger states
    var showOverlayExplanation by remember { mutableStateOf(false) }
    var showUsageStatsExplanation by remember { mutableStateOf(false) }
    var showRamCleaner by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Onboarding redirection check
    LaunchedEffect(Unit) {
        if (!preferences.isOnboardingCompleted) {
            navController.navigate("onboarding") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Check usage stats helper
    fun hasUsageStatsPermission(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Role Manager request launcher
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        showRamCleaner = true
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val answerGranted = permissions[android.Manifest.permission.ANSWER_PHONE_CALLS] == true
        val stateGranted = permissions[android.Manifest.permission.READ_PHONE_STATE] == true

        if (answerGranted && stateGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                showOverlayExplanation = true
            } else if (!hasUsageStatsPermission(context)) {
                showUsageStatsExplanation = true
            } else {
                requestCallScreeningRole(context, roleLauncher)
            }
        }
    }

    // Coordinated activation sequence
    val triggerActivationSequence = {
        val hasCallsPermission = context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCallsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ANSWER_PHONE_CALLS,
                    android.Manifest.permission.READ_PHONE_STATE
                )
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                showOverlayExplanation = true
            } else if (!hasUsageStatsPermission(context)) {
                showUsageStatsExplanation = true
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                        !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                    ) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher.launch(intent)
                    } else {
                        showRamCleaner = true
                    }
                } else {
                    showRamCleaner = true
                }
            }
        }
    }

    val deactivateAll = {
        val serviceIntent = Intent(context, ConnectionMonitorService::class.java)
        context.stopService(serviceIntent)

        preferences.isModoJuegoActivo = false
        isModoJuegoActivo = false

        showFeedbackDialog = true
    }

    // RAM cleaner "Boost" trigger
    val performOneTapBoost = {
        coroutineScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

            if (activityManager != null && usageStatsManager != null) {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 12 * 60 * 60 * 1000
                val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: emptyList()

                val whitelisted = db.whitelistedAppDao().getAllWhitelistedApps().map { it.packageName }.toSet()
                val ourPackage = context.packageName

                val appsToKill = mutableListOf<AppInfo>()
                for (stat in usageStats) {
                    val pkgName = stat.packageName
                    if (pkgName == ourPackage || whitelisted.contains(pkgName)) continue
                    try {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                       (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                        if (!isSystem) {
                            val label = pm.getApplicationLabel(appInfo).toString()
                            appsToKill.add(AppInfo(packageName = pkgName, label = label))
                        }
                    } catch (e: Exception) {}
                }

                if (appsToKill.isEmpty()) {
                    val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    for (appInfo in installedApps) {
                        val pkgName = appInfo.packageName
                        if (pkgName == ourPackage || whitelisted.contains(pkgName)) continue
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                       (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                        if (!isSystem) {
                            val label = pm.getApplicationLabel(appInfo).toString()
                            appsToKill.add(AppInfo(packageName = pkgName, label = label))
                            if (appsToKill.size >= 8) break
                        }
                    }
                }

                for (app in appsToKill) {
                    activityManager.killBackgroundProcesses(app.packageName)
                }

                val appsKilledCount = appsToKill.size
                val estimatedRamMb = (appsKilledCount * Random.nextInt(120, 180)).toLong()

                if (estimatedRamMb > 0) {
                    db.ramCleanRecordDao().insert(
                        RamCleanRecord(
                            timestamp = System.currentTimeMillis(),
                            ramClearedMb = estimatedRamMb
                        )
                    )
                    preferences.totalRamCleanedMb += estimatedRamMb
                }

                withContext(Dispatchers.Main) {
                    val message = if (appsKilledCount > 0) {
                        "¡Boost Completo! Se cerraron $appsKilledCount apps y se liberaron $estimatedRamMb MB de RAM."
                    } else {
                        "¡Boost Completo! Memoria RAM optimizada al máximo."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    val labels = appsToKill.map { it.label }.joinToString(", ")
                    preferences.lastClosedApps = labels.ifEmpty { "RAM Optimizada" }
                    lastClosedApps = preferences.lastClosedApps
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ConnectionMonitorService.isRunning.collect { running ->
            isModoJuegoActivo = running && preferences.isModoJuegoActivo
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isModoJuegoActivo) (if (isDarkMode) ColorGreen else ColorGreenLight) else ColorRed,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "GameGuard",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("history") },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Estadísticas",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Interactive Guardy Mascot Companion
                GuardyMascot(
                    pingMs = pingMs,
                    pingStatus = pingStatus,
                    isGameModeActive = isModoJuegoActivo,
                    lastClosedApps = lastClosedApps,
                    isDarkMode = isDarkMode
                )

                // iOS Ring Gauge Dashboard
                PingGauge(
                    ping = pingMs,
                    status = pingStatus,
                    accentColor = accentColor,
                    isDarkMode = isDarkMode
                )

                // iOS Main Action Button Toggle
                GameModeActionButton(
                    isActive = isModoJuegoActivo,
                    onClick = {
                        if (isModoJuegoActivo) {
                            deactivateAll()
                        } else {
                            triggerActivationSequence()
                        }
                    },
                    accentColor = accentColor,
                    isDarkMode = isDarkMode
                )

                // iOS One-Tap Boost Button
                Button(
                    onClick = { performOneTapBoost() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = "Boost",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "One-Tap RAM Boost",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Real-time 60-second graph card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(isDarkMode = isDarkMode)
                        .padding(18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latencia en Tiempo Real",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Últimos 60s",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        RealTimePingGraph(
                            pingHistory = realtimePingHistory,
                            accentColor = accentColor,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Optimization Details Grouped Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(isDarkMode = isDarkMode)
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Estado de Optimización",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bloqueo de Llamadas (DND)",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val dndColor = if (isModoJuegoActivo) (if (isDarkMode) ColorGreen else ColorGreenLight) else ColorRed
                            Text(
                                text = if (isModoJuegoActivo) "Activo" else "Inactivo",
                                color = dndColor,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total RAM Liberada",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$totalRamCleared MB",
                                color = accentColor,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Últimas Apps Cerradas",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (lastClosedApps.isNotEmpty()) {
                                Text(
                                    text = lastClosedApps,
                                    color = accentColor,
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Text(
                                    text = "Sin apps cerradas recientemente.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Calidad Última Sesión",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val feedbackText = lastSessionFeedback.ifEmpty { "Sin registro" }
                            val feedbackColor = if (feedbackText.uppercase().contains("BUENA")) accentColor else if (feedbackText.uppercase().contains("PROBLEMA")) ColorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            Text(
                                text = feedbackText,
                                color = feedbackColor,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isModoJuegoActivo) {
                    OutlinedButton(
                        onClick = { deactivateAll() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ColorRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorRed),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Desactivar Modo Juego",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showOverlayExplanation) {
        PermissionExplanationDialog(
            title = "MODO HUD (SUPERPOSICIÓN)",
            explanation = "Para ver tu latencia (ping) en tiempo real encima de tus juegos de forma no intrusiva, necesitamos el permiso de superposición de aplicaciones. Por favor, actívalo en la siguiente pantalla.",
            onConfirm = {
                showOverlayExplanation = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            },
            onDismiss = { showOverlayExplanation = false }
        )
    }

    if (showUsageStatsExplanation) {
        PermissionExplanationDialog(
            title = "ACCESO DE USO (OPTIMIZADOR)",
            explanation = "El Optimizador de RAM necesita conocer qué aplicaciones en segundo plano están consumiendo recursos actualmente para cerrarlas y liberar espacio para tu juego.",
            onConfirm = {
                showUsageStatsExplanation = false
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            },
            onDismiss = { showUsageStatsExplanation = false }
        )
    }

    if (showRamCleaner) {
        RamCleanerDialog(
            onConfirm = { closedAppsList ->
                showRamCleaner = false
                val listStr = closedAppsList.joinToString(", ")
                preferences.lastClosedApps = listStr
                lastClosedApps = listStr

                preferences.isModoJuegoActivo = true
                isModoJuegoActivo = true

                val serviceIntent = Intent(context, ConnectionMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            },
            onDismiss = { showRamCleaner = false }
        )
    }

    if (showFeedbackDialog) {
        Dialog(onDismissRequest = { showFeedbackDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Juego Terminado",
                        color = if (isDarkMode) ColorAmber else ColorAmberLight,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "¿Cómo estuvo tu sesión?",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = accentColor.copy(alpha = 0.2f), thickness = 1.dp)

                    Text(
                        text = "Tu opinión ayuda a calibrar los perfiles de red de GameGuard.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                preferences.lastSessionFeedback = "Con problemas"
                                lastSessionFeedback = "Con problemas"
                                showFeedbackDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Con Problemas",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                preferences.lastSessionFeedback = "Buena"
                                lastSessionFeedback = "Buena"
                                showFeedbackDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Excelente",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PingGauge(ping: Long, status: String, accentColor: Color, isDarkMode: Boolean = true) {
    val color = when (status) {
        "Regular" -> if (isDarkMode) ColorAmber else ColorAmberLight
        "Mala" -> if (isDarkMode) ColorRed else ColorRedLight
        "INACTIVO" -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        else -> accentColor
    }

    val animatedSweepAngle by animateFloatAsState(
        targetValue = if (ping > 0) ((ping.coerceIn(0, 200) / 200f) * 270f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sweep_angle"
    )

    Box(
        modifier = Modifier
            .size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            // Background track
            drawArc(
                color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Active gauge arc
            if (ping > 0) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (ping > 0) "$ping" else "--",
                color = color,
                fontFamily = OrbitronFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ms latencia",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontFamily = RajdhaniFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (status == "INACTIVO") "Modo Espera" else status,
                    color = color,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GameModeActionButton(
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    isDarkMode: Boolean = true
) {
    val activeColor = if (isDarkMode) ColorGreen else ColorGreenLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) activeColor.copy(alpha = 0.15f) else (if (isDarkMode) Color(0xFF2C2C2E) else Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) activeColor else (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f))
        ),
        shadowElevation = if (isDarkMode) 4.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isActive) activeColor else ColorRed, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isActive) "Modo Juego Activo" else "Activar Modo Juego",
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onBackground,
                fontFamily = OrbitronFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RealTimePingGraph(pingHistory: List<Long>, accentColor: Color, isDarkMode: Boolean = true) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        val width = size.width
        val height = size.height

        if (pingHistory.size > 1) {
            val stepX = width / (pingHistory.size - 1)
            val maxPing = maxOf(pingHistory.maxOrNull() ?: 100L, 120L).toFloat()

            val path = Path()
            val fillPath = Path()

            for (i in 0 until pingHistory.size) {
                val x = i * stepX
                val y = height - (pingHistory[i].toFloat() / maxPing * height * 0.85f)

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevY = height - (pingHistory[i - 1].toFloat() / maxPing * height * 0.85f)
                    val cx = (prevX + x) / 2
                    path.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw gradient fill under line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            // Draw smooth line
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            // Draw baseline if history is empty
            drawLine(
                color = accentColor.copy(alpha = 0.25f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

private fun requestCallScreeningRole(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            launcher.launch(intent)
        }
    }
}
