package com.jules.gameguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.ui.*
import androidx.compose.ui.graphics.Brush

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
        // Draw 3 gorgeous glowing glass-background blobs in both modes to create breathtaking depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                // Dark obsidian glass style glow blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.12f), Color.Transparent),
                        radius = width * 0.45f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.2f),
                    radius = width * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorAmber.copy(alpha = 0.08f), Color.Transparent),
                        radius = width * 0.4f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.8f, height * 0.5f),
                    radius = width * 0.4f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorRed.copy(alpha = 0.06f), Color.Transparent),
                        radius = width * 0.5f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.4f, height * 0.8f),
                    radius = width * 0.5f
                )
            } else {
                // Light frosty glass style glow blobs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.15f), Color.Transparent),
                        radius = width * 0.5f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.15f),
                    radius = width * 0.5f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ColorAmberLight.copy(alpha = 0.1f), Color.Transparent),
                        radius = width * 0.45f
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.45f),
                    radius = width * 0.45f
                )
            }
        }

        // Screen Content layered above the glowing background
        content()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                    // Navigation to ensure Onboarding runs first
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
                            containerColor = Color.Transparent, // transparent to let glowing background shine through
                            bottomBar = {
                                // Only show Bottom Tab Bar if onboarding is completed
                                val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
                                val destination = navBackStackEntry?.destination?.route
                                if (destination != "onboarding") {
                                    val barBg = if (isDarkMode) {
                                        Color.Black.copy(alpha = 0.85f)
                                    } else {
                                        Color(0xFF, 0xFA, 0xFA, 0xFC).copy(alpha = 0.9f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(barBg)
                                            .border(
                                                width = 0.5.dp,
                                                color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                                            )
                                            .padding(bottom = 12.dp, top = 8.dp)
                                    ) {
                                        NavigationBar(
                                            containerColor = Color.Transparent,
                                            tonalElevation = 0.dp,
                                            modifier = Modifier.height(60.dp)
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
                                                icon = { Text("📱", fontSize = 20.sp) },
                                                label = { Text("Inicio", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = activeColor,
                                                    unselectedIconColor = Color.Gray,
                                                    selectedTextColor = activeColor,
                                                    unselectedTextColor = Color.Gray,
                                                    indicatorColor = Color.Transparent
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
                                                icon = { Text("🚀", fontSize = 20.sp) },
                                                label = { Text("Lanzador", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = activeColor,
                                                    unselectedIconColor = Color.Gray,
                                                    selectedTextColor = activeColor,
                                                    unselectedTextColor = Color.Gray,
                                                    indicatorColor = Color.Transparent
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
                                                icon = { Text("📊", fontSize = 20.sp) },
                                                label = { Text("Estadísticas", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = activeColor,
                                                    unselectedIconColor = Color.Gray,
                                                    selectedTextColor = activeColor,
                                                    unselectedTextColor = Color.Gray,
                                                    indicatorColor = Color.Transparent
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
                                                icon = { Text("⚙", fontSize = 20.sp) },
                                                label = { Text("Ajustes", fontFamily = RajdhaniFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = activeColor,
                                                    unselectedIconColor = Color.Gray,
                                                    selectedTextColor = activeColor,
                                                    unselectedTextColor = Color.Gray,
                                                    indicatorColor = Color.Transparent
                                                )
                                            )
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
