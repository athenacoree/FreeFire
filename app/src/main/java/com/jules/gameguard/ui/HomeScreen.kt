package com.jules.gameguard.ui

import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.service.ConnectionMonitorService

enum class ActivationStep {
    NONE,
    EXPLAIN_OVERLAY,
    EXPLAIN_USAGE_STATS,
    SHOW_RAM_CLEANER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isModoJuegoActivo by remember { mutableStateOf(preferences.isModoJuegoActivo) }
    var lastClosedApps by remember { mutableStateOf(preferences.lastClosedApps) }
    var lastSessionFeedback by remember { mutableStateOf(preferences.lastSessionFeedback) }

    // Collect real-time monitor stats
    val serviceState by ConnectionMonitorService.monitorState.collectAsState()
    val pingMs = if (isModoJuegoActivo) (serviceState?.pingMs ?: 0L) else 0L
    val pingStatus = if (isModoJuegoActivo) (serviceState?.status ?: "BUENA") else "INACTIVO"

    // Dialog trigger states
    var showOverlayExplanation by remember { mutableStateOf(false) }
    var showUsageStatsExplanation by remember { mutableStateOf(false) }
    var showRamCleaner by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

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
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                showRamCleaner = true
            } else {
                showRamCleaner = true
            }
        } else {
            showRamCleaner = true
        }
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

    LaunchedEffect(Unit) {
        ConnectionMonitorService.isRunning.collect { running ->
            isModoJuegoActivo = running && preferences.isModoJuegoActivo
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isModoJuegoActivo) ColorCyan else ColorRed, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GAMEGUARD HUD",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("history") },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Text("📊", color = ColorCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Text("⚙", color = ColorCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = ColorBackground
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
                    .padding(16.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PingGauge(ping = pingMs, status = pingStatus)

                Spacer(modifier = Modifier.height(8.dp))

                GameModeActionButton(
                    isActive = isModoJuegoActivo,
                    onClick = {
                        if (isModoJuegoActivo) {
                            deactivateAll()
                        } else {
                            triggerActivationSequence()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "[ MONITOR DE ESTADO ]",
                            color = ColorCyan,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BLOQUEO DE LLAMADAS (DND):",
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val dndColor = if (isModoJuegoActivo) ColorCyan else ColorRed
                            Text(
                                text = if (isModoJuegoActivo) "ACTIVO (DND)" else "INACTIVO",
                                color = dndColor,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ÚLTIMAS APPS LIBERADAS:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (lastClosedApps.isNotEmpty()) {
                                Text(
                                    text = lastClosedApps,
                                    color = ColorCyan.copy(alpha = 0.85f),
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Text(
                                    text = "Ninguna app cerrada recientemente. Libera RAM para optimizar.",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ESTADO ÚLTIMA SESIÓN:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val feedbackText = lastSessionFeedback.ifEmpty { "SIN REGISTRO" }
                            val feedbackColor = if (feedbackText.uppercase().contains("BUENA")) ColorCyan else if (feedbackText.uppercase().contains("PROBLEMA")) ColorRed else Color.White.copy(alpha = 0.5f)
                            Text(
                                text = feedbackText.uppercase(),
                                color = feedbackColor,
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isModoJuegoActivo) {
                    Button(
                        onClick = { deactivateAll() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorRed.copy(alpha = 0.15f),
                            contentColor = ColorRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, ColorRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "DESACTIVAR TODO (UN TAP)",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
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
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(2.dp, ColorCyan, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "[ JUEGO TERMINADO ]",
                        color = ColorAmber,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "¿CÓMO ESTUVO TU SESIÓN?",
                        color = Color.White,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = ColorCyan.copy(alpha = 0.3f), thickness = 1.dp)

                    Text(
                        text = "Tu feedback nos ayuda a calibrar los perfiles de optimización de red de GameGuard.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "CON PROBLEMAS",
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
                                containerColor = ColorCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "BUENA",
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
fun PingGauge(ping: Long, status: String) {
    val color = when (status) {
        "Regular" -> ColorAmber
        "Mala" -> ColorRed
        "INACTIVO" -> Color.White.copy(alpha = 0.4f)
        else -> ColorCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val scale by if (status == "BUENA" && ping > 0) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val animatedSweepAngle by animateFloatAsState(
        targetValue = if (ping > 0) ((ping.coerceIn(0, 200) / 200f) * 360f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sweep_angle"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(150.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            if (ping > 0) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = (animatedSweepAngle * 270f / 360f),
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (ping > 0) "$ping" else "--",
                color = color,
                fontFamily = OrbitronFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "MS PING",
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )
            Text(
                text = status.uppercase(),
                color = color.copy(alpha = 0.8f),
                fontFamily = RajdhaniFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun GameModeActionButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "glow")

    // Animate glow as a float from 4f to 12f
    val shadowRadiusRaw by if (!isActive) {
        transition.animateFloat(
            initialValue = 4f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shadow_glow"
        )
    } else {
        remember { mutableStateOf(16f) }
    }

    val neonModifier = if (isActive) {
        Modifier.neonBorderAnimation(
            colors = listOf(ColorCyan, Color(0xFF, 0x00, 0xD4, 0xFF), ColorCyan)
        )
    } else {
        Modifier.border(1.5.dp, ColorCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    }

    Box(
        modifier = Modifier
            .size(width = 250.dp, height = 66.dp)
            .shadow(
                elevation = shadowRadiusRaw.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = ColorCyan,
                spotColor = ColorCyan
            )
            .background(
                if (isActive) ColorCyan.copy(alpha = 0.15f) else ColorGlassBg,
                shape = RoundedCornerShape(24.dp)
            )
            .then(neonModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isActive) ColorCyan else ColorRed, shape = CircleShape)
            )
            Text(
                text = if (isActive) "MODO JUEGO: ACTIVO" else "ACTIVAR MODO JUEGO",
                color = if (isActive) ColorCyan else Color.White,
                fontFamily = OrbitronFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
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
