package pe.edu.pucp.melanomaacral.data.real

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

// Salidas del modelo exportado (b0_melanoma_gradcam.onnx y .tflite, ver modelo_b0.json):
//   logit; cam 12 × 12 con signo; mapaRelu y mapaSigno de 384 × 384 (interpolación bilineal de ReLU(cam) y de cam).
// capas: promedio global de las 7 etapas y la capa final (2064 valores), solo en el ONNX con la salida «capas» (RF-05).
class SalidaModelo(val logit: Float, val cam: FloatArray, val mapaRelu: FloatArray, val mapaSigno: FloatArray, val capas: FloatArray? = null)

// Constantes del modelo final y la parte final de explicar_final.py (sha256 aa95813e), en Kotlin puro.
object PosprocesadoB0 {
    // Umbral del modelo final: promedio de los 15 umbrales primarios (addendum 10). Melanoma si logit > umbral.
    const val UMBRAL_LOGIT = -5.141666666666667
    const val TAU_MASCARA = 0.5f
    const val VERSION = "B0 final 050747d2"

    fun sigmoide(logit: Double): Double = 1.0 / (1.0 + exp(-logit))

    class Mapa(val ancho: Int, val alto: Int, val valores: FloatArray, val fallbackSinRelu: Boolean)

    // Recorta los mapas a la caja de la imagen y normaliza como explicar_final.explicar:
    // ReLU / máximo; si el mapa con ReLU es todo 0, el mapa con signo normalizado entre su mínimo y su máximo.
    fun mapaEnCaja(salida: SalidaModelo, caja: PreprocesadoB0.Caja): Mapa {
        val s = PreprocesadoB0.SIZE
        val w = caja.x1 - caja.x0; val h = caja.y1 - caja.y0
        fun recorte(m: FloatArray) = FloatArray(w * h) { i -> m[(caja.y0 + i / w) * s + caja.x0 + i % w] }
        val r = recorte(salida.mapaRelu)
        val maxR = r.maxOrNull() ?: 0f
        if (maxR > 0f) return Mapa(w, h, FloatArray(r.size) { (r[it] / maxR).coerceIn(0f, 1f) }, false)
        val sg = recorte(salida.mapaSigno)
        val lo = sg.minOrNull() ?: 0f; val hi = sg.maxOrNull() ?: 0f
        val v = if (hi > lo) FloatArray(sg.size) { ((sg[it] - lo) / (hi - lo)).coerceIn(0f, 1f) } else FloatArray(sg.size) { 1f }
        return Mapa(w, h, v, true)
    }

    // Evidencia EN CONTRA de melanoma: ReLU(-cam) recortado a la caja y normalizado a su máximo (la misma regla que
    // Grad-CAM). Si todo es 0 (el modelo no tiene evidencia en contra), se usa -cam normalizado entre mínimo y máximo.
    fun mapaEnContraEnCaja(salida: SalidaModelo, caja: PreprocesadoB0.Caja): Mapa {
        val invertida = FloatArray(salida.mapaSigno.size) { -salida.mapaSigno[it] }
        val s = SalidaModelo(salida.logit, salida.cam, FloatArray(invertida.size) { maxOf(0f, invertida[it]) }, invertida)
        return mapaEnCaja(s, caja)
    }

    // Reduce el mapa a una rejilla de a lo sumo `ladoMax` celdas por lado (promedio por área); la pantalla la
    // dibuja con suavizado bilineal.
    fun rejilla(mapa: Mapa, ladoMax: Int = 192): List<List<Float>> {
        val f = min(1.0, ladoMax.toDouble() / max(mapa.ancho, mapa.alto))
        val cw = max(1, Math.round(mapa.ancho * f).toInt()); val ch = max(1, Math.round(mapa.alto * f).toInt())
        return List(ch) { r ->
            val y0 = r * mapa.alto / ch; val y1 = max(y0 + 1, (r + 1) * mapa.alto / ch)
            List(cw) { c ->
                val x0 = c * mapa.ancho / cw; val x1 = max(x0 + 1, (c + 1) * mapa.ancho / cw)
                var s = 0f
                for (y in y0 until y1) for (x in x0 until x1) s += mapa.valores[y * mapa.ancho + x]
                s / ((y1 - y0) * (x1 - x0))
            }
        }
    }
}
