package pe.edu.pucp.melanomaacral.ui.screens.obtainimage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import pe.edu.pucp.melanomaacral.domain.model.withMeasures
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.ImageQualityRepository
import pe.edu.pucp.melanomaacral.domain.repository.ImageStorage
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.ui.appContainer
import java.io.File
import java.time.LocalDate

// Paso de captura. RF-02: primero las dos fotos clínicas, la de contexto (zona con la lesión señalada) y el
// primer plano; sin ellas no se llega a la dermoscópica (RF-01).
enum class CaptureStep { CONTEXT, CLOSE_UP, DERMOSCOPIC }

// Cómo se está obteniendo la imagen del paso actual.
enum class CaptureMode { CHOOSING, CAMERA }

// Una foto del flujo con su revisión de calidad (RF-03), hecha apenas se obtiene la foto.
data class CaptureSlot(
    val image: LesionImage? = null,
    val report: QualityReport? = null,
    val isChecking: Boolean = false,
    val checkError: String? = null
) {
    val hasImage: Boolean get() = image != null
    val qualityOk: Boolean get() = report?.allOk == true
}

data class ObtainImageUiState(
    val step: CaptureStep = CaptureStep.CONTEXT,
    val mode: CaptureMode = CaptureMode.CHOOSING,
    val context: CaptureSlot = CaptureSlot(),
    val closeUp: CaptureSlot = CaptureSlot(),
    val dermoscopic: CaptureSlot = CaptureSlot(),
    val previous: Evaluation? = null,          // si no es null, es un control (RF-13)
    val isProcessing: Boolean = false,
    val isRestoring: Boolean = false,         // leyendo una evaluación sin terminar
    val errorMessage: String? = null
) {
    val previousCaptureDate: LocalDate? get() = previous?.date

    val current: CaptureSlot
        get() = when (step) {
            CaptureStep.CONTEXT -> context
            CaptureStep.CLOSE_UP -> closeUp
            CaptureStep.DERMOSCOPIC -> dermoscopic
        }

    // Imagen previa que se superpone en la cámara según el paso (RF-13).
    val overlay: LesionImage?
        get() = when (step) {
            CaptureStep.CONTEXT -> previous?.images?.context
            CaptureStep.CLOSE_UP -> previous?.images?.closeUp
            CaptureStep.DERMOSCOPIC -> previous?.images?.dermoscopic
        }
}

