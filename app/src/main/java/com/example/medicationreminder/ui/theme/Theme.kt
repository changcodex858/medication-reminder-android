package com.example.medicationreminder.ui.theme

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

val BrandTerracotta = Color(0xFFC45B43)
val BrandTerracottaDeep = Color(0xFF813D31)
val BrandSage = Color(0xFF48685D)
val BrandSageDeep = Color(0xFF183B35)
val BrandInk = Color(0xFF202824)
val BrandMuted = Color(0xFF59635D)
val BrandPearl = Color(0xFFF7F5F1)
val BrandIvory = Color(0xFFFFFDF9)
val BrandLine = Color(0xFFE5E2DC)
val BrandAmber = Color(0xFFB66A16)

private val LightColors = lightColorScheme(
    primary = BrandTerracottaDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7DED6),
    onPrimaryContainer = Color(0xFF4E2118),
    secondary = BrandSage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE9E3),
    onSecondaryContainer = Color(0xFF18342D),
    tertiary = Color(0xFF79633E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E4C8),
    onTertiaryContainer = Color(0xFF44330F),
    background = BrandPearl,
    onBackground = BrandInk,
    surface = BrandIvory,
    onSurface = BrandInk,
    surfaceVariant = Color(0xFFEFEEE9),
    onSurfaceVariant = BrandMuted,
    outline = Color(0xFF8D958F),
    outlineVariant = BrandLine,
    error = Color(0xFFA23C32),
    onError = Color.White,
    errorContainer = Color(0xFFF8DEDA),
    onErrorContainer = Color(0xFF541D17),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A0),
    onPrimary = Color(0xFF5E1609),
    primaryContainer = Color(0xFF7D2F20),
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = Color(0xFFB5CCC2),
    onSecondary = Color(0xFF20372F),
    secondaryContainer = Color(0xFF354E45),
    onSecondaryContainer = Color(0xFFD1E8DE),
    tertiary = Color(0xFFDBC28F),
    onTertiary = Color(0xFF3C2F0C),
    tertiaryContainer = Color(0xFF554619),
    onTertiaryContainer = Color(0xFFF7DEA8),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE2EAE5),
    surface = Color(0xFF18201C),
    onSurface = Color(0xFFE2EAE5),
    surfaceVariant = Color(0xFF29322E),
    onSurfaceVariant = Color(0xFFBCC6C0),
    outline = Color(0xFF87918B),
    outlineVariant = Color(0xFF3E4944),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val PremiumTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5f).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3f).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 31.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 29.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1f.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2f.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.3f.sp,
    ),
)

@Composable
fun MedicationReminderTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PremiumTypography,
        shapes = PremiumShapes,
        content = content,
    )
}
