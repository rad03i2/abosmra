package com.radwan.abosmra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radwan.abosmra.R

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

private val CairoFontFamily = FontFamily(
    Font(R.font.cairo_variable, weight = FontWeight.ExtraLight),
    Font(R.font.cairo_variable, weight = FontWeight.Light),
    Font(R.font.cairo_variable, weight = FontWeight.Normal),
    Font(R.font.cairo_variable, weight = FontWeight.Medium),
    Font(R.font.cairo_variable, weight = FontWeight.SemiBold),
    Font(R.font.cairo_variable, weight = FontWeight.Bold),
    Font(R.font.cairo_variable, weight = FontWeight.ExtraBold)
)

private val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal_regular, weight = FontWeight.Light),
    Font(R.font.tajawal_regular, weight = FontWeight.Normal),
    Font(R.font.tajawal_medium, weight = FontWeight.Medium),
    Font(R.font.tajawal_medium, weight = FontWeight.SemiBold),
    Font(R.font.tajawal_bold, weight = FontWeight.Bold),
    Font(R.font.tajawal_bold, weight = FontWeight.ExtraBold)
)

private val NotoSansArabicFontFamily = FontFamily(
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.Light),
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.Normal),
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.Medium),
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.SemiBold),
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.Bold),
    Font(R.font.noto_sans_arabic_variable, weight = FontWeight.ExtraBold)
)

private val NotoKufiArabicFontFamily = FontFamily(
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.Light),
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.Normal),
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.Medium),
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.SemiBold),
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.Bold),
    Font(R.font.noto_kufi_arabic_variable, weight = FontWeight.ExtraBold)
)

internal fun fontFamilyFor(font: ArabicFontPreset): FontFamily =
    when (font) {
        ArabicFontPreset.CAIRO -> CairoFontFamily
        ArabicFontPreset.TAJAWAL -> TajawalFontFamily
        ArabicFontPreset.NOTO_SANS_ARABIC -> NotoSansArabicFontFamily
        ArabicFontPreset.NOTO_KUFI_ARABIC -> NotoKufiArabicFontFamily
    }

private fun appTypography(settings: TypographySettings): Typography {
    val family = fontFamilyFor(settings.font)
    val scale = TypographySettingsStore.sanitizeScale(settings.scale)

    fun style(
        weight: FontWeight,
        size: Float,
        lineHeight: Float
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (lineHeight * scale).sp
    )

    return Typography(
        headlineLarge = style(FontWeight.Bold, 36f, 45f),
        headlineMedium = style(FontWeight.Bold, 28f, 37f),
        headlineSmall = style(FontWeight.Bold, 23f, 32f),
        titleLarge = style(FontWeight.Bold, 21f, 30f),
        titleMedium = style(FontWeight.SemiBold, 17f, 25f),
        titleSmall = style(FontWeight.SemiBold, 15f, 22f),
        bodyLarge = style(FontWeight.Normal, 17f, 28f),
        bodyMedium = style(FontWeight.Normal, 15f, 24f),
        bodySmall = style(FontWeight.Normal, 13f, 20f),
        labelLarge = style(FontWeight.SemiBold, 15f, 22f),
        labelMedium = style(FontWeight.Medium, 13f, 19f)
    )
}

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
    typographySettings: TypographySettings = TypographySettings(),
    content: @Composable () -> Unit
) {
    val typography = remember(
        typographySettings.font,
        typographySettings.scale
    ) {
        appTypography(typographySettings)
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}
