package pe.edu.pucp.melanomaacral.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import java.time.LocalDate

// Registro de pacientes, lesiones y evaluaciones (RF-10, RF-11, RF-14).
// Fase 2: implementación en memoria (data/fake). Fase 5: base de datos local con Room.
// Los Flow avisan a las pantallas cada vez que los datos cambian.
interface RecordsRepository {
    val patients: Flow<List<Patient>>
    val lesions: Flow<List<Lesion>>
    val evaluations: Flow<List<Evaluation>>

    suspend fun createPatient(patientId: String)

    // Corrige el código de un paciente (RF-10): sus lesiones y su consentimiento pasan al código nuevo, y se conserva su
    // fecha de alta. La validación del código nuevo la hace quien llama.
    suspend fun renamePatient(oldId: String, newId: String) {
        throw UnsupportedOperationException("Este registro no permite corregir el código del paciente")
    }

    // Devuelve el ID de la lesión creada.
    suspend fun createLesion(patientId: String): String

    // Devuelve el ID de la evaluación guardada.
    suspend fun addEvaluation(
        lesionId: String,
        date: LocalDate,
        images: CapturedImages,
        clinicalData: ClinicalData,
        prediction: Prediction,
        recommendation: Recommendation
    ): String

    // Edita una evaluación ya guardada (mismo ID y fecha); queda marcada con la hora de la edición.
    suspend fun replaceEvaluation(
        evaluationId: String,
        images: CapturedImages,
        clinicalData: ClinicalData,
        prediction: Prediction,
        recommendation: Recommendation
    )

    suspend fun scheduleFollowUp(lesionId: String, date: LocalDate)

    // true si alguna evaluación guardada usa esta imagen. Antes de borrar una foto (al reemplazarla o al descartar un
    // borrador) hay que comprobarlo: al editar una evaluación, el borrador usa las mismas fotos de la evaluación
    // guardada, y un control puede reutilizar la foto clínica de otra evaluación.
    suspend fun isImageSaved(uri: String): Boolean = evaluations.first().any { e ->
        e.images.dermoscopic.uri == uri || e.images.context?.uri == uri || e.images.closeUp?.uri == uri
    }
}
