package pe.edu.pucp.melanomaacral.ui.screens.referral

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import java.time.LocalDate

// RF-15 y regla 6 del plan: el texto de derivación lleva los datos y avisa si es simulado.
class ReferralTextTest {

    private val evaluation = SampleRecords.evaluations(LocalDate.of(2026, 10, 27)).first()

    @Test
    fun resultadoSimulado_llevaElAvisoAlInicio() {
        val text = ReferralText.build("PAC-001", "LES-001", evaluation)

        assertTrue(text.startsWith(ReferralText.SIMULATED_WARNING))
    }

    @Test
    fun resultadoReal_noLlevaElAviso() {
        val real = evaluation.copy(prediction = evaluation.prediction.copy(isSimulated = false))

        assertFalse(ReferralText.build("PAC-001", "LES-001", real).contains(ReferralText.SIMULATED_WARNING))
    }

    @Test
    fun incluyePacienteLesionYVersionDelModelo() {
        val text = ReferralText.build("PAC-001", "LES-001", evaluation)

        assertTrue(text.contains("PAC-001"))
        assertTrue(text.contains("LES-001"))
        assertTrue(text.contains(evaluation.prediction.modelVersion))
    }

    @Test
    fun imagenFueraDeAlcance_incluyeLaAlerta() {
        val outOfScope = evaluation.copy(prediction = evaluation.prediction.copy(isOutOfScope = true))

        assertTrue(ReferralText.build("PAC-001", "LES-001", outOfScope).contains("fuera del alcance"))
    }
}
