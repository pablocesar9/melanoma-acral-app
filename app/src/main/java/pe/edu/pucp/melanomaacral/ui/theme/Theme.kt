package pe.edu.pucp.melanomaacral.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

// Colores semánticos del tema actual: MaterialTheme.semantic.refer, .followUp, .neutral, .qualityOk, .simulated.
val MaterialTheme.semantic: SemanticColors
    @Composable @ReadOnlyComposable get() = LocalSemanticColors.current

// Decisión de diseño: sin color dinámico (Material You). La app se ve igual en todos los
// teléfonos y los colores de la recomendación no dependen del fondo de pantalla.
@Composable
fun MelanomaAcralTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSemanticColors provides if (darkTheme) DarkSemantic else LightSemantic) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
