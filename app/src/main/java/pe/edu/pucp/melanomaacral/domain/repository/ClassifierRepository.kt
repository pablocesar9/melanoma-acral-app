package pe.edu.pucp.melanomaacral.domain.repository

import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.Prediction

// Contrato del clasificador. Tiene una implementación falsa (data/fake, fase 2)
// y tendrá una real con el modelo en el dispositivo (data/real, fase 5).
interface ClassifierRepository {
    suspend fun classify(image: LesionImage): Prediction
}
