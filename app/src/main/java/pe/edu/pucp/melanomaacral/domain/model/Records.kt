package pe.edu.pucp.melanomaacral.domain.model

import java.time.LocalDate

// Paciente identificado solo por un ID anónimo asignado por el establecimiento (RF-10).
data class Patient(val id: String, val createdOn: LocalDate)

// Lesión en seguimiento (RF-11). Su localización se toma de la evaluación más reciente.
data class Lesion(val id: String, val patientId: String, val nextFollowUp: LocalDate?)

// Una captura analizada de una lesión, con todo lo necesario para trazarla (RF-11, RNF-10).
data class Evaluation(
    val id: String,
    val lesionId: String,
    val date: LocalDate,
    val images: CapturedImages,
    val clinicalData: ClinicalData,
    val prediction: Prediction,
    val recommendation: Recommendation,
    val editedAtMillis: Long? = null   // si se corrigió después de guardarla: cuándo
)

// Evaluaciones de una lesión, de la más antigua a la más reciente.
// Si dos tienen la misma fecha, decide el ID (se asignan en orden creciente).
fun List<Evaluation>.forLesion(lesionId: String): List<Evaluation> =
    filter { it.lesionId == lesionId }.sortedWith(compareBy({ it.date }, { it.id }))
