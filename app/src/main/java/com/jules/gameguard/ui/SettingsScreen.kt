package com.jules.gameguard.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jules.gameguard.data.*
import com.jules.gameguard.service.ConnectionMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, preferences: GameGuardPreferences) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val db = remember { AppDatabase.getDatabase(context.applicationContext) }

    // Collect Room DB lists
    val customServers by db.customServerDao().getAllCustomServersFlow().collectAsState(initial = emptyList())
    val whitelistedApps by db.whitelistedAppDao().getAllWhitelistedAppsFlow().collectAsState(initial = emptyList())
    val allowedContacts by db.allowedContactDao().getAllAllowedContactsFlow().collectAsState(initial = emptyList())

    // Settings state
    var blockCallsEnabled by remember { mutableStateOf(preferences.isModoJuegoActivo) }
    var activeServerIp by remember { mutableStateOf(preferences.configurableServer) }
    var isConnectionMonitorRunning by remember { mutableStateOf(ConnectionMonitorService.isRunning.value) }

    var isDarkMode by remember { mutableStateOf(preferences.isDarkMode) }
    var selectedAccentColor by remember { mutableStateOf(preferences.accentColor) }
    var pingThreshold by remember { mutableStateOf(preferences.pingAlertThresholdMs.toFloat()) }
    var isPingVibrate by remember { mutableStateOf(preferences.isPingVibrateAlertEnabled) }
    var isPingSound by remember { mutableStateOf(preferences.isPingSoundAlertEnabled) }
    var autoCleanInterval by remember { mutableStateOf(preferences.autoRamCleanIntervalMins) }

    // New premium features states
    var isExclusiveConnEnabled by remember { mutableStateOf(preferences.isExclusiveConnectionEnabled) }
    var selectedProfile by remember { mutableStateOf(preferences.activePerformanceProfile) }
    var isDnsOptEnabled by remember { mutableStateOf(preferences.isDnsOptimizationEnabled) }
    var selectedDnsProvider by remember { mutableStateOf(preferences.dnsProvider) }

    // Input text fields
    var newServerName by remember { mutableStateOf("") }
    var newServerIp by remember { mutableStateOf("") }
    var newAppName by remember { mutableStateOf("") }
    var newAppPkg by remember { mutableStateOf("") }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    // Dynamic accent color
    val accentColor = if (isDarkMode) {
        when (selectedAccentColor.uppercase()) {
            "AMBER" -> ColorAmber
            "RED" -> ColorRed
            "VIOLET" -> ColorViolet
            else -> ColorCyan
        }
    } else {
        when (selectedAccentColor.uppercase()) {
            "AMBER" -> ColorAmberLight
            "RED" -> ColorRedLight
            "VIOLET" -> ColorVioletLight
            else -> ColorCyanLight
        }
    }

    LaunchedEffect(Unit) {
        ConnectionMonitorService.isRunning.collect { running ->
            isConnectionMonitorRunning = running
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
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
                actions = {
                    IconButton(
                        onClick = { navController.navigate("about") },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Acerca de",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Theme & Accent Color Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Apariencia y Tema",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Oscuro",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isChecked ->
                                isDarkMode = isChecked
                                preferences.isDarkMode = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Color de Acento",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val colorsList = listOf("CYAN", "AMBER", "RED", "VIOLET")
                            val colorMap = mapOf(
                                "CYAN" to if (isDarkMode) ColorCyan else ColorCyanLight,
                                "AMBER" to if (isDarkMode) ColorAmber else ColorAmberLight,
                                "RED" to if (isDarkMode) ColorRed else ColorRedLight,
                                "VIOLET" to if (isDarkMode) ColorViolet else ColorVioletLight
                            )

                            colorsList.forEach { colorStr ->
                                val active = colorMap[colorStr] ?: ColorCyan
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(active)
                                        .border(
                                            width = if (selectedAccentColor == colorStr) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedAccentColor = colorStr
                                            preferences.accentColor = colorStr
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // iOS PRO Performance & Network Settings Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Rendimiento y Red Pro",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Performance Profile
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Perfil de Rendimiento",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        IosSegmentedControl(
                            items = listOf("🔋 Ahorro", "⚖️ Balance", "⚡ Ultra"),
                            selectedIndex = when (selectedProfile) {
                                "BATTERY" -> 0
                                "BALANCED" -> 1
                                else -> 2
                            },
                            onItemSelected = { index ->
                                val prof = when (index) {
                                    0 -> "BATTERY"
                                    1 -> "BALANCED"
                                    else -> "ULTRA_GAMING"
                                }
                                selectedProfile = prof
                                preferences.activePerformanceProfile = prof
                                Toast.makeText(context, "Perfil de Rendimiento cambiado", Toast.LENGTH_SHORT).show()
                            },
                            isDarkMode = isDarkMode
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    // Exclusive Connection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conexión Exclusiva (Cortafuegos)",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prioriza todo el ancho de banda para tu juego",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isExclusiveConnEnabled,
                            onCheckedChange = { isChecked ->
                                val intentVpn = android.net.VpnService.prepare(context)
                                if (intentVpn != null) {
                                    Toast.makeText(context, "Por favor autorice el permiso de VPN para continuar", Toast.LENGTH_LONG).show()
                                    context.startActivity(intentVpn)
                                } else {
                                    isExclusiveConnEnabled = isChecked
                                    preferences.isExclusiveConnectionEnabled = isChecked
                                    val toggleIntent = Intent(context, ConnectionMonitorService::class.java).apply {
                                        action = "com.jules.gameguard.TOGGLE_VPN"
                                    }
                                    context.startService(toggleIntent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    // DNS Optimization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optimización DNS",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Utiliza DNS de baja latencia",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isDnsOptEnabled,
                            onCheckedChange = { isChecked ->
                                isDnsOptEnabled = isChecked
                                preferences.isDnsOptimizationEnabled = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    if (isDnsOptEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Servidor DNS",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            IosSegmentedControl(
                                items = listOf("Cloudflare", "Google", "AdGuard"),
                                selectedIndex = when (selectedDnsProvider) {
                                    "CLOUDFLARE" -> 0
                                    "GOOGLE" -> 1
                                    else -> 2
                                },
                                onItemSelected = { index ->
                                    val prov = when (index) {
                                        0 -> "CLOUDFLARE"
                                        1 -> "GOOGLE"
                                        else -> "ADGUARD"
                                    }
                                    selectedDnsProvider = prov
                                    preferences.dnsProvider = prov
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }

            // Call Blocking & Exclusions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Bloqueo de Llamadas (DND)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rechazar Llamadas Entrantes",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = blockCallsEnabled,
                            onCheckedChange = { isChecked ->
                                blockCallsEnabled = isChecked
                                preferences.isModoJuegoActivo = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    Text(
                        text = "Contactos Excepcionados",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    allowedContacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.name, color = MaterialTheme.colorScheme.onBackground, fontFamily = RajdhaniFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 13.sp)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.allowedContactDao().delete(contact)
                                }
                            }) {
                                Text("✕", color = ColorRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Add Contact Input
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newContactName,
                            onValueChange = { newContactName = it },
                            placeholder = { Text("Nombre") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            placeholder = { Text("Número") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (newContactName.isNotEmpty() && newContactPhone.isNotEmpty()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.allowedContactDao().insert(AllowedContact(name = newContactName, phoneNumber = newContactPhone))
                                    }
                                    newContactName = ""
                                    newContactPhone = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connection Monitor & Ping Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Servidores de Ping",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Servidor Activo: $activeServerIp",
                        color = accentColor,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val presetServers = listOf(
                        CustomServer(name = "Google DNS", ipOrDomain = "8.8.4.4"),
                        CustomServer(name = "Cloudflare", ipOrDomain = "1.1.1.1"),
                        CustomServer(name = "Free Fire Latam", ipOrDomain = "200.23.1.2")
                    )

                    (presetServers + customServers).forEach { server ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activeServerIp = server.ipOrDomain
                                    preferences.configurableServer = server.ipOrDomain
                                    Toast.makeText(context, "Servidor cambiado a ${server.name}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    server.name,
                                    color = if (activeServerIp == server.ipOrDomain) accentColor else MaterialTheme.colorScheme.onBackground,
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(server.ipOrDomain, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                            if (!presetServers.any { it.ipOrDomain == server.ipOrDomain }) {
                                IconButton(onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.customServerDao().delete(server)
                                    }
                                }) {
                                    Text("✕", color = ColorRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Add Custom Server Input
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newServerName,
                            onValueChange = { newServerName = it },
                            placeholder = { Text("Nombre") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newServerIp,
                            onValueChange = { newServerIp = it },
                            placeholder = { Text("IP / Dominio") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (newServerName.isNotEmpty() && newServerIp.isNotEmpty()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.customServerDao().insert(CustomServer(name = newServerName, ipOrDomain = newServerIp))
                                    }
                                    newServerName = ""
                                    newServerIp = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    // Ping Alert Threshold
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Umbral de Alerta (> ${pingThreshold.toInt()} ms)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Slider(
                            value = pingThreshold,
                            onValueChange = {
                                pingThreshold = it
                                preferences.pingAlertThresholdMs = it.toInt()
                            },
                            valueRange = 50f..250f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPingVibrate,
                                    onCheckedChange = {
                                        isPingVibrate = it
                                        preferences.isPingVibrateAlertEnabled = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Text("Vibrar", fontFamily = RajdhaniFontFamily, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPingSound,
                                    onCheckedChange = {
                                        isPingSound = it
                                        preferences.isPingSoundAlertEnabled = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Text("Alerta Sonora", fontFamily = RajdhaniFontFamily, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // RAM Cleaner & Whitelist Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Optimizador de RAM",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Limpieza Automática Programada",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        IosSegmentedControl(
                            items = listOf("OFF", "5 min", "10 min", "15 min", "30 min"),
                            selectedIndex = when (autoCleanInterval) {
                                5 -> 1
                                10 -> 2
                                15 -> 3
                                30 -> 4
                                else -> 0
                            },
                            onItemSelected = { index ->
                                val interval = when (index) {
                                    1 -> 5
                                    2 -> 10
                                    3 -> 15
                                    4 -> 30
                                    else -> 0
                                }
                                autoCleanInterval = interval
                                preferences.autoRamCleanIntervalMins = interval
                            },
                            isDarkMode = isDarkMode
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))

                    Text(
                        text = "Lista Blanca de Aplicaciones",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    whitelistedApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(app.appName, color = MaterialTheme.colorScheme.onBackground, fontFamily = RajdhaniFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(app.packageName, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.whitelistedAppDao().delete(app)
                                }
                            }) {
                                Text("✕", color = ColorRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Add custom package whitelist
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newAppName,
                            onValueChange = { newAppName = it },
                            placeholder = { Text("Nombre App") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newAppPkg,
                            onValueChange = { newAppPkg = it },
                            placeholder = { Text("com.paquete") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (newAppName.isNotEmpty() && newAppPkg.isNotEmpty()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.whitelistedAppDao().insert(WhitelistedApp(appName = newAppName, packageName = newAppPkg))
                                    }
                                    newAppName = ""
                                    newAppPkg = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
