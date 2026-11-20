package pe.edu.pucp.melanomaacral.ui.screens.result

import androidx.compose.foundation.Canvas
import pe.edu.pucp.melanomaacral.ui.formatted
import java.time.LocalDate
import pe.edu.pucp.melanomaacral.ui.components.ModelScoreBar
import pe.edu.pucp.melanomaacral.ui.components.ModelScore
import pe.edu.pucp.melanomaacral.data.real.EscalaJet
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.remember
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.EvaluationStep
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.RecommendationCard
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.components.StepIndicator
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import pe.edu.pucp.melanomaacral.ui.theme.semantic
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.SimulatedNotice
import pe.edu.pucp.melanomaacral.ui.components.SwitchRow
import pe.edu.pucp.melanomaacral.ui.description
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme

// P-08 Resultado — RF-04, RF-05, RF-08, RF-09, RF-20, RNF-10.

// Versión "con estado": obtiene el ViewModel y observa su uiState. Es la que usa la navegación.
@Composable
fun ResultScreen(
    lesionId: String,
    images: CapturedImages,
    clinicalData: ClinicalData,
    onDone: () -> Unit,
    onScheduleFollowUp: () -> Unit,
    onReferral: () -> Unit,
    onShowLimitations: () -> Unit,
    onBack: () -> Unit,
    onEdit: (DraftStage) -> Unit = {},
    viewModel: ResultViewModel = viewModel(factory = ResultViewModel.factory(lesionId, images, clinicalData))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nextFollowUp by viewModel.nextFollowUp.collectAsStateWithLifecycle()
    // Ya guardada, «Atrás» no puede reenviar el formulario (duplicaría la evaluación): abre la edición en los datos
    // clínicos, con todo precargado; al terminar reemplaza la evaluación guardada.
    val edit: (DraftStage) -> Unit = { stage -> viewModel.startEditing(stage) { onEdit(stage) } }
    val back = { if (uiState is ResultUiState.Success) edit(DraftStage.CLINICAL_DATA) else onBack() }
    BackHandler(onBack = back)
    ResultContent(
        uiState = uiState,
        onDone = onDone,
        onScheduleFollowUp = onScheduleFollowUp,
        onReferral = onReferral,
        onShowLimitations = onShowLimitations,
        onBack = back,
        onEdit = { edit(DraftStage.CONTEXT) },
        nextFollowUp = nextFollowUp
    )
}

