package pe.edu.pucp.melanomaacral.ui.screens.patients

import pe.edu.pucp.melanomaacral.ui.components.PendingEvaluationPill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import pe.edu.pucp.melanomaacral.ui.components.EmptyState
import pe.edu.pucp.melanomaacral.ui.components.OverdueFollowUpPill
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.message
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.time.LocalDate

// P-02 Pacientes — RF-10.
@Composable
fun PatientsScreen(
    onOpenPatient: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PatientsViewModel = viewModel(factory = PatientsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PatientsContent(
        uiState = uiState,
        onOpenPatient = onOpenPatient,
        onAddPatient = { viewModel.openNewPatientDialog() },
        onNewPatientIdChange = { viewModel.onNewPatientIdChange(it) },
        onConfirmNewPatient = { viewModel.createPatient()?.let(onOpenPatient) },
        onDismissNewPatient = { viewModel.dismissNewPatientDialog() },
        onBack = onBack
    )
}

@Composable
fun PatientsContent(
    uiState: PatientsUiState,
    onOpenPatient: (String) -> Unit,
    onAddPatient: () -> Unit,
    onNewPatientIdChange: (String) -> Unit,
    onConfirmNewPatient: () -> Unit,
    onDismissNewPatient: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = uiState.patients.filter { it.id.contains(query.trim(), ignoreCase = true) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Pacientes", onBack = onBack) },
        bottomBar = {
            if (uiState.patients.isNotEmpty()) {
                BottomActionBar(
                    primaryLabel = "Nuevo paciente",
                    onPrimary = onAddPatient,
                    primaryIcon = Icons.Filled.PersonAdd
                )
            }
        }
    ) { innerPadding ->
        if (uiState.patients.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Groups,
                title = "Aún no hay pacientes",
                text = "Registra al primer paciente con el código anónimo que asigna tu establecimiento.",
                actionLabel = "Nuevo paciente",
                onAction = onAddPatient,
                modifier = Modifier.padding(innerPadding).padding(top = Space.xxl)
            )
        } else {
            // LazyColumn: lista que solo dibuja los elementos visibles (sirve para listas largas).
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.l, end = Space.l, top = innerPadding.calculateTopPadding() + Space.s, bottom = innerPadding.calculateBottomPadding() + Space.l
                ),
                verticalArrangement = Arrangement.spacedBy(Space.m),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.patients.size > SEARCH_FROM) {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Buscar por código") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (visible.isEmpty()) {
                    item { Text("Ningún paciente coincide con «$query».", style = MaterialTheme.typography.bodyLarge) }
                }
                items(visible, key = { it.id }) { patient ->
                    PatientRow(patient = patient, onClick = { onOpenPatient(patient.id) })
                }
            }
        }

        if (uiState.dialog.isOpen) {
            NewPatientDialog(
                state = uiState.dialog,
                onIdChange = onNewPatientIdChange,
                onConfirm = onConfirmNewPatient,
                onDismiss = onDismissNewPatient
            )
        }
    }
}

// Con más de esta cantidad de pacientes aparece la búsqueda.
private const val SEARCH_FROM = 5

@Composable
private fun PatientRow(patient: PatientSummary, onClick: () -> Unit, today: LocalDate = LocalDate.now()) {
    val lastEvaluation = patient.lastEvaluation?.let { "Última evaluación: ${it.formatted()}" } ?: "Sin evaluaciones"
    val lesions = if (patient.lesionCount == 1) "1 lesión" else "${patient.lesionCount} lesiones"
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            modifier = Modifier.padding(Space.l)
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(patient.id, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$lesions · $lastEvaluation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (patient.hasOverdueFollowUp) {
                    OverdueFollowUpPill()
                }
                if (patient.pendingEvaluations > 0) {
                    PendingEvaluationPill(
                        if (patient.pendingEvaluations == 1) "Evaluación sin terminar"
                        else "${patient.pendingEvaluations} evaluaciones sin terminar"
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NewPatientDialog(
    state: NewPatientDialogState,
    onIdChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
        title = { Text("Nuevo paciente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text(
                    "Escribe el código con el que tu establecimiento identifica al paciente, por ejemplo su número de " +
                        "historia clínica. Lo asigna el establecimiento, no la app: sirve para encontrar al paciente aquí y " +
                        "en su historia. No escribas nombre ni número de documento (DNI): la app no guarda datos que " +
                        "identifiquen a la persona.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.idText,
                    onValueChange = onIdChange,
                    label = { Text("Código del paciente") },
                    singleLine = true,
                    isError = state.error != null,
                    leadingIcon = if (state.error != null) {
                        { Icon(Icons.Filled.ErrorOutline, contentDescription = "Error") }
                    } else null,
                    supportingText = { Text(state.error?.message() ?: "Por ejemplo: HC-004512") }
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Preview(showBackground = true)
@Composable
fun PatientsPreview() {
    MelanomaAcralTheme {
        PatientsContent(
            uiState = PatientsUiState(
                patients = listOf(
                    PatientSummary("PAC-001", 1, LocalDate.of(2026, 7, 15), hasOverdueFollowUp = true),
                    PatientSummary("PAC-002", 1, LocalDate.of(2026, 9, 27), hasOverdueFollowUp = false)
                )
            ),
            onOpenPatient = {}, onAddPatient = {}, onNewPatientIdChange = {},
            onConfirmNewPatient = {}, onDismissNewPatient = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Diálogo con error")
@Composable
fun NewPatientDialogPreview() {
    MelanomaAcralTheme {
        NewPatientDialog(
            state = NewPatientDialogState(isOpen = true, idText = "45678912", error = PatientIdError.LOOKS_LIKE_DNI),
            onIdChange = {}, onConfirm = {}, onDismiss = {}
        )
    }
}
