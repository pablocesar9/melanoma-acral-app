package pe.edu.pucp.melanomaacral.domain.repository

import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage

// Contrato del mapa de calor (RF-19). Hoy solo existe la implementación simulada;
// la real (Grad-CAM sobre el modelo en el dispositivo) llega con el módulo de IA.
interface ExplanationRepository {
    suspend fun explain(image: LesionImage, kind: ExplanationKind = ExplanationKind.FOR_MELANOMA): Explanation
}
