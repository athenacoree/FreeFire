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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CONFIGURACIÓN HUD",
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
                actions = {
                    IconButton(
                        onClick = { navController.navigate("about") },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Text(
                            text = "ℹ",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
                .padding(16.dp)
                .verticalScroll(scrollState)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Configuration Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ESTILO Y APARIENCIA",
                        color = accentColor,
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
                                text = "Modo Oscuro HUD",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isChecked ->
                                isDarkMode = isChecked
                                preferences.isDarkMode = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkMode) Color.Black else Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Color de Acento HUD",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                        .size(36.dp)
                                        .background(active, CircleShape)
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

            // PREMIUM IOS ADDED FUNCTIONS CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "FUNCIONES PRO Y PERFILES iOS",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // 1. Performance Profile
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Perfil de Rendimiento",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val profiles = listOf("BATTERY", "BALANCED", "ULTRA_GAMING")
                            val profileLabels = mapOf("BATTERY" to "🔋 Ahorro", "BALANCED" to "⚖️ Balance", "ULTRA_GAMING" to "⚡ Ultra")

                            profiles.forEach { profile ->
                                Button(
                                    onClick = {
                                        selectedProfile = profile
                                        preferences.activePerformanceProfile = profile
                                        Toast.makeText(context, "Perfil de Rendimiento: ${profileLabels[profile]}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedProfile == profile) accentColor else Color.White.copy(alpha = 0.05f),
                                        contentColor = if (selectedProfile == profile) {
                                            if (isDarkMode) Color.Black else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(profileLabels[profile] ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    // 2. Exclusive connection
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bloquea internet para otras apps excepto el juego seleccionado",
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
                                checkedThumbColor = if (isDarkMode) Color.Black else Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    // 3. DNS Optimization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optimización de DNS",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Usa DNS de alta velocidad para reducir latencia",
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
                                checkedThumbColor = if (isDarkMode) Color.Black else Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    if (isDnsOptEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Servidor DNS Preferido",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val providers = listOf("CLOUDFLARE", "GOOGLE", "ADGUARD")
                                val providerLabels = mapOf("CLOUDFLARE" to "⚡ Cloudflare", "GOOGLE" to "🔍 Google", "ADGUARD" to "🛡️ AdGuard")

                                providers.forEach { provider ->
                                    Button(
                                        onClick = {
                                            selectedDnsProvider = provider
                                            preferences.dnsProvider = provider
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedDnsProvider == provider) accentColor else Color.White.copy(alpha = 0.05f),
                                            contentColor = if (selectedDnsProvider == provider) {
                                                if (isDarkMode) Color.Black else Color.White
                                            } else {
                                                MaterialTheme.colorScheme.onBackground
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(providerLabels[provider] ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Call Blocking & Allowed Contacts Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "BLOQUEO DE LLAMADAS (DND)",
                        color = accentColor,
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
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = blockCallsEnabled,
                            onCheckedChange = { isChecked ->
                                blockCallsEnabled = isChecked
                                preferences.isModoJuegoActivo = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkMode) Color.Black else Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    Text(
                        text = "CONTACTOS PERMITIDOS (EXCEPCIONES)",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    allowedContacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.name, color = MaterialTheme.colorScheme.onBackground, fontFamily = RajdhaniFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.allowedContactDao().delete(contact)
                                }
                            }) {
                                Text("✕", color = ColorRed)
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
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            placeholder = { Text("Número") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", color = if (isDarkMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connection Monitor Configuration Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "MONITOR DE PING Y CALIDAD DE RED",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Active server selection
                    Text(
                        text = "Servidor Ping Activo: $activeServerIp",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Custom Server List
                    Text(
                        text = "LISTA DE SERVIDORES DE PING",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
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
                                .clickable {
                                    activeServerIp = server.ipOrDomain
                                    preferences.configurableServer = server.ipOrDomain
                                    Toast.makeText(context, "Servidor cambiado a ${server.name} (${server.ipOrDomain})", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(server.name, color = if (activeServerIp == server.ipOrDomain) accentColor else MaterialTheme.colorScheme.onBackground, fontFamily = RajdhaniFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(server.ipOrDomain, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                            if (!presetServers.any { it.ipOrDomain == server.ipOrDomain }) {
                                IconButton(onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.customServerDao().delete(server)
                                    }
                                }) {
                                    Text("✕", color = ColorRed)
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
                            placeholder = { Text("Nombre Servidor") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newServerIp,
                            onValueChange = { newServerIp = it },
                            placeholder = { Text("IP/Dominio") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", color = if (isDarkMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    // Ping Alert Threshold
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Alerta de Ping Alto (> ${pingThreshold.toInt()} ms)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
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

            // RAM Whitelist & Cleaner Configuration Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(isDarkMode = isDarkMode)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "OPTIMIZADOR DE RAM",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Scheduled automatic clean
                    Column {
                        Text(
                            text = "Limpieza Automática cada:",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val intervals = listOf(0, 5, 10, 15, 30)
                            intervals.forEach { mins ->
                                Button(
                                    onClick = {
                                        autoCleanInterval = mins
                                        preferences.autoRamCleanIntervalMins = mins
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (autoCleanInterval == mins) accentColor else Color.White.copy(alpha = 0.05f),
                                        contentColor = if (autoCleanInterval == mins) {
                                            if (isDarkMode) Color.Black else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (mins == 0) "OFF" else "${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    // Whitelisted apps list
                    Text(
                        text = "LISTA BLANCA DE APPS (NUNCA CERRAR)",
                        color = accentColor,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    whitelistedApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(app.appName, color = MaterialTheme.colorScheme.onBackground, fontFamily = RajdhaniFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(app.packageName, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.whitelistedAppDao().delete(app)
                                }
                            }) {
                                Text("✕", color = ColorRed)
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
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newAppPkg,
                            onValueChange = { newAppPkg = it },
                            placeholder = { Text("com.package") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", color = if (isDarkMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
