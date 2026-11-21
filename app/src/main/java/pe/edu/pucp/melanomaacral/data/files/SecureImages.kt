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

// RNF-05: las imágenes de la app se guardan cifradas con AES-256-GCM. Formato del archivo: cabecera de 4 bytes +
// IV de 12 bytes + texto cifrado con su etiqueta. La cabecera dice con qué clave se cifró:
//  - "MAC2": clave de datos de la app (KeyVault.imageDataKey), guardada cifrada con la clave del Keystore, igual que la
//    frase de paso de la base de datos. El cifrado corre en el procesador, con su aceleración AES.
//  - "MAC1": directamente con la clave del Keystore (versión anterior). En el Honor, descifrar así una foto de 500 KB
//    tardaba unos 1,25 s y las miniaturas cargaban lento; esas imágenes se recifran una vez a "MAC2" (encryptExisting).
// Los archivos sin cifrar (anteriores al cifrado, o las fotos de prueba en la JVM) se siguen leyendo tal cual.
object SecureImages {
    private val MAGIC1 = byteArrayOf('M'.code.toByte(), 'A'.code.toByte(), 'C'.code.toByte(), '1'.code.toByte())
    private val MAGIC2 = byteArrayOf('M'.code.toByte(), 'A'.code.toByte(), 'C'.code.toByte(), '2'.code.toByte())
    private const val HEADER = 4
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    // Clave de cifrado de las imágenes; null = no cifrar (pruebas en la JVM). La fija AppContainer al iniciar la app.
    @Volatile var key: SecretKey? = null
    // Clave del Keystore, solo para leer las imágenes "MAC1"; si es null se usa `key`.
    @Volatile var legacyKey: SecretKey? = null

    private fun starts(bytes: ByteArray, magic: ByteArray) =
        bytes.size > HEADER + IV_BYTES && magic.indices.all { bytes[it] == magic[it] }

    fun isEncrypted(bytes: ByteArray): Boolean = starts(bytes, MAGIC1) || starts(bytes, MAGIC2)

    // true si la imagen ya está en el formato actual ("MAC2").
    fun isCurrentFormat(bytes: ByteArray): Boolean = starts(bytes, MAGIC2)

    // `current` = true escribe la cabecera "MAC2" (clave de datos); false, "MAC1" (clave del Keystore, también la usa
    // KeyVault para guardar las claves de la app).
    fun encrypt(plain: ByteArray, k: SecretKey, current: Boolean = false): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Las claves del Keystore no aceptan un IV propio: se deja que el proveedor lo genere y se guarda el suyo.
        val usedIv = try {
            cipher.init(Cipher.ENCRYPT_MODE, k, GCMParameterSpec(TAG_BITS, iv)); iv
        } catch (e: java.security.InvalidAlgorithmParameterException) {
            cipher.init(Cipher.ENCRYPT_MODE, k); cipher.iv
        }
        return (if (current) MAGIC2 else MAGIC1) + usedIv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray, k: SecretKey): ByteArray {
        val iv = data.copyOfRange(HEADER, HEADER + IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(data, HEADER + IV_BYTES, data.size - HEADER - IV_BYTES)
    }

    private fun decryptImage(bytes: ByteArray): ByteArray {
        val k = (if (starts(bytes, MAGIC1)) legacyKey ?: key else key) ?: error("Imagen cifrada y sin clave disponible")
        return decrypt(bytes, k)
    }

    // Bytes de la imagen en claro, esté o no cifrada.
    fun readPlain(file: File): ByteArray {
        val bytes = file.readBytes()
        if (!isEncrypted(bytes)) return bytes
        return decryptImage(bytes)
    }

    // Escribe la imagen cifrada si hay clave; si no, en claro.
    fun write(file: File, plain: ByteArray) {
        val k = key
        file.writeBytes(if (k != null) encrypt(plain, k, current = true) else plain)
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

    // Pasa, una sola vez, las imágenes guardadas (img-*) al formato actual: cifra las que quedaron sin cifrar y
    // recifra con la clave de datos las "MAC1" (`legacy` = clave del Keystore con que se cifraron). Cada una se
    // escribe primero en un archivo temporal, se comprueba que se descifra igual al original y solo entonces lo
    // reemplaza (renombrar es atómico: quien la lea en ese momento obtiene una de las dos versiones, y las dos se
    // leen bien). Si algo falla, la imagen queda como estaba y se intenta otra vez al abrir la app.
    // Devuelve cuántas imágenes se cifraron o recifraron.
    fun encryptExisting(dir: File, k: SecretKey, legacy: SecretKey? = null): Int {
        val files = dir.listFiles() ?: return 0
        files.filter { it.name.endsWith(".cifrando") }.forEach { it.delete() }
        var n = 0
        for (f in files) {
            if (!f.isFile || !f.name.startsWith("img-") || f.name.endsWith(".cifrando")) continue
            val ok = runCatching {
                val bytes = f.readBytes()
                if (bytes.isEmpty() || isCurrentFormat(bytes)) return@runCatching false
                val plain = if (starts(bytes, MAGIC1)) decrypt(bytes, legacy ?: return@runCatching false) else bytes
                val enc = encrypt(plain, k, current = true)
                check(decrypt(enc, k).contentEquals(plain))
                val tmp = File(dir, f.name + ".cifrando")
                tmp.writeBytes(enc)
                check(tmp.renameTo(f))
                true
            }.getOrDefault(false)
            if (ok) n++
        }
        return n
    }
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

    // Clave de datos de las imágenes: AES-256 aleatoria, guardada cifrada con la clave del Keystore en la carpeta sin
    // respaldo de la app (como db.key). Así el Keystore solo descifra 32 bytes al iniciar, no cada imagen.
    fun imageDataKey(dir: File, keystoreKey: SecretKey = imageKey()): SecretKey {
        val f = File(dir, "img.key")
        if (f.isFile) return javax.crypto.spec.SecretKeySpec(SecureImages.decrypt(f.readBytes(), keystoreKey), "AES")
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val tmp = File(dir, "img.key.tmp")
        tmp.writeBytes(SecureImages.encrypt(raw, keystoreKey))
        check(tmp.renameTo(f)) { "No se pudo guardar la clave de las imágenes" }
        return javax.crypto.spec.SecretKeySpec(raw, "AES")
    }
}
