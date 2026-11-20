package pe.edu.pucp.melanomaacral.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta propia, de tono clínico y sobrio. Semilla #1F5F7A (azul petróleo).
// Valores escritos a mano al estilo de Material Theme Builder; las razones de contraste
// de cada pareja están en docs/r3/sistema-diseno.md (script docs/r3/herramientas/contraste.py).
// El color dinámico (Material You) está desactivado a propósito: los colores de la
// recomendación no pueden depender del fondo de pantalla de cada usuario.

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F5F7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4E7FF),
    onPrimaryContainer = Color(0xFF001E2C),
    inversePrimary = Color(0xFF8BCFF0),
    secondary = Color(0xFF4E616D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E5F4),
    onSecondaryContainer = Color(0xFF0A1E28),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9E),
    onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFF6FAFD),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484D),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEDF1F5),
    surfaceTint = Color(0xFF1F5F7A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FAFD),
    surfaceDim = Color(0xFFD7DBDE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDFE3E7)
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8BCFF0),
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFC4E7FF),
    inversePrimary = Color(0xFF1F5F7A),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333E),
    secondaryContainer = Color(0xFF364956),
    onSecondaryContainer = Color(0xFFD1E5F4),
    tertiary = Color(0xFFF0BF48),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFE0E3E6),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C8CD),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484D),
    inverseSurface = Color(0xFFE0E3E6),
    inverseOnSurface = Color(0xFF2C3135),
    surfaceTint = Color(0xFF8BCFF0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A3D),
    surfaceDim = Color(0xFF0F1417),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171C20),
    surfaceContainer = Color(0xFF1B2024),
    surfaceContainerHigh = Color(0xFF252B2E),
    surfaceContainerHighest = Color(0xFF303639)
)

// Colores semánticos de la app. Cada estado lleva siempre icono y texto, nunca solo color (WCAG 1.4.1).
// accent: para iconos y texto sobre el fondo; container/onContainer: para tarjetas.
class StatusColors(val accent: Color, val container: Color, val onContainer: Color)

class SemanticColors(
    val refer: StatusColors,       // derivar
    val followUp: StatusColors,    // control
    val neutral: StatusColors,     // estado neutro (gris azulado), p. ej. «Próximo control»
    val qualityOk: StatusColors,   // calidad de imagen adecuada (el verde es solo para esto)
    val simulated: StatusColors    // aviso SIMULADO
)

internal val LightSemantic = SemanticColors(
    refer = StatusColors(Color(0xFFB3261E), Color(0xFFF9DEDC), Color(0xFF410E0B)),
    followUp = StatusColors(Color(0xFF7A5900), Color(0xFFFFDF9E), Color(0xFF261A00)),
    neutral = StatusColors(Color(0xFF4A5A68), Color(0xFFDDE3EA), Color(0xFF161C22)),
    qualityOk = StatusColors(Color(0xFF1E6B3A), Color(0xFFC8F0D0), Color(0xFF00210C)),
    simulated = StatusColors(Color(0xFF7A5900), Color(0xFFFFF1B8), Color(0xFF3B2F00))
)

internal val DarkSemantic = SemanticColors(
    refer = StatusColors(Color(0xFFF2B8B5), Color(0xFF8C1D18), Color(0xFFF9DEDC)),
    followUp = StatusColors(Color(0xFFF0BF48), Color(0xFF5A4300), Color(0xFFFFDF9E)),
    neutral = StatusColors(Color(0xFFB4C4D2), Color(0xFF2F3A44), Color(0xFFDDE3EA)),
    qualityOk = StatusColors(Color(0xFF7ED69A), Color(0xFF0F5129), Color(0xFFC8F0D0)),
    simulated = StatusColors(Color(0xFFF0D060), Color(0xFF4D3E00), Color(0xFFFFF1B8))
)
