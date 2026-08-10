package com.jules.gameguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val preferences = remember { GameGuardPreferences(applicationContext) }

            GameGuardTheme(
                isDarkMode = preferences.isDarkMode,
                accentColorStr = preferences.accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (preferences.isDarkMode) ColorBackground else androidx.compose.ui.graphics.Color(0xFF, 0xF5, 0xF7, 0xFA)
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

                    Scaffold(
                        bottomBar = {
                            // Only show Bottom Tab Bar if onboarding is completed
                            val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
                            val destination = navBackStackEntry?.destination?.route
                            if (destination != "onboarding") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (preferences.isDarkMode) ColorBackground else Color(0xFF, 0xFA, 0xFA, 0xFC))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.08f))
                                        .padding(bottom = 12.dp, top = 8.dp)
                                ) {
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.height(60.dp)
                                    ) {
                                        val activeColor = when (preferences.accentColor.uppercase()) {
                                            "AMBER" -> ColorAmber
                                            "RED" -> ColorRed
                                            "VIOLET" -> Color(0xFF, 0xAF, 0x52, 0xDE)
                                            else -> ColorCyan
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
