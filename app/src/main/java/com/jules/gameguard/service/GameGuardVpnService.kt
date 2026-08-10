package com.jules.gameguard.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.jules.gameguard.data.GameGuardPreferences
import java.io.IOException

class GameGuardVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_START = "com.jules.gameguard.START_VPN"
        const val ACTION_STOP = "com.jules.gameguard.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START == action) {
                startVpn()
            } else if (ACTION_STOP == action) {
                stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val prefs = GameGuardPreferences(applicationContext)
        val builder = Builder()

        // Configure local sink IP to absorb blocked app traffic
        builder.setSession("GameGuard Exclusive VPN")
        builder.addAddress("10.0.0.2", 32)
        builder.addRoute("0.0.0.0", 0)

        // Custom DNS selection if enabled
        if (prefs.isDnsOptimizationEnabled) {
            val dnsSelected = when (prefs.dnsProvider.uppercase()) {
                "GOOGLE" -> "8.8.8.8"
                "ADGUARD" -> "94.140.14.14"
                else -> "1.1.1.1" // Cloudflare
            }
            builder.addDnsServer(dnsSelected)
        }

        val targetApp = prefs.exclusiveAppPackage
        val ourPackage = packageName

        // If exclusive connection is enabled and a target game/app is set, we run in reverse split-tunneling mode.
        // We add our package and the target game package as DISALLOWED applications.
        // This means they bypass the VPN entirely and have regular internet access.
        // All other installed apps on the device get automatically routed to the VPN interface (blackhole) and have NO internet.
        if (prefs.isExclusiveConnectionEnabled && targetApp.isNotEmpty()) {
            try {
                builder.addDisallowedApplication(ourPackage)
                builder.addDisallowedApplication(targetApp)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback in case package name is not valid
                builder.addAllowedApplication(ourPackage)
            }
        } else {
            builder.addAllowedApplication(ourPackage)
        }

        try {
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVpn() {
        if (vpnInterface != null) {
            try {
                vpnInterface?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            vpnInterface = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
