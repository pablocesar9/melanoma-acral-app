package pe.edu.pucp.melanomaacral.data.files

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.crypto.KeyGenerator

// RNF-05: las imágenes se guardan cifradas (AES-256-GCM) y se leen igual que antes; las que no estaban cifradas
// se siguen leyendo. En la JVM no hay Keystore: se usa una clave AES generada en la prueba.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SecureImagesTest {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val sample = File("src/test/resources/imagenes-ejemplo/ejemplo1.jpg")

    @After
    fun tearDown() { SecureImages.key = null; SecureImages.legacyKey = null }

    @Test
    fun cifrar_yDescifrar_devuelveLoMismo() {
        val plain = sample.readBytes()
        val enc = SecureImages.encrypt(plain, key)
        assertTrue(SecureImages.isEncrypted(enc))
        assertFalse(SecureImages.isEncrypted(plain))
        assertFalse(enc.copyOfRange(16, 64).contentEquals(plain.copyOfRange(0, 48)))
        assertArrayEquals(plain, SecureImages.decrypt(enc, key))
    }

    @Test
    fun elArchivoGuardado_noEsUnJpegLegible_peroLaAppLoDecodifica() {
        SecureImages.key = key
        val f = File.createTempFile("img", ".jpg")
        try {
            SecureImages.write(f, sample.readBytes())
            val bytes = f.readBytes()
            // Un JPEG empieza con FF D8; el archivo cifrado no.
            assertFalse(bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
            val bmp = SecureImages.decodeFile(f.path)
            assertNotNull(bmp)
            val plainBmp = android.graphics.BitmapFactory.decodeFile(sample.path)
            assertEquals(plainBmp.width, bmp!!.width)
            assertEquals(plainBmp.height, bmp.height)
        } finally {
            f.delete()
        }
    }

    @Test
    fun imagenSinCifrar_seSigueLeyendo() {
        SecureImages.key = key
        assertNotNull(SecureImages.decodeFile(sample.path))
        assertArrayEquals(sample.readBytes(), SecureImages.readPlain(sample))
    }

    @Test
    fun copiaParaCompartir_quedaEnClaro() {
        SecureImages.key = key
        val enc = File.createTempFile("img", ".jpg"); val out = File.createTempFile("out", ".jpg")
        try {
            SecureImages.write(enc, sample.readBytes())
            SecureImages.copyPlain(enc, out)
            assertArrayEquals(sample.readBytes(), out.readBytes())
        } finally {
            enc.delete(); out.delete()
        }
    }

    // Las imágenes antiguas sin cifrar se cifran una sola vez y siguen leyéndose igual; las demás no se tocan.
    @Test
    fun imagenesAntiguas_seCifranUnaSolaVez() {
        val dir = kotlin.io.path.createTempDirectory("imagenes").toFile()
        try {
            val plain = sample.readBytes()
            val antigua = File(dir, "img-antigua.jpg").apply { writeBytes(plain) }
            val otra = File(dir, "raw-temporal.jpg").apply { writeBytes(plain) }
            File(dir, "img-x.jpg.cifrando").writeBytes(byteArrayOf(1, 2, 3))
            assertEquals(1, SecureImages.encryptExisting(dir, key))
            assertTrue(SecureImages.isEncrypted(antigua.readBytes()))
            SecureImages.key = key
            assertArrayEquals(plain, SecureImages.readPlain(antigua))
            assertArrayEquals(plain, otra.readBytes())
            assertFalse(File(dir, "img-x.jpg.cifrando").exists())
            assertEquals(0, SecureImages.encryptExisting(dir, key))
        } finally {
            dir.deleteRecursively()
        }
    }

    // Las imágenes del formato anterior ("MAC1", clave del Keystore) se leen con legacyKey y pasan al formato actual.
    @Test
    fun imagenesDelFormatoAnterior_seRecifranConLaClaveDeDatos() {
        val viejaClave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val dir = kotlin.io.path.createTempDirectory("imagenes").toFile()
        try {
            val plain = sample.readBytes()
            val vieja = File(dir, "img-vieja.jpg").apply { writeBytes(SecureImages.encrypt(plain, viejaClave)) }
            assertFalse(SecureImages.isCurrentFormat(vieja.readBytes()))
            SecureImages.key = key; SecureImages.legacyKey = viejaClave
            assertArrayEquals(plain, SecureImages.readPlain(vieja))
            // Sin la clave anterior no se toca.
            assertEquals(0, SecureImages.encryptExisting(dir, key))
            assertEquals(1, SecureImages.encryptExisting(dir, key, viejaClave))
            assertTrue(SecureImages.isCurrentFormat(vieja.readBytes()))
            assertArrayEquals(plain, SecureImages.readPlain(vieja))
            assertArrayEquals(plain, SecureImages.decrypt(vieja.readBytes(), key))
            assertEquals(0, SecureImages.encryptExisting(dir, key, viejaClave))
        } finally {
            dir.deleteRecursively()
        }
    }
}
