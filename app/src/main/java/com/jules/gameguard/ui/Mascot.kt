package com.jules.gameguard.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jules.gameguard.ai.AiInferenceResult
import com.jules.gameguard.ai.LagRiskLevel
import kotlin.random.Random

/**
 * Guardy - The interactive cyber guardian mascot for GameGuard.
 * Displays state-dependent expressions and useful gamer tips on click.
 */
@Composable
fun GuardyMascot(
    pingMs: Long = 0,
    pingStatus: String = "INACTIVO",
    isGameModeActive: Boolean = false,
    lastClosedApps: String = "",
    aiResult: AiInferenceResult? = null,
    isDarkMode: Boolean = true
) {
    // Interactive state
    var clickedCount by remember { mutableStateOf(0) }
    var currentTipIndex by remember { mutableStateOf(-1) }

    // Animation for Mascot scale (bouncing effect)
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_bounce")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Dynamic rotation animation for ears/digital aura
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Cute game optimization tips
    val guardyTips = listOf(
        "Consejo de Guardy: Juega cerca de tu router o conéctate a WiFi de 5GHz para una estabilidad insuperable.",
        "Consejo de Guardy: ¿Sabías que el DNS de Cloudflare (1.1.1.1) reduce hasta 15ms de ruteo en Latinoamérica?",
        "Consejo de Guardy: Activa la 'Conexión Exclusiva' para congelar el internet de apps en segundo plano.",
        "Consejo de Guardy: He guardado tus analíticas de red. ¡Revisa la pestaña de Estadísticas para ver tus mejores horas!",
        "Consejo de Guardy: Haz un 'One-Tap Boost' antes de cada partida para limpiar la caché de RAM y evitar tirones.",
        "Consejo de Guardy: Agrega a tus familiares a la lista de contactos permitidos para no perder llamadas urgentes."
    )

    // Determine state
    val face: String
    val speech: String
    val accentColor = if (isDarkMode) ColorCyan else ColorCyanLight
    val bubbleBg = if (isDarkMode) Color(0xFF, 0x1C, 0x1C, 0x1E) else Color(0xFF, 0xFF, 0xFF)
    val textColor = if (isDarkMode) Color.White else Color.Black

    if (currentTipIndex != -1) {
        face = "🦊💡"
        speech = guardyTips[currentTipIndex % guardyTips.size]
    } else if (aiResult != null && isGameModeActive) {
        when (aiResult.lagRisk) {
            LagRiskLevel.CRITICAL -> {
                face = "🦊🤖"
                speech = "IA GameGuard (Score: ${aiResult.gamingPerformanceScore}): ${aiResult.recommendation.description}"
            }
            LagRiskLevel.HIGH -> {
                face = "🦊⚡"
                speech = "IA GameGuard (${aiResult.aiConfidencePercent}% confianza): Detecté riesgo moderado de lag. ${aiResult.recommendation.actionName} activado."
            }
            LagRiskLevel.MODERATE -> {
                face = "🦊✨"
                speech = "IA GameGuard: Conexión ${aiResult.estimatedPingStability}. Score de rendimiento: ${aiResult.gamingPerformanceScore}/100."
            }
            LagRiskLevel.LOW -> {
                face = "🦊🚀"
                speech = "IA GameGuard: ¡Sistema en estado óptimo (${aiResult.gamingPerformanceScore}/100)! Red estable y procesador libre."
            }
        }
    } else if (!isGameModeActive) {
        face = "🦊💤"
        speech = "¡Zzz... Activa el Modo Juego para que la IA despierte y optimice tu experiencia de juego!"
    } else {
        when (pingStatus.uppercase()) {
            "MALA" -> {
                face = "🦊🔥"
                speech = "¡Alerta roja! El ping está en $pingMs ms. He activado escudos de red máximos para mitigar el lag."
            }
            "REGULAR" -> {
                face = "🦊⚡"
                speech = "Detecto ruidos en la conexión ($pingMs ms). ¡Despreocúpate, sigo calibrando el canal de juego!"
            }
            "BUENA" -> {
                face = "🦊✨"
                speech = "¡Conexión ultra optimizada! Ping excelente ($pingMs ms). Todo bajo control, ¡ve por esa victoria!"
            }
            else -> {
                face = "🦊👍"
                speech = "¡Hola! Estoy listo para optimizar tu red con IA y bloquear interrupciones. ¡Haz clic en mí para un consejo!"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable {
                clickedCount++
                currentTipIndex = Random.nextInt(guardyTips.size)
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.01f))
                    } else {
                        listOf(Color.Black.copy(alpha = 0.02f), Color.Black.copy(alpha = 0.005f))
                    }
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 0.5.dp,
                color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Guardy Visual Figure
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(idleScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glowing border ring
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = face,
                    fontSize = 28.sp,
                    modifier = Modifier.scale(if (rotationAngle > 0) 1.05f else 1.0f)
                )
            }
        }

        // Speech Bubble
        Box(
            modifier = Modifier
                .weight(1f)
                .background(bubbleBg, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                .border(
                    width = 0.5.dp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GUARDY",
                    color = accentColor,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = speech,
                    color = textColor,
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
