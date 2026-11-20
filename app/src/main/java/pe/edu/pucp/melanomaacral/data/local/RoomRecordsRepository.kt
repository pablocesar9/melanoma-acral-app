package pe.edu.pucp.melanomaacral.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import java.time.LocalDate

// Registro persistente (reemplaza a InMemoryRecordsRepository). Funciona sin conexión (RNF-01).
class RoomRecordsRepository(private val dao: RecordsDao) : RecordsRepository {

    override val patients: Flow<List<Patient>> = dao.observePatients().map { l -> l.map { it.toDomain() } }
    override val lesions: Flow<List<Lesion>> = dao.observeLesions().map { l -> l.map { it.toDomain() } }
    override val evaluations: Flow<List<Evaluation>> = dao.observeEvaluations().map { l -> l.map { it.toDomain() } }

    override suspend fun createPatient(patientId: String) {
        dao.insertPatient(PatientEntity(patientId, LocalDate.now().toEpochDay()))
    }

    override suspend fun createLesion(patientId: String): String {
        val id = "LES-" + (dao.lesionCount() + 1).toString().padStart(3, '0')
        dao.insertLesion(LesionEntity(id, patientId, nextFollowUpEpochDay = null))
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
        val id = "EV-" + (dao.evaluationCount() + 1).toString().padStart(3, '0')
        dao.insertEvaluation(
            Evaluation(id, lesionId, date, images, clinicalData, prediction, recommendation).toEntity()
        )
        return id
    }

    override suspend fun replaceEvaluation(
        evaluationId: String,
        images: CapturedImages,
        clinicalData: ClinicalData,
        prediction: Prediction,
        recommendation: Recommendation
    ) {
        val old = dao.evaluation(evaluationId)?.toDomain() ?: error("No existe la evaluación $evaluationId")
        dao.updateEvaluation(
            old.copy(
                images = images, clinicalData = clinicalData, prediction = prediction,
                recommendation = recommendation, editedAtMillis = System.currentTimeMillis()
            ).toEntity()
        )
    }

    override suspend fun scheduleFollowUp(lesionId: String, date: LocalDate) {
        dao.setNextFollowUp(lesionId, date.toEpochDay())
    }
}
