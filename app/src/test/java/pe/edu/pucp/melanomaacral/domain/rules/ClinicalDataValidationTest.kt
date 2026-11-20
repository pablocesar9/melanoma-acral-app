package pe.edu.pucp.melanomaacral.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// RF-07: el diámetro se escribe a mano y debe validarse.
class ClinicalDataValidationTest {

    @Test
    fun aceptaPuntoDecimal() {
        assertEquals(5.5f, ClinicalDataValidation.parseDiameter("5.5")!!, 0.0001f)
    }

    @Test
    fun aceptaComaDecimal() {
        assertEquals(5.5f, ClinicalDataValidation.parseDiameter("5,5")!!, 0.0001f)
    }

    @Test
    fun ignoraEspacios() {
        assertEquals(7f, ClinicalDataValidation.parseDiameter(" 7 ")!!, 0.0001f)
    }

    @Test
    fun rechazaTextoNoNumerico() {
        assertNull(ClinicalDataValidation.parseDiameter("abc"))
    }

    @Test
    fun rechazaCeroYNegativos() {
        assertNull(ClinicalDataValidation.parseDiameter("0"))
        assertNull(ClinicalDataValidation.parseDiameter("-3"))
    }

    @Test
    fun rechazaValoresMayoresAlMaximo() {
        assertNull(ClinicalDataValidation.parseDiameter("150"))
    }
}
