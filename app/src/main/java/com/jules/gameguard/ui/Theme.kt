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

// Define modern premium iOS style palette colors
val ColorBackground = Color(0xFF, 0x0A, 0x0A, 0x0F) // iOS Pure pitch black #000000 / deep gray
val ColorCyan = Color(0xFF, 0x00, 0x7A, 0xFF)       // Premium iOS Blue #007AFF
val ColorAmber = Color(0xFF, 0xFF, 0x95, 0x00)      // Premium iOS Orange #FF9500
val ColorRed = Color(0xFF, 0xFF, 0x3B, 0x30)        // Premium iOS Red #FF3B30
val ColorGlassBg = Color(0x1F, 0x1F, 0x24, 0xFF)    // solid high-end dark background
val ColorSurfaceDark = Color(0xFF, 0x1C, 0x1C, 0x1E) // iOS 17 Grouped Surface #1C1C1E

// Use clean, modern system sans-serif font families for high professionalism and readability
val OrbitronFontFamily = FontFamily.Default
val RajdhaniFontFamily = FontFamily.Default

// Completely removed blur modifiers that cause letters to overlap and distort behind cards.
// Cards are now structured as high-end solid and semi-solid iOS panels.
fun Modifier.glassmorphism(
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 0.dp
): Modifier {
    return this
        .background(ColorSurfaceDark, shape = RoundedCornerShape(cornerRadius))
        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.neonBorderAnimation(
    colors: List<Color> = listOf(ColorCyan, ColorCyan),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
): Modifier {
    // Replaced neon blinking lights with elegant clean static iOS borders
    return this.border(
        BorderStroke(1.5.dp, colors.firstOrNull() ?: ColorCyan),
        shape
    )
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

@Composable
fun GameGuardTheme(
    isDarkMode: Boolean = true,
    accentColorStr: String = "CYAN",
    content: @Composable () -> Unit
) {
    val accent = when (accentColorStr.uppercase()) {
        "AMBER" -> ColorAmber
        "RED" -> ColorRed
        "VIOLET" -> Color(0xFF, 0xAF, 0x52, 0xDE) // iOS violet / purple
        else -> ColorCyan
    }

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = ColorAmber,
            onSecondary = Color.White,
            error = ColorRed,
            onError = Color.White,
            background = Color(0xFF, 0x00, 0x00, 0x00), // Pure Black iOS Background
            onBackground = Color.White,
            surface = ColorSurfaceDark,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            secondary = ColorAmber,
            onSecondary = Color.Black,
            error = ColorRed,
            onError = Color.White,
            background = Color(0xFF, 0xF2, 0xF2, 0xF7), // Standard iOS Grouped Background
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GameGuardTypography,
        content = content
    )
}
