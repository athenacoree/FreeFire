package com.jules.gameguard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define modern premium iOS style palette colors (Apple system inspired)
val ColorBackground = Color(0xFF000000)       // Pure Pitch Dark mode background
val ColorBackgroundLight = Color(0xFFF2F2F7)  // Apple Light Grouped background

val ColorCyan = Color(0xFF0A84FF)             // Premium classic iOS Dark Blue/Cyan
val ColorCyanLight = Color(0xFF007AFF)        // Premium classic iOS Light Blue
val ColorAmber = Color(0xFFFF9F0A)            // Premium iOS Dark Orange
val ColorAmberLight = Color(0xFFFF9500)       // Premium iOS Light Orange
val ColorRed = Color(0xFFFF453A)              // Premium iOS Dark Red
val ColorRedLight = Color(0xFFFF3B30)         // Premium iOS Light Red
val ColorGreen = Color(0xFF30D158)            // Premium iOS Dark Green
val ColorGreenLight = Color(0xFF34C759)       // Premium iOS Light Green
val ColorViolet = Color(0xFFBF5AF2)           // Premium iOS Dark Violet
val ColorVioletLight = Color(0xFFAF52DE)      // Premium iOS Light Violet

val ColorSurfaceDark = Color(0xFF1C1C1E)     // Dark Mode Grouped Surface #1C1C1E
val ColorSurfaceLight = Color(0xFFFFFFFF)    // Light Mode Grouped Surface #FFFFFF

// System font definitions
val OrbitronFontFamily = FontFamily.Default
val RajdhaniFontFamily = FontFamily.Default

/**
 * Premium crystal glassmorphic container modifier for iOS aesthetic.
 * Simulates frosted acrylic glass with subtle borders and shadows.
 */
fun Modifier.glassmorphism(
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    isDarkMode: Boolean = true
): Modifier {
    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2C2C2E).copy(alpha = 0.85f),
                Color(0xFF1C1C1E).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.95f),
                Color(0xFFF8F9FA).copy(alpha = 0.90f)
            )
        )
    }

    val finalBorderColor = if (borderColor != Color.Transparent) {
        borderColor
    } else {
        if (isDarkMode) Color(0xFFFFFFFF).copy(alpha = 0.12f) else Color(0xFF000000).copy(alpha = 0.06f)
    }

    val shadowColor = if (isDarkMode) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.05f)

    return this
        .shadow(
            elevation = if (isDarkMode) 6.dp else 4.dp,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .background(brush = bgGradient, shape = RoundedCornerShape(cornerRadius))
        .border(BorderStroke(0.8.dp, finalBorderColor), RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.neonBorderAnimation(
    colors: List<Color> = listOf(ColorCyan, ColorCyan),
    shape: RoundedCornerShape = RoundedCornerShape(20.dp)
): Modifier {
    return this.border(
        BorderStroke(1.5.dp, colors.firstOrNull() ?: ColorCyan),
        shape
    )
}

/**
 * Custom iOS Segmented Control component.
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String = { it.toString() },
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(containerBg, RoundedCornerShape(10.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val targetBg = if (isSelected) {
                if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFFFFFFF)
            } else {
                Color.Transparent
            }
            val animatedBg by animateColorAsState(
                targetValue = targetBg,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "segmented_bg"
            )

            val textColor = if (isSelected) {
                if (isDarkMode) Color.White else Color.Black
            } else {
                if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(animatedBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onItemSelected(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemLabel(item),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = OrbitronFontFamily
                )
            }
        }
    }
}

@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedItem = items.getOrNull(selectedIndex) ?: items.firstOrNull() ?: ""
    IosSegmentedControl(
        items = items,
        selectedItem = selectedItem,
        onItemSelected = { item ->
            val idx = items.indexOf(item)
            if (idx >= 0) onItemSelected(idx)
        },
        itemLabel = { it },
        isDarkMode = isDarkMode,
        modifier = modifier
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
            onPrimary = Color.White,
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
