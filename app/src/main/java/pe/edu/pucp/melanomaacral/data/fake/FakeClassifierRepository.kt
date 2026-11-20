package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.delay
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.repository.ClassifierRepository

// Clasificador de prueba: devuelve siempre la misma predicción, marcada como simulada.
// Sirve para construir y mostrar las pantallas antes de tener el modelo real.
class FakeClassifierRepository(
    private val probability: Float = 0.18f,
    private val outOfScope: Boolean = false,
    private val delayMillis: Long = 800
) : ClassifierRepository {

    override suspend fun classify(image: LesionImage): Prediction {
        delay(delayMillis)   // simula el tiempo de inferencia para ver el estado de carga
        return Prediction(
            probMelanoma = probability,
            isOutOfScope = outOfScope,
            modelVersion = "simulado-0",
            threshold = 0.30f,   // valor de ejemplo
            isSimulated = true,
            score = logit(probability),
            scoreThreshold = logit(0.30f)
        )
    }

    private fun logit(p: Float): Float = kotlin.math.ln(p / (1 - p))
}
