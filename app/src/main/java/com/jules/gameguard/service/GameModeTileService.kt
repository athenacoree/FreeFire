package com.jules.gameguard.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.jules.gameguard.data.GameGuardPreferences

class GameModeTileService : TileService() {

    private lateinit var preferences: GameGuardPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = GameGuardPreferences(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isActive = preferences.isModoJuegoActivo
        val newState = !isActive
        preferences.isModoJuegoActivo = newState

        val serviceIntent = Intent(this, ConnectionMonitorService::class.java)
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            stopService(serviceIntent)
        }

        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isActive = preferences.isModoJuegoActivo

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Game Mode"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) "Activo" else "Inactivo"
        }
        tile.updateTile()
    }
}
