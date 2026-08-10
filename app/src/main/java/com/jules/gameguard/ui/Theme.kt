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

// Define modern premium iOS style palette colors (Apple system classic inspired)
val ColorBackground = Color(0xFF, 0x00, 0x00, 0x00)       // Dark mode background (Pure pitch black)
val ColorBackgroundLight = Color(0xFF, 0xF2, 0xF2, 0xF7)  // Light mode background (Apple Light Grouped)
val ColorCyan = Color(0xFF, 0x0A, 0x84, 0xFF)             // Premium classic iOS Dark Blue/Cyan
val ColorCyanLight = Color(0xFF, 0x00, 0x7A, 0xFF)        // Premium classic iOS Light Blue
val ColorAmber = Color(0xFF, 0xFF, 0x9F, 0x0A)            // Premium iOS Dark Orange
val ColorAmberLight = Color(0xFF, 0xFF, 0x95, 0x00)       // Premium iOS Light Orange
val ColorRed = Color(0xFF, 0xFF, 0x45, 0x3A)              // Premium iOS Dark Red
val ColorRedLight = Color(0xFF, 0xFF, 0x3B, 0x30)         // Premium iOS Light Red
val ColorGreen = Color(0xFF, 0x30, 0xD1, 0x58)            // Premium iOS Dark Green
val ColorGreenLight = Color(0xFF, 0x34, 0xC7, 0x59)       // Premium iOS Light Green
val ColorViolet = Color(0xFF, 0xBF, 0x5A, 0xF2)           // Premium iOS Dark Violet
val ColorVioletLight = Color(0xFF, 0xAF, 0x52, 0xDE)      // Premium iOS Light Violet

val ColorSurfaceDark = Color(0xFF, 0x1C, 0x1C, 0x1E)     // Dark Mode Grouped Surface #1C1C1E
val ColorSurfaceLight = Color(0xFF, 0xFF, 0xFF, 0xFF)    // Light Mode Grouped Surface #FFFFFF

// System font definitions
val OrbitronFontFamily = FontFamily.Default
val RajdhaniFontFamily = FontFamily.Default

/**
 * Premium crystal glassmorphic container modifier.
 * Simulates frosted acrylic glass with translucent backgrounds, light reflections, and custom corners.
 */
fun Modifier.glassmorphism(
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 0.dp,
    isDarkMode: Boolean = true
): Modifier {
    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF, 0xFF, 0xFF, 0x14), // Elegant light reflection at the top
                Color(0xFF, 0xFF, 0xFF, 0x04)  // Ultra clear body
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF, 0xFF, 0xFF, 0xE0), // Highly reflective crisp frosted white glass
                Color(0xFF, 0xFF, 0xFF, 0xAA)  // Translucent bottom
            )
        )
    }

    val finalBorderColor = if (borderColor != Color.Transparent) {
        borderColor
    } else {
        if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
    }

    return this
        .background(brush = bgGradient, shape = RoundedCornerShape(cornerRadius))
        .border(BorderStroke(1.dp, finalBorderColor), RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.neonBorderAnimation(
    colors: List<Color> = listOf(ColorCyan, ColorCyan),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
): Modifier {
    // Beautiful clean iOS borders
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
    val accent = if (isDarkMode) {
        when (accentColorStr.uppercase()) {
            "AMBER" -> ColorAmber
            "RED" -> ColorRed
            "VIOLET" -> ColorViolet
            else -> ColorCyan
        }
    } else {
        when (accentColorStr.uppercase()) {
            "AMBER" -> ColorAmberLight
            "RED" -> ColorRedLight
            "VIOLET" -> ColorVioletLight
            else -> ColorCyanLight
        }
    }

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = ColorAmber,
            onSecondary = Color.White,
            error = ColorRed,
            onError = Color.White,
            background = ColorBackground,
            onBackground = Color.White,
            surface = ColorSurfaceDark,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            secondary = ColorAmberLight,
            onSecondary = Color.Black,
            error = ColorRedLight,
            onError = Color.White,
            background = ColorBackgroundLight,
            onBackground = Color.Black,
            surface = ColorSurfaceLight,
            onSurface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GameGuardTypography,
        content = content
    )
}
