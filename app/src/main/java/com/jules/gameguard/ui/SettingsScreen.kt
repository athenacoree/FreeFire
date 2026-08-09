package com.jules.gameguard.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    // Keep service running state updated
    LaunchedEffect(Unit) {
        ConnectionMonitorService.isRunning.collect { running ->
            isConnectionMonitorRunning = running
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("Atrás", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Bloqueo de Llamadas", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Activar Bloqueo de Llamadas")
                Switch(
                    checked = blockCallsEnabled,
                    onCheckedChange = { isChecked ->
                        blockCallsEnabled = isChecked
                        preferences.isModoJuegoActivo = isChecked
                    }
                )
            }

            HorizontalDivider()

            Text("Monitor de Conexión (Ping)", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = serverIp,
                onValueChange = {
                    serverIp = it
                    preferences.configurableServer = it
                },
                label = { Text("Servidor de Ping (IP o Dominio)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Iniciar Servicio de Monitor")
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
                    }
                )
            }
        }
    }
}
