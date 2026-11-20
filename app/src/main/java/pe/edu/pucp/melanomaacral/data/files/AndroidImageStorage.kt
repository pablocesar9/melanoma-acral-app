package pe.edu.pucp.melanomaacral.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.pucp.melanomaacral.domain.model.ImageSource
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.repository.ImageStorage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Almacenamiento en context.filesDir/images: carpeta privada de la app, invisible para la
// galería y para otras apps. Se borra al desinstalar la app.
class AndroidImageStorage(private val context: Context) : ImageStorage {

    private val directory: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    override fun newCameraFile(): File = File(directory, "raw-${UUID.randomUUID()}.jpg")

    override suspend fun registerCameraFile(file: File): LesionImage = withContext(Dispatchers.IO) {
        normalize(file, ImageSource.CAMERA).also { if (file.exists() && file.path != it.uri) file.delete() }
    }

    override suspend fun importFromGallery(uri: String): LesionImage = withContext(Dispatchers.IO) {
        val temp = File(directory, "raw-${UUID.randomUUID()}.jpg")
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(uri))
                ?: throw IllegalArgumentException("No se pudo abrir la imagen elegida")
            input.use { src -> FileOutputStream(temp).use { dst -> src.copyTo(dst) } }
            normalize(temp, ImageSource.GALLERY)
        } finally {
            temp.delete()
        }
    }

    override fun delete(image: LesionImage) {
        val file = File(image.uri)
        if (file.parentFile == directory) file.delete()
    }

    // Decodifica reducida, corrige la orientación EXIF y reescribe como JPEG sin metadatos.
    private fun normalize(source: File, origin: ImageSource): LesionImage {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "El archivo no es una imagen válida" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, ImageStorage.MAX_SIDE_PX)
        }
        var bitmap = BitmapFactory.decodeFile(source.path, options)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")

        val orientation = ExifInterface(source.path)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        val (fitW, fitH) = ImageScaling.fit(bitmap.width, bitmap.height, ImageStorage.MAX_SIDE_PX)
        if (fitW != bitmap.width || fitH != bitmap.height) {
            bitmap = Bitmap.createScaledBitmap(bitmap, fitW, fitH, true)
        }
        if (!matrix.isIdentity) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        val target = File(directory, "img-${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, ImageStorage.JPEG_QUALITY, it) }
        val image = LesionImage(
            uri = target.path,
            source = origin,
            widthPx = bitmap.width,
            heightPx = bitmap.height,
            sizeBytes = target.length(),
            capturedAtMillis = System.currentTimeMillis()
        )
        bitmap.recycle()
        return image
    }
}
