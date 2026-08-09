package com.jules.gameguard.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jules.gameguard.R

// Define HUD gaming palette colors
val ColorBackground = Color(0x0A, 0x0A, 0x0F, 0xFF) // #0A0A0F
val ColorCyan = Color(0x00, 0xF0, 0xFF, 0xFF)       // #00F0FF (Positive)
val ColorAmber = Color(0xFF, 0xB8, 0x00, 0xFF)      // #FFB800 (Warning)
val ColorRed = Color(0xFF, 0x38, 0x60, 0xFF)        // #FF3860 (Error/Mala)
val ColorGlassBg = Color(0xAA, 0x0A, 0x0A, 0x14)    // Glass semi-transparent black
val ColorSurfaceDark = Color(0x12, 0x12, 0x1A, 0xFF)

// Define fonts packaged locally
val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Bold)
)

val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Normal),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

// Reusable modifiers for Gaming HUD aesthetic
fun Modifier.glassmorphism(
    borderColor: Color = ColorCyan.copy(alpha = 0.3f),
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 8.dp
): Modifier {
    return this
        .blur(blurRadius)
        .background(ColorGlassBg, shape = RoundedCornerShape(cornerRadius))
        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.neonBorderAnimation(
    colors: List<Color> = listOf(ColorCyan, Color(0xFF, 0x00, 0xD4, 0xFF), ColorCyan),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_border_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_border_angle"
    )

    return this.drawWithContent {
        drawContent()
        val brush = Brush.sweepGradient(
            colors = colors,
            center = size.center
        )
        // Draw the neon border stroke
        drawRoundRect(
            brush = brush,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this)),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// Define complete Typography
val GameGuardTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

private val GameGuardColorScheme = darkColorScheme(
    primary = ColorCyan,
    onPrimary = Color.Black,
    secondary = ColorAmber,
    onSecondary = Color.Black,
    error = ColorRed,
    onError = Color.White,
    background = ColorBackground,
    onBackground = Color.White,
    surface = ColorSurfaceDark,
    onSurface = Color.White
)

@Composable
fun GameGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameGuardColorScheme,
        typography = GameGuardTypography,
        content = content
    )
}
