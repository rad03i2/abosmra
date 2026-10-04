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

val GasGreen = Color(0xFF0B6252)
val GasGreenDark = Color(0xFF073E35)
val GasMint = Color(0xFFDDF3EC)
val GasCream = Color(0xFFF7F8F6)
val GasOrange = Color(0xFFE9A23B)
val DebtRed = Color(0xFFB33A32)
val PaidGreen = Color(0xFF147B50)
val SoftSurface = Color(0xFFF0F3F1)
val Ink = Color(0xFF15201D)
val MutedInk = Color(0xFF68746F)

private val LightColors = lightColorScheme(
    primary = GasGreen,
    onPrimary = Color.White,
    primaryContainer = GasMint,
    onPrimaryContainer = GasGreenDark,
    secondary = GasOrange,
    onSecondary = Color(0xFF352300),
    secondaryContainer = Color(0xFFFFEBC8),
    onSecondaryContainer = Color(0xFF4E3500),
    tertiary = Color(0xFF47645B),
    background = GasCream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD4DDDA),
    outlineVariant = Color(0xFFE5EAE8),
    error = DebtRed,
    errorContainer = Color(0xFFFFE7E4),
    onErrorContainer = Color(0xFF611815)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF83D8C2),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF0C5145),
    onPrimaryContainer = Color(0xFFB9F1E1),
    secondary = Color(0xFFFFC66F),
    onSecondary = Color(0xFF432D00),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFE2E9E6),
    surface = Color(0xFF171D1B),
    onSurface = Color(0xFFE2E9E6),
    surfaceVariant = Color(0xFF222A27),
    onSurfaceVariant = Color(0xFFB7C2BE),
    outline = Color(0xFF48534F),
    outlineVariant = Color(0xFF303936),
    error = Color(0xFFFFB4AC),
    errorContainer = Color(0xFF5A1A16)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 33.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
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
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
