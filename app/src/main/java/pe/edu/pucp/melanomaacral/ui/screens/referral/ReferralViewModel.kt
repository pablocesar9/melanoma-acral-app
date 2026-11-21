package pe.edu.pucp.melanomaacral.ui.screens.referral

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
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.ReferralAttachment
import pe.edu.pucp.melanomaacral.domain.model.ReferralExport
import pe.edu.pucp.melanomaacral.domain.model.ReferralPackage
import pe.edu.pucp.melanomaacral.domain.model.ReferralPackages
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.domain.repository.ReferralExporter
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.model.ProfessionalDecision

data class ReferralUiState(
    val isLoading: Boolean = true,
    val lesionId: String = "",
    val text: String? = null,                         // null si la lesión no tiene evaluaciones
    val attachments: List<ReferralAttachment> = emptyList(),
    val isExporting: Boolean = false,
    val errorMessage: String? = null
)

class ReferralViewModel(
    private val lesionId: String,
    records: RecordsRepository,
    private val exporter: ReferralExporter,
    private val audit: suspend (AuditAction, String) -> Unit = { _, _ -> },
    decisions: kotlinx.coroutines.flow.Flow<List<ProfessionalDecision>> = kotlinx.coroutines.flow.flowOf(emptyList())
) : ViewModel() {

    private data class Local(val isExporting: Boolean = false, val errorMessage: String? = null)

    private val local = MutableStateFlow(Local())
    private var currentPackage: ReferralPackage? = null

    val uiState: StateFlow<ReferralUiState> =
        combine(records.lesions, records.evaluations, local, decisions) { lesions, evaluations, l, ds ->
            val lesion = lesions.find { it.id == lesionId }
            val pkg = lesion?.let { ReferralPackages.build(it.patientId, lesionId, evaluations) }
                ?.let { p -> p.copy(decision = ds.find { it.evaluationId == p.latest.id }) }
            currentPackage = pkg
            ReferralUiState(
                isLoading = false,
                lesionId = lesionId,
                text = pkg?.let { ReferralText.build(it.patientId, it.lesionId, it.latest) },
                attachments = pkg?.attachments.orEmpty(),
                isExporting = l.isExporting,
                errorMessage = l.errorMessage
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferralUiState(lesionId = lesionId))

    // Genera el PDF y entrega su URI para abrir el menú "Compartir" (RF-15).
    fun exportPdf(onReady: (ReferralExport) -> Unit) {
        val pkg = currentPackage ?: return
        local.value = Local(isExporting = true)
        viewModelScope.launch {
            try {
                val uri = exporter.exportPdf(pkg)
                audit(AuditAction.EXPORT, "PDF de derivación de $lesionId")
                local.value = Local()
                onReady(uri)
            } catch (e: Exception) {
                local.value = Local(errorMessage = "No se pudo generar el PDF: ${e.message}")
            }
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ReferralViewModel(lesionId, container.recordsRepository, container.referralExporter, container.accessRepository::audit, container.decisionRepository.all)
            }
        }
    }
}
