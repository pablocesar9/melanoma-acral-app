package pe.edu.pucp.melanomaacral.domain.model

import kotlinx.serialization.Serializable

// Localización acral de la lesión (RF-06).
enum class AcralLocation { PALM, SOLE, DIGIT, NAIL }

// ¿El paciente reporta que la lesión cambió? (RF-16)
enum class ReportedChange { YES, NO, UNKNOWN }

// Tiempo de evolución de la lesión (RF-16). Categorías provisionales: validar en la fase 3.
enum class EvolutionTime { LESS_THAN_3_MONTHS, FROM_3_TO_12_MONTHS, MORE_THAN_12_MONTHS, UNKNOWN }

// Sexo del paciente (RF-16).
enum class Sex { FEMALE, MALE, UNSPECIFIED }

// Datos clínicos mínimos de una evaluación (RF-06, RF-07, RF-09, RF-16).
// @Serializable: viaja dentro de la ruta de navegación de P-07 a P-08.
@Serializable
data class ClinicalData(
    val location: AcralLocation,
    val maxDiameterMm: Float,
    val isElevatedOrPalpable: Boolean,
    // RF-09: lesión que no cura o que crece rápido (criterios de derivación del primer nivel).
    val isNotHealing: Boolean = false,
    val isGrowingRapidly: Boolean = false,
    val hadPreviousProcedure: Boolean,
    val reportedChange: ReportedChange,
    val evolutionTime: EvolutionTime,
    // RF-16 (completan los datos mínimos). Con valor por defecto para los datos anteriores a este cambio.
    val ageYears: Int? = null,
    val sex: Sex? = null,
    val hasSkinCancerHistory: Boolean = false,   // antecedente personal o familiar de cáncer de piel
    val hasBleeding: Boolean = false,
    val hasPain: Boolean = false,
    val hasItching: Boolean = false,
    // Anotaciones libres del profesional (opcional).
    val notes: String = ""
)
