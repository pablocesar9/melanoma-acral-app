package pe.edu.pucp.melanomaacral.ui.screens.patient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.pucp.melanomaacral.data.fake.InMemoryDraftRepository
import pe.edu.pucp.melanomaacral.data.fake.InMemoryRecordsRepository
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError

// RF-10 y RNF-12: corregir el código del paciente valida el código nuevo como el alta y deja el cambio registrado.
@OptIn(ExperimentalCoroutinesApi::class)
class PatientViewModelTest {

    private val records = InMemoryRecordsRepository()
    private val audited = mutableListOf<Pair<AuditAction, String>>()
    private lateinit var vm: PatientViewModel
    private lateinit var first: String
    private lateinit var second: String

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val ids = runBlocking { records.patients.first().map { it.id } }
        first = ids[0]; second = ids[1]
        vm = PatientViewModel(first, records, InMemoryDraftRepository()) { a, t -> audited += a to t }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun codigoDeOtroPaciente_seRechaza() {
        vm.openRename(); vm.onRenameTextChange(second)
        var renamed: String? = null
        vm.confirmRename { renamed = it }
        assertEquals(PatientIdError.DUPLICATE, vm.renameState.value.error)
        assertNull(renamed)
    }

    @Test fun numeroDeDocumento_seRechaza() {
        vm.openRename(); vm.onRenameTextChange("45780123")
        vm.confirmRename { }
        assertEquals(PatientIdError.LOOKS_LIKE_DNI, vm.renameState.value.error)
    }

    @Test fun codigoValido_seCorrigeYQuedaEnElRegistro() = runBlocking {
        vm.openRename(); vm.onRenameTextChange("HC-900")
        var renamed: String? = null
        vm.confirmRename { renamed = it }
        assertEquals("HC-900", renamed)
        assertFalse(vm.renameState.value.isOpen)
        val ids = records.patients.first().map { it.id }
        assertTrue("HC-900" in ids && first !in ids)
        assertTrue(audited.any { it.first == AuditAction.MODIFY && it.second == "Paciente $first: código corregido a HC-900" })
    }
}
