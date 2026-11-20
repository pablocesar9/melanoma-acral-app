package pe.edu.pucp.melanomaacral.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Escala tipográfica de Material 3 con Roboto (fuente del sistema, sin descargas).
// Ajuste propio: el texto pequeño de lectura (bodySmall, labelMedium) sube a 14 sp
// para cumplir el criterio de «texto de al menos 14 sp» del proyecto.
private val Roboto = FontFamily.Default

private fun style(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Roboto, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp
)

val AppTypography = Typography(
    displayLarge = style(FontWeight.Normal, 57, 64, -0.25),
    displayMedium = style(FontWeight.Normal, 45, 52),
    displaySmall = style(FontWeight.Normal, 36, 44),
    headlineLarge = style(FontWeight.SemiBold, 32, 40),
    headlineMedium = style(FontWeight.SemiBold, 28, 36),
    headlineSmall = style(FontWeight.SemiBold, 24, 32),
    titleLarge = style(FontWeight.SemiBold, 22, 28),
    titleMedium = style(FontWeight.SemiBold, 16, 24, 0.15),
    titleSmall = style(FontWeight.SemiBold, 14, 20, 0.1),
    bodyLarge = style(FontWeight.Normal, 16, 24, 0.5),
    bodyMedium = style(FontWeight.Normal, 14, 20, 0.25),
    bodySmall = style(FontWeight.Normal, 14, 20, 0.25),
    labelLarge = style(FontWeight.Medium, 14, 20, 0.1),
    labelMedium = style(FontWeight.Medium, 14, 20, 0.3),
    labelSmall = style(FontWeight.Medium, 12, 16, 0.5)   // reservado; la app no lo usa para texto de lectura
)
