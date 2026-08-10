package com.jules.gameguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import com.jules.gameguard.data.AppDatabase
import com.jules.gameguard.data.BlockedCall
import com.jules.gameguard.data.GameGuardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = GameGuardPreferences(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""

        if (prefs.isModoJuegoActivo) {
            // Check if number is whitelisted
            var isWhitelisted = false
            runBlocking(Dispatchers.IO) {
                val allowedContacts = db.allowedContactDao().getAllAllowedContacts()
                isWhitelisted = allowedContacts.any { contact ->
                    val cleanContact = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                    val cleanRaw = rawNumber.replace("[^0-9]".toRegex(), "")
                    cleanContact.isNotEmpty() && cleanRaw.isNotEmpty() &&
                            (cleanRaw.endsWith(cleanContact) || cleanContact.endsWith(cleanRaw))
                }
            }

            if (isWhitelisted) {
                // Let call through
                allowCall(callDetails)
            } else {
                // Reject call
                rejectCall(callDetails)

                // Log to Room
                CoroutineScope(Dispatchers.IO).launch {
                    db.blockedCallDao().insert(
                        BlockedCall(
                            phoneNumber = rawNumber.ifEmpty { "Número Oculto/Desconocido" },
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                sendSilentNotification(rawNumber)
            }
        } else {
            allowCall(callDetails)
        }
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun rejectCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)
    }

    private fun sendSilentNotification(number: String) {
        val channelId = "game_guard_silent_calls"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Llamadas Bloqueadas (Silencioso)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones silenciosas de llamadas bloqueadas durante el Modo Juego"
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val displayNum = number.ifEmpty { "Número Oculto" }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Llamada rechazada de: $displayNum")
            .setContentText("Se rechazó automáticamente por estar en Modo Juego.")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
