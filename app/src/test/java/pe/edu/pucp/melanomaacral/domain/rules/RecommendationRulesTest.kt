package pe.edu.pucp.melanomaacral.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange

// Pruebas de las reglas de recomendación (RF-07, RF-08, RF-09).
class RecommendationRulesTest {

    // Caso base: lesión pequeña, plana y con probabilidad baja.
    private val lowRisk = Prediction(
        probMelanoma = 0.10f, isOutOfScope = false, modelVersion = "test", threshold = 0.30f, isSimulated = true
    )
    private val smallFlatLesion = ClinicalData(
        location = AcralLocation.SOLE,
        maxDiameterMm = 5f,
        isElevatedOrPalpable = false,
        isNodularOrBlue = false,
        hadPreviousProcedure = false,
        reportedChange = ReportedChange.NO,
        evolutionTime = EvolutionTime.MORE_THAN_12_MONTHS
    )

    @Test
    fun lesionPequenaConProbabilidadBaja_recomiendaControl() {
        val result = RecommendationRules.recommend(lowRisk, smallFlatLesion)

        assertEquals(ManagementAction.FOLLOW_UP, result.action)
        assertEquals(RecommendationReason.SMALL_BELOW_THRESHOLD, result.reason)
        assertTrue(result.followUpAllowed)
    }

    @Test
    fun probabilidadIgualAlUmbral_deriva() {
        val result = RecommendationRules.recommend(lowRisk.copy(probMelanoma = 0.30f), smallFlatLesion)

        assertEquals(ManagementAction.REFER, result.action)
        assertEquals(RecommendationReason.ABOVE_THRESHOLD, result.reason)
    }

    @Test
    fun diametroMayorA7mm_deriva() {
        val result = RecommendationRules.recommend(lowRisk, smallFlatLesion.copy(maxDiameterMm = 7.5f))

        assertEquals(ManagementAction.REFER, result.action)
        assertEquals(RecommendationReason.DIAMETER_OVER_LIMIT, result.reason)
    }

    @Test
    fun diametroDeExactamente7mm_noDerivaPorDiametro() {
        // La regla es "mayor a 7 mm": 7 mm exactos todavía van a control.
        val result = RecommendationRules.recommend(lowRisk, smallFlatLesion.copy(maxDiameterMm = 7f))

        assertEquals(ManagementAction.FOLLOW_UP, result.action)
    }

    @Test
    fun lesionElevada_derivaYBloqueaControl() {
        val result = RecommendationRules.recommend(lowRisk, smallFlatLesion.copy(isElevatedOrPalpable = true))

        assertEquals(ManagementAction.REFER, result.action)
        assertEquals(RecommendationReason.NOT_APT_FOR_FOLLOW_UP, result.reason)
        assertFalse(result.followUpAllowed)
    }

    @Test
    fun lesionNodularOAzul_bloqueaControl() {
        val result = RecommendationRules.recommend(lowRisk, smallFlatLesion.copy(isNodularOrBlue = true))

        assertFalse(result.followUpAllowed)
    }

    @Test
    fun imagenFueraDeAlcance_noRecomiendaNada() {
        val result = RecommendationRules.recommend(lowRisk.copy(isOutOfScope = true), smallFlatLesion)

        assertEquals(ManagementAction.NONE, result.action)
        assertEquals(RecommendationReason.OUT_OF_SCOPE, result.reason)
    }
}
