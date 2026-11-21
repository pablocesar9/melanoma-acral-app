package pe.edu.pucp.melanomaacral.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ImageSource
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.Sex
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import java.time.LocalDate

// RF-10, RF-11, RF-14, RNF-07: el registro persistente guarda y devuelve lo mismo que recibe.
// Usa una base de datos Room en memoria (Robolectric), con el mismo esquema de la app.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRecordsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomRecordsRepository
    private val today = LocalDate.of(2026, 11, 14)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomRecordsRepository(db.recordsDao(), db)
    }

    @After
    fun tearDown() = db.close()

    private val sample = SampleRecords.evaluations(today).first().let {
        // con los datos de RF-16 completos (edad, sexo, síntomas, antecedente)
        it.copy(clinicalData = it.clinicalData.copy(
            ageYears = 47, sex = Sex.MALE, hasBleeding = true, hasItching = true, hasSkinCancerHistory = true
        ))
    }

    // RF-10: corregir el código mueve las lesiones y el consentimiento, conserva la fecha de alta y borra el código anterior.
    @Test
    fun corregirCodigo_mueveLesionesYConsentimiento() = runBlocking {
        repo.createPatient("PAC-100")
        val lesion = repo.createLesion("PAC-100")
        db.governanceDao().upsertConsent(ConsentEntity("PAC-100", "ACCEPTED", 1L, "usuario", 1L))
        val alta = repo.patients.first().single().createdOn

        repo.renamePatient("PAC-100", "HC-200")

        assertEquals(listOf("HC-200"), repo.patients.first().map { it.id })
        assertEquals(alta, repo.patients.first().single().createdOn)
        assertEquals("HC-200", repo.lesions.first().single { it.id == lesion }.patientId)
        assertNull(db.governanceDao().consent("PAC-100"))
        assertNotNull(db.governanceDao().consent("HC-200"))
    }

    @Test
    fun crearPaciente_loDevuelveEnLaLista() = runBlocking {
        repo.createPatient("PAC-100")

        assertEquals(listOf("PAC-100"), repo.patients.first().map { it.id })
    }

    @Test
    fun lesionesNuevas_recibenIdsConsecutivos() = runBlocking {
        repo.createPatient("PAC-100")

        val a = repo.createLesion("PAC-100")
        val b = repo.createLesion("PAC-100")

        assertEquals("LES-001", a)
        assertEquals("LES-002", b)
        assertNull(repo.lesions.first().first().nextFollowUp)
    }

    @Test
    fun evaluacion_seGuardaConTodosSusDatos() = runBlocking {
        repo.createPatient("PAC-100")
        val lesionId = repo.createLesion("PAC-100")
        val images = CapturedImages(
            dermoscopic = LesionImage("/data/files/d.jpg", ImageSource.CAMERA, 1200, 900, 345_000, 1L, 42.5f, 120f),
            context = LesionImage("/data/files/c.jpg", ImageSource.GALLERY, 800, 600, 100_000, 2L)
        )

        repo.addEvaluation(lesionId, today, images, sample.clinicalData, sample.prediction, sample.recommendation)

        val saved = repo.evaluations.first().single()
        assertEquals(images, saved.images)
        assertEquals(sample.clinicalData, saved.clinicalData)
        assertEquals(sample.prediction, saved.prediction)
        assertEquals(sample.recommendation, saved.recommendation)
        assertEquals(today, saved.date)
    }

    @Test
    fun evaluacionSinFotoDeContexto_devuelveContextNulo() = runBlocking {
        repo.createPatient("PAC-100")
        val lesionId = repo.createLesion("PAC-100")

        repo.addEvaluation(lesionId, today, sample.images, sample.clinicalData, sample.prediction, sample.recommendation)

        assertNull(repo.evaluations.first().single().images.context)
    }

    @Test
    fun evaluaciones_seOrdenanPorFechaParaElHistorial() = runBlocking {
        repo.createPatient("PAC-100")
        val lesionId = repo.createLesion("PAC-100")
        repo.addEvaluation(lesionId, today, sample.images, sample.clinicalData, sample.prediction, sample.recommendation)
        repo.addEvaluation(lesionId, today.minusMonths(3), sample.images, sample.clinicalData, sample.prediction, sample.recommendation)

        val dates = repo.evaluations.first().forLesion(lesionId).map { it.date }

        assertEquals(listOf(today.minusMonths(3), today), dates)
    }

    @Test
    fun programarControl_actualizaLaLesion() = runBlocking {
        repo.createPatient("PAC-100")
        val lesionId = repo.createLesion("PAC-100")

        repo.scheduleFollowUp(lesionId, today.plusMonths(3))

        assertEquals(today.plusMonths(3), repo.lesions.first().single().nextFollowUp)
        assertNotNull(repo.lesions.first().single())
    }

    @Test(expected = Exception::class)
    fun pacienteDuplicado_seRechaza(): Unit = runBlocking {
        repo.createPatient("PAC-100")
        repo.createPatient("PAC-100")
    }

    @Test(expected = Exception::class)
    fun lesionDePacienteInexistente_seRechaza(): Unit = runBlocking {
        repo.createLesion("NO-EXISTE")
    }
}
