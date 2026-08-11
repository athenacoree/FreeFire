package com.jules.gameguard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.GameGuardPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Acerca de",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = accentColor
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    .border(1.5.dp, accentColor, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(42.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GameGuard Optimizer",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Versión 2.5 (Pro Glass Edition)",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = accentColor.copy(alpha = 0.15f))

            // Companion Guardy
            GuardyMascot(
                pingMs = 0,
                pingStatus = "ACERCA",
                isGameModeActive = true,
                isDarkMode = isDarkMode
            )

            // Changelog Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Novedades y Registro de Cambios",
                        color = if (isDarkMode) ColorAmber else ColorAmberLight,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val changelogLines = listOf(
                        "• Guardy (🦊): Mascota interactiva con recomendaciones en tiempo real.",
                        "• Interfaz iOS Glassmorphism en Modos Claro y Oscuro.",
                        "• Segmented Controls y navegación estilo iOS.",
                        "• Detección y optimización automática para Free Fire y otros juegos.",
                        "• Gráfico de ping dinámico en tiempo real.",
                        "• One-Tap Boost de memoria RAM.",
                        "• Filtros de llamadas entrantes y lista blanca de contactos.",
                        "• Respaldo y exportación de datos en formato JSON y CSV."
                    )

                    changelogLines.forEach { line ->
                        Text(
                            text = line,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@jules-gameguard.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback GameGuard v2.5")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorRed)
            ) {
                Text(
                    text = "Reportar Error / Feedback",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
