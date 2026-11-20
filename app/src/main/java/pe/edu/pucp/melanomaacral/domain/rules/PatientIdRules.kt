package pe.edu.pucp.melanomaacral.domain.rules

enum class PatientIdError { BLANK, TOO_LONG, LOOKS_LIKE_DNI, DUPLICATE }

// RF-10 y RNF-06: el paciente se registra con un ID anónimo asignado por el establecimiento.
// Se rechaza lo que parece un DNI (8 dígitos) para no guardar un documento de identidad.
object PatientIdRules {

    const val MAX_LENGTH = 20

    private val dniPattern = Regex("""\d{8}""")

    // Devuelve el error, o null si el ID es válido.
    fun validate(id: String, existingIds: Collection<String>): PatientIdError? {
        val trimmed = id.trim()
        return when {
            trimmed.isEmpty() -> PatientIdError.BLANK
            trimmed.length > MAX_LENGTH -> PatientIdError.TOO_LONG
            dniPattern.matches(trimmed) -> PatientIdError.LOOKS_LIKE_DNI
            trimmed in existingIds -> PatientIdError.DUPLICATE
            else -> null
        }
    }
}
