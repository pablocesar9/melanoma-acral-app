package pe.edu.pucp.melanomaacral.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.pucp.melanomaacral.data.files.ImageScaling
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import java.io.File

// Muestra una imagen guardada (archivo interno de la app). Si la imagen no existe o es de
// los datos de ejemplo (sample://, simulated://), muestra el recuadro de relleno.
// Se decodifica reducida (maxSidePx) y fuera del hilo principal.
@Composable
fun LesionImageView(
    image: LesionImage?,
    label: String,
    modifier: Modifier = Modifier,
    maxSidePx: Int = 1024,
    alpha: Float = 1f,
    contentScale: ContentScale = ContentScale.Fit,
    // true: alto según la proporción de la imagen (ocupa todo el ancho). false: usa el tamaño de `modifier` (miniaturas).
    fillAspect: Boolean = true
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, image?.uri, maxSidePx) {
        value = image?.let { loadScaled(it.uri, maxSidePx) }
    }
    val loaded = bitmap
    if (loaded == null) {
        ImagePlaceholder(label = label, modifier = modifier)
    } else {
        Image(
            bitmap = loaded,
            contentDescription = label,
            alpha = alpha,
            contentScale = contentScale,
            modifier = if (fillAspect) {
                modifier.fillMaxWidth().aspectRatio(loaded.width.toFloat() / loaded.height)
            } else modifier
        )
    }
}

private suspend fun loadScaled(path: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.isFile) return@withContext null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return@withContext null
    val options = BitmapFactory.Options().apply {
        inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
    }
    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}
