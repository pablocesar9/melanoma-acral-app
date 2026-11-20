package pe.edu.pucp.melanomaacral.ui.screens.patient

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.EmptyState
import pe.edu.pucp.melanomaacral.ui.components.FollowUpStatusPill
import pe.edu.pucp.melanomaacral.ui.components.PendingEvaluationPill
import pe.edu.pucp.melanomaacral.ui.components.RecommendationPill
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.time.LocalDate

// P-03 Paciente — RF-11.
@Composable
fun PatientScreen(
    patientId: String,
    onOpenLesion: (String) -> Unit,
    onLesionCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PatientViewModel = viewModel(factory = PatientViewModel.factory(patientId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PatientContent(
        uiState = uiState,
        onOpenLesion = onOpenLesion,
        onNewLesion = { viewModel.createLesion(onCreated = onLesionCreated) },
        onBack = onBack
    )
}

@Composable
fun PatientContent(
    uiState: PatientUiState,
    onOpenLesion: (String) -> Unit,
    onNewLesion: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Paciente ${uiState.patientId}", onBack = onBack, subtitle = "Lesiones en seguimiento") },
        bottomBar = {
            BottomActionBar(
                primaryLabel = "Nueva lesión",
                onPrimary = onNewLesion,
                primaryIcon = Icons.Filled.AddCircle
            )
        }
    ) { innerPadding ->
        if (uiState.lesions.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Healing,
                title = "Sin lesiones registradas",
                text = "Registra una lesión nueva para tomar sus fotos y obtener una recomendación.",
                modifier = Modifier.padding(innerPadding).padding(top = Space.xxl)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.l, end = Space.l, top = innerPadding.calculateTopPadding() + Space.s,
                    bottom = innerPadding.calculateBottomPadding() + Space.l
                ),
                verticalArrangement = Arrangement.spacedBy(Space.m),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.lesions, key = { it.id }) { lesion ->
                    LesionRow(lesion = lesion, onClick = { onOpenLesion(lesion.id) })
                }
            }
        }
    }
}

@Composable
private fun LesionRow(lesion: LesionSummary, onClick: () -> Unit, today: LocalDate = LocalDate.now()) {
    val title = lesion.location?.let { "${lesion.id} · ${it.label()}" } ?: lesion.id
    val captures = when (lesion.captureCount) {
        0 -> "Sin capturas"
        1 -> "1 captura"
        else -> "${lesion.captureCount} capturas"
    }
    val last = lesion.lastCapture?.let { " · última: ${it.formatted()}" } ?: ""
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    captures + last,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s), modifier = Modifier.fillMaxWidth()) {
                    lesion.lastAction?.let { RecommendationPill(it) }
                }
                FollowUpStatusPill(lesion.nextFollowUp, today)
                if (lesion.hasPendingEvaluation) PendingEvaluationPill()
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PatientPreview() {
    MelanomaAcralTheme {
        PatientContent(
            uiState = PatientUiState(
                patientId = "PAC-001",
                lesions = listOf(
                    LesionSummary("LES-001", AcralLocation.SOLE, 2, LocalDate.of(2026, 7, 15), LocalDate.of(2026, 10, 15), isOverdue = true, lastAction = ManagementAction.FOLLOW_UP),
                    LesionSummary("LES-002", AcralLocation.PALM, 1, LocalDate.of(2026, 9, 2), null, isOverdue = false, lastAction = ManagementAction.REFER),
                    LesionSummary("LES-003", null, 0, null, null, isOverdue = false)
                )
            ),
            onOpenLesion = {}, onNewLesion = {}, onBack = {}
        )
    }
}
