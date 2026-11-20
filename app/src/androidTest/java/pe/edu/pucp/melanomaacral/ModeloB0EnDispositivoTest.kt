package pe.edu.pucp.melanomaacral

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.pucp.melanomaacral.data.real.MotorInferencia
import pe.edu.pucp.melanomaacral.data.real.MotorLiteRt
import pe.edu.pucp.melanomaacral.data.real.MotorOnnx
import pe.edu.pucp.melanomaacral.data.real.PosprocesadoB0
import pe.edu.pucp.melanomaacral.data.real.PreprocesadoB0
import java.io.File
import kotlin.math.abs

// Fase 4 en el teléfono real: latencia de LiteRT y ONNX Runtime (D2) y paridad con PyTorch (D4).
// Usa las 27 imágenes del paquete de paridad (androidTest/assets/paridad).
// Correr:  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=pe.edu.pucp.melanomaacral.ModeloB0EnDispositivoTest
// Informe: logcat (etiqueta ModeloB0Test) y /sdcard/Android/data/pe.edu.pucp.melanomaacral/files/modelo_b0_en_dispositivo.txt
@RunWith(AndroidJUnit4::class)
class ModeloB0EnDispositivoTest {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val app = inst.targetContext            // assets de la app: los modelos
    private val pruebas = inst.context              // assets de la prueba: el paquete de paridad
    private val informe = StringBuilder()

    private fun say(s: String) { Log.i("ModeloB0Test", s); informe.appendLine(s) }

    private class Caso(val nombre: String, val v: JsonObject, val entrada: FloatArray)

    private fun bitmapRgb(b: Bitmap): PreprocesadoB0.Rgb {
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        return PreprocesadoB0.Rgb.desdeArgb(b.width, b.height, px)
    }

    private val opciones = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inScaled = false }

    private fun casos(): List<Caso> = pruebas.assets.list("paridad")!!.sorted().map { n ->
        val v = Json.parseToJsonElement(pruebas.assets.open("paridad/$n/valores.json").bufferedReader().readText()) as JsonObject
        val b = pruebas.assets.open("paridad/$n/entrada_modelo_384.png").use { BitmapFactory.decodeStream(it, null, opciones)!! }
        val rgb = bitmapRgb(b).rgb
        Caso(n, v, FloatArray(rgb.size) { rgb[it].toFloat() })
    }

    private fun percentil(xs: List<Double>, p: Double) = xs.sorted()[((xs.size - 1) * p).toInt()]

    @Test
    fun latenciaYParidadDeLosDosRuntimes() {
        val cs = casos()
        say("Teléfono: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}; ${cs.size} imágenes")
        var cambian = 0; var peorLogit = 0.0
        for (crear in listOf<() -> MotorInferencia>({ MotorLiteRt(app) }, { MotorOnnx(app) })) {
            val t0 = SystemClock.elapsedRealtimeNanos()
            crear().use { m ->
                val carga = (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
                val t1 = SystemClock.elapsedRealtimeNanos(); m.inferir(cs[0].entrada)
                val fria = (SystemClock.elapsedRealtimeNanos() - t1) / 1e6
                var dMax = 0.0; var camRel = 0.0; var lado = 0
                for (c in cs) {
                    val o = m.inferir(c.entrada)
                    val ref = c.v.getValue("logit_pytorch_fp32").jsonPrimitive.double
                    dMax = maxOf(dMax, abs(o.logit - ref))
                    if ((o.logit > PosprocesadoB0.UMBRAL_LOGIT) != (ref > PosprocesadoB0.UMBRAL_LOGIT)) lado++
                    val camRef = c.v.getValue("cam_12x12").jsonArray.flatMap { f -> f.jsonArray.map { it.jsonPrimitive.float } }
                    val esc = camRef.maxOf { abs(it) } + 1e-12f
                    camRel = maxOf(camRel, (camRef.indices.maxOf { abs(o.cam[it] - camRef[it]) } / esc).toDouble())
                }
                val tiempos = List(50) { i ->
                    val t = SystemClock.elapsedRealtimeNanos(); m.inferir(cs[i % cs.size].entrada)
                    (SystemClock.elapsedRealtimeNanos() - t) / 1e6
                }
                say("${m.nombre}: carga ${"%.0f".format(carga)} ms; primera inferencia ${"%.0f".format(fria)} ms; " +
                    "en caliente p50 ${"%.0f".format(percentil(tiempos, 0.5))} ms, p95 ${"%.0f".format(percentil(tiempos, 0.95))} ms (50 corridas, 4 hilos)")
                say("${m.nombre}: frente a PyTorch fp32, máx |dif logit| ${"%.4f".format(dMax)}, cambian de lado del umbral $lado de ${cs.size}, máx error relativo del CAM ${"%.2e".format(camRel)}")
                cambian += lado; peorLogit = maxOf(peorLogit, dMax)
            }
        }
        guardar()
        assertTrue("alguna imagen cambia de lado del umbral frente a PyTorch\n$informe", cambian == 0)
        assertTrue("diferencia de logit $peorLogit frente a PyTorch\n$informe", peorLogit < 0.05)
    }

    // Camino completo como en la app: JPEG original -> decodificador de Android -> preprocesado en Kotlin -> LiteRT.
    @Test
    fun caminoCompletoDesdeElJpeg() {
        val cs = casos()
        MotorLiteRt(app).use { m ->
            var peor = 0; var peorLogit = 0.0; var lado = 0; val tPre = ArrayList<Double>()
            for (c in cs) {
                val nombre = pruebas.assets.list("paridad/${c.nombre}")!!.first { it.startsWith("original") }
                val bytes = pruebas.assets.open("paridad/${c.nombre}/$nombre").use { it.readBytes() }
                val t = SystemClock.elapsedRealtimeNanos()
                var b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)!!
                val giro = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
                }
                if (giro != 0f) b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(giro) }, false)
                val pre = PreprocesadoB0.preprocesar(bitmapRgb(b))
                tPre += (SystemClock.elapsedRealtimeNanos() - t) / 1e6
                val e = pre.entrada.rgb
                val dif = e.indices.maxOf { abs(e[it] - c.entrada[it].toInt()) }
                val o = m.inferir(FloatArray(e.size) { e[it].toFloat() })
                val ref = c.v.getValue("logit_pytorch_fp32").jsonPrimitive.double
                if ((o.logit > PosprocesadoB0.UMBRAL_LOGIT) != (ref > PosprocesadoB0.UMBRAL_LOGIT)) lado++
                peor = maxOf(peor, dif); peorLogit = maxOf(peorLogit, abs(o.logit - ref))
                say("${c.nombre}: ${pre.interpolacion}, máx |dif| de la entrada $dif, logit ${"%.3f".format(o.logit)} (PyTorch ${"%.3f".format(ref)})")
            }
            say("Camino completo: máx |dif| de la entrada $peor, máx |dif logit| ${"%.3f".format(peorLogit)}, cambian de lado $lado de ${cs.size}; " +
                "decodificar y preprocesar p50 ${"%.0f".format(percentil(tPre, 0.5))} ms")
        }
        guardar()
    }

    private fun guardar() {
        val f = File(app.getExternalFilesDir(null), "modelo_b0_en_dispositivo.txt")
        f.appendText(informe.toString() + "\n")
    }
}