// Versión "sin estado": recibe el estado y emite eventos. No sabe de dónde vienen los datos.
// Es la que usan los @Preview.
@Composable
fun ResultContent(
    uiState: ResultUiState,
    onDone: () -> Unit,
    onScheduleFollowUp: () -> Unit,
    onReferral: () -> Unit,
    onShowLimitations: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    nextFollowUp: LocalDate? = null,
    modifier: Modifier = Modifier
) {
    val success = uiState as? ResultUiState.Success
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Resultado", onBack = onBack) },
        bottomBar = {
            if (success != null) {
                val refer = success.recommendation.action == ManagementAction.REFER
                if (success.recommendation.action == ManagementAction.NONE) {
                    // RF-05: imagen fuera de alcance, sin recomendación: solo se puede volver a la lesión.
                    BottomActionBar(primaryLabel = "Guardar y volver a la lesión", onPrimary = onDone)
                } else BottomActionBar(
                    primaryLabel = if (refer) "Preparar derivación" else if (nextFollowUp != null) "Cambiar control" else "Programar control",
                    onPrimary = if (refer) onReferral else onScheduleFollowUp,
                    primaryIcon = if (refer) Icons.Filled.Description else Icons.Filled.Event,
                    enabled = refer || success.recommendation.followUpAllowed,
                    secondaryLabel = "Guardar y volver a la lesión",
                    onSecondary = onDone,
                    secondaryIsText = true
                )
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is ResultUiState.Loading -> LoadingContent("Analizando imagen…", Modifier.padding(innerPadding))
            is ResultUiState.Success -> SuccessContent(
                state = uiState,
                onScheduleFollowUp = onScheduleFollowUp,
                onReferral = onReferral,
                onShowLimitations = onShowLimitations,
                onEdit = onEdit,
                nextFollowUp = nextFollowUp,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    state: ResultUiState.Success,
    onScheduleFollowUp: () -> Unit,
    onReferral: () -> Unit,
    onShowLimitations: () -> Unit,
    onEdit: () -> Unit,
    nextFollowUp: LocalDate?,
    modifier: Modifier = Modifier
) {
    val prediction = state.prediction
    val recommendation = state.recommendation
    val clinical = state.clinicalData
    val refer = recommendation.action == ManagementAction.REFER
    var showDetails by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Space.l)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        StepIndicator(EvaluationStep.Result)

        // Confirmación de guardado, arriba y visible (antes quedaba al final de la página).
        NoticeCard(
            title = if (state.wasEdited) "Edición guardada" else "Evaluación guardada",
            text = (state.evaluationId?.let { "Quedó en el historial de la lesión como $it. " } ?: "Quedó en el historial de la lesión. ") +
                "Puedes editarla con «Editar esta evaluación» o con «Atrás».",
            kind = NoticeKind.Saved
        )

        nextFollowUp?.let {
            NoticeCard(
                title = "Control programado para el ${it.formatted()}",
                text = "Puedes cambiarlo. Cuando termines, usa «Guardar y volver a la lesión».",
                kind = NoticeKind.Saved
            )
        }

        // 1) Qué hacer: la recomendación, primero y grande.
        // RF-05: con la imagen fuera de alcance no hay recomendación; solo el aviso de abajo.
        val evaluable = recommendation.action != ManagementAction.NONE
        if (evaluable) {
            RecommendationCard(action = recommendation.action, reason = recommendation.reason.description())
        }

        // Todo lo simulado se ve como SIMULADO.
        if (prediction.isSimulated) {
            SimulatedNotice(
                title = "RESULTADO SIMULADO",
                text = "Generado por un clasificador de prueba. No usar para decisiones clínicas."
            )
        }

        // 2) Por qué: aviso de fuera de alcance, o la probabilidad del modelo.
        if (prediction.isOutOfScope) {
            NoticeCard(
                title = "Imagen fuera del alcance del modelo",
                text = "La imagen no parece una lesión acral dermoscópica o no se puede evaluar, por eso no se muestra un resultado ni una recomendación. Para repetir la captura, usa «Editar esta evaluación».",
                kind = NoticeKind.Warning
            )
        } else {
            // RF-05: fuera de alcance no se muestra el resultado del modelo (no es confiable).
            ScoreSection(prediction)
        }

        // 3) Cómo se ve: imagen y mapa de calor (opcional).
        state.images?.let { images ->
            HeatmapSection(images.dermoscopic, state.explanation, prediction.isSimulated)
        }

        // 4) Otras acciones, con el motivo cuando están bloqueadas.
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            SectionTitle("Otras acciones")
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(Size.minTouch)) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(Size.iconInCard))
                Spacer(Modifier.size(Space.s))
                Text("Editar esta evaluación")
            }
            if (!evaluable) {
                // Sin recomendación no hay control que programar ni derivación que preparar (RF-05).
            } else if (refer) {
                OutlinedButton(
                    onClick = onScheduleFollowUp,
                    enabled = recommendation.followUpAllowed,
                    modifier = Modifier.fillMaxWidth().height(Size.minTouch)
                ) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(Size.iconInCard))
                    Spacer(Modifier.size(Space.s))
                    Text("Programar control")
                }
                if (!recommendation.followUpAllowed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "No se puede programar control: esta lesión debe derivarse.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                OutlinedButton(onClick = onReferral, modifier = Modifier.fillMaxWidth().height(Size.minTouch)) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(Size.iconInCard))
                    Spacer(Modifier.size(Space.s))
                    Text("Preparar derivación")
                }
            }
        }

        // 5) Límites: el aviso de apoyo siempre visible (RF-20, RNF-11).
        NoticeCard(
            title = "Herramienta de apoyo",
            text = "No reemplaza el juicio clínico ni la evaluación del especialista.",
            kind = NoticeKind.Support
        )
        TextButton(onClick = onShowLimitations) { Text("Ver limitaciones del sistema") }

        // 6) Detalles técnicos plegados (RNF-10).
        TextButton(onClick = { showDetails = !showDetails }) {
            Text(if (showDetails) "Ocultar detalles técnicos" else "Ver detalles técnicos")
        }
        AnimatedVisibility(visible = showDetails) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text("Datos usados: ${clinical.location.label()} · ${clinical.maxDiameterMm} mm", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Modelo: ${prediction.modelVersion}. ${ModelScore.summary(prediction)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (clinical.notes.isNotBlank()) {
                    Text("Anotaciones: ${clinical.notes}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

    }
}

// RF-19: la imagen con el mapa de calor encima (activable). Grad-CAM del modelo, o SIMULADO con el repositorio falso.
@Composable
private fun HeatmapSection(image: LesionImage, explanation: Explanation?, simulated: Boolean) {
    var showHeatmap by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        SectionTitle("Imagen analizada")
        Box(modifier = Modifier.clip(MaterialTheme.shapes.medium)) {
            LesionImageView(
                image,
                label = if (showHeatmap) "Imagen dermoscópica con el mapa de calor" + (if (explanation?.isSimulated == true) " simulado" else "") else "Imagen dermoscópica analizada"
            )
            if (showHeatmap && explanation != null) {
                HeatmapOverlay(explanation, Modifier.matchParentSize())
            }
        }
        if (explanation != null) {
            val against = explanation.kind == ExplanationKind.AGAINST_MELANOMA
            SwitchRow(label = "Mostrar mapa de calor", checked = showHeatmap, onCheckedChange = { showHeatmap = it })
            AnimatedVisibility(visible = showHeatmap) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(
                        if (against) "Zonas que llevaron al modelo a NO sugerir melanoma"
                        else "Zonas que llevaron al modelo a sugerir melanoma",
                        style = MaterialTheme.typography.titleSmall
                    )
                    HeatmapLegend()
                }
            }
            if (explanation.isSimulated) {
                SimulatedNotice(
                    title = "MAPA DE CALOR SIMULADO",
                    text = "No indica regiones reales de la imagen: es una mancha fija de prueba."
                )
            }
            Text(
                "El mapa de calor es una ayuda visual; puede confundir y su utilidad está pendiente de validación por especialistas. " +
                "Muestra dónde se apoyó el modelo, no dónde está la lesión.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Leyenda de la escala del mapa de calor (la misma escala «jet» de las figuras de la tesis).
@Composable
private fun HeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Text("Menor", style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Brush.horizontalGradient(List(11) { Color(EscalaJet.argb(it / 10f)) }))
        )
        Text("Mayor influencia", style = MaterialTheme.typography.bodyMedium)
    }
}

