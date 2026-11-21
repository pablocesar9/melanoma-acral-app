package pe.edu.pucp.melanomaacral.ui.screens.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdRules
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

// Diálogo para corregir el código del paciente.
data class RenamePatientState(
    val isOpen: Boolean = false,
    val idText: String = "",
    val error: PatientIdError? = null,
    val failure: String? = null
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

    private val rename = MutableStateFlow(RenamePatientState())
    val renameState: StateFlow<RenamePatientState> = rename.asStateFlow()

    fun openRename() { rename.value = RenamePatientState(isOpen = true, idText = patientId) }

    fun dismissRename() { rename.value = RenamePatientState() }

    fun onRenameTextChange(text: String) = rename.update { it.copy(idText = text, error = null, failure = null) }

    // Corrige el código con la misma validación del alta y deja el cambio en el registro de acciones (RNF-12).
    fun confirmRename(onRenamed: (String) -> Unit) {
        val newId = rename.value.idText.trim()
        if (newId == patientId) { dismissRename(); return }
        viewModelScope.launch {
            val others = records.patients.first().map { it.id }.filter { it != patientId }
            val error = PatientIdRules.validate(newId, others)
            if (error != null) { rename.update { it.copy(error = error) }; return@launch }
            try {
                records.renamePatient(patientId, newId)
            } catch (e: Exception) {
                rename.update { it.copy(failure = "No se pudo corregir el código: ${e.message}") }
                return@launch
            }
            audit(AuditAction.MODIFY, "Paciente $patientId: código corregido a $newId")
            rename.value = RenamePatientState()
            onRenamed(newId)
        }
    }

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
