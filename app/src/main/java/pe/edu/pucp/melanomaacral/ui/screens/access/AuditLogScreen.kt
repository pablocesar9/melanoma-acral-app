package pe.edu.pucp.melanomaacral.ui.screens.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.model.AuditEntry
import pe.edu.pucp.melanomaacral.ui.appContainer
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Registro de acciones sobre los datos (RNF-12): quién hizo qué y cuándo, del más reciente al más antiguo.

class AuditLogViewModel(log: kotlinx.coroutines.flow.Flow<List<AuditEntry>>) : ViewModel() {
    val entries: StateFlow<List<AuditEntry>?> = log.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuditLogViewModel(appContainer().accessRepository.log) }
        }
    }
}

fun AuditAction.label(): String = when (this) {
    AuditAction.LOGIN -> "Ingresó"
    AuditAction.LOGOUT -> "Cerró sesión"
    AuditAction.VIEW -> "Vio"
    AuditAction.CREATE -> "Creó"
    AuditAction.MODIFY -> "Modificó"
    AuditAction.EXPORT -> "Exportó"
}

private val FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

@Composable
fun AuditLogScreen(onBack: () -> Unit, viewModel: AuditLogViewModel = viewModel(factory = AuditLogViewModel.factory)) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    AuditLogContent(entries = entries.orEmpty(), onBack = onBack)
}

@Composable
fun AuditLogContent(entries: List<AuditEntry>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Registro de acciones", subtitle = "${entries.size} acciones", onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
            items(entries, key = { it.id }) { e ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.m),
                    verticalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Text("${e.username} · ${e.action.label()}", style = MaterialTheme.typography.titleSmall)
                    Text(e.target, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        FORMAT.format(Instant.ofEpochMilli(e.atMillis).atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
