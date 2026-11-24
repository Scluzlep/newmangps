package com.scluzlep.newmangps.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// Xiaomi HyperOS Color Palette
val HyperBlue = Color(0xFF007DFF)
val HyperBlueVariant = Color(0xFF0066D6)
val HyperBackgroundLight = Color(0xFFF4F6F9)
val HyperBackgroundDark = Color(0xFF0C0D0E)
val HyperSurfaceLight = Color(0xFFFFFFFF)
val HyperSurfaceDark = Color(0xFF191A1D)
val HyperCardLight = Color(0xFFFFFFFF)
val HyperCardDark = Color(0xFF222428)
val HyperCardBorderLight = Color(0xFFE5E7EB)
val HyperCardBorderDark = Color(0xFF2E3136)

val HyperTextPrimaryLight = Color(0xFF111827)
val HyperTextSecondaryLight = Color(0xFF6B7280)
val HyperTextPrimaryDark = Color(0xFFF3F4F6)
val HyperTextSecondaryDark = Color(0xFF9CA3AF)

val HyperGreen = Color(0xFF28C76F)
val HyperYellow = Color(0xFFFF9F43)
val HyperRed = Color(0xFFEA5455)

// HyperOS Squircles and Radii
val HyperCardShape = RoundedCornerShape(22.dp)
val HyperButtonShape = RoundedCornerShape(16.dp)
val HyperChipShape = RoundedCornerShape(12.dp)
val HyperPillShape = RoundedCornerShape(50)

private val LightColorScheme = lightColorScheme(
    primary = HyperBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = HyperBlueVariant,
    secondary = HyperBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F1FF),
    onSecondaryContainer = HyperBlueVariant,
    tertiary = HyperGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F8EE),
    onTertiaryContainer = HyperGreen,
    background = HyperBackgroundLight,
    onBackground = HyperTextPrimaryLight,
    surface = HyperSurfaceLight,
    onSurface = HyperTextPrimaryLight,
    surfaceVariant = HyperCardLight,
    onSurfaceVariant = HyperTextSecondaryLight,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = Color(0xFFF4F6F9),
    surfaceContainerHigh = Color(0xFFEEF1F5),
    surfaceContainerHighest = Color(0xFFE5E7EB),
    outline = HyperCardBorderLight,
    outlineVariant = HyperCardBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = HyperBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00448A),
    onPrimaryContainer = Color.White,
    secondary = HyperBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF153459),
    onSecondaryContainer = Color.White,
    tertiary = HyperGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF123624),
    onTertiaryContainer = HyperGreen,
    background = HyperBackgroundDark,
    onBackground = HyperTextPrimaryDark,
    surface = HyperSurfaceDark,
    onSurface = HyperTextPrimaryDark,
    surfaceVariant = HyperCardDark,
    onSurfaceVariant = HyperTextSecondaryDark,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF0C0D0E),
    surfaceContainerLow = Color(0xFF141518),
    surfaceContainer = Color(0xFF191A1D),
    surfaceContainerHigh = Color(0xFF222428),
    surfaceContainerHighest = Color(0xFF2C2F35),
    outline = HyperCardBorderDark,
    outlineVariant = HyperCardBorderDark
)

val HyperTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun HyperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val miuixColors = if (darkTheme) {
        miuixDarkColorScheme(
            primary = HyperBlue,
            primaryVariant = HyperBlueVariant,
            background = HyperBackgroundDark,
            surface = HyperSurfaceDark,
            surfaceContainer = HyperSurfaceDark,
            dividerLine = HyperCardBorderDark
        )
    } else {
        miuixLightColorScheme(
            primary = HyperBlue,
            primaryVariant = HyperBlueVariant,
            background = HyperBackgroundLight,
            surface = HyperSurfaceLight,
            surfaceContainer = HyperSurfaceLight,
            dividerLine = HyperCardBorderLight
        )
    }

    MiuixTheme(colors = miuixColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HyperTypography,
            content = content
        )
    }
}

