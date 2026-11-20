package pe.edu.pucp.melanomaacral.ui.screens.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdRules
import pe.edu.pucp.melanomaacral.ui.appContainer
import java.time.LocalDate

// Lo que la lista muestra de cada paciente.
data class PatientSummary(
    val id: String,
    val lesionCount: Int,
    val lastEvaluation: LocalDate?,
    val hasOverdueFollowUp: Boolean,
    val pendingEvaluations: Int = 0   // evaluaciones empezadas y sin terminar
)

// Estado del diálogo "Registrar paciente".
data class NewPatientDialogState(
    val isOpen: Boolean = false,
    val idText: String = "",
    val error: PatientIdError? = null
)

data class PatientsUiState(
    val patients: List<PatientSummary> = emptyList(),
    val dialog: NewPatientDialogState = NewPatientDialogState()
)

class PatientsViewModel(
    private val records: RecordsRepository,
    private val drafts: DraftRepository
) : ViewModel() {

    private val dialog = MutableStateFlow(NewPatientDialogState())

    // combine: cada vez que cambia cualquiera de los cuatro flujos, recalcula el estado.
    // stateIn: lo convierte en un StateFlow que la pantalla puede observar.
    val uiState: StateFlow<PatientsUiState> =
        combine(records.patients, records.lesions, records.evaluations, dialog, drafts.drafts) { patients, lesions, evaluations, dialogState, pending ->
            PatientsUiState(
                patients = summarize(patients, lesions, evaluations, LocalDate.now(), pending.keys),
                dialog = dialogState
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientsUiState())

    fun openNewPatientDialog() {
        dialog.value = NewPatientDialogState(isOpen = true)
    }

    fun dismissNewPatientDialog() {
        dialog.value = NewPatientDialogState()
    }

    fun onNewPatientIdChange(text: String) {
        dialog.update { it.copy(idText = text, error = null) }
    }

    // Devuelve el ID creado, o null si no es válido (el error queda en el estado del diálogo).
    fun createPatient(): String? {
        val id = dialog.value.idText.trim()
        val error = PatientIdRules.validate(id, uiState.value.patients.map { it.id })
        if (error != null) {
            dialog.update { it.copy(error = error) }
            return null
        }
        viewModelScope.launch { records.createPatient(id) }
        dialog.value = NewPatientDialogState()
        return id
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                PatientsViewModel(container.recordsRepository, container.draftRepository)
            }
        }
    }
}

private fun summarize(
    patients: List<Patient>,
    lesions: List<Lesion>,
    evaluations: List<Evaluation>,
    today: LocalDate,
    pendingLesionIds: Set<String> = emptySet()
): List<PatientSummary> = patients.sortedBy { it.id }.map { patient ->
    val ownLesions = lesions.filter { it.patientId == patient.id }
    val ownLesionIds = ownLesions.map { it.id }.toSet()
    PatientSummary(
        id = patient.id,
        lesionCount = ownLesions.size,
        lastEvaluation = evaluations.filter { it.lesionId in ownLesionIds }.maxOfOrNull { it.date },
        hasOverdueFollowUp = ownLesions.any { it.nextFollowUp?.isBefore(today) == true },
        pendingEvaluations = ownLesionIds.count { it in pendingLesionIds }
    )
}
