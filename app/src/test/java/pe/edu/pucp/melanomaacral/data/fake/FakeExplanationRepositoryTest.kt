package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.LesionImage

// RF-19 + regla 6 del plan: el mapa de calor falso siempre se declara simulado.
class FakeExplanationRepositoryTest {
    @Test
    fun mapaSimulado_tieneLaRejillaPedidaYValoresEnRango() = runBlocking {
        val e = FakeExplanationRepository(size = 7).explain(LesionImage("x"))
        assertTrue(e.isSimulated)
        assertEquals(7, e.rows)
        assertEquals(7, e.columns)
        assertTrue(e.grid.flatten().all { it in 0f..1f })
        assertEquals(1f, e.grid[3][3], 0.0001f)   // máximo en el centro
    }
}
