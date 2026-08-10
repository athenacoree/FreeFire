package com.jules.gameguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.BlockedCall
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.data.PingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val preferences = remember { GameGuardPreferences(context.applicationContext) }
    val accentColorStr = preferences.accentColor
    val accentColor = when (accentColorStr.uppercase()) {
        "AMBER" -> ColorAmber
        "RED" -> ColorRed
        "VIOLET" -> Color(0xFF, 0x00, 0xD4, 0xFF)
        else -> ColorCyan
    }

    // Database flow setup
    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val recordsFlow = remember { db.pingRecordDao().getAllRecordsFlow() }
    val blockedCallsFlow = remember { db.blockedCallDao().getAllBlockedCallsFlow() }

    val blockedCalls by blockedCallsFlow.collectAsState(initial = emptyList())

    // JSON import/restore dialog state
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Map records into hourly averages for the last 7 days
    val hourlyAverages by recordsFlow.map { records ->
        val sumArray = DoubleArray(24)
        val countArray = IntArray(24)

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        val last7DaysRecords = records.filter { it.timestamp >= sevenDaysAgo }

        for (record in last7DaysRecords) {
            cal.timeInMillis = record.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
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

    // CSV Export Handler
    val exportCSV = {
        coroutineScope.launch(Dispatchers.IO) {
            val records = db.pingRecordDao().getAllRecords()
            if (records.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No hay registros para exportar", Toast.LENGTH_SHORT).show()
                }
            } else {
                val csvBuilder = StringBuilder()
                csvBuilder.append("ID,Timestamp,PingMs,PacketLossPercent,Status\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                for (record in records) {
                    val dateStr = sdf.format(Date(record.timestamp))
                    csvBuilder.append("${record.id},\"$dateStr\",${record.pingMs},${record.packetLossPercent},\"${record.status}\"\n")
                }

                // Write to cache file
                try {
                    val sharedFolder = File(context.cacheDir, "shared")
                    sharedFolder.mkdirs()
                    val csvFile = File(sharedFolder, "ping_history.csv")
                    val fos = FileOutputStream(csvFile)
                    fos.write(csvBuilder.toString().toByteArray())
                    fos.close()

                    val fileUri = FileProvider.getUriForFile(context, "com.jules.gameguard.fileprovider", csvFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/comma-separated-values"
                        putExtra(Intent.EXTRA_SUBJECT, "Historial de Ping GameGuard CSV")
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent.createChooser(shareIntent, "Exportar CSV"))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error exportando CSV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Share Graph as Image Handler
    val shareGraphAsImage = {
        coroutineScope.launch(Dispatchers.IO) {
            val records = db.pingRecordDao().getAllRecords()
            if (records.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No hay suficientes datos para generar imagen", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    val width = 800
                    val height = 600
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint()

                    // Background
                    paint.color = android.graphics.Color.parseColor("#0A0A0F")
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                    // Header
                    paint.color = android.graphics.Color.parseColor("#00F0FF")
                    paint.textSize = 34f
                    paint.isAntiAlias = true
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    canvas.drawText("🛡 GAMEGUARD HUD ANALYTICS", 50f, 70f, paint)

                    // Divider
                    paint.color = android.graphics.Color.parseColor("#1F3860")
                    canvas.drawLine(50f, 95f, 750f, 95f, paint)

                    // Subtitle / Stats
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 20f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                    canvas.drawText("Total de mediciones: ${records.size}", 50f, 140f, paint)

                    val avgPing = records.map { it.pingMs }.average().toInt()
                    val maxPing = records.maxOfOrNull { it.pingMs } ?: 0L
                    val avgLoss = records.map { it.packetLossPercent }.average().toInt()

                    canvas.drawText("Latencia Promedio: $avgPing ms", 50f, 180f, paint)
                    canvas.drawText("Pico de Latencia: $maxPing ms", 50f, 220f, paint)
                    canvas.drawText("Pérdida de Paquetes Promedio: $avgLoss%", 50f, 260f, paint)

                    // Draw a simple decorative bar chart
                    paint.color = android.graphics.Color.parseColor("#00F0FF")
                    canvas.drawRect(50f, 320f, 750f, 550f, paint) // border
                    paint.color = android.graphics.Color.parseColor("#12121A")
                    canvas.drawRect(53f, 323f, 747f, 547f, paint) // inner content

                    // Draw vertical bars for the last 15 entries
                    val lastRecords = records.take(15).reversed()
                    if (lastRecords.isNotEmpty()) {
                        val barCount = lastRecords.size
                        val startX = 80f
                        val endX = 720f
                        val chartHeight = 180f
                        val startY = 520f
                        val barWidth = (endX - startX) / barCount - 10f

                        val maxRecordPing = maxOf(lastRecords.maxOf { it.pingMs }.toFloat(), 100f)

                        for (i in lastRecords.indices) {
                            val r = lastRecords[i]
                            val barHeight = (r.pingMs.toFloat() / maxRecordPing) * chartHeight
                            paint.color = if (r.status == "Mala") {
                                android.graphics.Color.parseColor("#FF3860")
                            } else if (r.status == "Regular") {
                                android.graphics.Color.parseColor("#FFB800")
                            } else {
                                android.graphics.Color.parseColor("#00F0FF")
                            }

                            val left = startX + i * (barWidth + 10f)
                            val top = startY - barHeight
                            val right = left + barWidth
                            val bottom = startY

                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                    }

                    // Save bitmap to cache
                    val sharedFolder = File(context.cacheDir, "shared")
                    sharedFolder.mkdirs()
                    val imageFile = File(sharedFolder, "ping_analytics.png")
                    val fos = FileOutputStream(imageFile)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()

                    val fileUri = FileProvider.getUriForFile(context, "com.jules.gameguard.fileprovider", imageFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_SUBJECT, "Reporte de Calidad de Red GameGuard")
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Gráfica"))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al generar imagen: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // JSON Backup Handler
    val exportJSONBackup = {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val backupObj = JSONObject()

                // 1. Prefs
                val prefsObj = JSONObject().apply {
                    put("isDarkMode", preferences.isDarkMode)
                    put("accentColor", preferences.accentColor)
                    put("pingAlertThresholdMs", preferences.pingAlertThresholdMs)
                    put("isPingVibrateAlertEnabled", preferences.isPingVibrateAlertEnabled)
                    put("isPingSoundAlertEnabled", preferences.isPingSoundAlertEnabled)
                    put("autoRamCleanIntervalMins", preferences.autoRamCleanIntervalMins)
                    put("totalRamCleanedMb", preferences.totalRamCleanedMb)
                }
                backupObj.put("preferences", prefsObj)

                // 2. Ping records
                val pingArr = JSONArray()
                val records = db.pingRecordDao().getAllRecords()
                for (r in records) {
                    val o = JSONObject().apply {
                        put("timestamp", r.timestamp)
                        put("pingMs", r.pingMs)
                        put("packetLossPercent", r.packetLossPercent)
                        put("status", r.status)
                    }
                    pingArr.put(o)
                }
                backupObj.put("pingRecords", pingArr)

                // 3. Blocked calls
                val blockedArr = JSONArray()
                withContext(Dispatchers.Main) {
                    for (c in blockedCalls) {
                        val o = JSONObject().apply {
                            put("phoneNumber", c.phoneNumber)
                            put("timestamp", c.timestamp)
                        }
                        blockedArr.put(o)
                    }
                }
                backupObj.put("blockedCalls", blockedArr)

                val jsonStr = backupObj.toString(4)

                withContext(Dispatchers.Main) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("GameGuard Backup", jsonStr)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "¡Copia de seguridad copiada al portapapeles en formato JSON!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en copia de seguridad: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // JSON Restore Handler
    val restoreJSONBackup = { jsonStr: String ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val backupObj = JSONObject(jsonStr)

                // 1. Preferences
                if (backupObj.has("preferences")) {
                    val prefsObj = backupObj.getJSONObject("preferences")
                    withContext(Dispatchers.Main) {
                        if (prefsObj.has("isDarkMode")) preferences.isDarkMode = prefsObj.getBoolean("isDarkMode")
                        if (prefsObj.has("accentColor")) preferences.accentColor = prefsObj.getString("accentColor")
                        if (prefsObj.has("pingAlertThresholdMs")) preferences.pingAlertThresholdMs = prefsObj.getInt("pingAlertThresholdMs")
                        if (prefsObj.has("isPingVibrateAlertEnabled")) preferences.isPingVibrateAlertEnabled = prefsObj.getBoolean("isPingVibrateAlertEnabled")
                        if (prefsObj.has("isPingSoundAlertEnabled")) preferences.isPingSoundAlertEnabled = prefsObj.getBoolean("isPingSoundAlertEnabled")
                        if (prefsObj.has("autoRamCleanIntervalMins")) preferences.autoRamCleanIntervalMins = prefsObj.getInt("autoRamCleanIntervalMins")
                        if (prefsObj.has("totalRamCleanedMb")) preferences.totalRamCleanedMb = prefsObj.getLong("totalRamCleanedMb")
                    }
                }

                // 2. Ping records
                if (backupObj.has("pingRecords")) {
                    val pingArr = backupObj.getJSONArray("pingRecords")
                    for (i in 0 until pingArr.length()) {
                        val o = pingArr.getJSONObject(i)
                        db.pingRecordDao().insertRecord(
                            PingRecord(
                                timestamp = o.getLong("timestamp"),
                                pingMs = o.getLong("pingMs"),
                                packetLossPercent = o.getInt("packetLossPercent"),
                                status = o.getString("status")
                            )
                        )
                    }
                }

                // 3. Blocked calls
                if (backupObj.has("blockedCalls")) {
                    val blockedArr = backupObj.getJSONArray("blockedCalls")
                    for (i in 0 until blockedArr.length()) {
                        val o = blockedArr.getJSONObject(i)
                        db.blockedCallDao().insert(
                            BlockedCall(
                                phoneNumber = o.getString("phoneNumber"),
                                timestamp = o.getLong("timestamp")
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "¡Copia de seguridad restaurada con éxito!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al restaurar JSON: Formato Inválido", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HISTORIAL Y ANALÍTICAS",
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
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "[ CONTROL DE RENDIMIENTO ]",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Revisa la estabilidad histórica de tu red, el registro de llamadas bloqueadas durante tus partidas y gestiona copias de seguridad de tus analíticas de optimización.",
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
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LATENCIA PROMEDIO POR HORA (7D)",
                        color = Color.White,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    PingHourlyChart(hourlyAverages = hourlyAverages)

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
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "[ FRANGAS DE HORAS CRÍTICAS ]",
                        color = ColorRed,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    if (worstHours.isNotEmpty()) {
                        Text(
                            text = "Evita jugar partidas competitivas en estas franjas de horas críticas detectadas:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp
                        )

                        worstHours.forEachIndexed { index, hourAvg ->
                            val formattedHour = String.format("%02d:00 - %02d:59", hourAvg.hour, hourAvg.hour)
                            val statusColor = when {
                                hourAvg.averagePingMs >= 150 -> ColorRed
                                hourAvg.averagePingMs >= 80 -> ColorAmber
                                else -> accentColor
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
                            text = "No hay suficientes datos recolectados. Activa el monitor para analizar tu red.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Blocked Calls list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "[ REGISTRO DE LLAMADAS BLOQUEADAS ]",
                        color = ColorAmber,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    if (blockedCalls.isNotEmpty()) {
                        val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
                        blockedCalls.forEach { call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(call.phoneNumber, color = Color.White, fontFamily = RajdhaniFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(sdf.format(Date(call.timestamp)), color = Color.White.copy(alpha = 0.5f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                                }
                                Text(
                                    text = "RECHAZADA 🚫",
                                    color = ColorRed,
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No has recibido llamadas molestas durante el Modo Juego recientemente.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Utilities & Action Cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "[ UTILIDADES DE DATOS HUD ]",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportCSV() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("EXPORTAR CSV", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { shareGraphAsImage() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("COMPARTIR IMG", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportJSONBackup() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAmber.copy(alpha = 0.15f), contentColor = ColorAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ColorAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("COPIA JSON", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAmber.copy(alpha = 0.15f), contentColor = ColorAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ColorAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESTAURAR JSON", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(ColorGlassBg, shape = RoundedCornerShape(16.dp))
                    .border(2.dp, ColorAmber, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "RESTAURAR COPIA JSON",
                        color = ColorAmber,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("Pega el JSON de backup aquí...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showRestoreDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CANCELAR", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (restoreJsonText.isNotEmpty()) {
                                    restoreJSONBackup(restoreJsonText)
                                    restoreJsonText = ""
                                    showRestoreDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAmber, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESTAURAR", fontSize = 11.sp, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                        }
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

            val barCount = 24
            val spacing = 4.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            for (i in 0 until barCount) {
                val hourData = hourlyAverages.getOrNull(i) ?: HourPingAverage(i, 0.0, 0)
                val pingVal = hourData.averagePingMs.toFloat()

                val barColor = when {
                    pingVal == 0f -> Color.White.copy(alpha = 0.05f)
                    pingVal >= 150f -> ColorRed
                    pingVal >= 80f -> ColorAmber
                    else -> ColorCyan
                }

                val ratio = if (pingVal > 0f) (pingVal / maxPing) else 0.02f
                val barHeight = height * ratio

                val x = i * (barWidth + spacing)
                val y = height - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

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
