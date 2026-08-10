package com.jules.gameguard.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.GameGuardPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCallsDialog by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }

    val isDarkMode = preferences.isDarkMode

    val accentColor = if (isDarkMode) {
        when (preferences.accentColor.uppercase()) {
            "AMBER" -> ColorAmber
            "RED" -> ColorRed
            "VIOLET" -> ColorViolet
            else -> ColorCyan
        }
    } else {
        when (preferences.accentColor.uppercase()) {
            "AMBER" -> ColorAmberLight
            "RED" -> ColorRedLight
            "VIOLET" -> ColorVioletLight
            else -> ColorCyanLight
        }
    }

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val callsGranted = permissions[android.Manifest.permission.ANSWER_PHONE_CALLS] == true
        if (callsGranted) {
            showOverlayDialog = true
        } else {
            showOverlayDialog = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GAMEGUARD OPTIMIZER",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = accentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = "CONEXIÓN ESTABLE & SIN INTERRUPCIONES",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isDarkMode) ColorAmber else ColorAmberLight,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            // Let's introduce Guardy on Onboarding Screen!
            GuardyMascot(
                pingMs = 0,
                pingStatus = "ONBOARDING",
                isGameModeActive = true,
                isDarkMode = isDarkMode
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "[ BIENVENIDO RECLUTA ]",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "GameGuard te permite reducir tu ping en juegos y evitar llamadas que interrumpen tu partida. Para lograrlo, necesitamos configurar algunos permisos vitales en tu sistema.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )

                    HorizontalDivider(color = accentColor.copy(alpha = 0.15f))

                    Text(
                        text = "1. Llamadas y Estado de Red (DND)\n" +
                               "2. Superposición de Aplicaciones (HUD Burbuja)\n" +
                               "3. Estadísticas de Uso (Optimizador de RAM)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    showCallsDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "INICIAR CONFIGURACIÓN",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.Black else Color.White
                )
            }
        }
    }

    if (showCallsDialog) {
        PermissionExplanationDialog(
            title = "PERMISO DE LLAMADAS (DND)",
            explanation = "Necesitamos acceso para leer el estado del teléfono y responder llamadas para poder desviarlas silenciosamente mientras estás en partida.",
            onConfirm = {
                showCallsDialog = false
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ANSWER_PHONE_CALLS,
                        android.Manifest.permission.READ_PHONE_STATE
                    )
                )
            },
            onDismiss = {
                showCallsDialog = false
                showOverlayDialog = true
            }
        )
    }

    if (showOverlayDialog) {
        PermissionExplanationDialog(
            title = "PANTALLA FLOTANTE (HUD)",
            explanation = "Permite a GameGuard renderizar la burbuja de ping en tiempo real encima de tu juego favorito sin interrumpir tu vista.",
            onConfirm = {
                showOverlayDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
                showUsageStatsDialog = true
            },
            onDismiss = {
                showOverlayDialog = false
                showUsageStatsDialog = true
            }
        )
    }

    if (showUsageStatsDialog) {
        PermissionExplanationDialog(
            title = "ACCESO DE USO (RAM)",
            explanation = "Permite identificar qué aplicaciones en segundo plano están ralentizando el teléfono para poder cerrarlas y liberar RAM para Free Fire.",
            onConfirm = {
                showUsageStatsDialog = false
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
                preferences.isOnboardingCompleted = true
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
            onDismiss = {
                showUsageStatsDialog = false
                preferences.isOnboardingCompleted = true
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        )
    }
}
