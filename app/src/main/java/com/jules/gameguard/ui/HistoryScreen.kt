package com.jules.gameguard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.PingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Database flow setup
    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val recordsFlow = remember { db.pingRecordDao().getAllRecordsFlow() }

    // Map records into hourly averages for the last 7 days
    val hourlyAverages by recordsFlow.map { records ->
        // Generate list for 24 hours, prefilled with 0.0 and count 0
        val sumArray = DoubleArray(24)
        val countArray = IntArray(24)

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        // Filter last 7 days records
        val last7DaysRecords = records.filter { it.timestamp >= sevenDaysAgo }

        for (record in last7DaysRecords) {
            cal.timeInMillis = record.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY) // 0 to 23
            if (hour in 0..23) {
                sumArray[hour] += record.pingMs.toDouble()
                countArray[hour]++
            }
        }

        List(24) { hour ->
            val count = countArray[hour]
            val avg = if (count > 0) sumArray[hour] / count else 0.0
            HourPingAverage(hour = hour, averagePingMs = avg, recordCount = count)
        }
    }.collectAsState(initial = List(24) { HourPingAverage(it, 0.0, 0) })

    // Find peak/worst hours
    val worstHours = remember(hourlyAverages) {
        hourlyAverages
            .filter { it.recordCount > 0 && it.averagePingMs > 0 }
            .sortedByDescending { it.averagePingMs }
            .take(3)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HISTORIAL DE PING",
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
                .verticalScroll(scrollState)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen header/explanation card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "[ RENDIMIENTOÚLTIMOS 7 DÍAS ]",
                        color = ColorCyan,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Monitorea la estabilidad de tu red organizada por franjas horarias. Identifica las horas con mayor latencia para evitar partidas competitivas en esos rangos.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Graphic Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GRÁFICO DE LATENCIA POR HORA",
                        color = Color.White,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Draw the custom gaming canvas chart
                    PingHourlyChart(hourlyAverages = hourlyAverages)

                    // Legend indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = ColorCyan, text = "Buena (<80 ms)")
                        LegendItem(color = ColorAmber, text = "Regular (80-150)")
                        LegendItem(color = ColorRed, text = "Mala (>150 ms)")
                    }
                }
            }

            // Analysis / Worst hours section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "[ PEOR HORARIO RELEVADO ]",
                        color = ColorRed,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    if (worstHours.isNotEmpty()) {
                        Text(
                            text = "Evita jugar en estas horas para no sufrir de lag constante:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp
                        )

                        worstHours.forEachIndexed { index, hourAvg ->
                            val formattedHour = String.format("%02d:00 - %02d:59", hourAvg.hour, hourAvg.hour)
                            val statusColor = when {
                                hourAvg.averagePingMs >= 150 -> ColorRed
                                hourAvg.averagePingMs >= 80 -> ColorAmber
                                else -> ColorCyan
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = ColorRed,
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formattedHour,
                                        color = Color.White,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${hourAvg.averagePingMs.toInt()} ms avg",
                                    color = statusColor,
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No hay suficientes datos de ping recolectados en los últimos 7 días. Activa el Modo Juego para registrar latencias de tu conexión.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PingHourlyChart(
    hourlyAverages: List<HourPingAverage>,
    modifier: Modifier = Modifier
) {
    // Max ping value for scaling, ensure it's at least 150 to scale nicely
    val maxPing = remember(hourlyAverages) {
        val maxFromData = hourlyAverages.maxOfOrNull { it.averagePingMs } ?: 0.0
        maxOf(maxFromData, 200.0).toFloat()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw horizontal threshold guidelines
            val levels = listOf(80f, 150f)
            levels.forEach { level ->
                val ratio = level / maxPing
                val y = height - (height * ratio)
                if (y in 0f..height) {
                    val linePathColor = if (level == 80f) ColorAmber.copy(alpha = 0.2f) else ColorRed.copy(alpha = 0.2f)
                    drawLine(
                        color = linePathColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw 24 hour bars
            val barCount = 24
            val spacing = 4.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            for (i in 0 until barCount) {
                val hourData = hourlyAverages.getOrNull(i) ?: HourPingAverage(i, 0.0, 0)
                val pingVal = hourData.averagePingMs.toFloat()

                // Calculate color depending on ping severity
                val barColor = when {
                    pingVal == 0f -> Color.White.copy(alpha = 0.05f) // unused/no data placeholder
                    pingVal >= 150f -> ColorRed
                    pingVal >= 80f -> ColorAmber
                    else -> ColorCyan
                }

                // Calculate bar height based on data or minimum placeholder if records count is 0
                val ratio = if (pingVal > 0f) (pingVal / maxPing) else 0.02f
                val barHeight = height * ratio

                val x = i * (barWidth + spacing)
                val y = height - barHeight

                // Draw solid bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Optional grid accent at top of the bar
                if (pingVal > 0f) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, 2.dp.toPx())
                    )
                }
            }
        }
    }

    // Custom scale indicator labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("00:00", color = Color.White.copy(alpha = 0.4f), fontFamily = OrbitronFontFamily, fontSize = 9.sp)
        Text("06:00", color = Color.White.copy(alpha = 0.4f), fontFamily = OrbitronFontFamily, fontSize = 9.sp)
        Text("12:00", color = Color.White.copy(alpha = 0.4f), fontFamily = OrbitronFontFamily, fontSize = 9.sp)
        Text("18:00", color = Color.White.copy(alpha = 0.4f), fontFamily = OrbitronFontFamily, fontSize = 9.sp)
        Text("23:59", color = Color.White.copy(alpha = 0.4f), fontFamily = OrbitronFontFamily, fontSize = 9.sp)
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontFamily = RajdhaniFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

data class HourPingAverage(
    val hour: Int,
    val averagePingMs: Double,
    val recordCount: Int
)
