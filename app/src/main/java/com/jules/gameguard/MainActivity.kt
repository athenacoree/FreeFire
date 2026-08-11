package com.jules.gameguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.ui.*

@Composable
fun GlowingBackgroundDecorator(
    isDarkMode: Boolean,
    isGameModeActive: Boolean,
    accentColorStr: String,
    content: @Composable () -> Unit
) {
    val activeColor = when (accentColorStr.uppercase()) {
        "AMBER" -> if (isDarkMode) ColorAmber else ColorAmberLight
        "RED" -> if (isDarkMode) ColorRed else ColorRedLight
        "VIOLET" -> if (isDarkMode) ColorViolet else ColorVioletLight
        else -> if (isDarkMode) ColorCyan else ColorCyanLight
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                // Dark obsidian style ambient glow blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.15f), Color.Transparent),
                        radius = width * 0.55f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.15f),
                    radius = width * 0.55f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorAmber.copy(alpha = 0.08f), Color.Transparent),
                        radius = width * 0.45f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.45f),
                    radius = width * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorViolet.copy(alpha = 0.07f), Color.Transparent),
                        radius = width * 0.5f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.35f, height * 0.82f),
                    radius = width * 0.5f
                )
            } else {
                // Light frosty style glow blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.12f), Color.Transparent),
                        radius = width * 0.55f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.15f, height * 0.12f),
                    radius = width * 0.55f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorAmberLight.copy(alpha = 0.08f), Color.Transparent),
                        radius = width * 0.5f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.5f),
                    radius = width * 0.5f
                )
            }
        }

        content()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences = remember { GameGuardPreferences(applicationContext) }
            val isDarkMode = preferences.isDarkMode

            GameGuardTheme(
                isDarkMode = isDarkMode,
                accentColorStr = preferences.accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDarkMode) ColorBackground else ColorBackgroundLight
                ) {
                    val navController = rememberNavController()
                    var currentTab by remember { mutableStateOf("home") }

                    LaunchedEffect(Unit) {
                        if (!preferences.isOnboardingCompleted) {
                            navController.navigate("onboarding") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }

                    GlowingBackgroundDecorator(
                        isDarkMode = isDarkMode,
                        isGameModeActive = preferences.isModoJuegoActivo,
                        accentColorStr = preferences.accentColor
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
                                val destination = navBackStackEntry?.destination?.route
                                if (destination != "onboarding") {
                                    val barBg = if (isDarkMode) {
                                        Color(0xFF161618).copy(alpha = 0.92f)
                                    } else {
                                        Color(0xFFF9F9FB).copy(alpha = 0.94f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = barBg,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 0.8.dp,
                                                color = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                                            ),
                                            shadowElevation = if (isDarkMode) 8.dp else 4.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                        ) {
                                            NavigationBar(
                                                containerColor = Color.Transparent,
                                                tonalElevation = 0.dp,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                val activeColor = if (isDarkMode) {
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

                                                NavigationBarItem(
                                                    selected = currentTab == "home",
                                                    onClick = {
                                                        currentTab = "home"
                                                        navController.navigate("home") {
                                                            popUpTo("home") { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                                    label = { Text("Inicio", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = activeColor,
                                                        unselectedIconColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        selectedTextColor = activeColor,
                                                        unselectedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        indicatorColor = activeColor.copy(alpha = 0.12f)
                                                    )
                                                )

                                                NavigationBarItem(
                                                    selected = currentTab == "launcher",
                                                    onClick = {
                                                        currentTab = "launcher"
                                                        navController.navigate("launcher") {
                                                            popUpTo("home") { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Lanzador") },
                                                    label = { Text("Lanzador", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = activeColor,
                                                        unselectedIconColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        selectedTextColor = activeColor,
                                                        unselectedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        indicatorColor = activeColor.copy(alpha = 0.12f)
                                                    )
                                                )

                                                NavigationBarItem(
                                                    selected = currentTab == "history",
                                                    onClick = {
                                                        currentTab = "history"
                                                        navController.navigate("history") {
                                                            popUpTo("home") { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Estadísticas") },
                                                    label = { Text("Estadísticas", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = activeColor,
                                                        unselectedIconColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        selectedTextColor = activeColor,
                                                        unselectedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        indicatorColor = activeColor.copy(alpha = 0.12f)
                                                    )
                                                )

                                                NavigationBarItem(
                                                    selected = currentTab == "settings",
                                                    onClick = {
                                                        currentTab = "settings"
                                                        navController.navigate("settings") {
                                                            popUpTo("home") { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                                                    label = { Text("Ajustes", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = activeColor,
                                                        unselectedIconColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        selectedTextColor = activeColor,
                                                        unselectedTextColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                                        indicatorColor = activeColor.copy(alpha = 0.12f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { paddingValues ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.padding(paddingValues)
                            ) {
                                composable("home") {
                                    HomeScreen(navController = navController, preferences = preferences)
                                }
                                composable("launcher") {
                                    GameLauncherTab(preferences = preferences)
                                }
                                composable("settings") {
                                    SettingsScreen(navController = navController, preferences = preferences)
                                }
                                composable("history") {
                                    HistoryScreen(navController = navController)
                                }
                                composable("onboarding") {
                                    OnboardingScreen(navController = navController, preferences = preferences)
                                }
                                composable("about") {
                                    AboutScreen(navController = navController, preferences = preferences)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
