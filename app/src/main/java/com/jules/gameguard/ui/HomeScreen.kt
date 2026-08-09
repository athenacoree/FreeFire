package com.jules.gameguard.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.GameGuardPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    var isModoJuegoActivo by remember { mutableStateOf(preferences.isModoJuegoActivo) }

    // Role Manager request launcher
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Callback can check role status again
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val answerGranted = permissions[android.Manifest.permission.ANSWER_PHONE_CALLS] == true
        val stateGranted = permissions[android.Manifest.permission.READ_PHONE_STATE] == true
        if (answerGranted && stateGranted) {
            // Permissions granted, now request role if needed
            requestCallScreeningRole(context, roleLauncher)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GameGuard Home") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Text("Config", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = {
                        val newValue = !isModoJuegoActivo
                        preferences.isModoJuegoActivo = newValue
                        isModoJuegoActivo = newValue

                        if (newValue) {
                            // When enabling game mode, request role & permissions if not granted
                            val hasAnswer = context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasState = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (!hasAnswer || !hasState) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ANSWER_PHONE_CALLS,
                                        android.Manifest.permission.READ_PHONE_STATE
                                    )
                                )
                            } else {
                                requestCallScreeningRole(context, roleLauncher)
                            }
                        }
                    },
                    modifier = Modifier.size(220.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isModoJuegoActivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isModoJuegoActivo) "Desactivar\nModo Juego" else "Activar\nModo Juego",
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    )
                }

                Text(
                    text = if (isModoJuegoActivo) "Modo Juego ACTIVO" else "Modo Juego INACTIVO",
                    style = MaterialTheme.typography.titleMedium
                )
            }
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