class ObtainImageViewModel(
    private val lesionId: String,
    records: RecordsRepository,
    private val storage: ImageStorage,
    private val quality: ImageQualityRepository,
    private val drafts: DraftRepository,
    // Empieza a analizar la dermoscópica en segundo plano mientras se llenan los datos clínicos.
    private val prepareModel: (LesionImage) -> Unit = {}
) : ViewModel() {

    private data class Local(
        val step: CaptureStep = CaptureStep.CONTEXT,
        val mode: CaptureMode = CaptureMode.CHOOSING,
        val context: CaptureSlot = CaptureSlot(),
        val closeUp: CaptureSlot = CaptureSlot(),
        val dermoscopic: CaptureSlot = CaptureSlot(),
        val isProcessing: Boolean = false,
        val isRestoring: Boolean = true,
        val errorMessage: String? = null
    ) {
        fun slot(step: CaptureStep) = when (step) {
            CaptureStep.CONTEXT -> context
            CaptureStep.CLOSE_UP -> closeUp
            CaptureStep.DERMOSCOPIC -> dermoscopic
        }
        fun withSlot(step: CaptureStep, slot: CaptureSlot) = when (step) {
            CaptureStep.CONTEXT -> copy(context = slot)
            CaptureStep.CLOSE_UP -> copy(closeUp = slot)
            CaptureStep.DERMOSCOPIC -> copy(dermoscopic = slot)
        }
    }

    // Las dos fotos clínicas se revisan como foto clínica; la dermoscópica, con encuadre.
    private fun kindOf(step: CaptureStep) = if (step == CaptureStep.DERMOSCOPIC) ImageKind.DERMOSCOPIC else ImageKind.CONTEXT

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<ObtainImageUiState> =
        combine(records.evaluations, local) { evaluations, l ->
            ObtainImageUiState(
                step = l.step,
                mode = l.mode,
                context = l.context,
                closeUp = l.closeUp,
                dermoscopic = l.dermoscopic,
                previous = evaluations.forLesion(lesionId).lastOrNull(),
                isProcessing = l.isProcessing,
                isRestoring = l.isRestoring,
                errorMessage = l.errorMessage
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObtainImageUiState(isRestoring = true))

    // Si la lesión tiene una evaluación sin terminar, se retoma donde quedó. Es asíncrono: process() lo espera,
    // porque si Android recrea la pantalla mientras el selector de fotos está abierto, la foto elegida llega antes
    // de terminar de restaurar y, sin esperar, se guardaba en el paso equivocado y luego se perdía.
    private val restore = viewModelScope.launch {
        val draft = drafts.get(lesionId)
        local.update { l ->
            if (draft == null) l.copy(isRestoring = false)
            else l.copy(
                isRestoring = false,
                step = when {
                    draft.stage == DraftStage.CONTEXT || draft.context == null -> CaptureStep.CONTEXT
                    draft.stage == DraftStage.CLOSE_UP || draft.closeUp == null -> CaptureStep.CLOSE_UP
                    else -> CaptureStep.DERMOSCOPIC
                },
                context = CaptureSlot(draft.context, draft.contextReport),
                closeUp = CaptureSlot(draft.closeUp, draft.closeUpReport),
                dermoscopic = CaptureSlot(draft.dermoscopic, draft.dermoscopicReport)
            )
        }
        // Fotos restauradas sin revisión de calidad guardada (por ejemplo, al editar una evaluación): se revisan de nuevo.
        for (step in CaptureStep.entries) {
            val slot = local.value.slot(step)
            if (slot.image != null && slot.report == null && slot.checkError == null) recheck(step, slot.image)
        }
    }

    private fun recheck(step: CaptureStep, image: LesionImage) {
        local.update { it.withSlot(step, CaptureSlot(image = image, isChecking = true)) }
        viewModelScope.launch {
            val kind = kindOf(step)
            val checked = try {
                CaptureSlot(image = image, report = quality.check(image, kind))
            } catch (e: Exception) {
                CaptureSlot(image = image, checkError = e.message ?: "No se pudo revisar la imagen")
            }
            if (local.value.slot(step).image == image) {
                local.update { it.withSlot(step, checked) }
                saveDraft()
            }
        }
    }

    fun newCameraFile(): File = storage.newCameraFile()

    fun openCamera() = local.update { it.copy(mode = CaptureMode.CAMERA, errorMessage = null) }

    fun closeCamera() = local.update { it.copy(mode = CaptureMode.CHOOSING) }

    fun reportError(message: String) = local.update { it.copy(errorMessage = message, isProcessing = false) }

    // Foto tomada con la cámara (RF-01, RF-02).
    fun onCameraFile(file: File) = process { storage.registerCameraFile(file) }

    // Imagen elegida con el selector de fotos (RF-21).
    fun onGalleryUri(uri: String) = process { storage.importFromGallery(uri) }

    private fun goTo(step: CaptureStep) {
        local.update { it.copy(step = step, mode = CaptureMode.CHOOSING, errorMessage = null) }
        saveDraft()
    }

    // Pasa al primer plano. RF-02: solo con la foto de contexto ya obtenida.
    fun goToCloseUp(): Boolean {
        if (!local.value.context.hasImage) return false
        goTo(CaptureStep.CLOSE_UP)
        return true
    }

    // Pasa a la imagen dermoscópica. RF-02: solo con las dos fotos clínicas ya obtenidas.
    // Si la dermoscópica ya se había tomado, se conserva.
    fun goToDermoscopic(): Boolean {
        val l = local.value
        if (!l.context.hasImage || !l.closeUp.hasImage) return false
        goTo(CaptureStep.DERMOSCOPIC)
        return true
    }

    // Vuelve a la foto de contexto sin perder ninguna foto.
    fun goToContext() = goTo(CaptureStep.CONTEXT)

    // «Atrás» dentro del flujo: un paso hacia la foto de contexto.
    fun goBack() = when (local.value.step) {
        CaptureStep.DERMOSCOPIC -> goTo(CaptureStep.CLOSE_UP)
        CaptureStep.CLOSE_UP, CaptureStep.CONTEXT -> goTo(CaptureStep.CONTEXT)
    }

    // Guarda en el teléfono lo hecho hasta ahora, para poder salir y retomarlo (sin foto, nada que guardar).
    // Conserva el formulario de datos clínicos si ya se había empezado.
    private fun saveDraft(stage: DraftStage? = null) {
        val l = local.value
        if (l.context.image == null && l.closeUp.image == null && l.dermoscopic.image == null) return
        viewModelScope.launch {
            drafts.update(lesionId) { old ->
                (old ?: EvaluationDraft(lesionId)).copy(
                    stage = stage ?: when (l.step) {
                        CaptureStep.CONTEXT -> DraftStage.CONTEXT
                        CaptureStep.CLOSE_UP -> DraftStage.CLOSE_UP
                        CaptureStep.DERMOSCOPIC -> DraftStage.DERMOSCOPIC
                    },
                    context = l.context.image,
                    contextReport = l.context.report,
                    closeUp = l.closeUp.image,
                    closeUpReport = l.closeUp.report,
                    dermoscopic = l.dermoscopic.image,
                    dermoscopicReport = l.dermoscopic.report
                )
            }
        }
    }

    // Al pasar a los datos clínicos.
    fun onContinueToClinicalData() {
        saveDraft(DraftStage.CLINICAL_DATA)
        local.value.dermoscopic.image?.let(prepareModel)
    }

    // Las tres fotos con sus medidas de calidad (RNF-07), o null si falta alguna (RF-02).
    fun capturedImages(): CapturedImages? {
        val l = local.value
        val context = l.context.image ?: return null
        val closeUp = l.closeUp.image ?: return null
        val dermoscopic = l.dermoscopic.image ?: return null
        return CapturedImages(
            dermoscopic = dermoscopic.withMeasures(l.dermoscopic.report),
            context = context.withMeasures(l.context.report),
            closeUp = closeUp.withMeasures(l.closeUp.report)
        )
    }

    private fun process(obtain: suspend () -> LesionImage) {
        local.update { it.copy(isProcessing = true, errorMessage = null, mode = CaptureMode.CHOOSING) }
        viewModelScope.launch {
            restore.join()
            val step = local.value.step
            val image = try {
                obtain()
            } catch (e: Exception) {
                reportError("No se pudo procesar la imagen: ${e.message}")
                return@launch
            }
            // La foto que se reemplaza se borra (no queda ningún archivo huérfano).
            local.value.slot(step).image?.let { storage.delete(it) }
            local.update { it.withSlot(step, CaptureSlot(image = image, isChecking = true)).copy(isProcessing = false) }
            saveDraft()

            // RF-03: la calidad se revisa en cuanto se obtiene cada foto, no al final.
            val kind = kindOf(step)
            val checked = try {
                CaptureSlot(image = image, report = quality.check(image, kind))
            } catch (e: Exception) {
                CaptureSlot(image = image, checkError = e.message ?: "No se pudo revisar la imagen")
            }
            // Si mientras tanto se reemplazó la foto, esta revisión ya no sirve.
            if (local.value.slot(step).image == image) {
                local.update { it.withSlot(step, checked) }
                saveDraft()
            }
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ObtainImageViewModel(
                    lesionId, container.recordsRepository, container.imageStorage,
                    container.imageQualityRepository, container.draftRepository,
                    prepareModel = container::prepareModel
                )
            }
        }
    }
}

private fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
    value = block(value)
}
