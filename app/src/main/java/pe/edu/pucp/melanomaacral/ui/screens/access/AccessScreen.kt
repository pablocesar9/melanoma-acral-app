package pe.edu.pucp.melanomaacral.ui.screens.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.repository.AccessRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.theme.Space

// P-14 Acceso por usuario — RNF-12 (cada acción queda con el usuario), RNF-05 (datos en el teléfono).
// Si todavía no hay usuarios, la pantalla crea el primero (uso con usuarios de prueba en el prototipo).

data class AccessUiState(
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,      // true: crear usuario; false: ingresar
    val username: String = "",
    val pin: String = "",
    val pinRepeat: String = "",
    val error: String? = null,
    val isWorking: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isWorking && username.isNotBlank() && pin.isNotBlank() && (!isCreating || pinRepeat.isNotBlank())
}

class AccessViewModel(private val access: AccessRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AccessUiState())
    val uiState: StateFlow<AccessUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasUsers = access.hasUsers()
            _uiState.update { it.copy(isLoading = false, isCreating = !hasUsers) }
        }
    }

    fun onUsername(v: String) = _uiState.update { it.copy(username = v, error = null) }
    fun onPin(v: String) = _uiState.update { it.copy(pin = v.filter(Char::isDigit).take(8), error = null) }
    fun onPinRepeat(v: String) = _uiState.update { it.copy(pinRepeat = v.filter(Char::isDigit).take(8), error = null) }
    fun toggleMode() = _uiState.update { it.copy(isCreating = !it.isCreating, pin = "", pinRepeat = "", error = null) }

    fun submit() {
        val s = _uiState.value
        if (!s.canSubmit) return
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val error = if (s.isCreating) {
                if (s.pin != s.pinRepeat) "Los dos PIN no coinciden." else access.createUser(s.username, s.pin)
            } else {
                if (access.login(s.username, s.pin)) null else "Usuario o PIN incorrecto."
            }
            _uiState.update { it.copy(isWorking = false, error = error, pin = if (error != null) "" else it.pin) }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AccessViewModel(appContainer().accessRepository) }
        }
    }
}

@Composable
fun AccessScreen(viewModel: AccessViewModel = viewModel(factory = AccessViewModel.factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccessContent(
        uiState = uiState,
        onUsername = viewModel::onUsername,
        onPin = viewModel::onPin,
        onPinRepeat = viewModel::onPinRepeat,
        onSubmit = viewModel::submit,
        onToggleMode = viewModel::toggleMode
    )
}

@Composable
fun AccessContent(
    uiState: AccessUiState,
    onUsername: (String) -> Unit,
    onPin: (String) -> Unit,
    onPinRepeat: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = if (uiState.isCreating) "Crear usuario" else "Ingreso", onBack = null) },
        bottomBar = {
            if (!uiState.isLoading) {
                BottomActionBar(
                    primaryLabel = if (uiState.isCreating) "Crear usuario e ingresar" else "Ingresar",
                    onPrimary = onSubmit,
                    primaryIcon = if (uiState.isCreating) Icons.Filled.PersonAdd else Icons.AutoMirrored.Filled.Login,
                    enabled = uiState.canSubmit,
                    disabledReason = if (uiState.isWorking) "Verificando…" else "Complete el usuario y el PIN.",
                    secondaryLabel = if (uiState.isCreating) "Ya tengo usuario" else "Crear otro usuario",
                    onSecondary = onToggleMode,
                    secondaryIsText = true
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
            Text(
                if (uiState.isCreating) "Cree el usuario del establecimiento con el que trabajará en este teléfono."
                else "Ingrese con su usuario del establecimiento.",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = uiState.username,
                onValueChange = onUsername,
                label = { Text("Usuario del establecimiento") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.pin,
                onValueChange = onPin,
                label = { Text("PIN (4 a 8 dígitos)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.isCreating) {
                OutlinedTextField(
                    value = uiState.pinRepeat,
                    onValueChange = onPinRepeat,
                    label = { Text("Repita el PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            uiState.error?.let { NoticeCard(title = "No se pudo ingresar", text = it, kind = NoticeKind.Error) }
            NoticeCard(
                title = "Registro de acciones",
                text = "Cada acción sobre los datos (ver, crear, modificar, exportar) queda registrada con su usuario. " +
                    "Los datos se guardan solo en este teléfono.",
                kind = NoticeKind.Info
            )
        }
    }
}
