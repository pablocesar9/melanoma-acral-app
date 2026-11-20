package pe.edu.pucp.melanomaacral.ui.screens.clinicaldata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import kotlinx.coroutines.flow.first
import pe.edu.pucp.melanomaacral.ui.appContainer

// Guarda lo que el usuario va escribiendo. Al estar en el ViewModel, el formulario no se pierde
// si el teléfono gira; y al guardarse en la evaluación sin terminar, tampoco si se sale del flujo.
class ClinicalDataViewModel(
    private val lesionId: String,
    private val drafts: DraftRepository,
    private val records: RecordsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClinicalDataUiState())
    val uiState: StateFlow<ClinicalDataUiState> = _uiState.asStateFlow()

    private var touched = false

    // true si se precargaron datos de la evaluación anterior (la pantalla lo avisa).
    private val _prefilled = MutableStateFlow(false)
    val prefilled: StateFlow<Boolean> = _prefilled.asStateFlow()

    // ID de la evaluación que se está editando (null si es una evaluación nueva).
    private val _editingId = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = _editingId.asStateFlow()

    init {
        // Retoma el formulario a medio llenar, si lo hay; si no, precarga lo estable de la evaluación anterior.
        viewModelScope.launch {
            val draft = drafts.get(lesionId)
            _editingId.value = draft?.editingEvaluationId
            val saved = draft?.clinicalForm
            if (saved != null) {
                if (!touched) _uiState.value = ClinicalDataUiState.fromDraft(saved)
            } else {
                val last = records.evaluations.first().forLesion(lesionId).lastOrNull()
                if (last != null && !touched) {
                    _uiState.value = ClinicalDataUiState.prefilledFrom(last.clinicalData)
                    _prefilled.value = true
                }
            }
            drafts.update(lesionId) { it?.copy(stage = DraftStage.CLINICAL_DATA) }
        }
    }

    // La pantalla envía una copia del estado con el campo que cambió.
    fun onFormChange(newState: ClinicalDataUiState) {
        touched = true
        _uiState.value = newState
        viewModelScope.launch {
            drafts.update(lesionId) { old ->
                (old ?: EvaluationDraft(lesionId)).copy(stage = DraftStage.CLINICAL_DATA, clinicalForm = newState.toDraft())
            }
        }
    }

    // Al volver a las fotos, la evaluación pendiente queda en el paso de la imagen dermoscópica.
    fun onBackToImages() {
        viewModelScope.launch {
            drafts.update(lesionId) { it?.copy(stage = DraftStage.DERMOSCOPIC, clinicalForm = _uiState.value.toDraft()) }
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { appContainer().let { ClinicalDataViewModel(lesionId, it.draftRepository, it.recordsRepository) } }
        }
    }
}
