package pe.edu.pucp.melanomaacral.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// RF-10 y RNF-06: ID anónimo, sin documentos de identidad.
class PatientIdRulesTest {

    private val existing = listOf("PAC-001", "PAC-002")

    @Test
    fun idValido_noTieneError() {
        assertNull(PatientIdRules.validate("PAC-003", existing))
    }

    @Test
    fun idVacioOConSoloEspacios_esError() {
        assertEquals(PatientIdError.BLANK, PatientIdRules.validate("   ", existing))
    }

    @Test
    fun idDemasiadoLargo_esError() {
        assertEquals(PatientIdError.TOO_LONG, PatientIdRules.validate("X".repeat(21), existing))
    }

    @Test
    fun ochoDigitos_pareceDni_esError() {
        assertEquals(PatientIdError.LOOKS_LIKE_DNI, PatientIdRules.validate("45678912", existing))
    }

    @Test
    fun ochoDigitosConPrefijo_noPareceDni() {
        assertNull(PatientIdRules.validate("HC-45678912", existing))
    }

    @Test
    fun idRepetido_esError_inclusoConEspacios() {
        assertEquals(PatientIdError.DUPLICATE, PatientIdRules.validate(" PAC-001 ", existing))
    }
}
