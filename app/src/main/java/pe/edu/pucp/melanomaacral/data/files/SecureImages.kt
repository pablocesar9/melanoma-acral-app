package pe.edu.pucp.melanomaacral.data.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

// RNF-05: las imágenes de la app se guardan cifradas con AES-256-GCM. La clave vive en el Keystore de Android
// (no sale del almacén seguro del teléfono). Formato del archivo: "MAC1" + IV de 12 bytes + texto cifrado con su etiqueta.
// Los archivos sin cifrar (anteriores a esta versión, o las fotos de prueba en la JVM) se siguen leyendo tal cual.
object SecureImages {
    private val MAGIC = byteArrayOf('M'.code.toByte(), 'A'.code.toByte(), 'C'.code.toByte(), '1'.code.toByte())
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    // Clave de cifrado; null = no cifrar (pruebas en la JVM). La fija AppContainer al iniciar la app.
    @Volatile var key: SecretKey? = null

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + IV_BYTES && (MAGIC.indices).all { bytes[it] == MAGIC[it] }

    fun encrypt(plain: ByteArray, k: SecretKey): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Las claves del Keystore no aceptan un IV propio: se deja que el proveedor lo genere y se guarda el suyo.
        val usedIv = try {
            cipher.init(Cipher.ENCRYPT_MODE, k, GCMParameterSpec(TAG_BITS, iv)); iv
        } catch (e: java.security.InvalidAlgorithmParameterException) {
            cipher.init(Cipher.ENCRYPT_MODE, k); cipher.iv
        }
        return MAGIC + usedIv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray, k: SecretKey): ByteArray {
        val iv = data.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(data, MAGIC.size + IV_BYTES, data.size - MAGIC.size - IV_BYTES)
    }

    // Bytes de la imagen en claro, esté o no cifrada.
    fun readPlain(file: File): ByteArray {
        val bytes = file.readBytes()
        if (!isEncrypted(bytes)) return bytes
        val k = key ?: error("Imagen cifrada y sin clave disponible")
        return decrypt(bytes, k)
    }

    // Escribe la imagen cifrada si hay clave; si no, en claro.
    fun write(file: File, plain: ByteArray) {
        val k = key
        file.writeBytes(if (k != null) encrypt(plain, k) else plain)
    }

    fun writeJpeg(file: File, bitmap: Bitmap, quality: Int) {
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        write(file, out.toByteArray())
    }

    // Reemplazo de BitmapFactory.decodeFile que entiende los archivos cifrados.
    fun decodeFile(path: String, options: BitmapFactory.Options? = null): Bitmap? {
        val file = File(path)
        if (!file.isFile) return null
        val bytes = runCatching { readPlain(file) }.getOrNull() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    fun exifOrientation(path: String): Int = runCatching {
        ExifInterface(ByteArrayInputStream(readPlain(File(path))))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    // Copia en claro (para compartir la derivación por FileProvider, en la caché de la app).
    fun copyPlain(src: File, dst: File) = dst.writeBytes(readPlain(src))
}

// Clave AES-256 del Keystore de Android para los datos de la app (RNF-05).
object KeyVault {
    private const val ALIAS = "melanoma-acral-datos"

    fun imageKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    // Frase de paso de la base de datos (SQLCipher): 32 bytes aleatorios en hexadecimal (64 caracteres ASCII),
    // guardados cifrados con la clave del Keystore en la carpeta sin respaldo de la app.
    fun databasePassphraseHex(dir: File): ByteArray {
        val f = File(dir, "db.key")
        val k = imageKey()
        if (f.isFile) return SecureImages.decrypt(f.readBytes(), k)
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hex = raw.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.US_ASCII)
        f.writeBytes(SecureImages.encrypt(hex, k))
        return hex
    }
}
