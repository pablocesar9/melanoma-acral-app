package pe.edu.pucp.melanomaacral.domain.model

import kotlinx.serialization.Serializable

// Origen de una imagen (RNF-07).
@Serializable
enum class ImageSource { CAMERA, GALLERY }

// Qué muestra la imagen (RF-02, RF-01).
@Serializable
enum class ImageKind { CONTEXT, DERMOSCOPIC }

// Referencia a la imagen de una lesión (cámara o galería) y sus metadatos técnicos (RNF-07).
// Se usa una URI/ruta en lugar de un Bitmap para que el dominio no dependa de Android
// y se pueda probar con pruebas unitarias normales.
// Las imágenes reales viven en el almacenamiento interno de la app (no en la galería, RNF-05).
@Serializable
data class LesionImage(
    val uri: String,
    val source: ImageSource = ImageSource.CAMERA,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val sizeBytes: Long = 0,
    val capturedAtMillis: Long = 0,
    // Métricas de la revisión de calidad (heurística, no IA). null = aún no medida.
    val sharpness: Float? = null,
    val meanLuma: Float? = null
)

// Imágenes de una captura: la dermoscópica y la clínica de contexto (RF-02).
// La de contexto es obligatoria en el flujo real; sigue siendo nullable en el modelo
// para los datos de ejemplo y las capturas anteriores a esta regla.
// @Serializable: viaja dentro de las rutas de navegación del flujo de evaluación.
@Serializable
data class CapturedImages(
    val dermoscopic: LesionImage,
    val context: LesionImage?
)
