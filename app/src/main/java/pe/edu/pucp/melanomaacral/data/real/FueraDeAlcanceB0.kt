package pe.edu.pucp.melanomaacral.data.real

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

// RF-05: aviso de imagen fuera del alcance del modelo, sin otro modelo. El ONNX entrega, además del logit y el mapa,
// la salida «capas»: el promedio global de las 7 etapas y de la capa final de B0 (2064 valores). Una capa lineal
// (regresión logística) separa las imágenes del entrenamiento de fotos comunes e imágenes sintéticas:
//   puntaje = Σ pesos_k · (capas_k − media_k) / desv_k + sesgo;  fuera de alcance si puntaje < umbral.
// Parámetros y evaluación: assets/modelos/fuera_de_alcance/ (fuera_de_alcance.json) y exportacion-movil-ood (ood2.py):
// con el umbral, 1.0 % de imágenes válidas rechazadas (validación cruzada por lesión) y 100 % de fotos no vistas.
class FueraDeAlcanceB0(
    private val media: FloatArray, private val desv: FloatArray, private val pesos: FloatArray,
    val sesgo: Double, val umbral: Double
) {
    init {
        require(media.size == pesos.size && desv.size == pesos.size) { "parámetros de fuera de alcance incoherentes" }
    }

    val n: Int get() = pesos.size

    fun puntaje(capas: FloatArray): Double {
        require(capas.size == n) { "se esperaban $n valores de capas y llegaron ${capas.size}" }
        var s = sesgo
        for (k in 0 until n) s += pesos[k].toDouble() * ((capas[k].toDouble() - media[k]) / desv[k])
        return s
    }

    fun fueraDeAlcance(capas: FloatArray): Boolean = puntaje(capas) < umbral

    companion object {
        private const val DIR = "modelos/fuera_de_alcance"

        fun flotantes(bytes: ByteArray): FloatArray =
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().let { b -> FloatArray(b.remaining()).also { b.get(it) } }

        fun desdeAssets(context: Context): FueraDeAlcanceB0 {
            fun leer(n: String) = context.assets.open("$DIR/$n").use { it.readBytes() }
            val json = JSONObject(String(leer("fuera_de_alcance.json")))
            return FueraDeAlcanceB0(
                flotantes(leer("media.f32")), flotantes(leer("desv.f32")), flotantes(leer("pesos.f32")),
                json.getDouble("sesgo"), json.getDouble("umbral")
            )
        }
    }
}
