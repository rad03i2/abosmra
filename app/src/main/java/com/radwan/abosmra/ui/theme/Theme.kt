package com.radwan.abosmra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GasGreen = Color(0xFF0A6B57)
val GasGreenDark = Color(0xFF06483D)
val GasCream = Color(0xFFF7F8F8)
val GasOrange = Color(0xFFD98B18)
val DebtRed = Color(0xFFB33A32)
val PaidGreen = Color(0xFF137A4A)
val SoftSurface = Color(0xFFF0F3F2)
val Ink = Color(0xFF14201D)
val MutedInk = Color(0xFF67736F)

private val LightColors = lightColorScheme(
    primary = GasGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF1EB),
    onPrimaryContainer = GasGreenDark,
    secondary = GasOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7C2),
    onSecondaryContainer = Color(0xFF4D3200),
    background = GasCream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD9E0DD),
    outlineVariant = Color(0xFFE8ECEA),
    error = DebtRed,
    errorContainer = Color(0xFFFFE6E3),
    onErrorContainer = Color(0xFF5F1612)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF79D8BE),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF075344),
    onPrimaryContainer = Color(0xFFB2F0DD),
    secondary = Color(0xFFFFC56D),
    background = Color(0xFF101513),
    onBackground = Color(0xFFE2E9E6),
    surface = Color(0xFF171D1B),
    onSurface = Color(0xFFE2E9E6),
    surfaceVariant = Color(0xFF202825),
    onSurfaceVariant = Color(0xFFB9C4C0),
    outline = Color(0xFF46524E),
    error = Color(0xFFFFB4AC)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GasLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
