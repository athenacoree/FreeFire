package com.jules.gameguard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun AboutScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val accentColor = when (preferences.accentColor.uppercase()) {
        "AMBER" -> ColorAmber
        "RED" -> ColorRed
        "VIOLET" -> Color(0xFF, 0x00, 0xD4, 0xFF)
        else -> ColorCyan
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ACERCA DE",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Text(
                            text = "◀",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .border(2.dp, accentColor, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡",
                    fontSize = 36.sp
                )
            }

            Text(
                text = "GAMEGUARD OPTIMIZER",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Versión 2.0.0 (HUD Edition)",
                fontFamily = RajdhaniFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = accentColor.copy(alpha = 0.15f))

            // Changelog
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "[ REGISTRO DE CAMBIOS ]",
                        color = ColorAmber,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    val changelogLines = listOf(
                        "• Integración completa con Glance widgets para pantalla de inicio.",
                        "• Monitoreo y detección automática de Free Fire para activar Game Mode.",
                        "• Gráfico de ping en tiempo real en los últimos 60 segundos.",
                        "• One-Tap Boost para liberar RAM en un solo toque.",
                        "• Soporte para múltiples servidores de ping y lista blanca de apps.",
                        "• Lista de contactos permitidos durante el Modo Juego.",
                        "• Soporte completo de modo claro/oscuro y selector de color de acento.",
                        "• Backup y restauración de historial en JSON y exportación CSV."
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@jules-gameguard.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Reporte de Bug - GameGuard v2.0")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, ColorRed, RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = ColorRed.copy(alpha = 0.1f), contentColor = ColorRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "REPORTAR ERROR / COMPARTIR FEEDBACK",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
