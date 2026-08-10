package com.jules.gameguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jules.gameguard.data.GameGuardPreferences
import com.jules.gameguard.ui.GameGuardTheme
import com.jules.gameguard.ui.ColorBackground
import com.jules.gameguard.ui.HomeScreen
import com.jules.gameguard.ui.SettingsScreen
import com.jules.gameguard.ui.HistoryScreen
import com.jules.gameguard.ui.OnboardingScreen
import com.jules.gameguard.ui.AboutScreen

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
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController, preferences = preferences)
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
