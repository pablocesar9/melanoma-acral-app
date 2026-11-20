package pe.edu.pucp.melanomaacral.data.fake

import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.repository.ExplanationRepository
import kotlin.math.exp

// Mapa de calor de prueba: una mancha gaussiana en el centro. NO viene del modelo.
class FakeExplanationRepository(private val size: Int = 7) : ExplanationRepository {

    override suspend fun explain(image: LesionImage, kind: ExplanationKind): Explanation {
        val center = (size - 1) / 2f
        val sigma = size / 4f
        val grid = List(size) { r ->
            List(size) { c ->
                val d2 = (r - center) * (r - center) + (c - center) * (c - center)
                exp(-d2 / (2 * sigma * sigma))
            }
        }
        return Explanation(grid, isSimulated = true, kind = kind)
    }
}
