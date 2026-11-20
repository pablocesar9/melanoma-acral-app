package pe.edu.pucp.melanomaacral.ui.screens.clinicaldata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex

// El botón "Analizar" solo se habilita con el formulario completo y válido.
class ClinicalDataUiStateTest {

    private val complete = ClinicalDataUiState(
        ageText = "58",
        sex = Sex.FEMALE,
        location = AcralLocation.PALM,
        diameterText = "6,5",
        reportedChange = ReportedChange.UNKNOWN,
        evolutionTime = EvolutionTime.LESS_THAN_3_MONTHS
    )

    @Test
    fun formularioVacio_noSePuedeEnviar() {
        assertFalse(ClinicalDataUiState().canSubmit)
    }

    @Test
    fun formularioCompleto_produceLosDatosClinicos() {
        val data = complete.toClinicalDataOrNull()!!

        assertEquals(AcralLocation.PALM, data.location)
        assertEquals(6.5f, data.maxDiameterMm, 0.0001f)
        assertEquals(ReportedChange.UNKNOWN, data.reportedChange)
        assertTrue(complete.canSubmit)
    }

    @Test
    fun sinLocalizacion_noSePuedeEnviar() {
        assertNull(complete.copy(location = null).toClinicalDataOrNull())
    }

    @Test
    fun diametroInvalido_muestraErrorYNoSePuedeEnviar() {
        val state = complete.copy(diameterText = "abc")

        assertTrue(state.showDiameterError)
        assertFalse(state.canSubmit)
    }

    @Test
    fun diametroVacio_noMuestraErrorTodavia() {
        assertFalse(complete.copy(diameterText = "").showDiameterError)
    }

    @Test
    fun sinEdadOSinSexo_noSePuedeEnviar() {
        assertFalse(complete.copy(ageText = "").canSubmit)
        assertFalse(complete.copy(sex = null).canSubmit)
    }

    @Test
    fun edadFueraDeRango_muestraError() {
        assertTrue(complete.copy(ageText = "150").showAgeError)
        assertTrue(complete.copy(ageText = "abc").showAgeError)
    }

    @Test
    fun sintomasYAntecedentes_pasanALosDatosClinicos() {
        val data = complete.copy(hasBleeding = true, hasItching = true, hasSkinCancerHistory = true).toClinicalDataOrNull()!!

        assertEquals(58, data.ageYears)
        assertEquals(Sex.FEMALE, data.sex)
        assertTrue(data.hasBleeding && data.hasItching && data.hasSkinCancerHistory)
        assertFalse(data.hasPain)
    }
}
