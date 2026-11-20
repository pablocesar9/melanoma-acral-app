package pe.edu.pucp.melanomaacral.data.real

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// Un runtime de inferencia para el modelo exportado. Entrada: 384 × 384 × 3 valores 0-255 (RGB, NHWC).
// Dos implementaciones para comparar en el Honor (decisión D2 del plan): LiteRT y ONNX Runtime.
interface MotorInferencia : AutoCloseable {
    val nombre: String
    fun inferir(entrada: FloatArray): SalidaModelo
}

private const val N_ENTRADA = PreprocesadoB0.SIZE * PreprocesadoB0.SIZE * 3
private const val N_MAPA = PreprocesadoB0.SIZE * PreprocesadoB0.SIZE

private fun leerAsset(context: Context, ruta: String): ByteArray = context.assets.open(ruta).use { it.readBytes() }

private fun bufferDirecto(nFloats: Int): ByteBuffer = ByteBuffer.allocateDirect(nFloats * 4).order(ByteOrder.nativeOrder())

private fun FloatBuffer.aArreglo(n: Int): FloatArray = FloatArray(n).also { rewind(); get(it) }

// LiteRT (antes TensorFlow Lite) con el Interpreter clásico y XNNPACK (activado por defecto).
// Firma "serving_default": entrada args_0; salidas output_0..3 = logit, cam, mapa_relu, mapa_signo (modelo_b0.json).
class MotorLiteRt(context: Context, hilos: Int = 4) : MotorInferencia {
    override val nombre = "LiteRT"
    private val interprete: Interpreter
    private val entrada = bufferDirecto(N_ENTRADA)
    private val salidas = mapOf(
        "output_0" to bufferDirecto(1), "output_1" to bufferDirecto(144),
        "output_2" to bufferDirecto(N_MAPA), "output_3" to bufferDirecto(N_MAPA)
    )

    init {
        val bytes = leerAsset(context, "modelos/b0_melanoma_gradcam.tflite")
        val modelo = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.rewind() }
        interprete = Interpreter(modelo, Interpreter.Options().setNumThreads(hilos))
    }

    override fun inferir(entrada: FloatArray): SalidaModelo {
        this.entrada.rewind(); this.entrada.asFloatBuffer().put(entrada)
        salidas.values.forEach { it.rewind() }
        interprete.runSignature(mapOf("args_0" to this.entrada), salidas, "serving_default")
        fun f(k: String, n: Int) = salidas.getValue(k).also { it.rewind() }.asFloatBuffer().aArreglo(n)
        return SalidaModelo(f("output_0", 1)[0], f("output_1", 144), f("output_2", N_MAPA), f("output_3", N_MAPA))
    }

    override fun close() = interprete.close()
}

// ONNX Runtime para Android (CPU). Entrada "imagen"; salidas logit, cam, mapa_relu, mapa_signo y, en el modelo con
// RF-05 (b0_melanoma_gradcam_capas.onnx, mismos pesos), "capas" (1 × 2064). Si ese archivo no está, usa el anterior.
class MotorOnnx(context: Context, hilos: Int = 4) : MotorInferencia {
    override val nombre = "ONNX Runtime"
    private val entorno = OrtEnvironment.getEnvironment()
    private val sesion: OrtSession

    init {
        val opciones = OrtSession.SessionOptions().apply { setIntraOpNumThreads(hilos) }
        val conCapas = runCatching { context.assets.open(MODELO_CAPAS).close() }.isSuccess
        sesion = entorno.createSession(leerAsset(context, if (conCapas) MODELO_CAPAS else "modelos/b0_melanoma_gradcam.onnx"), opciones)
    }

    override fun inferir(entrada: FloatArray): SalidaModelo {
        val forma = longArrayOf(1, PreprocesadoB0.SIZE.toLong(), PreprocesadoB0.SIZE.toLong(), 3)
        OnnxTensor.createTensor(entorno, FloatBuffer.wrap(entrada), forma).use { tensor ->
            sesion.run(mapOf("imagen" to tensor)).use { r ->
                fun f(k: String, n: Int) = (r.get(k).get() as OnnxTensor).floatBuffer.aArreglo(n)
                val capas = if ("capas" in sesion.outputNames) f("capas", N_CAPAS) else null
                return SalidaModelo(f("logit", 1)[0], f("cam", 144), f("mapa_relu", N_MAPA), f("mapa_signo", N_MAPA), capas)
            }
        }
    }

    override fun close() = sesion.close()

    companion object {
        const val MODELO_CAPAS = "modelos/b0_melanoma_gradcam_capas.onnx"
        const val N_CAPAS = 2064
    }
}
