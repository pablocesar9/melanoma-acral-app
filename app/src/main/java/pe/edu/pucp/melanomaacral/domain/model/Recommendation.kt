package pe.edu.pucp.melanomaacral.domain.model

// Acción de manejo recomendada (RF-08): dos niveles, derivar o control. No hay «sin acción» por el resultado del
// modelo: haría falta un segundo umbral que el modelo no tiene, y en el primer nivel es más prudente indicar control.
// NONE solo se usa cuando la imagen está fuera del alcance del modelo (RF-05): no se recomienda nada.
enum class ManagementAction {
    REFER,
    FOLLOW_UP,
    NONE
}

// Por qué se recomendó esa acción. La pantalla traduce cada motivo a un texto.
enum class RecommendationReason {
    OUT_OF_SCOPE,            // RF-05: la imagen está fuera del alcance del modelo; no se recomienda nada
    NOT_APT_FOR_FOLLOW_UP,   // RF-09: lesión elevada, palpable, nodular o azul
    ABOVE_THRESHOLD,         // RF-08: probabilidad mayor o igual al umbral
    DIAMETER_OVER_LIMIT,     // RF-07: diámetro mayor a 7 mm (algoritmo de 3 pasos)
    SMALL_BELOW_THRESHOLD    // RF-07: lesión pequeña con probabilidad bajo el umbral
}

data class Recommendation(
    val action: ManagementAction,
    val reason: RecommendationReason,
    val followUpAllowed: Boolean   // false bloquea "Programar control" (RF-09)
)
