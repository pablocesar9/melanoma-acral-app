package pe.edu.pucp.melanomaacral.ui.screens.obtainimage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.pucp.melanomaacral.data.fake.InMemoryDraftRepository
import pe.edu.pucp.melanomaacral.data.fake.InMemoryRecordsRepository
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.model.ImageSource
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.repository.ImageQualityRepository
import pe.edu.pucp.melanomaacral.domain.repository.ImageStorage
import java.io.File

// RF-02: sin la foto clínica de contexto no se llega a la dermoscópica.
// RF-03: cada foto se revisa apenas se obtiene. Se puede ir y volver entre pasos sin perder fotos,
// y lo hecho queda guardado como evaluación sin terminar.
@OptIn(ExperimentalCoroutinesApi::class)
class ObtainImageViewModelTest {

    private class FakeStorage : ImageStorage {
        var counter = 0
        val deleted = mutableListOf<LesionImage>()
        override fun newCameraFile() = File("raw-${counter++}.jpg")
        override suspend fun registerCameraFile(file: File) = LesionImage("img-${counter++}", ImageSource.CAMERA)
        override suspend fun importFromGallery(uri: String) = LesionImage("img-${counter++}", ImageSource.GALLERY)
        override fun delete(image: LesionImage) { deleted += image }
    }

    // Revisión de calidad que aprueba o rechaza según se le pida, y anota qué revisó.
    private class FakeQuality : ImageQualityRepository {
        var nextOk = true
        val checked = mutableListOf<Pair<LesionImage, ImageKind>>()
        override suspend fun check(image: LesionImage, kind: ImageKind): QualityReport {
            checked += image to kind
            return QualityReport(focusOk = nextOk, lightingOk = true, framingOk = true, isSimulated = false, sharpness = 42f)
        }
    }

    private val storage = FakeStorage()
    private val quality = FakeQuality()
    private val drafts = InMemoryDraftRepository()
    private lateinit var vm: ObtainImageViewModel
    private lateinit var collector: Job

    private fun newViewModel(): ObtainImageViewModel {
        val created = ObtainImageViewModel("LES-001", InMemoryRecordsRepository(), storage, quality, drafts)
        // uiState usa WhileSubscribed: hace falta un suscriptor para que se actualice.
        collector = CoroutineScope(UnconfinedTestDispatcher()).launch { created.uiState.collect {} }
        return created
    }

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = newViewModel()
    }

    @After fun tearDown() {
        collector.cancel()
        Dispatchers.resetMain()
    }

    @Test fun empiezaPorLaFotoClinica() {
        assertEquals(CaptureStep.CONTEXT, vm.uiState.value.step)
    }

    @Test fun fotoClinica_seRevisaEnSeguidaYSeQuedaEnElPaso1() {
        quality.nextOk = false
        vm.onGalleryUri("content://x")

        val state = vm.uiState.value
        assertEquals(CaptureStep.CONTEXT, state.step)
        assertNotNull(state.context.image)
        assertNotNull(state.context.report)
        assertFalse(state.context.qualityOk)
        assertEquals(ImageKind.CONTEXT, quality.checked.single().second)
    }

    @Test fun sinFotoClinica_noSePasaALaDermoscopica() {
        assertFalse(vm.goToDermoscopic())
        assertEquals(CaptureStep.CONTEXT, vm.uiState.value.step)
    }

    @Test fun dermoscopica_seRevisaConEncuadreYCompletaLasDosFotos() {
        vm.onGalleryUri("content://a")
        assertTrue(vm.goToDermoscopic())
        vm.onCameraFile(File("x.jpg"))

        assertEquals(ImageKind.DERMOSCOPIC, quality.checked.last().second)
        val images = vm.capturedImages()
        assertNotNull(images)
        assertNotNull(images!!.context)
        assertEquals(ImageSource.CAMERA, images.dermoscopic.source)
        assertEquals(42f, images.dermoscopic.sharpness)   // RNF-07: la medida viaja con la imagen
    }

    @Test fun volverYAvanzar_conservaLasDosFotos() {
        vm.onGalleryUri("content://a")
        vm.goToDermoscopic()
        vm.onGalleryUri("content://b")
        val context = vm.uiState.value.context.image
        val dermoscopic = vm.uiState.value.dermoscopic.image

        vm.goToContext()
        assertEquals(CaptureStep.CONTEXT, vm.uiState.value.step)
        vm.goToDermoscopic()

        assertEquals(context, vm.uiState.value.context.image)
        assertEquals(dermoscopic, vm.uiState.value.dermoscopic.image)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun repetirLaFotoClinica_soloReemplazaEsaYBorraLaAnterior() {
        vm.onGalleryUri("content://a")
        vm.goToDermoscopic()
        vm.onGalleryUri("content://b")
        val oldContext = vm.uiState.value.context.image!!
        val dermoscopic = vm.uiState.value.dermoscopic.image

        vm.goToContext()
        vm.onGalleryUri("content://c")

        assertEquals(listOf(oldContext), storage.deleted)
        assertEquals(dermoscopic, vm.uiState.value.dermoscopic.image)
        assertTrue(vm.uiState.value.context.image != oldContext)
    }

    @Test fun loHecho_quedaGuardadoYSeRetomaEnOtraVisita() = runBlocking {
        vm.onGalleryUri("content://a")
        vm.goToDermoscopic()
        val draft = drafts.get("LES-001")
        assertNotNull(draft)
        assertEquals(DraftStage.DERMOSCOPIC, draft!!.stage)
        assertNotNull(draft.contextReport)

        collector.cancel()
        val reopened = newViewModel()
        assertEquals(CaptureStep.DERMOSCOPIC, reopened.uiState.value.step)
        assertEquals(draft.context, reopened.uiState.value.context.image)
    }

    @Test fun sinFotos_noSeGuardaNingunPendiente() = runBlocking {
        vm.goToContext()
        assertNull(drafts.get("LES-001"))
    }
}
