package pe.edu.pucp.melanomaacral.domain.model

// Mapa de calor de las regiones de la imagen que influyeron en el resultado (RF-19).
// grid[fila][columna], valores de 0 (sin influencia) a 1 (máxima influencia).
// isSimulated = true: no refleja nada del modelo; sirve para construir la pantalla.
// Qué evidencia muestra el mapa. A favor de melanoma: Grad-CAM (cuando el modelo sugiere melanoma).
// En contra: ReLU del CAM con signo invertido (cuando no lo sugiere). Análisis XAI del 2026-11-17: Grad-CAM es fiel en los
// casos positivos, pero en los negativos solo pinta ruido; la evidencia en contra sí es fiel en ellos y cae sobre la lesión.
enum class ExplanationKind { FOR_MELANOMA, AGAINST_MELANOMA }

data class Explanation(
    val grid: List<List<Float>>,
    val isSimulated: Boolean,
    val kind: ExplanationKind = ExplanationKind.FOR_MELANOMA
) {
    val rows: Int get() = grid.size
    val columns: Int get() = grid.firstOrNull()?.size ?: 0
}
