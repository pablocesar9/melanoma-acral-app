package pe.edu.pucp.melanomaacral.ui.screens.lesion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.EmptyState
import pe.edu.pucp.melanomaacral.ui.components.FollowUpStatusPill
import pe.edu.pucp.melanomaacral.ui.components.RecommendationPill
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.ui.yesNo
import pe.edu.pucp.melanomaacral.ui.description
import pe.edu.pucp.melanomaacral.ui.components.ModelScoreBar
import pe.edu.pucp.melanomaacral.ui.components.ModelScore
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.time.LocalDate

// P-04 Lesión (historial) — RF-11, RF-12, RF-14.
@Composable
fun LesionScreen(
    lesionId: String,
    onNewCapture: () -> Unit,
    onResume: (EvaluationDraft) -> Unit,
    onReferral: () -> Unit,
    onBack: () -> Unit,
    viewModel: LesionViewModel = viewModel(factory = LesionViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var blocked by remember { mutableStateOf(false) }
    LesionContent(
        uiState = uiState,
        onNewCapture = onNewCapture,
        onReferral = onReferral,
        onBack = onBack,
        onResume = onResume,
        onDiscardDraft = viewModel::discardDraft,
        onEdit = { ev -> viewModel.startEditing(ev, onReady = onResume, onBlocked = { blocked = true }) }
    )
    if (blocked) {
        AlertDialog(
            onDismissRequest = { blocked = false },
            title = { Text("Hay una evaluación sin terminar") },
            text = { Text("Termínala o descártala antes de editar otra evaluación de esta lesión.") },
            confirmButton = { TextButton(onClick = { blocked = false }) { Text("Entendido") } }
        )
    }
}

@Composable
fun LesionContent(
    uiState: LesionUiState,
    onNewCapture: () -> Unit,
    onReferral: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onResume: (EvaluationDraft) -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    onEdit: (Evaluation) -> Unit = {}
) {
    var viewing by remember { mutableStateOf<Evaluation?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val draft = uiState.draft
    val subtitle = listOfNotNull(
        uiState.patientId?.let { "Paciente $it" },
        uiState.location?.label()
    ).joinToString(" · ")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Lesión ${uiState.lesionId}", onBack = onBack, subtitle = subtitle) },
        bottomBar = {
            BottomActionBar(
                primaryLabel = when {
                    draft != null -> "Continuar evaluación"
                    uiState.evaluations.isEmpty() -> "Primera captura"
                    else -> "Nuevo control"
                },
                onPrimary = { if (draft != null) onResume(draft) else onNewCapture() },
                primaryIcon = if (draft != null) Icons.Filled.PlayArrow else Icons.Filled.AddAPhoto,
                secondaryLabel = "Preparar derivación",
                onSecondary = if (uiState.evaluations.isNotEmpty()) onReferral else null,
                secondaryIsText = true
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Space.l)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            draft?.let { PendingEvaluationCard(it, onDiscard = { confirmDiscard = true }) }

            FollowUpSummary(uiState)

            // RF-12: comparación lado a lado de las dos capturas más recientes.
            if (uiState.evaluations.size >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    SectionTitle("Comparación")
                    val (previous, latest) = uiState.evaluations.takeLast(2)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        ComparisonColumn(previous, title = "Anterior", modifier = Modifier.weight(1f))
                        ComparisonColumn(latest, title = "Más reciente", modifier = Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                SectionTitle("Historial de capturas")
                if (uiState.evaluations.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "Aún no hay capturas",
                        text = "Toma la foto clínica y la imagen dermoscópica para obtener una recomendación."
                    )
                }
                // Historial: la más reciente primero.
                for (evaluation in uiState.evaluations.asReversed()) {
                    HistoryCard(evaluation, onClick = { viewing = evaluation })
                }
            }
        }
    }
    viewing?.let { evaluation ->
        EvaluationDialog(evaluation = evaluation, onDismiss = { viewing = null }, onEdit = { viewing = null; onEdit(evaluation) })
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar la evaluación sin terminar?") },
            text = { Text("Se borrarán las fotos y los datos que ya ingresaste. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscardDraft()
                }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancelar") } }
        )
    }
}

// Evaluación empezada y sin terminar: se ve arriba para retomarla (con el botón principal) o descartarla.
@Composable
private fun PendingEvaluationCard(draft: EvaluationDraft, onDiscard: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Space.l),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            draft.context?.let {
                LesionImageView(
                    it, label = "Foto clínica de la evaluación sin terminar",
                    modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
                    maxSidePx = 256, contentScale = ContentScale.Crop, fillAspect = false
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Icon(Icons.Filled.PendingActions, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Evaluación sin terminar", style = MaterialTheme.typography.titleMedium)
                }
                Text(draft.stageLabel + ".", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onDiscard) { Text("Descartar") }
            }
        }
    }
}

