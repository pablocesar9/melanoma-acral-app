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
        val file = File(image.uri)
        require(file.isFile) { "No se encuentra la imagen" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, ImageQualityAnalyzer.ANALYSIS_SIDE_PX)
        }
        val decoded = BitmapFactory.decodeFile(file.path, options)
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
        val report = ImageQualityAnalyzer.analyze(
            luma, bitmap.width, bitmap.height,
            originalWidth = bounds.outWidth, originalHeight = bounds.outHeight,
            checkFraming = kind == ImageKind.DERMOSCOPIC
        )
        bitmap.recycle()
        report
    }
}
