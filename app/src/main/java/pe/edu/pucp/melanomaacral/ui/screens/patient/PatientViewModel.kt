package pe.edu.pucp.melanomaacral.ui.screens.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import java.time.LocalDate

// Lo que la lista muestra de cada lesión.
data class LesionSummary(
    val id: String,
    val location: AcralLocation?,   // null si aún no tiene evaluaciones
    val captureCount: Int,
    val lastCapture: LocalDate?,
    val nextFollowUp: LocalDate?,
    val isOverdue: Boolean,
    val lastAction: ManagementAction? = null,  // recomendación de la última evaluación (para la tarjeta)
    val hasPendingEvaluation: Boolean = false   // evaluación empezada y sin terminar
)

data class PatientUiState(
    val patientId: String,
    val lesions: List<LesionSummary> = emptyList()
)

class PatientViewModel(
    private val patientId: String,
    private val records: RecordsRepository,
    private val drafts: DraftRepository,
    private val audit: suspend (AuditAction, String) -> Unit = { _, _ -> }
) : ViewModel() {

    init { viewModelScope.launch { audit(AuditAction.VIEW, "Paciente $patientId") } }

    val uiState: StateFlow<PatientUiState> =
        combine(records.lesions, records.evaluations, drafts.drafts) { lesions, evaluations, pending ->
            val today = LocalDate.now()
            PatientUiState(
                patientId = patientId,
                lesions = lesions.filter { it.patientId == patientId }.map { lesion ->
                    val own = evaluations.forLesion(lesion.id)
                    LesionSummary(
                        id = lesion.id,
                        location = own.lastOrNull()?.clinicalData?.location,
                        captureCount = own.size,
                        lastCapture = own.lastOrNull()?.date,
                        nextFollowUp = lesion.nextFollowUp,
                        isOverdue = lesion.nextFollowUp?.isBefore(today) == true,
                        lastAction = own.lastOrNull()?.recommendation?.action,
                        hasPendingEvaluation = lesion.id in pending
                    )
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientUiState(patientId))

    // Crea la lesión y entrega su ID a quien la pidió (la pantalla, para navegar).
    fun createLesion(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = records.createLesion(patientId)
            audit(AuditAction.CREATE, "Lesión $id de $patientId")
            onCreated(id)
        }
    }

    companion object {
        fun factory(patientId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                PatientViewModel(patientId, container.recordsRepository, container.draftRepository, container.accessRepository::audit)
            }
        }
    }
}
