package pe.edu.pucp.melanomaacral.domain.rules

// Validación de los datos clínicos que se escriben a mano (RF-07).
object ClinicalDataValidation {

    // Límite para atrapar errores de tipeo (por ejemplo, "55" en lugar de "5,5").
    const val MAX_DIAMETER_MM = 100f

    const val MAX_AGE_YEARS = 120

    // Edad en años enteros (0 a 120), o null si el texto no es válido.
    fun parseAge(text: String): Int? {
        val value = text.trim().toIntOrNull() ?: return null
        return if (value in 0..MAX_AGE_YEARS) value else null
    }

    // Convierte el texto ingresado en un diámetro válido, o devuelve null si no lo es.
    // Acepta coma o punto decimal ("5,5" o "5.5").
    fun parseDiameter(text: String): Float? {
        val value = text.trim().replace(',', '.').toFloatOrNull() ?: return null
        return if (value > 0f && value <= MAX_DIAMETER_MM) value else null
    }
}
