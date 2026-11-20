package pe.edu.pucp.melanomaacral.data.real

// Escala de color «jet» de matplotlib (tramos lineales), la misma de las figuras de Grad-CAM de la tesis:
// 0 = azul oscuro, 1 = rojo oscuro. Kotlin puro: la usan la pantalla (Compose) y el PDF (Android Canvas).
object EscalaJet {
    // Opacidad del mapa sobre la imagen, como en gradcam_img.py (alpha = 0.45).
    const val OPACIDAD = 0.45f

    private fun tramo(x: Float, puntos: FloatArray): Float {
        var i = 2
        while (i < puntos.size) {
            val x0 = puntos[i - 2]; val y0 = puntos[i - 1]; val x1 = puntos[i]; val y1 = puntos[i + 1]
            if (x <= x1) return y0 + (y1 - y0) * ((x - x0) / (x1 - x0)).coerceIn(0f, 1f)
            i += 2
        }
        return puntos.last()
    }

    private val R = floatArrayOf(0f, 0f, 0.35f, 0f, 0.66f, 1f, 0.89f, 1f, 1f, 0.5f)
    private val G = floatArrayOf(0f, 0f, 0.125f, 0f, 0.375f, 1f, 0.64f, 1f, 0.91f, 0f, 1f, 0f)
    private val B = floatArrayOf(0f, 0.5f, 0.11f, 1f, 0.34f, 1f, 0.65f, 0f, 1f, 0f)

    // Color ARGB (0xAARRGGBB) de un valor entre 0 y 1.
    fun argb(v: Float, alpha: Float = 1f): Int {
        val x = v.coerceIn(0f, 1f)
        fun c(f: Float) = (f.coerceIn(0f, 1f) * 255 + 0.5f).toInt()
        return (c(alpha) shl 24) or (c(tramo(x, R)) shl 16) or (c(tramo(x, G)) shl 8) or c(tramo(x, B))
    }
}
