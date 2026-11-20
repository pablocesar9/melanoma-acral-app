package pe.edu.pucp.melanomaacral.data.fake

import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import java.time.LocalDate

// Datos de ejemplo para recorrer la app. Todas las predicciones están marcadas como simuladas.
// Las fechas se calculan desde "hoy" para que siempre haya un control vencido y uno futuro.
object SampleRecords {

    fun patients(today: LocalDate): List<Patient> = listOf(
        Patient(id = "PAC-001", createdOn = today.minusMonths(7)),
        Patient(id = "PAC-002", createdOn = today.minusMonths(1))
    )

    fun lesions(today: LocalDate): List<Lesion> = listOf(
        Lesion(id = "LES-001", patientId = "PAC-001", nextFollowUp = today.minusDays(12)),  // vencido
        Lesion(id = "LES-002", patientId = "PAC-002", nextFollowUp = today.plusMonths(2))
    )

    fun evaluations(today: LocalDate): List<Evaluation> {
        val sole = ClinicalData(
            location = AcralLocation.SOLE,
            maxDiameterMm = 4.5f,
            isElevatedOrPalpable = false,
            isNodularOrBlue = false,
            hadPreviousProcedure = false,
            reportedChange = ReportedChange.NO,
            evolutionTime = EvolutionTime.MORE_THAN_12_MONTHS
        )
        val palm = sole.copy(
            location = AcralLocation.PALM,
            maxDiameterMm = 3f,
            evolutionTime = EvolutionTime.FROM_3_TO_12_MONTHS
        )
        return listOf(
            Evaluation("EV-001", "LES-001", today.minusMonths(6).minusDays(12), images("ej1"), sole, prediction(0.12f), followUp),
            Evaluation("EV-002", "LES-001", today.minusMonths(3).minusDays(12), images("ej2"), sole.copy(maxDiameterMm = 5f), prediction(0.16f), followUp),
            Evaluation("EV-003", "LES-002", today.minusMonths(1), images("ej3"), palm, prediction(0.08f), followUp)
        )
    }

    private val followUp = Recommendation(
        action = ManagementAction.FOLLOW_UP,
        reason = RecommendationReason.SMALL_BELOW_THRESHOLD,
        followUpAllowed = true
    )

    private fun images(name: String) = CapturedImages(dermoscopic = LesionImage("sample://$name"), context = null)

    private fun prediction(probability: Float) = Prediction(
        probMelanoma = probability,
        isOutOfScope = false,
        modelVersion = "simulado-0",
        threshold = 0.30f,
        isSimulated = true
    )
}
