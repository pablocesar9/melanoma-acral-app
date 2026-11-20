package pe.edu.pucp.melanomaacral.ui.screens.clinicaldata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.ChoiceChips
import pe.edu.pucp.melanomaacral.ui.components.EvaluationStep
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.SegmentedChoice
import pe.edu.pucp.melanomaacral.ui.components.StepIndicator
import pe.edu.pucp.melanomaacral.ui.components.ToggleChip
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex
import pe.edu.pucp.melanomaacral.domain.rules.RecommendationRules
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.components.SwitchRow
import pe.edu.pucp.melanomaacral.ui.diameterErrorMessage
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import kotlin.math.roundToInt

// P-07 Datos clínicos — RF-06, RF-07, RF-09 (captura de las exclusiones), RF-16.
@Composable
fun ClinicalDataScreen(
    lesionId: String,
    onSubmit: (ClinicalData) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    viewModel: ClinicalDataViewModel = viewModel(factory = ClinicalDataViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefilled by viewModel.prefilled.collectAsStateWithLifecycle()
    val editingId by viewModel.editingId.collectAsStateWithLifecycle()
    val back = {
        viewModel.onBackToImages()
        onBack()
    }
    BackHandler(onBack = back)
    ClinicalDataContent(
        uiState = uiState,
        onChange = { viewModel.onFormChange(it) },
        onSubmit = onSubmit,
        onBack = back,
        onExit = onExit,
        prefilled = prefilled,
        editingId = editingId
    )
}

@Composable
fun ClinicalDataContent(
    uiState: ClinicalDataUiState,
    onChange: (ClinicalDataUiState) -> Unit,
    onSubmit: (ClinicalData) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit = {},
    prefilled: Boolean = false,
    editingId: String? = null,
    modifier: Modifier = Modifier
) {
    val limit = RecommendationRules.DIAMETER_LIMIT_MM.roundToInt()
    val missing = buildList {
        if (uiState.ageYears == null) add("edad")
        if (uiState.sex == null) add("sexo")
        if (uiState.location == null) add("localización")
        if (uiState.diameterMm == null) add("diámetro")
        if (uiState.reportedChange == null) add("cambios reportados")
        if (uiState.evolutionTime == null) add("tiempo de evolución")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Datos clínicos",
                onBack = onBack,
                subtitle = "Se guarda sola si sales",
                actions = { androidx.compose.material3.TextButton(onClick = onExit) { Text("Salir") } }
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryLabel = "Ver resultado",
                onPrimary = { uiState.toClinicalDataOrNull()?.let(onSubmit) },
                primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
                enabled = uiState.canSubmit,
                disabledReason = "Falta: " + missing.joinToString(", ") + "."
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
            StepIndicator(EvaluationStep.ClinicalData)
            Text(
                "Los campos con * son obligatorios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            editingId?.let {
                NoticeCard(
                    title = "Editando la evaluación $it",
                    text = "Los datos guardados están precargados. Al ver el resultado se recalcula y reemplaza esa evaluación; " +
                        "con «Atrás» puedes volver a las fotos sin perder nada.",
                    kind = NoticeKind.Info
                )
            }
            if (prefilled) {
                NoticeCard(
                    title = "Datos de la evaluación anterior",
                    text = "Se copiaron la localización, la edad, el sexo y los antecedentes de esta lesión. Revísalos; " +
                        "el diámetro, los cambios, los síntomas y los signos de alarma se llenan de nuevo.",
                    kind = NoticeKind.Info
                )
            }

            FormSection("Lesión") {
                Text("Localización *", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(
                    options = AcralLocation.entries,
                    selected = uiState.location,
                    label = { it.label() },
                    onSelect = { onChange(uiState.copy(location = it)) }
                )
                OutlinedTextField(
                    value = uiState.diameterText,
                    onValueChange = { onChange(uiState.copy(diameterText = it)) },
                    label = { Text("Diámetro máximo *") },
                    suffix = { Text("mm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.showDiameterError,
                    supportingText = {
                        Text(if (uiState.showDiameterError) diameterErrorMessage() else "Más de $limit mm se recomienda derivar.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FormSection("Paciente") {
                OutlinedTextField(
                    value = uiState.ageText,
                    onValueChange = { onChange(uiState.copy(ageText = it)) },
                    label = { Text("Edad *") },
                    suffix = { Text("años") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.showAgeError,
                    supportingText = { if (uiState.showAgeError) Text("Escribe una edad entre 0 y 120.") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Sexo *", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(
                    options = Sex.entries,
                    selected = uiState.sex,
                    label = { it.label() },
                    onSelect = { onChange(uiState.copy(sex = it)) }
                )
            }

            FormSection("Historia de la lesión") {
                Text("¿El paciente reporta cambios en la lesión? *", style = MaterialTheme.typography.titleSmall)
                SegmentedChoice(
                    options = ReportedChange.entries,
                    selected = uiState.reportedChange,
                    label = { it.label() },
                    onSelect = { onChange(uiState.copy(reportedChange = it)) }
                )
                Text("Tiempo de evolución *", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(
                    options = EvolutionTime.entries,
                    selected = uiState.evolutionTime,
                    label = { it.label() },
                    onSelect = { onChange(uiState.copy(evolutionTime = it)) }
                )
                Text("Antecedentes", style = MaterialTheme.typography.titleSmall)
                ToggleChip(
                    "Cáncer de piel personal o familiar",
                    uiState.hasSkinCancerHistory
                ) { onChange(uiState.copy(hasSkinCancerHistory = it)) }
                ToggleChip(
                    "Procedimiento previo en la zona",
                    uiState.hadPreviousProcedure
                ) { onChange(uiState.copy(hadPreviousProcedure = it)) }
            }

            FormSection("Síntomas") {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    ToggleChip("Sangra", uiState.hasBleeding) { onChange(uiState.copy(hasBleeding = it)) }
                    ToggleChip("Duele", uiState.hasPain) { onChange(uiState.copy(hasPain = it)) }
                    ToggleChip("Pica", uiState.hasItching) { onChange(uiState.copy(hasItching = it)) }
                }
            }

            FormSection("Signos de alarma") {
                SwitchRow(
                    label = "Elevada o palpable",
                    checked = uiState.isElevatedOrPalpable,
                    onCheckedChange = { onChange(uiState.copy(isElevatedOrPalpable = it)) }
                )
                SwitchRow(
                    label = "No cura",
                    checked = uiState.isNotHealing,
                    onCheckedChange = { onChange(uiState.copy(isNotHealing = it)) }
                )
                SwitchRow(
                    label = "Crece rápido",
                    checked = uiState.isGrowingRapidly,
                    onCheckedChange = { onChange(uiState.copy(isGrowingRapidly = it)) }
                )
                NoticeCard(
                    title = "Criterios de derivación",
                    text = "Si se cumple alguno de estos signos, o si la lesión cambió, sangra, duele o pica, la lesión no se monitorea: la recomendación será derivar.",
                    kind = NoticeKind.Info
                )
            }

            FormSection("Anotaciones (opcional)") {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { if (it.length <= NOTES_MAX) onChange(uiState.copy(notes = it)) },
                    label = { Text("Anotaciones del profesional") },
                    placeholder = { Text("Por ejemplo: contexto, hallazgos, indicaciones dadas") },
                    supportingText = { Text("${uiState.notes.length} / $NOTES_MAX. Sin nombres ni documentos del paciente.") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private const val NOTES_MAX = 500

// Sección del formulario: encabezado y su contenido en una tarjeta.
@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        SectionTitle(title)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m), content = content)
        }
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
fun ClinicalDataPreview() {
    MelanomaAcralTheme {
        ClinicalDataContent(
            uiState = ClinicalDataUiState(location = AcralLocation.SOLE, diameterText = "abc"),
            onChange = {}, onSubmit = {}, onBack = {}
        )
    }
}
