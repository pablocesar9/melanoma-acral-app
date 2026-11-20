package pe.edu.pucp.melanomaacral.domain.repository

import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import java.io.File

// Guarda las imágenes en el almacenamiento interno de la app, fuera de la galería pública
// (privacidad, RNF-05). Toda imagen que entra (cámara o galería) se normaliza: se corrige
// la orientación, se limita el tamaño y se reescribe como JPEG sin metadatos EXIF
// (así se elimina la ubicación GPS, RNF-06).
interface ImageStorage {
    // Archivo donde la cámara debe escribir la foto antes de registrarla.
    fun newCameraFile(): File

    // Normaliza una foto ya tomada por la cámara y devuelve su referencia con metadatos (RNF-07).
    suspend fun registerCameraFile(file: File): LesionImage

    // Copia una imagen elegida con el selector de fotos (content://) al almacenamiento interno (RF-21).
    suspend fun importFromGallery(uri: String): LesionImage

    // Borra un archivo propio de la app (por ejemplo, una foto descartada al recapturar).
    fun delete(image: LesionImage)

    companion object {
        // Lado máximo tras normalizar. 2048 px basta para la revisión visual y la derivación,
        // y mantiene los archivos y la memoria en un tamaño razonable en un teléfono de 4 GB.
        const val MAX_SIDE_PX = 2048
        const val JPEG_QUALITY = 92
    }
}

