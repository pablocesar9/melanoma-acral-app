package pe.edu.pucp.melanomaacral.ui.screens.lesion

import pe.edu.pucp.melanomaacral.domain.repository.ImageStorage
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.ui.screens.clinicaldata.ClinicalDataUiState
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import java.time.LocalDate

data class LesionUiState(
    val lesionId: String,
    val patientId: String? = null,
    val location: AcralLocation? = null,
    val nextFollowUp: LocalDate? = null,
    val isOverdue: Boolean = false,
    val evaluations: List<Evaluation> = emptyList(),  // de la más antigua a la más reciente
    val draft: EvaluationDraft? = null                // evaluación empezada y sin terminar
)

class LesionViewModel(
    private val lesionId: String,
    private val records: RecordsRepository,
    private val drafts: DraftRepository,
    private val storage: ImageStorage
) : ViewModel() {

    val uiState: StateFlow<LesionUiState> =
        combine(records.lesions, records.evaluations, drafts.drafts) { lesions, evaluations, pending ->
            val lesion = lesions.find { it.id == lesionId }
            val own = evaluations.forLesion(lesionId)
            LesionUiState(
                lesionId = lesionId,
                patientId = lesion?.patientId,
                location = own.lastOrNull()?.clinicalData?.location,
                nextFollowUp = lesion?.nextFollowUp,
                isOverdue = lesion?.nextFollowUp?.isBefore(LocalDate.now()) == true,
                evaluations = own,
                draft = pending[lesionId]
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LesionUiState(lesionId))

    // Editar una evaluación ya guardada: crea un borrador con todo precargado (en los datos clínicos) que, al terminar,
    // reemplaza esa evaluación. Si ya hay otra evaluación sin terminar en esta lesión, no la pisa: devuelve false.
    fun startEditing(evaluation: Evaluation, onReady: (EvaluationDraft) -> Unit, onBlocked: () -> Unit) {
        viewModelScope.launch {
            val current = drafts.get(lesionId)
            if (current != null && current.editingEvaluationId != evaluation.id) { onBlocked(); return@launch }
            val draft = current ?: EvaluationDraft(
                lesionId = lesionId, stage = DraftStage.CLINICAL_DATA,
                context = evaluation.images.context, closeUp = evaluation.images.closeUp,
                dermoscopic = evaluation.images.dermoscopic,
                clinicalForm = ClinicalDataUiState.fromClinicalData(evaluation.clinicalData).toDraft(),
                updatedAtMillis = System.currentTimeMillis(), editingEvaluationId = evaluation.id
            )
            drafts.update(lesionId) { draft }
            onReady(draft)
        }
    }

    // Descarta la evaluación sin terminar y borra sus fotos (no quedan archivos huérfanos).
    fun discardDraft() {
        viewModelScope.launch {
            drafts.get(lesionId)?.allImages?.forEach { storage.delete(it) }
            drafts.delete(lesionId)
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LesionViewModel(lesionId, container.recordsRepository, container.draftRepository, container.imageStorage)
            }
        }
    }
}
