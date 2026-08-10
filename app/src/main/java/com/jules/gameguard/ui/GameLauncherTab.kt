package com.jules.gameguard.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.service.ConnectionMonitorService

data class SimpleAppInfo(
    val packageName: String,
    val label: String
)

@Composable
fun GameLauncherTab(preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val pm = context.packageManager

    var installedApps by remember { mutableStateOf<List<SimpleAppInfo>>(emptyList()) }
    var selectedGamePkg by remember { mutableStateOf(preferences.exclusiveAppPackage) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        val apps = mutableListOf<SimpleAppInfo>()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in packages) {
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                           (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            if (!isSystem || launchIntent != null) {
                val label = pm.getApplicationLabel(appInfo).toString()
                apps.add(SimpleAppInfo(appInfo.packageName, label))
            }
        }
        installedApps = apps.sortedBy { it.label }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App selection header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(isDarkMode = isDarkMode)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "[ LANZADOR EXCLUSIVO iOS ]",
                    color = accentColor,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Selecciona un juego para otorgarle CONEXIÓN EXCLUSIVA. Al activar el Modo Juego, se bloqueará la conexión a internet de todas las demás apps excepto el juego seleccionado.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Active Game Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(isDarkMode = isDarkMode)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "JUEGO CONEXIÓN EXCLUSIVA",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedGamePkg.isNotEmpty()) {
                    var appLabel = selectedGamePkg
                    try {
                        val info = pm.getApplicationInfo(selectedGamePkg, 0)
                        appLabel = pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {}

                    Text(
                        text = appLabel,
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedGamePkg,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "NINGÚN JUEGO SELECCIONADO",
                        color = ColorRed,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAppPickerDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDarkMode) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SELECCIONAR JUEGO / APP",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Fast Game Launcher Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(isDarkMode = isDarkMode)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LANZAMIENTO RÁPIDO Y BOOST",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Lanza tu juego directamente con un boost de RAM automático para optimizar la latencia desde el primer segundo.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (selectedGamePkg.isNotEmpty()) {
                            val launchIntent = pm.getLaunchIntentForPackage(selectedGamePkg)
                            if (launchIntent != null) {
                                val serviceIntent = Intent(context, ConnectionMonitorService::class.java)
                                context.startService(serviceIntent)
                                preferences.isModoJuegoActivo = true

                                Toast.makeText(context, "¡Lanzando juego con boost ultra!", Toast.LENGTH_SHORT).show()
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "No se puede abrir este juego", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Por favor, selecciona un juego primero.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedGamePkg.isNotEmpty()
                ) {
                    Text(
                        text = "LANZAR JUEGO AHORA 🚀",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedGamePkg.isNotEmpty()) {
                            if (isDarkMode) Color.Black else Color.White
                        } else {
                            Color.Unspecified
                        }
                    )
                }
            }
        }
    }

    if (showAppPickerDialog) {
        Dialog(onDismissRequest = { showAppPickerDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SELECCIONAR JUEGO",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(installedApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedGamePkg = app.packageName
                                        preferences.exclusiveAppPackage = app.packageName
                                        showAppPickerDialog = false
                                        Toast.makeText(context, "Juego configurado: ${app.label}", Toast.LENGTH_SHORT).show()
                                    }
                                    .background(
                                        if (isDarkMode) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = app.label,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showAppPickerDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CERRAR",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