// Ficha de una evaluación guardada: imágenes, resultado, datos clínicos y anotaciones, con la opción de editarla.
@Composable
private fun EvaluationDialog(evaluation: Evaluation, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val c = evaluation.clinicalData
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        dismissButton = { TextButton(onClick = onEdit) { Text("Editar") } },
        title = { Text("Evaluación ${evaluation.id} · ${evaluation.date.formatted()}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.m)
            ) {
                evaluation.editedAtMillis?.let {
                    Text("Editada después de guardarla.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RecommendationPill(evaluation.recommendation.action)
                Text(evaluation.recommendation.reason.description(), style = MaterialTheme.typography.bodyMedium)
                if (!evaluation.prediction.isOutOfScope) {
                    Text(ModelScore.summary(evaluation.prediction) + if (evaluation.prediction.isSimulated) " (SIMULADO)." else ".",
                        style = MaterialTheme.typography.bodyMedium)
                    ModelScoreBar(evaluation.prediction)
                }
                Text("Datos clínicos", style = MaterialTheme.typography.labelLarge)
                val sintomas = listOfNotNull("sangra".takeIf { c.hasBleeding }, "duele".takeIf { c.hasPain }, "pica".takeIf { c.hasItching })
                Text(
                    listOfNotNull(
                        c.ageYears?.let { "Edad: $it años" }, c.sex?.let { "Sexo: ${it.label()}" },
                        "Localización: ${c.location.label()}", "Diámetro: ${c.maxDiameterMm} mm",
                        "Elevada o palpable: ${c.isElevatedOrPalpable.yesNo()}", "No cura: ${c.isNotHealing.yesNo()}", "Crece rápido: ${c.isGrowingRapidly.yesNo()}",
                        "Cambios: ${c.reportedChange.label()}", "Evolución: ${c.evolutionTime.label()}",
                        "Síntomas: ${if (sintomas.isEmpty()) "ninguno" else sintomas.joinToString(", ")}",
                        "Antecedente de cáncer de piel: ${c.hasSkinCancerHistory.yesNo()}",
                        "Procedimiento previo: ${c.hadPreviousProcedure.yesNo()}"
                    ).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (c.notes.isNotBlank()) {
                    Text("Anotaciones", style = MaterialTheme.typography.labelLarge)
                    Text(c.notes, style = MaterialTheme.typography.bodyMedium)
                }
                Text("Imagen dermoscópica", style = MaterialTheme.typography.labelLarge)
                LesionImageView(evaluation.images.dermoscopic, label = "Imagen dermoscópica", maxSidePx = 1600)
                evaluation.images.context?.let {
                    Text("Foto clínica de contexto", style = MaterialTheme.typography.labelLarge)
                    LesionImageView(it, label = "Foto clínica de contexto", maxSidePx = 1600)
                }
            }
        }
    )
}

@Composable
private fun HistoryCard(evaluation: Evaluation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            modifier = Modifier.padding(Space.m)
        ) {
            LesionImageView(
                evaluation.images.dermoscopic,
                label = "Imagen dermoscópica del ${evaluation.date.formatted()}",
                modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small),
                maxSidePx = 256,
                contentScale = ContentScale.Crop,
                fillAspect = false
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(evaluation.date.formatted(), style = MaterialTheme.typography.titleMedium)
                RecommendationPill(evaluation.recommendation.action)
                Text(
                    buildString {
                        append(if (evaluation.prediction.suggestsMelanoma) "El modelo sugiere melanoma" else "El modelo no sugiere melanoma")
                        if (evaluation.prediction.isSimulated) append(" (SIMULADO)")
                        if (evaluation.editedAtMillis != null) append(" · editada")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Estado del control arriba: pendiente, hoy o vencido (con icono y texto).
@Composable
private fun FollowUpSummary(uiState: LesionUiState) {
    val date = uiState.nextFollowUp
    if (date == null) {
        Text("Sin control programado.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FollowUpStatusPill(date)
    }
}

@Composable
private fun ComparisonColumn(evaluation: Evaluation, title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LesionImageView(
            evaluation.images.dermoscopic,
            label = "Imagen dermoscópica del ${evaluation.date.formatted()}",
            modifier = Modifier.clip(MaterialTheme.shapes.small)
        )
        Text(evaluation.date.formatted(), style = MaterialTheme.typography.bodyMedium)
        Text(
            (if (evaluation.prediction.suggestsMelanoma) "Sugiere melanoma" else "No sugiere melanoma") +
                (if (evaluation.prediction.isSimulated) " (simulado)" else "") + " · ${evaluation.clinicalData.maxDiameterMm} mm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun LesionPreview() {
    val today = LocalDate.of(2026, 10, 27)
    MelanomaAcralTheme {
        LesionContent(
            uiState = LesionUiState(
                lesionId = "LES-001",
                patientId = "PAC-001",
                location = AcralLocation.SOLE,
                nextFollowUp = today.minusDays(12),
                isOverdue = true,
                evaluations = SampleRecords.evaluations(today).filter { it.lesionId == "LES-001" }
            ),
            onNewCapture = {}, onReferral = {}, onBack = {}
        )
    }
}
