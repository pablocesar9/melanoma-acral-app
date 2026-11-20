package pe.edu.pucp.melanomaacral.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralText
import java.time.LocalDate

// RF-15: el paquete lleva de 2 a 3 imágenes y no fija la especialidad de destino.
class ReferralPackagesTest {

    private val today = LocalDate.of(2026, 11, 14)
    private val evals = SampleRecords.evaluations(today).filter { it.lesionId == "LES-001" }
    private val withContext = evals.map { it.copy(images = it.images.copy(context = LesionImage("ctx-${it.id}"))) }

    @Test
    fun lesionSinEvaluaciones_noTienePaquete() {
        assertNull(ReferralPackages.build("PAC-001", "LES-001", emptyList()))
    }

    @Test
    fun primeraCapturaConContexto_llevaDosImagenes() {
        val pkg = ReferralPackages.build("PAC-001", "LES-001", withContext.take(1))!!
        assertEquals(listOf("Foto clínica de contexto", "Imagen dermoscópica"), pkg.attachments.map { it.label })
    }

    @Test
    fun control_agregaLaDermoscopicaAnterior() {
        val pkg = ReferralPackages.build("PAC-001", "LES-001", withContext)!!
        assertEquals(3, pkg.attachments.size)
        assertEquals(evals.last().id, pkg.latest.id)
        assertEquals(evals.first().id, pkg.previous!!.id)
    }

    @Test
    fun textoDeDerivacion_noFijaEspecialidadDeDestino() {
        val text = ReferralText.build("PAC-001", "LES-001", evals.last())
        assertTrue(text.contains("según el flujo de referencia de su establecimiento"))
    }
}
