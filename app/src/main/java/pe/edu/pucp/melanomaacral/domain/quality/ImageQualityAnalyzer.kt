package pe.edu.pucp.melanomaacral.domain.quality

import pe.edu.pucp.melanomaacral.domain.model.LightingProblem
import pe.edu.pucp.melanomaacral.domain.model.QualityReport

// Revisión de calidad determinista (RF-03). NO es IA: son tres medidas clásicas de imagen.
// Kotlin puro, sin Android, para probarla con pruebas unitarias.
//
// Los umbrales son PROVISIONALES: se eligieron con valores habituales de la literatura de
// visión por computador y deben calibrarse con imágenes dermoscópicas reales del dataset
// (fase de validación). Cada uno está documentado aquí para poder ajustarlo en un solo lugar.
object ImageQualityAnalyzer {

    // Lado mayor al que se reduce la imagen antes de medir. La nitidez depende de la escala:
    // los umbrales de abajo solo valen para este tamaño.
    const val ANALYSIS_SIDE_PX = 512

    // Resolución mínima: lado menor de la imagen original en píxeles. Por debajo, el detalle
    // dermoscópico (retículo, puntos, glóbulos) no se distingue y el modelo (entrada de
    // 224–384 px) recibiría una imagen ampliada.
    const val MIN_SHORT_SIDE_PX = 480

    // Nitidez: varianza del laplaciano sobre luma 0..255 a ANALYSIS_SIDE_PX. Escala continua (no binaria).
    // CALIBRADO el 2026-11-17 con 600 imágenes dermatoscópicas del dataset (200 por fuente, semilla 0, mismo cálculo que
    // aquí): mediana 16 en BALD, 267 en DERM12345 y 212 en MEL-KOR; percentil 2 del conjunto = 5. Con el valor anterior
    // (100, regla de Pech-Pacheco et al., 2000, pensada para fotos comunes) se marcaba como borrosa el 94 % de BALD y el
    // 22 % de las otras dos fuentes: la piel es lisa y tiene pocos bordes. Con 5, el 2 % de las imágenes del dataset.
    const val MIN_SHARPNESS = 5f

    // Nitidez tras suavizar el ruido (promedio 3x3 antes del laplaciano). El ruido de una foto con
    // poca luz sube la varianza y hace pasar por nítida una imagen borrosa; al suavizarlo solo
    // quedan los bordes reales. CALIBRADO con las mismas 600 imágenes: percentil 2 = 1,9 (antes 10, que marcaba
    // como borrosas a muchas imágenes válidas). Una foto muy borrosa medida en el teléfono dio ~1.
    const val MIN_SHARPNESS_DENOISED = 1.9f

    // Exposición: brillo medio aceptable (0..255) y fracción máxima de píxeles
    // casi negros (<= DARK_LEVEL) o casi blancos (>= BRIGHT_LEVEL).
    // Subido de 50 a 80 el 2026-11-14: la cámara del teléfono sube sola la exposición y una escena
    // con poca luz sale con brillo ~40-60; con 50 pasaban fotos oscuras. PROVISIONAL.
    const val MIN_MEAN_LUMA = 80f
    const val MAX_MEAN_LUMA = 200f
    const val DARK_LEVEL = 10
    const val BRIGHT_LEVEL = 245
    const val MAX_CLIPPED_FRACTION = 0.25f

    // Encuadre: el cuarto central de la imagen debe tener contenido (desviación estándar del
    // brillo >= MIN_CENTER_STDDEV). Un campo uniforme (por ejemplo, el dermoscopio sin apoyar
    // en la piel o la lente tapada) no tiene lesión que evaluar.
    const val MIN_CENTER_STDDEV = 8f

    // luma: brillo 0..255 por píxel, fila por fila (width * height elementos).
    // originalWidth/originalHeight: tamaño de la imagen antes de reducirla.
    fun analyze(
        luma: IntArray,
        width: Int,
        height: Int,
        originalWidth: Int = width,
        originalHeight: Int = height,
        checkFraming: Boolean = true   // false para la foto clínica de contexto (no hay una lesión centrada)
    ): QualityReport {
        require(width > 0 && height > 0 && luma.size == width * height) { "Dimensiones inconsistentes" }

        val sharpness = laplacianVariance(luma, width, height)
        val sharpnessDenoised = laplacianVariance(boxBlur3(luma, width, height), width, height)
        val mean = luma.average().toFloat()
        val dark = luma.count { it <= DARK_LEVEL }.toFloat() / luma.size
        val bright = luma.count { it >= BRIGHT_LEVEL }.toFloat() / luma.size
        val centerStd = centerStdDev(luma, width, height)

        val problem = when {
            mean < MIN_MEAN_LUMA || dark > MAX_CLIPPED_FRACTION -> LightingProblem.TOO_DARK
            mean > MAX_MEAN_LUMA || bright > MAX_CLIPPED_FRACTION -> LightingProblem.TOO_BRIGHT
            else -> LightingProblem.NONE
        }
        val resolutionOk = minOf(originalWidth, originalHeight) >= MIN_SHORT_SIDE_PX

        return QualityReport(
            focusOk = sharpness >= MIN_SHARPNESS && sharpnessDenoised >= MIN_SHARPNESS_DENOISED,
            lightingOk = problem == LightingProblem.NONE,
            framingOk = !checkFraming || centerStd >= MIN_CENTER_STDDEV,
            isSimulated = false,
            isHeuristic = true,
            lightingProblem = problem,
            resolutionOk = resolutionOk,
            sharpness = sharpness,
            meanLuma = mean,
            sharpnessDenoised = sharpnessDenoised
        )
    }

    // Promedio 3x3 (los bordes de la imagen se copian). Suaviza el ruido antes de medir nitidez.
    fun boxBlur3(luma: IntArray, width: Int, height: Int): IntArray {
        if (width < 3 || height < 3) return luma
        val out = luma.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0
                for (dy in -1..1) for (dx in -1..1) sum += luma[(y + dy) * width + (x + dx)]
                out[y * width + x] = sum / 9
            }
        }
        return out
    }

    // Varianza del laplaciano de 4 vecinos sobre los píxeles interiores.
    fun laplacianVariance(luma: IntArray, width: Int, height: Int): Float {
        if (width < 3 || height < 3) return 0f
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val lap = (luma[i - 1] + luma[i + 1] + luma[i - width] + luma[i + width] - 4 * luma[i]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        val mean = sum / n
        return (sumSq / n - mean * mean).toFloat()
    }

    private fun centerStdDev(luma: IntArray, width: Int, height: Int): Float {
        val x0 = width / 4
        val x1 = width - width / 4
        val y0 = height / 4
        val y1 = height - height / 4
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val v = luma[y * width + x].toDouble()
                sum += v
                sumSq += v * v
                n++
            }
        }
        if (n == 0) return 0f
        val mean = sum / n
        return kotlin.math.sqrt(maxOf(0.0, sumSq / n - mean * mean)).toFloat()
    }
}
