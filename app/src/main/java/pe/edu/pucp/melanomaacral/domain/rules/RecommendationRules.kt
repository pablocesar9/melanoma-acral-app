package pe.edu.pucp.melanomaacral.domain.rules

import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange

// Reglas de RF-08: combinan la salida del modelo con reglas clínicas documentadas.
// PROVISIONAL: el orden y los criterios deben validarse con el especialista.
object RecommendationRules {

    // Umbral del algoritmo de 3 pasos (Koga) [12][15].
    const val DIAMETER_LIMIT_MM = 7f

    fun recommend(prediction: Prediction, clinical: ClinicalData): Recommendation {
        // RF-09: si la lesión cumple un criterio de derivación del primer nivel, se deriva y no se monitorea.
        val followUpAllowed = !meetsReferralCriteria(clinical)

        // Se evalúan en orden: la primera condición que se cumple decide.
        val (action, reason) = when {
            prediction.isOutOfScope ->
                ManagementAction.NONE to RecommendationReason.OUT_OF_SCOPE
            !followUpAllowed ->
                ManagementAction.REFER to RecommendationReason.NOT_APT_FOR_FOLLOW_UP
            prediction.probMelanoma >= prediction.threshold ->
                ManagementAction.REFER to RecommendationReason.ABOVE_THRESHOLD
            clinical.maxDiameterMm > DIAMETER_LIMIT_MM ->
                ManagementAction.REFER to RecommendationReason.DIAMETER_OVER_LIMIT
            else ->
                ManagementAction.FOLLOW_UP to RecommendationReason.SMALL_BELOW_THRESHOLD
        }

        return Recommendation(action, reason, followUpAllowed)
    }

    // RF-09: criterios de derivación del primer nivel: cambio de aspecto (incluida la elevación) o síntomas,
    // sangrado, lesión que no cura o crecimiento rápido.
    fun meetsReferralCriteria(c: ClinicalData): Boolean =
        c.reportedChange == ReportedChange.YES ||
            c.isElevatedOrPalpable ||
            c.hasPain || c.hasItching ||
            c.hasBleeding ||
            c.isNotHealing ||
            c.isGrowingRapidly
}
