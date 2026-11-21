package pe.edu.pucp.melanomaacral.domain.rules

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// RNF-12: reglas del acceso por usuario. El PIN nunca se guarda: se guarda su derivación PBKDF2 con una sal propia.
object AccessRules {
    const val MIN_PIN = 4
    const val MAX_PIN = 8
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    private val USERNAME = Regex("^[a-z0-9._-]{3,20}$")

    // Motivo por el que el usuario o el PIN no sirven, o null si sirven.
    fun validate(username: String, pin: String): String? = when {
        !USERNAME.matches(username) -> "El usuario debe tener de 3 a 20 caracteres: minúsculas, números, punto, guion o guion bajo."
        pin.length !in MIN_PIN..MAX_PIN || !pin.all { it.isDigit() } -> "El PIN debe tener de $MIN_PIN a $MAX_PIN dígitos."
        else -> null
    }

    fun normalize(username: String): String = username.trim().lowercase()

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    // Comparación en tiempo constante.
    fun matches(pin: String, salt: ByteArray, expected: ByteArray): Boolean {
        val actual = hash(pin, salt)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }
}
