package com.jules.gameguard.ui

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class AppInfo(
    val packageName: String,
    val label: String,
    val isChecked: MutableState<Boolean> = mutableStateOf(true)
)

fun getRecentBackgroundApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

    val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return emptyList()
    val seenPackages = mutableSetOf<String>()
    val ourPackage = context.packageName
    val recentApps = mutableListOf<AppInfo>()

    // Sort stats by last time used (most recent first)
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
            // App not found or uninstalled, skip
        }
    }

    // Fallback: if we didn't find any app via usage stats, find some installed user apps as a fallback
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
                if (recentApps.size >= 12) break // Limit list size
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
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = getRecentBackgroundApps(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                 .background(ColorSurfaceDark, shape = RoundedCornerShape(16.dp))
                 .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "OPTIMIZADOR DE RAM",
                    color = ColorCyan,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Selecciona las aplicaciones en segundo plano que deseas cerrar para liberar memoria y activar el Modo Juego:",
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = ColorCyan.copy(alpha = 0.2f), thickness = 1.dp)

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron apps en segundo plano.",
                            color = Color.White.copy(alpha = 0.5f),
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
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = Color.White,
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = RajdhaniFontFamily,
                                        fontSize = 12.sp
                                    )
                                }

                                Checkbox(
                                    checked = app.isChecked.value,
                                    onCheckedChange = { app.isChecked.value = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ColorCyan,
                                        uncheckedColor = Color.White.copy(alpha = 0.4f),
                                        checkmarkColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ColorCyan.copy(alpha = 0.2f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(ColorRed.copy(alpha = 0.6f))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ColorRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCELAR",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
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
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OPTIMIZAR AHORA",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