// Mapa de calor como en la tesis (gradcam_img.py y explicar_final.py): escala «jet» con opacidad 0,45 sobre
// toda la imagen y suavizado bilineal al escalarlo.
@Composable
private fun HeatmapOverlay(explanation: Explanation, modifier: Modifier = Modifier) {
    val imagen = remember(explanation) {
        val w = explanation.columns; val h = explanation.rows
        val px = IntArray(w * h)
        explanation.grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> px[r * w + c] = EscalaJet.argb(v, EscalaJet.OPACIDAD) } }
        android.graphics.Bitmap.createBitmap(px, w, h, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Canvas(modifier) {
        drawImage(imagen, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low)
    }
}

// Salida del modelo frente a su umbral (RF-04): secundaria a la recomendación; sin porcentaje, porque el puntaje
// no está calibrado como probabilidad. Marcada SIMULADO si viene del clasificador de prueba.
@Composable
private fun ScoreSection(prediction: Prediction) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Text("Resultado del modelo", style = MaterialTheme.typography.titleMedium)
                if (prediction.isSimulated) {
                    Surface(color = MaterialTheme.semantic.simulated.container, contentColor = MaterialTheme.semantic.simulated.onContainer, shape = MaterialTheme.shapes.small) {
                        Text("SIMULADO", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs))
                    }
                }
            }
            Text(
                if (prediction.suggestsMelanoma) "Sugiere melanoma" else "No sugiere melanoma",
                style = MaterialTheme.typography.headlineSmall
            )
            ModelScoreBar(prediction)
            Text(ModelScore.summary(prediction) + ".", style = MaterialTheme.typography.bodyMedium)
            Text(
                ModelScore.NOTE + " Es un apoyo, no un diagnóstico.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var howTo by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { howTo = !howTo }) { Text(if (howTo) "Ocultar la explicación" else "¿Cómo se lee el puntaje?") }
            AnimatedVisibility(visible = howTo) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    ModelScore.HOW_TO_READ.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// --- Vistas previas: tres estados distintos sin instalar la app ---

private val previewPrediction = Prediction(
    probMelanoma = 0.18f, isOutOfScope = false, modelVersion = "simulado-0", threshold = 0.30f, isSimulated = true
)

private val previewClinicalData = ClinicalData(
    location = AcralLocation.SOLE,
    maxDiameterMm = 5f,
    isElevatedOrPalpable = false,
    hadPreviousProcedure = false,
    reportedChange = ReportedChange.NO,
    evolutionTime = EvolutionTime.MORE_THAN_12_MONTHS
)

@Preview(showBackground = true, heightDp = 1000, name = "Control")
@Composable
fun ResultFollowUpPreview() {
    MelanomaAcralTheme {
        ResultContent(
            uiState = ResultUiState.Success(
                prediction = previewPrediction,
                recommendation = Recommendation(
                    ManagementAction.FOLLOW_UP, RecommendationReason.SMALL_BELOW_THRESHOLD, followUpAllowed = true
                ),
                clinicalData = previewClinicalData
            ),
            onDone = {}, onScheduleFollowUp = {}, onReferral = {}, onShowLimitations = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Fuera de alcance")
@Composable
fun ResultOutOfScopePreview() {
    MelanomaAcralTheme {
        ResultContent(
            uiState = ResultUiState.Success(
                prediction = previewPrediction.copy(probMelanoma = 0.64f, isOutOfScope = true),
                recommendation = Recommendation(
                    ManagementAction.NONE, RecommendationReason.OUT_OF_SCOPE, followUpAllowed = false
                ),
                clinicalData = previewClinicalData
            ),
            onDone = {}, onScheduleFollowUp = {}, onReferral = {}, onShowLimitations = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Cargando")
@Composable
fun ResultLoadingPreview() {
    MelanomaAcralTheme {
        ResultContent(
            uiState = ResultUiState.Loading,
            onDone = {}, onScheduleFollowUp = {}, onReferral = {}, onShowLimitations = {}, onBack = {}
        )
    }
}
