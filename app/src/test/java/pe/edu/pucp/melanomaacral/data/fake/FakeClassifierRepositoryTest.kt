package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.LesionImage

// Regla 6 del plan: toda salida falsa se etiqueta como simulada.
class FakeClassifierRepositoryTest {

    private val image = LesionImage(uri = "test://imagen")

    @Test
    fun todaPrediccionFalsaEstaMarcadaComoSimulada() = runBlocking {
        val prediction = FakeClassifierRepository(delayMillis = 0).classify(image)

        assertTrue(prediction.isSimulated)
    }

    @Test
    fun devuelveLaProbabilidadConfigurada() = runBlocking {
        val prediction = FakeClassifierRepository(probability = 0.64f, delayMillis = 0).classify(image)

        assertEquals(0.64f, prediction.probMelanoma, 0.0001f)
    }
}
