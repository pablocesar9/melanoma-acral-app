package pe.edu.pucp.melanomaacral.domain.model

// Plazos de control (RF-14): corto plazo de 3 meses; largo plazo de 6 a 12 meses [2].
enum class FollowUpInterval(val months: Long) {
    SHORT_3_MONTHS(3),
    LONG_6_MONTHS(6),
    LONG_12_MONTHS(12)
}
