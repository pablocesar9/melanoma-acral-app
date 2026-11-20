package pe.edu.pucp.melanomaacral.ui

import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.FollowUpInterval
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex
import pe.edu.pucp.melanomaacral.domain.rules.ClinicalDataValidation
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdRules
import pe.edu.pucp.melanomaacral.domain.rules.RecommendationRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Textos que la interfaz muestra para cada valor del dominio.
// Están en un solo lugar para reutilizarlos y, más adelante, moverlos a strings.xml.

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun LocalDate.formatted(): String = format(dateFormatter)

fun Float.toPercent(): Int = (this * 100).roundToInt()

fun Boolean.yesNo(): String = if (this) "Sí" else "No"

fun AcralLocation.label(): String = when (this) {
    AcralLocation.PALM -> "Palma"
    AcralLocation.SOLE -> "Planta"
    AcralLocation.DIGIT -> "Dedo"
    AcralLocation.NAIL -> "Uña"
}

fun Sex.label(): String = when (this) {
    Sex.FEMALE -> "Mujer"
    Sex.MALE -> "Hombre"
    Sex.UNSPECIFIED -> "No especifica"
}

fun ReportedChange.label(): String = when (this) {
    ReportedChange.YES -> "Sí"
    ReportedChange.NO -> "No"
    ReportedChange.UNKNOWN -> "No sabe"
}

fun EvolutionTime.label(): String = when (this) {
    EvolutionTime.LESS_THAN_3_MONTHS -> "Menos de 3 meses"
    EvolutionTime.FROM_3_TO_12_MONTHS -> "De 3 a 12 meses"
    EvolutionTime.MORE_THAN_12_MONTHS -> "Más de 12 meses"
    EvolutionTime.UNKNOWN -> "Desconocido"
}

fun FollowUpInterval.label(): String = when (this) {
    FollowUpInterval.SHORT_3_MONTHS -> "3 meses (corto plazo)"
    FollowUpInterval.LONG_6_MONTHS -> "6 meses (largo plazo)"
    FollowUpInterval.LONG_12_MONTHS -> "12 meses (largo plazo)"
}

fun ManagementAction.label(): String = when (this) {
    ManagementAction.REFER -> "Derivar al especialista"
    ManagementAction.FOLLOW_UP -> "Programar control"
    ManagementAction.NONE -> "Sin recomendación"
}

fun RecommendationReason.description(): String {
    val limit = RecommendationRules.DIAMETER_LIMIT_MM.roundToInt()
    return when (this) {
        RecommendationReason.OUT_OF_SCOPE ->
            "La imagen está fuera del alcance del modelo, así que no se puede evaluar y no hay recomendación."
        RecommendationReason.NOT_APT_FOR_FOLLOW_UP ->
            "La lesión cumple un criterio de derivación (cambio de aspecto o elevación, síntomas, sangrado, lesión que no cura o que crece rápido): no se monitorea, se deriva."
        RecommendationReason.ABOVE_THRESHOLD ->
            "La probabilidad del modelo alcanza el nivel en que se recomienda derivar."
        RecommendationReason.DIAMETER_OVER_LIMIT ->
            "El diámetro supera los $limit mm (algoritmo de 3 pasos)."
        RecommendationReason.SMALL_BELOW_THRESHOLD ->
            "Lesión de hasta $limit mm con probabilidad baja del modelo: seguimiento clínico y dermoscópico."
    }
}

fun PatientIdError.message(): String = when (this) {
    PatientIdError.BLANK -> "Ingresa el código del paciente."
    PatientIdError.TOO_LONG -> "Máximo ${PatientIdRules.MAX_LENGTH} caracteres."
    PatientIdError.LOOKS_LIKE_DNI -> "Parece un DNI. Usa el código del establecimiento (por ejemplo, la historia clínica), no un documento."
    PatientIdError.DUPLICATE -> "Ya existe un paciente con ese ID."
}

fun diameterErrorMessage(): String =
    "Ingresa un número mayor que 0 y hasta ${ClinicalDataValidation.MAX_DIAMETER_MM.roundToInt()}."
