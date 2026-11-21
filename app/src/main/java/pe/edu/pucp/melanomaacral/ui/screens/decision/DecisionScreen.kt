package pe.edu.pucp.melanomaacral.ui.screens.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.DecisionChoice
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.repository.DecisionRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.ChoiceChips
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.RecommendationPill
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.components.SegmentedChoice
import pe.edu.pucp.melanomaacral.ui.components.shortLabel
import pe.edu.pucp.melanomaacral.ui.theme.Space

// P-13 Decisión del profesional — RF-23, RF-08. Después del resultado, el profesional registra si acepta,
// modifica o descarta la sugerencia. La sugerencia de la aplicación no cambia: la decisión se guarda aparte.

fun DecisionChoice.label(): String = when (this) {
    DecisionChoice.ACCEPTS -> "Acepta la sugerencia"
    DecisionChoice.MODIFIES -> "La modifica"
    DecisionChoice.DISCARDS -> "La descarta"
}

data class DecisionUiState(
    val evaluationId: String,
    val suggested: ManagementAction,
    val choice: DecisionChoice? = null,
    val newAction: ManagementAction? = null,
    val reason: String = "",
    val isSaving: Boolean = false
) {
    // Conducta que puede elegir si modifica: la otra de las dos (derivar o control).
    val alternatives: List<ManagementAction> get() = listOf(ManagementAction.REFER, ManagementAction.FOLLOW_UP)

    // Motivo de por qué no se puede guardar, o null si se puede.
    val blockReason: String?
        get() = when {
            choice == null -> "Elija qué decide."
            choice == DecisionChoice.MODIFIES && newAction == null -> "Elija la nueva conducta."
            choice == DecisionChoice.MODIFIES && newAction == suggested -> "La nueva conducta es igual a la sugerida: elija «Acepta»."
            choice != DecisionChoice.ACCEPTS && reason.isBlank() -> "Escriba el motivo."
            else -> null
        }

    val finalAction: ManagementAction?
        get() = when (choice) {
            DecisionChoice.ACCEPTS -> suggested
            DecisionChoice.MODIFIES -> newAction
            else -> null
        }
}

class DecisionViewModel(
    evaluationId: String,
    suggested: ManagementAction,
    private val decisions: DecisionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DecisionUiState(evaluationId, suggested))
    val uiState: StateFlow<DecisionUiState> = _uiState.asStateFlow()

    init {
        // Si ya había una decisión, se precarga para corregirla.
        viewModelScope.launch {
            decisions.observe(evaluationId).first()?.let { d ->
                _uiState.update {
                    it.copy(choice = d.choice, newAction = if (d.choice == DecisionChoice.MODIFIES) d.finalAction else null, reason = d.reason)
                }
            }
        }
    }

    fun choose(c: DecisionChoice) = _uiState.update { it.copy(choice = c) }
    fun chooseAction(a: ManagementAction) = _uiState.update { it.copy(newAction = a) }
    fun onReason(r: String) = _uiState.update { it.copy(reason = r.take(300)) }

    fun save(onDone: () -> Unit) {
        val s = _uiState.value
        val choice = s.choice ?: return
        if (s.blockReason != null || s.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            decisions.save(s.evaluationId, choice, s.finalAction, s.reason)
            onDone()
        }
    }

    companion object {
        fun factory(evaluationId: String, suggested: ManagementAction): ViewModelProvider.Factory = viewModelFactory {
            initializer { DecisionViewModel(evaluationId, suggested, appContainer().decisionRepository) }
        }
    }
}

@Composable
fun DecisionScreen(
    evaluationId: String,
    suggested: ManagementAction,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: DecisionViewModel = viewModel(factory = DecisionViewModel.factory(evaluationId, suggested))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DecisionContent(
        uiState = uiState,
        onChoose = viewModel::choose,
        onChooseAction = viewModel::chooseAction,
        onReason = viewModel::onReason,
        onSave = { viewModel.save(onSaved) },
        onBack = onBack
    )
}

@Composable
fun DecisionContent(
    uiState: DecisionUiState,
    onChoose: (DecisionChoice) -> Unit,
    onChooseAction: (ManagementAction) -> Unit,
    onReason: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Su decisión", subtitle = "Evaluación ${uiState.evaluationId}", onBack = onBack) },
        bottomBar = {
            BottomActionBar(
                primaryLabel = "Guardar decisión",
                onPrimary = onSave,
                primaryIcon = Icons.Filled.Save,
                enabled = uiState.blockReason == null && !uiState.isSaving,
                disabledReason = uiState.blockReason
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Space.l)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.l)
        ) {
            NoticeCard(
                title = "La decisión es suya",
                text = "La aplicación sugiere; usted decide. La sugerencia no cambia: su decisión queda registrada con la evaluación.",
                kind = NoticeKind.Support
            )
            SectionTitle("Sugerencia de la aplicación")
            RecommendationPill(uiState.suggested)
            SectionTitle("¿Qué decide usted?")
            ChoiceChips(options = DecisionChoice.entries, selected = uiState.choice, label = { it.label() }, onSelect = onChoose)
            if (uiState.choice == DecisionChoice.MODIFIES) {
                SectionTitle("Nueva conducta")
                SegmentedChoice(
                    options = uiState.alternatives,
                    selected = uiState.newAction,
                    label = { it.shortLabel() },
                    onSelect = onChooseAction
                )
            }
            OutlinedTextField(
                value = uiState.reason,
                onValueChange = onReason,
                label = { Text(if (uiState.choice == DecisionChoice.ACCEPTS || uiState.choice == null) "Motivo (opcional)" else "Motivo") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "La decisión queda registrada con la evaluación y con su usuario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
