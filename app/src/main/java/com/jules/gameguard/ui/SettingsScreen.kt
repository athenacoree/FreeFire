package com.jules.gameguard.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.service.ConnectionMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    var blockCallsEnabled by remember { mutableStateOf(preferences.isModoJuegoActivo) }
    var serverIp by remember { mutableStateOf(preferences.configurableServer) }
    var isConnectionMonitorRunning by remember {
        mutableStateOf(ConnectionMonitorService.isRunning.value)
    }

    LaunchedEffect(Unit) {
        ConnectionMonitorService.isRunning.collect { running ->
            isConnectionMonitorRunning = running
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CONFIGURACIÓN",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Text(
                            text = "◀",
                            color = ColorCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Call Blocking Card (Glassmorphism)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "BLOQUEO DE LLAMADAS (DND)",
                        color = ColorCyan,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rechazar llamadas entrantes",
                                color = Color.White,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Silencia y rechaza llamadas automáticamente mientras juegas",
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = blockCallsEnabled,
                            onCheckedChange = { isChecked ->
                                blockCallsEnabled = isChecked
                                preferences.isModoJuegoActivo = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ColorCyan,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            // Section 2: Ping Monitor Configuration Card (Glassmorphism)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "MONITOR DE CONEXIÓN",
                        color = ColorCyan,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = {
                            serverIp = it
                            preferences.configurableServer = it
                        },
                        label = {
                            Text(
                                "SERVIDOR DE PING (IP / DOMINIO)",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCyan.copy(alpha = 0.8f)
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorCyan,
                            unfocusedBorderColor = ColorCyan.copy(alpha = 0.4f),
                            focusedLabelColor = ColorCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = ColorCyan
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Iniciar Servicio de Monitoreo",
                                color = Color.White,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ejecuta el servicio en segundo plano con la burbuja de ping",
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = isConnectionMonitorRunning,
                            onCheckedChange = { isChecked ->
                                val serviceIntent = Intent(context, ConnectionMonitorService::class.java)
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } else {
                                    context.stopService(serviceIntent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ColorCyan,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    }
}
