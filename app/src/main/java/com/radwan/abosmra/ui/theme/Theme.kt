package com.radwan.abosmra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GasGreen = Color(0xFF0F6657)
val GasGreenDark = Color(0xFF08483E)
val GasCream = Color(0xFFFFFDF6)
val GasOrange = Color(0xFFF59E0B)
val DebtRed = Color(0xFFB42318)
val PaidGreen = Color(0xFF17803D)
val SoftSurface = Color(0xFFF4F7F5)

private val LightColors = lightColorScheme(
    primary = GasGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F1E9),
    onPrimaryContainer = GasGreenDark,
    secondary = GasOrange,
    onSecondary = Color(0xFF3E2A00),
    background = GasCream,
    onBackground = Color(0xFF18201E),
    surface = Color.White,
    onSurface = Color(0xFF18201E),
    surfaceVariant = SoftSurface,
    error = DebtRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD8C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF075345),
    secondary = Color(0xFFFFC66A),
    background = Color(0xFF101614),
    surface = Color(0xFF18201E),
    surfaceVariant = Color(0xFF26312E),
    error = Color(0xFFFFB4AB)
)

@Composable
fun GasLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
