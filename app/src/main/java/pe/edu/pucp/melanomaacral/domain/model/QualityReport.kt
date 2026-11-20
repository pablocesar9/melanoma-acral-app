package pe.edu.pucp.melanomaacral.domain.model

import kotlinx.serialization.Serializable

// Problema de iluminación detectado (RF-03: decir la causa del rechazo).
enum class LightingProblem { NONE, TOO_DARK, TOO_BRIGHT }

// Resultado de la revisión de calidad de una imagen (RF-03).
// isSimulated = true: viene del repositorio falso. isHeuristic = true: viene de la revisión
// determinista (nitidez, exposición, resolución); no es un modelo de IA.
@Serializable
data class QualityReport(
    val focusOk: Boolean,
    val lightingOk: Boolean,
    val framingOk: Boolean,
    val isSimulated: Boolean,
    val isHeuristic: Boolean = false,
    val lightingProblem: LightingProblem = LightingProblem.NONE,
    val resolutionOk: Boolean = true,
    val sharpness: Float? = null,   // varianza del laplaciano
    val meanLuma: Float? = null,    // brillo medio, 0..255
    val sharpnessDenoised: Float? = null   // varianza del laplaciano tras suavizar el ruido
) {
    val allOk: Boolean
        get() = focusOk && lightingOk && framingOk && resolutionOk
}
