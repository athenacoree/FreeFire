package com.jules.gameguard.ui

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jules.gameguard.service.ConnectionMonitorService

class PingGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by ConnectionMonitorService.monitorState.collectAsState()
            val ping = state?.pingMs ?: 0L
            val status = state?.status ?: "Inactivo"

            val textColor = when (status) {
                "Regular" -> ColorProvider(ColorAmber)
                "Mala" -> ColorProvider(ColorRed)
                "Inactivo" -> ColorProvider(Color.Gray)
                else -> ColorProvider(ColorCyan)
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAMEGUARD",
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (ping > 0) "$ping ms" else "-- ms",
                        style = TextStyle(
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = status.uppercase(),
                        style = TextStyle(
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

class PingGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PingGlanceWidget()
}
