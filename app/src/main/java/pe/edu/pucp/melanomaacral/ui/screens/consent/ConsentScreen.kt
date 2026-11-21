package pe.edu.pucp.melanomaacral.ui.screens.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import pe.edu.pucp.melanomaacral.domain.model.ConsentType
import pe.edu.pucp.melanomaacral.domain.repository.ConsentRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.ChoiceChips
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// P-12 Consentimiento informado — RF-17, RNF-06. Va antes de la captura: si el paciente ya dio su consentimiento,
// la pantalla pasa directo a la captura; si no, lo pide. Sin consentimiento no se habilita la captura.

fun ConsentType.label(): String = when (this) {
    ConsentType.SIGNED -> "Sí, firmó el formato del establecimiento"
    ConsentType.VERBAL -> "Sí, de forma verbal registrada"
    ConsentType.REFUSED -> "No"
}

data class ConsentUiState(
    val isLoading: Boolean = true,
    val patientId: String? = null,
    val selected: ConsentType? = null,
    val previousRefusal: Boolean = false,   // ya había dicho que no
    val refusedNow: Boolean = false,        // se acaba de registrar «No»
    val today: LocalDate = LocalDate.now()
)

class ConsentViewModel(
    private val lesionId: String,
    private val records: RecordsRepository,
    private val consents: ConsentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    // true cuando el paciente tiene consentimiento: la pantalla pasa a la captura.
    private val _allowed = MutableStateFlow(false)
    val allowed: StateFlow<Boolean> = _allowed.asStateFlow()

    init {
        viewModelScope.launch {
            val patientId = records.lesions.first().find { it.id == lesionId }?.patientId
            val consent = patientId?.let { consents.get(it) }
            if (consent?.allowsCapture == true) _allowed.value = true
            _uiState.update { it.copy(isLoading = false, patientId = patientId, previousRefusal = consent?.type == ConsentType.REFUSED) }
        }
    }

    fun select(type: ConsentType) = _uiState.update { it.copy(selected = type, refusedNow = false) }

    fun register() {
        val s = _uiState.value
        val patientId = s.patientId ?: return
        val type = s.selected ?: return
        viewModelScope.launch {
            consents.record(patientId, type)
            if (type == ConsentType.REFUSED) _uiState.update { it.copy(refusedNow = true, previousRefusal = true) }
            else _allowed.value = true
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ConsentViewModel(lesionId, c.recordsRepository, c.consentRepository)
            }
        }
    }
}

@Composable
fun ConsentScreen(
    lesionId: String,
    onConsented: () -> Unit,
    onBack: () -> Unit,
    viewModel: ConsentViewModel = viewModel(factory = ConsentViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    LaunchedEffect(allowed) { if (allowed) onConsented() }
    ConsentContent(uiState = uiState, onSelect = viewModel::select, onRegister = viewModel::register, onBack = onBack)
}

private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ConsentContent(
    uiState: ConsentUiState,
    onSelect: (ConsentType) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Consentimiento", subtitle = uiState.patientId?.let { "Paciente $it" }, onBack = onBack) },
        bottomBar = {
            if (!uiState.isLoading) {
                BottomActionBar(
                    primaryLabel = "Registrar y continuar",
                    onPrimary = onRegister,
                    primaryIcon = Icons.Filled.Check,
                    enabled = uiState.selected != null,
                    disabledReason = "Elija la respuesta del paciente."
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingContent("Cargando…", Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Space.l)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.l)
        ) {
            SectionTitle("Antes de tomar o guardar imágenes")
            Text(
                "Explique al paciente:\n" +
                    "• para qué se usan las imágenes (apoyo al tamizaje y a la referencia);\n" +
                    "• que se guardan en este teléfono con un código, sin nombre ni DNI;\n" +
                    "• que puede retirar su consentimiento.",
                style = MaterialTheme.typography.bodyLarge
            )
            SectionTitle("¿El paciente da su consentimiento?")
            ChoiceChips(
                options = ConsentType.entries,
                selected = uiState.selected,
                label = { it.label() },
                onSelect = onSelect
            )
            Text("Fecha: ${DATE.format(uiState.today)}", style = MaterialTheme.typography.bodyMedium)
            if (uiState.refusedNow || uiState.previousRefusal) {
                NoticeCard(
                    title = "Sin consentimiento",
                    text = "El paciente no dio su consentimiento: no se habilita la captura. Si cambia de opinión, registre de nuevo su respuesta.",
                    kind = NoticeKind.Warning
                )
            } else {
                NoticeCard(
                    title = "Requisito para capturar",
                    text = "Sin consentimiento no se habilita la captura.",
                    kind = NoticeKind.Info
                )
            }
        }
    }
}
