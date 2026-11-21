package pe.edu.pucp.melanomaacral.data.files

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.quality.ImageQualityAnalyzer
import pe.edu.pucp.melanomaacral.domain.repository.ImageQualityRepository
import java.io.File

// Calidad real, sin IA: decodifica la imagen reducida, la pasa a escala de grises y aplica
// ImageQualityAnalyzer (nitidez, exposición, resolución, encuadre). Es una heurística.
class HeuristicImageQualityRepository : ImageQualityRepository {

    override suspend fun check(image: LesionImage, kind: ImageKind): QualityReport = withContext(Dispatchers.Default) {
        val l = luma(File(image.uri))
        ImageQualityAnalyzer.analyze(
            l.luma, l.width, l.height,
            originalWidth = l.originalWidth, originalHeight = l.originalHeight,
            checkFraming = kind == ImageKind.DERMOSCOPIC,
            thresholds = if (kind == ImageKind.DERMOSCOPIC) ImageQualityAnalyzer.DERMOSCOPIC else ImageQualityAnalyzer.CLINICAL
        )
    }

    class Luma(val luma: IntArray, val width: Int, val height: Int, val originalWidth: Int, val originalHeight: Int)

    // Brillo por píxel de la imagen reducida a ANALYSIS_SIDE_PX, tal como lo mide check. La calibración de RF-03 en el
    // teléfono (CalidadEnDispositivoTest) usa esta misma función.
    fun luma(file: File): Luma {
        require(file.isFile) { "No se encuentra la imagen" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        SecureImages.decodeFile(file.path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, ImageQualityAnalyzer.ANALYSIS_SIDE_PX)
        }
        val decoded = SecureImages.decodeFile(file.path, options)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")
        // La nitidez depende de la escala: se lleva siempre al mismo lado mayor (inSampleSize
        // solo reduce por potencias de 2 y deja entre 512 y 1023 px).
        val longest = maxOf(decoded.width, decoded.height)
        val bitmap = if (longest > ImageQualityAnalyzer.ANALYSIS_SIDE_PX) {
            val k = ImageQualityAnalyzer.ANALYSIS_SIDE_PX.toFloat() / longest
            android.graphics.Bitmap.createScaledBitmap(
                decoded, (decoded.width * k).toInt().coerceAtLeast(1), (decoded.height * k).toInt().coerceAtLeast(1), true
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val luma = IntArray(pixels.size) {
            val c = pixels[it]
            // Luma Rec. 601
            ((0.299 * Color.red(c)) + (0.587 * Color.green(c)) + (0.114 * Color.blue(c))).toInt()
        }
        val result = Luma(luma, bitmap.width, bitmap.height, bounds.outWidth, bounds.outHeight)
        bitmap.recycle()
        return result
    }
}
