package com.jules.gameguard.ui

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jules.gameguard.data.GameGuardPreferences

data class AppInfo(
    val packageName: String,
    val label: String,
    val isChecked: MutableState<Boolean> = mutableStateOf(true)
)

fun getRecentBackgroundApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 24 * 60 * 60 * 1000

    val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return emptyList()
    val seenPackages = mutableSetOf<String>()
    val ourPackage = context.packageName
    val recentApps = mutableListOf<AppInfo>()

    val sortedStats = usageStats.sortedByDescending { it.lastTimeUsed }

    for (stat in sortedStats) {
        val pkgName = stat.packageName
        if (pkgName == ourPackage || seenPackages.contains(pkgName)) continue

        try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                           (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            if (!isSystem) {
                val label = pm.getApplicationLabel(appInfo).toString()
                recentApps.add(AppInfo(packageName = pkgName, label = label))
                seenPackages.add(pkgName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // App not found
        }
    }

    if (recentApps.isEmpty()) {
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedApps) {
            val pkgName = appInfo.packageName
            if (pkgName == ourPackage) continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                           (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            if (!isSystem) {
                val label = pm.getApplicationLabel(appInfo).toString()
                recentApps.add(AppInfo(packageName = pkgName, label = label))
                if (recentApps.size >= 12) break
            }
        }
    }

    return recentApps
}

fun killApps(context: Context, appsToKill: List<String>) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
    for (pkg in appsToKill) {
        activityManager.killBackgroundProcesses(pkg)
    }
}

@Composable
fun RamCleanerDialog(
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { GameGuardPreferences(context.applicationContext) }
    val isDarkMode = preferences.isDarkMode

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    val accentColor = if (isDarkMode) {
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

    LaunchedEffect(Unit) {
        apps = getRecentBackgroundApps(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .glassmorphism(isDarkMode = isDarkMode)
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Optimizador de RAM",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Selecciona las aplicaciones en segundo plano que deseas cerrar para liberar memoria:",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron aplicaciones en segundo plano.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 12.sp
                                    )
                                }

                                Checkbox(
                                    checked = app.isChecked.value,
                                    onCheckedChange = { app.isChecked.value = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val selectedApps = apps.filter { it.isChecked.value }
                            val packageNames = selectedApps.map { it.packageName }
                            val names = selectedApps.map { it.label }
                            killApps(context, packageNames)

                            val message = "RAM Optimizada: Se cerraron ${selectedApps.size} apps"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                            onConfirm(names)
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Optimizar",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
