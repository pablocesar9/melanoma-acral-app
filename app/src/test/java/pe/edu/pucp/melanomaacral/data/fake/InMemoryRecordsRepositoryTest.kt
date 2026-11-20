package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InMemoryRecordsRepositoryTest {

    private val today = LocalDate.of(2026, 10, 27)

    @Test
    fun crearPaciente_apareceEnLaLista() = runBlocking {
        val repository = InMemoryRecordsRepository(today)

        repository.createPatient("PAC-099")

        assertTrue(repository.patients.first().any { it.id == "PAC-099" })
    }

    @Test
    fun crearLesion_devuelveUnIdNuevoYLaAsociaAlPaciente() = runBlocking {
        val repository = InMemoryRecordsRepository(today)

        val id = repository.createLesion("PAC-002")

        val lesion = repository.lesions.first().single { it.id == id }
        assertEquals("PAC-002", lesion.patientId)
        assertEquals(null, lesion.nextFollowUp)
    }

    @Test
    fun programarControl_actualizaSoloEsaLesion() = runBlocking {
        val repository = InMemoryRecordsRepository(today)
        val date = today.plusMonths(3)

        repository.scheduleFollowUp("LES-001", date)

        val lesions = repository.lesions.first()
        assertEquals(date, lesions.single { it.id == "LES-001" }.nextFollowUp)
        assertEquals(today.plusMonths(2), lesions.single { it.id == "LES-002" }.nextFollowUp)
    }
}
