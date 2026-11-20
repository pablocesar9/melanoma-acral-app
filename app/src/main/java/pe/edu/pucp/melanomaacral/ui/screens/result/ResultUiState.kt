package pe.edu.pucp.melanomaacral.ui.screens.result

import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation

// Todo lo que la pantalla de resultado necesita para dibujarse.
// "sealed": solo existen estos estados, y el compilador obliga a tratarlos todos.
sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Success(
        val prediction: Prediction,
        val recommendation: Recommendation,
        val clinicalData: ClinicalData,
        val images: CapturedImages? = null,
        val explanation: Explanation? = null,
        val evaluationId: String? = null,   // ID con el que quedó guardada
        val wasEdited: Boolean = false      // true si corrigió una evaluación anterior
    ) : ResultUiState
}
