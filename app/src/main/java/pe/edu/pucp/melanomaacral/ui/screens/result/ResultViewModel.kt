package pe.edu.pucp.melanomaacral.ui.screens.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.ui.screens.clinicaldata.ClinicalDataUiState
import pe.edu.pucp.melanomaacral.domain.repository.ClassifierRepository
import pe.edu.pucp.melanomaacral.domain.repository.ExplanationRepository
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.domain.rules.RecommendationRules
import pe.edu.pucp.melanomaacral.ui.appContainer
import java.time.LocalDate

// Analiza la imagen, aplica las reglas de recomendación y guarda la evaluación.
// La pantalla solo observa uiState.
class ResultViewModel(
    private val lesionId: String,
    private val images: CapturedImages,
    private val clinicalData: ClinicalData,
    private val classifier: ClassifierRepository,
    private val explanations: ExplanationRepository,
    private val records: RecordsRepository,
    private val drafts: DraftRepository
) : ViewModel() {

    // _uiState (privado, modificable) solo lo cambia el ViewModel.
    // uiState (público, solo lectura) es lo que observa la pantalla.
    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    // Próximo control de la lesión (cambia si se programa desde esta pantalla).
    val nextFollowUp: StateFlow<LocalDate?> = records.lesions
        .map { l -> l.find { it.id == lesionId }?.nextFollowUp }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        analyze()
    }

    private fun analyze() {
        // viewModelScope: la corrutina se cancela sola si el ViewModel se destruye.
        viewModelScope.launch {
            val prediction = classifier.classify(images.dermoscopic)
            val recommendation = RecommendationRules.recommend(prediction, clinicalData)
            // Cada análisis queda en el historial de la lesión, con la versión del modelo y el umbral usados
            // (RF-11, RNF-10). Si se estaba editando una evaluación, se reemplaza esa (mismo ID y fecha).
            val editing = drafts.get(lesionId)?.editingEvaluationId
            val id = if (editing != null) {
                records.replaceEvaluation(editing, images, clinicalData, prediction, recommendation); editing
            } else {
                records.addEvaluation(lesionId, LocalDate.now(), images, clinicalData, prediction, recommendation)
            }
            // La evaluación ya quedó guardada: deja de estar pendiente (sus fotos siguen en uso).
            drafts.delete(lesionId)
            // Mapa según lo que sugiere el modelo: a favor de melanoma (Grad-CAM) o en contra (análisis XAI).
            val kind = if (prediction.suggestsMelanoma) ExplanationKind.FOR_MELANOMA else ExplanationKind.AGAINST_MELANOMA
            val explanation = if (prediction.isOutOfScope) null else explanations.explain(images.dermoscopic, kind)
            _uiState.value = ResultUiState.Success(
                prediction, recommendation, clinicalData, images, explanation, evaluationId = id, wasEdited = editing != null
            )
        }
    }

    // Editar la evaluación recién guardada: deja un borrador con todo precargado que, al terminar, la reemplaza.
    // stage = paso en el que se abre el flujo (los demás siguen accesibles con «Atrás» y «Siguiente»).
    fun startEditing(stage: DraftStage, onReady: () -> Unit) {
        val state = _uiState.value as? ResultUiState.Success ?: return
        val id = state.evaluationId ?: return
        viewModelScope.launch {
            drafts.update(lesionId) {
                EvaluationDraft(
                    lesionId = lesionId, stage = stage,
                    context = images.context, dermoscopic = images.dermoscopic,
                    clinicalForm = ClinicalDataUiState.fromClinicalData(clinicalData).toDraft(),
                    updatedAtMillis = System.currentTimeMillis(), editingEvaluationId = id
                )
            }
            onReady()
        }
    }

    companion object {
        // Le dice a Android cómo crear este ViewModel con los datos de la evaluación.
        fun factory(
            lesionId: String,
            images: CapturedImages,
            clinicalData: ClinicalData
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ResultViewModel(
                    lesionId = lesionId,
                    images = images,
                    clinicalData = clinicalData,
                    classifier = container.classifierRepository,
                    explanations = container.explanationRepository,
                    records = container.recordsRepository,
                    drafts = container.draftRepository
                )
            }
        }
    }
}
