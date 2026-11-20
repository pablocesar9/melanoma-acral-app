package pe.edu.pucp.melanomaacral.ui.screens.obtainimage

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.EvaluationStep
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.StepIndicator
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.screens.quality.PhotoQualityCard
import pe.edu.pucp.melanomaacral.ui.screens.quality.QualityCheckingCard
import pe.edu.pucp.melanomaacral.ui.screens.quality.QualityMethodNotice
import pe.edu.pucp.melanomaacral.ui.screens.quality.QualityVerdictCard
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import pe.edu.pucp.melanomaacral.ui.theme.Space
import java.io.File

// P-05 Obtener imágenes y P-06 Revisión de calidad — RF-01, RF-02, RF-03, RF-13, RF-21, RNF-04.
// Dos pasos en orden: 1) foto clínica de contexto, 2) imagen dermoscópica. Cada foto se revisa
// apenas se obtiene. Se puede ir y volver entre los dos pasos sin perder las fotos, y lo hecho
// queda guardado como evaluación sin terminar si se sale del flujo.
@Composable
fun ObtainImageScreen(
    lesionId: String,
    onImagesReady: (CapturedImages) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    viewModel: ObtainImageViewModel = viewModel(factory = ObtainImageViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onGalleryUri(uri.toString())
    }
    // «Atrás» en el paso 2 vuelve al paso 1, no fuera del flujo.
    BackHandler(enabled = uiState.step == CaptureStep.DERMOSCOPIC && uiState.mode != CaptureMode.CAMERA) {
        viewModel.goToContext()
    }
    ObtainImageContent(
        uiState = uiState,
        cameraFile = viewModel::newCameraFile,
        onOpenCamera = viewModel::openCamera,
        onCloseCamera = viewModel::closeCamera,
        onCameraFile = viewModel::onCameraFile,
        onPickFromGallery = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onGoToContext = viewModel::goToContext,
        onGoToDermoscopic = { viewModel.goToDermoscopic() },
        onFinish = {
            viewModel.capturedImages()?.let {
                viewModel.onContinueToClinicalData()
                onImagesReady(it)
            }
        },
        onBack = {
            if (uiState.step == CaptureStep.DERMOSCOPIC) viewModel.goToContext() else onBack()
        },
        onExit = onExit
    )
}

