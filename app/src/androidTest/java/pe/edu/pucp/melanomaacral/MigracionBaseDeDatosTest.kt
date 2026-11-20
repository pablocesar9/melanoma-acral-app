package pe.edu.pucp.melanomaacral

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.pucp.melanomaacral.data.local.AppDatabase

// La migración v1 -> v2 conserva las evaluaciones ya guardadas en el teléfono y deja las columnas nuevas en null.
// Room valida además que el esquema migrado sea idéntico al exportado en app/schemas/.../2.json.
@RunWith(AndroidJUnit4::class)
class MigracionBaseDeDatosTest {
    private val nombre = "prueba-migracion.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun v1_a_v2_conservaLasEvaluaciones() {
        helper.createDatabase(nombre, 1).apply {
            execSQL("INSERT INTO patients (id, createdOnEpochDay) VALUES ('HC-1', 20000)")
            execSQL("INSERT INTO lesions (id, patientId, nextFollowUpEpochDay) VALUES ('LES-001', 'HC-1', NULL)")
            execSQL(
                """INSERT INTO evaluations (id, lesionId, dateEpochDay, derm_uri, derm_source, derm_width, derm_height, derm_size,
                   derm_captured_at, derm_sharpness, derm_luma, ctx_uri, ctx_source, ctx_width, ctx_height, ctx_size, ctx_captured_at,
                   ctx_sharpness, ctx_luma, location, maxDiameterMm, isElevatedOrPalpable, isNodularOrBlue, hadPreviousProcedure,
                   reportedChange, evolutionTime, ageYears, sex, hasSkinCancerHistory, hasBleeding, hasPain, hasItching,
                   probMelanoma, isOutOfScope, modelVersion, threshold, isSimulated, action, reason, followUpAllowed)
                   VALUES ('EV-001', 'LES-001', 20000, '/x/d.jpg', 'CAMERA', 100, 100, 1000, 0, NULL, NULL, NULL, NULL, NULL, NULL,
                   NULL, NULL, NULL, NULL, 'SOLE', 4.0, 0, 0, 0, 'NO', 'UNKNOWN', 40, 'FEMALE', 0, 0, 0, 0,
                   0.1, 0, 'simulado-0', 0.3, 1, 'FOLLOW_UP', 'SMALL_BELOW_THRESHOLD', 1)"""
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(nombre, 2, true, AppDatabase.MIGRATION_1_2)
        db.query("SELECT id, location, notes, score, scoreThreshold, editedAtMillis FROM evaluations").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("EV-001", c.getString(0))
            assertEquals("SOLE", c.getString(1))
            for (i in 2..5) assertNull(c.getString(i))
        }
    }

    // v2 -> v3 (RF-09): se quita isNodularOrBlue y las columnas nuevas quedan en 0 en las filas existentes.
    @Test
    fun v2_a_v3_conservaLasEvaluaciones() {
        val nombre3 = "prueba-migracion-3.db"
        helper.createDatabase(nombre3, 2).apply {
            execSQL("INSERT INTO patients (id, createdOnEpochDay) VALUES ('HC-1', 20000)")
            execSQL("INSERT INTO lesions (id, patientId, nextFollowUpEpochDay) VALUES ('LES-001', 'HC-1', NULL)")
            execSQL(
                """INSERT INTO evaluations (id, lesionId, dateEpochDay, derm_uri, derm_source, derm_width, derm_height, derm_size,
                   derm_captured_at, location, maxDiameterMm, isElevatedOrPalpable, isNodularOrBlue, hadPreviousProcedure,
                   reportedChange, evolutionTime, hasSkinCancerHistory, hasBleeding, hasPain, hasItching,
                   probMelanoma, isOutOfScope, modelVersion, threshold, isSimulated, action, reason, followUpAllowed)
                   VALUES ('EV-001', 'LES-001', 20000, '/x/d.jpg', 'CAMERA', 100, 100, 1000, 0, 'SOLE', 4.0, 0, 0, 0,
                   'NO', 'UNKNOWN', 0, 0, 0, 0, 0.1, 0, 'simulado-0', 0.3, 1, 'FOLLOW_UP', 'SMALL_BELOW_THRESHOLD', 1)"""
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(nombre3, 3, true)
        db.query("SELECT id, location, isNotHealing, isGrowingRapidly FROM evaluations").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("EV-001", c.getString(0))
            assertEquals("SOLE", c.getString(1))
            assertEquals(0, c.getInt(2))
            assertEquals(0, c.getInt(3))
        }
    }
}
