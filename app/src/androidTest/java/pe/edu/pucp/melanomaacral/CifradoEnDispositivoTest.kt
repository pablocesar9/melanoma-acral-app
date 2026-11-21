package pe.edu.pucp.melanomaacral

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.pucp.melanomaacral.data.files.KeyVault
import pe.edu.pucp.melanomaacral.data.files.SecureImages
import pe.edu.pucp.melanomaacral.data.local.AppDatabase
import pe.edu.pucp.melanomaacral.data.local.DatabaseEncryption
import pe.edu.pucp.melanomaacral.data.local.RoomRecordsRepository
import java.io.File

// RNF-05 en el teléfono: la base de datos queda cifrada con SQLCipher (también una que existía sin cifrar, sin perder
// registros) y las imágenes se cifran con la clave del Keystore.
@RunWith(AndroidJUnit4::class)
class CifradoEnDispositivoTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun baseSinCifrar_secifraYConservaLosRegistros() = runBlocking {
        val name = "prueba-cifrado.db"
        val file = context.getDatabasePath(name)
        listOf("", "-wal", "-shm", ".cifrada").forEach { File(file.path + it).delete() }
        // 1. Base de datos sin cifrar, como la de las versiones anteriores.
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name).build().apply {
            RoomRecordsRepository(recordsDao()).createPatient("P-001")
            close()
        }
        assertTrue(DatabaseEncryption.isPlain(file))
        // 2. Se cifra y se abre con la frase de paso.
        System.loadLibrary("sqlcipher")
        val pass = KeyVault.databasePassphraseHex(context.noBackupFilesDir)
        DatabaseEncryption.encryptIfPlain(file, pass)
        assertFalse(DatabaseEncryption.isPlain(file))
        val db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name)
            .openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(pass)).build()
        assertEquals(listOf("P-001"), RoomRecordsRepository(db.recordsDao()).patients.first().map { it.id })
        db.close()
    }

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    // Caso del Honor (2026-11-20): una base v2 sin cifrar se cifra y después Room la migra de v2 a v5 sin perder
    // las evaluaciones.
    @Test
    fun baseV2SinCifrar_secifraYMigraAv5() = runBlocking {
        val name = "prueba-cifrado-v2.db"
        val file = context.getDatabasePath(name)
        listOf("", "-wal", "-shm", ".cifrada").forEach { File(file.path + it).delete() }
        helper.createDatabase(name, 2).apply {
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
        System.loadLibrary("sqlcipher")
        val pass = KeyVault.databasePassphraseHex(context.noBackupFilesDir)
        DatabaseEncryption.encryptIfPlain(file, pass)
        assertFalse(DatabaseEncryption.isPlain(file))
        val db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(pass)).build()
        val sdb = db.openHelper.readableDatabase
        assertEquals(5, sdb.version)
        sdb.query("SELECT id FROM evaluations").use { c -> assertEquals(1, c.count) }
        assertEquals(listOf("HC-1"), RoomRecordsRepository(db.recordsDao()).patients.first().map { it.id })
        db.close()
    }

    // Opcional: si se copió la base real del teléfono como prueba-real.db (con su -wal y -shm), se cifra y se migra
    // la copia, y se informa cuántas filas quedan en cada tabla. Los conteos esperados se pasan con
    // -e pacientes N -e lesiones N -e evaluaciones N. Sin la copia, la prueba se salta.
    @Test
    fun copiaDeLaBaseReal_secifraYMigraSinPerderRegistros() {
        val file = context.getDatabasePath("prueba-real.db")
        org.junit.Assume.assumeTrue(file.isFile)
        val args = InstrumentationRegistry.getArguments()
        System.loadLibrary("sqlcipher")
        val pass = KeyVault.databasePassphraseHex(context.noBackupFilesDir)
        DatabaseEncryption.encryptIfPlain(file, pass)
        assertFalse(DatabaseEncryption.isPlain(file))
        val db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "prueba-real.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(pass)).build()
        val sdb = db.openHelper.readableDatabase
        assertEquals(5, sdb.version)
        for ((tabla, clave) in listOf("patients" to "pacientes", "lesions" to "lesiones", "evaluations" to "evaluaciones")) {
            val n = sdb.query("SELECT count(*) FROM $tabla").use { c -> c.moveToFirst(); c.getInt(0) }
            android.util.Log.i("CifradoEnDispositivoTest", "prueba-real.db: $tabla = $n")
            args.getString(clave)?.let { assertEquals(tabla, it.toInt(), n) }
        }
        db.close()
    }

    @Test
    fun imagen_seCifraConLaClaveDeDatosGuardadaConElKeystore() {
        val ks = KeyVault.imageKey()
        SecureImages.key = KeyVault.imageDataKey(context.noBackupFilesDir, ks)
        SecureImages.legacyKey = ks
        // La clave de datos se guarda cifrada y se recupera igual al abrir otra vez la app.
        assertTrue(SecureImages.key!!.encoded.contentEquals(KeyVault.imageDataKey(context.noBackupFilesDir, ks).encoded))
        val f = File(context.cacheDir, "prueba-cifrado.jpg")
        val plain = ByteArray(1000) { (it % 251).toByte() }
        SecureImages.write(f, plain)
        assertTrue(SecureImages.isCurrentFormat(f.readBytes()))
        assertTrue(plain.contentEquals(SecureImages.readPlain(f)))
        // Una imagen del formato anterior (clave del Keystore) se sigue leyendo y se recifra con la clave de datos.
        val dir = File(context.cacheDir, "prueba-imagenes").apply { deleteRecursively(); mkdirs() }
        val vieja = File(dir, "img-vieja.jpg").apply { writeBytes(SecureImages.encrypt(plain, ks)) }
        assertTrue(plain.contentEquals(SecureImages.readPlain(vieja)))
        assertEquals(1, SecureImages.encryptExisting(dir, SecureImages.key!!, ks))
        assertTrue(SecureImages.isCurrentFormat(vieja.readBytes()))
        assertTrue(plain.contentEquals(SecureImages.readPlain(vieja)))
        dir.deleteRecursively(); f.delete()
    }

    // Tiempo de descifrar 500 KB con la clave del Keystore (formato anterior) y con la clave de datos (actual). Se
    // informa en la salida de la instrumentación (INSTRUMENTATION_STATUS: descifrado=…), porque el Honor no muestra el
    // registro de la app.
    @Test
    fun tiempoDeDescifrado_keystoreFrenteAClaveDeDatos() {
        val ks = KeyVault.imageKey()
        val data = KeyVault.imageDataKey(context.noBackupFilesDir, ks)
        val plain = ByteArray(500_000).also { java.security.SecureRandom().nextBytes(it) }
        fun medir(enc: ByteArray, k: javax.crypto.SecretKey) = (1..5).map {
            val t0 = System.nanoTime(); SecureImages.decrypt(enc, k); (System.nanoTime() - t0) / 1_000_000
        }
        val tKs = medir(SecureImages.encrypt(plain, ks), ks)
        val tData = medir(SecureImages.encrypt(plain, data, current = true), data)
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0, android.os.Bundle().apply { putString("descifrado", "500000 bytes; Keystore: $tKs ms; clave de datos: $tData ms") }
        )
        assertTrue(tData.average() < tKs.average())
    }
}
