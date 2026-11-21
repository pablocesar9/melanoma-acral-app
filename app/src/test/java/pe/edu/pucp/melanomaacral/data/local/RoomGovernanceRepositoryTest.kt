package pe.edu.pucp.melanomaacral.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.model.ConsentType
import pe.edu.pucp.melanomaacral.domain.model.DecisionChoice
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import java.time.LocalDate

// RF-17, RF-23 y RNF-12: consentimiento, decisión del profesional y acceso por usuario con registro de acciones,
// sobre una base de datos Room en memoria con el esquema de la app.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomGovernanceRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var records: RoomRecordsRepository
    private lateinit var access: RoomAccessRepository
    private lateinit var consents: RoomConsentRepository
    private lateinit var decisions: RoomDecisionRepository
    private val today = LocalDate.of(2026, 11, 20)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        records = RoomRecordsRepository(db.recordsDao())
        access = RoomAccessRepository(db.governanceDao())
        consents = RoomConsentRepository(db.governanceDao(), access, today = { today })
        decisions = RoomDecisionRepository(db.governanceDao(), access)
    }

    @After
    fun tearDown() = db.close()

    // ---- RNF-12 ----

    @Test
    fun primerUsuario_seCreaEIngresa() = runBlocking {
        assertFalse(access.hasUsers())
        assertNull(access.createUser("Usuario1", "1234"))
        assertTrue(access.hasUsers())
        assertEquals("usuario1", access.currentUser.value)   // el usuario se guarda en minúsculas
    }

    @Test
    fun elPinNoSeGuardaEnClaro() = runBlocking {
        access.createUser("usuario1", "1234")
        val user = db.governanceDao().user("usuario1")!!
        assertFalse(user.pinHash.contentEquals("1234".toByteArray()))
        assertEquals(32, user.pinHash.size)
        assertEquals(16, user.salt.size)
    }

    @Test
    fun ingreso_conPinCorrecto_yRechazoConPinIncorrecto() = runBlocking {
        access.createUser("usuario1", "1234")
        access.logout()
        assertNull(access.currentUser.value)
        assertFalse(access.login("usuario1", "9999"))
        assertNull(access.currentUser.value)
        assertFalse(access.login("otro", "1234"))
        assertTrue(access.login("usuario1", "1234"))
        assertEquals("usuario1", access.currentUser.value)
    }

    @Test
    fun usuarioRepetido_oPinInvalido_seRechazan() = runBlocking {
        access.createUser("usuario1", "1234")
        assertNotNull(access.createUser("usuario1", "5678"))
        assertNotNull(access.createUser("usuario2", "12"))
        assertNotNull(access.createUser("usuario2", "12ab"))
        assertNotNull(access.createUser("u", "1234"))
    }

    @Test
    fun cadaAccion_quedaConSuUsuario() = runBlocking {
        access.createUser("usuario1", "1234")
        access.audit(AuditAction.VIEW, "Paciente P-001")
        access.audit(AuditAction.EXPORT, "PDF de derivación de LES-001")
        val log = access.log.first()
        assertTrue(log.all { it.username == "usuario1" })
        assertEquals(
            listOf(AuditAction.CREATE, AuditAction.LOGIN, AuditAction.VIEW, AuditAction.EXPORT),
            log.sortedBy { it.id }.map { it.action }
        )
    }

    @Test
    fun sinSesion_noSeRegistraNada() = runBlocking {
        access.audit(AuditAction.VIEW, "Paciente P-001")
        assertTrue(access.log.first().isEmpty())
    }

    // ---- RF-17 ----

    @Test
    fun consentimiento_seRegistraConFechaYUsuario_ySoloElNoBloquea() = runBlocking {
        access.createUser("usuario1", "1234")
        records.createPatient("P-001")
        assertNull(consents.get("P-001"))

        consents.record("P-001", ConsentType.REFUSED)
        assertFalse(consents.get("P-001")!!.allowsCapture)

        consents.record("P-001", ConsentType.VERBAL)
        val c = consents.get("P-001")!!
        assertTrue(c.allowsCapture)
        assertEquals(today, c.date)
        assertEquals("usuario1", c.recordedBy)
        assertTrue(access.log.first().any { it.action == AuditAction.MODIFY && it.target.contains("P-001") })
    }

    @Test
    fun consentimientoFirmado_habilitaLaCaptura() = runBlocking {
        records.createPatient("P-002")
        consents.record("P-002", ConsentType.SIGNED)
        assertTrue(consents.get("P-002")!!.allowsCapture)
    }

    // ---- RF-23 ----

    @Test
    fun decision_seGuardaConUsuario_ySePuedeCambiar() = runBlocking {
        access.createUser("usuario1", "1234")
        records.createPatient("P-001")
        val lesion = records.createLesion("P-001")
        val e = SampleRecords.evaluations(today).first()
        val id = records.addEvaluation(lesion, today, e.images, e.clinicalData, e.prediction, e.recommendation)

        assertNull(decisions.observe(id).first())
        decisions.save(id, DecisionChoice.MODIFIES, ManagementAction.FOLLOW_UP, "  Lesión estable en controles previos ")
        val d = decisions.observe(id).first()!!
        assertEquals(DecisionChoice.MODIFIES, d.choice)
        assertEquals(ManagementAction.FOLLOW_UP, d.finalAction)
        assertEquals("Lesión estable en controles previos", d.reason)
        assertEquals("usuario1", d.decidedBy)

        decisions.save(id, DecisionChoice.DISCARDS, null, "Otra causa")
        assertEquals(DecisionChoice.DISCARDS, decisions.observe(id).first()!!.choice)
        assertEquals(1, decisions.all.first().size)
        // La sugerencia de la aplicación no cambia.
        assertEquals(e.recommendation, records.evaluations.first().single().recommendation)
    }
}
