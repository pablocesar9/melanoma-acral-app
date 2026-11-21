package pe.edu.pucp.melanomaacral.data.real

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.repository.ClassifierRepository
import pe.edu.pucp.melanomaacral.domain.repository.ExplanationRepository

enum class Runtime { LITERT, ONNX }

// Resultado de UNA inferencia: la probabilidad y el Grad-CAM salen de la misma pasada del modelo.
class AnalisisB0(
    val uri: String, val logit: Float,
    val mapa: PosprocesadoB0.Mapa,          // Grad-CAM: evidencia a favor de melanoma
    val mapaEnContra: PosprocesadoB0.Mapa,  // evidencia en contra de melanoma
    val motor: String, val msPreprocesado: Long, val msInferencia: Long,
    val puntajeAlcance: Double? = null,     // RF-05; null si el motor no entrega «capas» (LiteRT o el ONNX anterior)
    val fueraDeAlcance: Boolean = false
)

// Corre el modelo final B0 en el teléfono. El resultado de la última imagen queda en memoria para que el
// clasificador y el mapa de calor (que la app pide por separado) no repitan la inferencia.
class AnalizadorB0(private val context: Context, private val runtime: Runtime = Runtime.ONNX) {
    private val mutex = Mutex()
    private val alcance: FueraDeAlcanceB0? by lazy { runCatching { FueraDeAlcanceB0.desdeAssets(context) }.getOrNull() }
    private var motor: MotorInferencia? = null
    private var ultimo: AnalisisB0? = null

    suspend fun analizar(image: LesionImage): AnalisisB0 {
        mutex.withLock {
        ultimo?.takeIf { it.uri == image.uri }?.let { return it }
        return withContext(Dispatchers.Default) {
            val m = motor ?: (if (runtime == Runtime.LITERT) MotorLiteRt(context) else MotorOnnx(context)).also { motor = it }
            val t0 = SystemClock.elapsedRealtime()
            val pre = PreprocesadoB0.preprocesar(leerRgb(image.uri))
            val entrada = FloatArray(pre.entrada.rgb.size) { pre.entrada.rgb[it].toFloat() }
            val t1 = SystemClock.elapsedRealtime()
            val salida = m.inferir(entrada)
            val t2 = SystemClock.elapsedRealtime()
            val regla = alcance
            val puntaje = if (regla != null && salida.capas != null) regla.puntaje(salida.capas) else null
            AnalisisB0(
                image.uri, salida.logit, PosprocesadoB0.mapaEnCaja(salida, pre.caja),
                PosprocesadoB0.mapaEnContraEnCaja(salida, pre.caja), m.nombre, t1 - t0, t2 - t1,
                puntaje, puntaje != null && regla != null && puntaje < regla.umbral
            ).also {
                Log.i("ModeloB0", "${m.nombre}: logit ${it.logit}, alcance ${it.puntajeAlcance} (fuera ${it.fueraDeAlcance}), preprocesado ${it.msPreprocesado} ms, inferencia ${it.msInferencia} ms, " +
                    "fallback ${it.mapa.fallbackSinRelu}, ${pre.interpolacion} escala ${"%.4f".format(pre.escala)}")
                ultimo = it
            }
        }
        }
    }

    // RGB de 8 bits con la orientación EXIF aplicada (las fotos guardadas por la app ya vienen enderezadas).
    private fun leerRgb(ruta: String): PreprocesadoB0.Rgb {
        val opciones = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inScaled = false }
        var bmp = pe.edu.pucp.melanomaacral.data.files.SecureImages.decodeFile(ruta, opciones) ?: error("No se pudo leer la imagen $ruta")
        val giro = when (pe.edu.pucp.melanomaacral.data.files.SecureImages.exifOrientation(ruta)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (giro != 0f) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(giro) }, false)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        return PreprocesadoB0.Rgb.desdeArgb(bmp.width, bmp.height, px)
    }
}

// Clasificador real (RF-04): probabilidad = sigmoide(logit); umbral = sigmoide(umbral_logit) del modelo final.
// RF-05: fuera de alcance según la capa lineal sobre las capas del mismo modelo (FueraDeAlcanceB0).
class OnDeviceClassifierRepository(private val analizador: AnalizadorB0) : ClassifierRepository {
    override suspend fun classify(image: LesionImage): Prediction {
        val a = analizador.analizar(image)
        return Prediction(
            probMelanoma = PosprocesadoB0.sigmoide(a.logit.toDouble()).toFloat(),
            isOutOfScope = a.fueraDeAlcance,
            modelVersion = "${PosprocesadoB0.VERSION} (${a.motor})",
            threshold = PosprocesadoB0.sigmoide(PosprocesadoB0.UMBRAL_LOGIT).toFloat(),
            isSimulated = false,
            score = a.logit,
            scoreThreshold = PosprocesadoB0.UMBRAL_LOGIT.toFloat()
        )
    }
}

// Mapa de calor real (RF-19), de la misma inferencia: Grad-CAM (a favor) o evidencia en contra, recortados a la
// imagen y normalizados como en la tesis. Cuál se muestra lo decide quien llama, según la predicción del modelo.
class OnDeviceExplanationRepository(private val analizador: AnalizadorB0) : ExplanationRepository {
    override suspend fun explain(image: LesionImage, kind: ExplanationKind): Explanation {
        val a = analizador.analizar(image)
        val mapa = if (kind == ExplanationKind.FOR_MELANOMA) a.mapa else a.mapaEnContra
        return Explanation(PosprocesadoB0.rejilla(mapa), isSimulated = false, kind = kind)
    }
}
