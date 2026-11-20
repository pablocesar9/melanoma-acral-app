package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import java.time.LocalDate

// Registro en memoria: se pierde al cerrar la app. Empieza con datos de ejemplo
// para poder recorrer todas las pantallas. Se reemplaza por Room en la fase 5.
class InMemoryRecordsRepository(today: LocalDate = LocalDate.now()) : RecordsRepository {

    private val _patients = MutableStateFlow(SampleRecords.patients(today))
    private val _lesions = MutableStateFlow(SampleRecords.lesions(today))
    private val _evaluations = MutableStateFlow(SampleRecords.evaluations(today))

    override val patients: Flow<List<Patient>> = _patients
    override val lesions: Flow<List<Lesion>> = _lesions
    override val evaluations: Flow<List<Evaluation>> = _evaluations

    override suspend fun createPatient(patientId: String) {
        _patients.update { it + Patient(id = patientId, createdOn = LocalDate.now()) }
    }

    override suspend fun createLesion(patientId: String): String {
        val id = "LES-" + (_lesions.value.size + 1).toString().padStart(3, '0')
        _lesions.update { it + Lesion(id = id, patientId = patientId, nextFollowUp = null) }
        return id
    }

    override suspend fun addEvaluation(
        lesionId: String,
        date: LocalDate,
        images: CapturedImages,
        clinicalData: ClinicalData,
        prediction: Prediction,
        recommendation: Recommendation
    ): String {
        val id = "EV-" + (_evaluations.value.size + 1).toString().padStart(3, '0')
        _evaluations.update {
            it + Evaluation(id, lesionId, date, images, clinicalData, prediction, recommendation)
        }
        return id
    }

    override suspend fun replaceEvaluation(
        evaluationId: String,
        images: CapturedImages,
        clinicalData: ClinicalData,
        prediction: Prediction,
        recommendation: Recommendation
    ) {
        _evaluations.update { list ->
            list.map {
                if (it.id == evaluationId) it.copy(
                    images = images, clinicalData = clinicalData, prediction = prediction,
                    recommendation = recommendation, editedAtMillis = System.currentTimeMillis()
                ) else it
            }
        }
    }

    override suspend fun scheduleFollowUp(lesionId: String, date: LocalDate) {
        _lesions.update { list ->
            list.map { if (it.id == lesionId) it.copy(nextFollowUp = date) else it }
        }
    }
}