@Composable
fun ObtainImageContent(
    uiState: ObtainImageUiState,
    cameraFile: () -> File,
    onOpenCamera: () -> Unit,
    onCloseCamera: () -> Unit,
    onCameraFile: (File) -> Unit,
    onPickFromGallery: () -> Unit,
    onGoToContext: () -> Unit,
    onGoToDermoscopic: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isContext = uiState.step == CaptureStep.CONTEXT
    val inCamera = uiState.mode == CaptureMode.CAMERA && !uiState.isProcessing
    val slot = uiState.current
    val hasAnyImage = uiState.context.hasImage || uiState.dermoscopic.hasImage
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Obtener imágenes",
                onBack = onBack,
                subtitle = if (hasAnyImage) "Se guarda sola si sales" else null,
                actions = { TextButton(onClick = onExit) { Text("Salir") } }
            )
        },
        bottomBar = {
            if (uiState.isProcessing || uiState.isRestoring || inCamera) return@Scaffold
            val nextLabel = if (isContext) "Siguiente: imagen dermoscópica" else "Continuar a datos clínicos"
            val onNext = if (isContext) onGoToDermoscopic else onFinish
            when {
                // Sin foto en este paso: obtenerla.
                !slot.hasImage -> BottomActionBar(
                    primaryLabel = if (isContext) "Tomar foto clínica" else "Capturar imagen dermoscópica",
                    onPrimary = onOpenCamera,
                    primaryIcon = Icons.Filled.CameraAlt,
                    secondaryLabel = "Elegir de la galería",
                    onSecondary = onPickFromGallery
                )
                slot.isChecking -> BottomActionBar(
                    primaryLabel = nextLabel, onPrimary = onNext, enabled = false,
                    disabledReason = "Revisando la calidad de la foto…"
                )
                slot.qualityOk -> BottomActionBar(
                    primaryLabel = nextLabel,
                    onPrimary = onNext,
                    primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    secondaryLabel = "Repetir la foto",
                    onSecondary = onOpenCamera,
                    secondaryIsText = true
                )
                // Calidad insuficiente (o no se pudo revisar): se recomienda repetir, sin bloquear.
                else -> BottomActionBar(
                    primaryLabel = "Repetir la foto",
                    onPrimary = onOpenCamera,
                    primaryIcon = Icons.Filled.CameraAlt,
                    secondaryLabel = "Continuar de todos modos",
                    onSecondary = onNext,
                    secondaryIsText = true
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isRestoring) {
            LoadingContent("Abriendo la evaluación…", Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (uiState.isProcessing) {
            LoadingContent("Guardando la imagen en el teléfono…", Modifier.padding(innerPadding))
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
            StepIndicator(if (isContext) EvaluationStep.ClinicalPhoto else EvaluationStep.Dermoscopic)

            if (inCamera) {
                CameraCapture(
                    outputFile = cameraFile,
                    overlay = uiState.overlay,
                    onCaptured = onCameraFile,
                    onCancel = onCloseCamera
                )
                return@Column
            }

            CaptureStepSwitcher(uiState, onGoToContext, onGoToDermoscopic)

            if (!slot.hasImage) {
                CaptureInstructions(isContext)
            } else {
                val image = slot.image!!
                val title = if (isContext) "Foto clínica de contexto" else "Imagen dermoscópica"
                when {
                    slot.isChecking -> QualityCheckingCard()
                    slot.report != null -> QualityVerdictCard(ok = slot.qualityOk, isContext = isContext)
                    else -> NoticeCard(
                        "No se pudo revisar la imagen",
                        slot.checkError ?: "Error desconocido. Puedes repetir la foto.",
                        NoticeKind.Error
                    )
                }
                PhotoQualityCard(title = title, image = image, report = slot.report, showFraming = !isContext)
                TextButton(onClick = onPickFromGallery) { Text("Elegir otra de la galería") }
                slot.report?.let { QualityMethodNotice(it) }
            }

            uiState.previousCaptureDate?.let { date ->
                NoticeCard(
                    title = "Es un control",
                    text = "En la cámara se superpone la captura del ${date.formatted()} para repetir el encuadre.",
                    kind = NoticeKind.Info
                )
            }
            uiState.errorMessage?.let { NoticeCard("No se pudo guardar", it, NoticeKind.Error) }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Las imágenes se guardan solo dentro de la app, no en la galería del teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Selector de paso: permite ir y volver entre las dos fotos. El paso 2 se habilita con la foto
// clínica ya obtenida (RF-02). Cada botón muestra si su foto ya está y si su calidad es adecuada.
@Composable
private fun CaptureStepSwitcher(
    uiState: ObtainImageUiState,
    onGoToContext: () -> Unit,
    onGoToDermoscopic: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StepSegment(
            label = "1. Foto clínica",
            slot = uiState.context,
            selected = uiState.step == CaptureStep.CONTEXT,
            enabled = true,
            index = 0,
            onClick = onGoToContext
        )
        StepSegment(
            label = "2. Dermoscópica",
            slot = uiState.dermoscopic,
            selected = uiState.step == CaptureStep.DERMOSCOPIC,
            enabled = uiState.context.hasImage,
            index = 1,
            onClick = onGoToDermoscopic
        )
    }
}

@Composable
private fun androidx.compose.material3.SingleChoiceSegmentedButtonRowScope.StepSegment(
    label: String,
    slot: CaptureSlot,
    selected: Boolean,
    enabled: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val status = when {
        !slot.hasImage -> "sin foto"
        slot.isChecking -> "revisando"
        slot.qualityOk -> "calidad adecuada"
        else -> "calidad insuficiente"
    }
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
        icon = {
            when {
                !slot.hasImage || slot.isChecking -> SegmentedButtonDefaults.Icon(active = selected)
                slot.qualityOk -> Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                else -> Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        },
        modifier = Modifier.semantics { contentDescription = "$label, $status" }
    ) {
        Text(label, maxLines = 1, textAlign = TextAlign.Center)
    }
}

// Indicaciones antes de tomar la foto del paso.
@Composable
private fun CaptureInstructions(isContext: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Space.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            FramingGuide(circle = !isContext)
            Text(
                if (isContext) "Foto clínica de contexto" else "Imagen dermoscópica",
                style = MaterialTheme.typography.titleLarge
            )
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                val tips = if (isContext) listOf(
                    "Toma una foto normal, sin dermatoscopio.",
                    "Muestra la zona (el pie o la mano) y señala la lesión.",
                    "Si puedes, añade luego un primer plano.",
                    "Busca buena luz. Sin esta foto no se puede continuar."
                ) else listOf(
                    "Acopla el dermatoscopio al teléfono, con luz polarizada y acople estable.",
                    "Apóyalo sobre la piel y centra la lesión en el círculo.",
                    "Espera a que enfoque antes de capturar."
                )
                tips.forEach { Tip(it) }
            }
        }
    }
}

@Composable
private fun Tip(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

// Guía de encuadre dibujada: esquinas para la foto clínica, círculo para la dermoscópica.
@Composable
private fun FramingGuide(circle: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = Modifier
            .size(140.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .semantics { contentDescription = if (circle) "Guía: lesión centrada en un círculo" else "Guía: zona completa con la lesión señalada" }
    ) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        if (circle) {
            drawCircle(color, radius = size.minDimension * 0.34f, style = stroke)
            drawCircle(color, radius = 4.dp.toPx())
        } else {
            val m = size.minDimension * 0.18f
            val l = size.minDimension * 0.2f
            val w = size.width
            val h = size.height
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(color, Offset(x, y), Offset(x + dx * l, y), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(x, y), Offset(x, y + dy * l), stroke.width, StrokeCap.Round)
            }
            corner(m, m, 1f, 1f); corner(w - m, m, -1f, 1f); corner(m, h - m, 1f, -1f); corner(w - m, h - m, -1f, -1f)
            drawCircle(color, radius = 10.dp.toPx(), center = Offset(w * 0.55f, h * 0.5f), style = stroke)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun ObtainImagePreview() {
    MelanomaAcralTheme {
        ObtainImageContent(
            uiState = ObtainImageUiState(),
            cameraFile = { File("x") }, onOpenCamera = {}, onCloseCamera = {}, onCameraFile = {},
            onPickFromGallery = {}, onGoToContext = {}, onGoToDermoscopic = {}, onFinish = {},
            onBack = {}, onExit = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun ObtainImageDermoscopicPreview() {
    MelanomaAcralTheme {
        ObtainImageContent(
            uiState = ObtainImageUiState(
                step = CaptureStep.DERMOSCOPIC,
                context = CaptureSlot(image = LesionImage("ejemplo"))
            ),
            cameraFile = { File("x") }, onOpenCamera = {}, onCloseCamera = {}, onCameraFile = {},
            onPickFromGallery = {}, onGoToContext = {}, onGoToDermoscopic = {}, onFinish = {},
            onBack = {}, onExit = {}
        )
    }
}
