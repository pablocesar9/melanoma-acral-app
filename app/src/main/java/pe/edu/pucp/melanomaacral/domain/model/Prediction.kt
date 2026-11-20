package pe.edu.pucp.melanomaacral.domain.model

// Salida del clasificador para una imagen (RF-04, RF-05).
data class Prediction(
    val probMelanoma: Float,     // probabilidad de melanoma acral, entre 0 y 1
    val isOutOfScope: Boolean,   // la imagen no parece acral, dermoscópica o melanocítica (RF-05)
    val modelVersion: String,    // trazabilidad del modelo (RNF-10)
    val threshold: Float,        // umbral de derivación usado en esta predicción (RNF-10)
    val isSimulated: Boolean,    // true si viene del clasificador falso (regla 6 del plan)
    // Puntaje del modelo (logit) y su umbral. La salida NO está calibrada como probabilidad (tesis, objetivo 2):
    // la app muestra el puntaje frente al umbral, no un porcentaje. null en evaluaciones anteriores a este cambio.
    val score: Float? = null,
    val scoreThreshold: Float? = null
) {
    // ¿El modelo sugiere melanoma? (por encima del umbral)
    val suggestsMelanoma: Boolean
        get() = if (score != null && scoreThreshold != null) score > scoreThreshold else probMelanoma >= threshold
}
