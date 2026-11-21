package pe.edu.pucp.melanomaacral

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun imagen_seCifraConLaClaveDelKeystore() {
        SecureImages.key = KeyVault.imageKey()
        val f = File(context.cacheDir, "prueba-cifrado.jpg")
        val plain = ByteArray(1000) { (it % 251).toByte() }
        SecureImages.write(f, plain)
        assertTrue(SecureImages.isEncrypted(f.readBytes()))
        assertTrue(plain.contentEquals(SecureImages.readPlain(f)))
        f.delete()
    }
}
