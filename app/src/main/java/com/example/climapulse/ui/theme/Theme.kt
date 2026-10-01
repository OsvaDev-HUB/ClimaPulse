package com.example.climapulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.climapulse.R

private val ClimaPulseColors = darkColorScheme(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer,
    onSecondaryContainer = md_onSecondaryContainer,
    tertiary = md_tertiary,
    onTertiary = md_onTertiary,
    tertiaryContainer = md_tertiaryContainer,
    onTertiaryContainer = md_onTertiaryContainer,
    error = md_error,
    onError = md_onError,
    errorContainer = md_errorContainer,
    onErrorContainer = md_onErrorContainer,
    background = md_background,
    onBackground = md_onBackground,
    surface = md_surface,
    onSurface = md_onSurface,
    surfaceVariant = md_surfaceContainerHigh,
    onSurfaceVariant = md_onSurfaceVariant,
    surfaceContainerLowest = md_surfaceContainerLowest,
    surfaceContainerLow = md_surfaceContainerLow,
    surfaceContainer = md_surfaceContainer,
    surfaceContainerHigh = md_surfaceContainerHigh,
    surfaceContainerHighest = md_surfaceContainerHighest,
    outline = md_outline,
    outlineVariant = md_outlineVariant,
    inverseSurface = md_inverseSurface,
    inverseOnSurface = md_inverseOnSurface,
    inversePrimary = md_inversePrimary,
    scrim = NavyDeep
)

// Poppins para títulos (personalidad de marca); Roboto del sistema para el cuerpo (legibilidad).
private val Poppins = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private val base = Typography()

private val ClimaPulseTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
)

/** Estilo para cifras grandes (lecturas). */
val MetricValueStyle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 48.sp)

private val ClimaPulseShapes = Shapes(
    extraSmall = ClimaShapes.Small,
    small = ClimaShapes.Small,
    medium = ClimaShapes.Medium,
    large = ClimaShapes.Large,
    extraLarge = ClimaShapes.ExtraLarge
)

@Composable
fun ClimaPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClimaPulseColors,
        typography = ClimaPulseTypography,
        shapes = ClimaPulseShapes,
        content = content
    )
}
