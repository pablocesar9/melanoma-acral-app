package pe.edu.pucp.melanomaacral.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.DecisionChoice
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.ui.screens.decision.DecisionUiState

// RNF-12 (reglas del usuario y del PIN) y RF-23 (cuándo se puede guardar la decisión del profesional).
class AccessAndDecisionRulesTest {

    @Test
    fun usuarioYPin_validos() {
        assertNull(AccessRules.validate("usuario1", "1234"))
        assertNull(AccessRules.validate("m.garcia", "12345678"))
    }

    @Test
    fun usuarioYPin_invalidos() {
        assertNotNull(AccessRules.validate("ab", "1234"))
        assertNotNull(AccessRules.validate("con espacio", "1234"))
        assertNotNull(AccessRules.validate("usuario1", "123"))
        assertNotNull(AccessRules.validate("usuario1", "123456789"))
        assertNotNull(AccessRules.validate("usuario1", "12a4"))
    }

    @Test
    fun hash_dependeDeLaSal_yVerifica() {
        val s1 = AccessRules.newSalt()
        val s2 = AccessRules.newSalt()
        val h1 = AccessRules.hash("1234", s1)
        assertFalse(h1.contentEquals(AccessRules.hash("1234", s2)))
        assertTrue(AccessRules.matches("1234", s1, h1))
        assertFalse(AccessRules.matches("1235", s1, h1))
    }

    @Test
    fun decision_aceptar_noPideMotivo_yConservaLaSugerencia() {
        val s = DecisionUiState("EV-001", ManagementAction.REFER, choice = DecisionChoice.ACCEPTS)
        assertNull(s.blockReason)
        assertEquals(ManagementAction.REFER, s.finalAction)
    }

    @Test
    fun decision_modificar_pideConductaDistintaYMotivo() {
        val base = DecisionUiState("EV-001", ManagementAction.REFER, choice = DecisionChoice.MODIFIES)
        assertNotNull(base.blockReason)
        assertNotNull(base.copy(newAction = ManagementAction.REFER, reason = "x").blockReason)
        assertNotNull(base.copy(newAction = ManagementAction.FOLLOW_UP).blockReason)
        val ok = base.copy(newAction = ManagementAction.FOLLOW_UP, reason = "Estable")
        assertNull(ok.blockReason)
        assertEquals(ManagementAction.FOLLOW_UP, ok.finalAction)
    }

    @Test
    fun decision_descartar_pideMotivo_ySinConducta() {
        val s = DecisionUiState("EV-001", ManagementAction.FOLLOW_UP, choice = DecisionChoice.DISCARDS)
        assertNotNull(s.blockReason)
        assertNull(s.copy(reason = "Otra causa").blockReason)
        assertNull(s.copy(reason = "Otra causa").finalAction)
    }

    @Test
    fun decision_sinEleccion_noSeGuarda() {
        assertNotNull(DecisionUiState("EV-001", ManagementAction.REFER).blockReason)
    }
}
