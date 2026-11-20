package pe.edu.pucp.melanomaacral.domain.model

// Lo que se comparte al derivar: el PDF y, como archivos aparte, las imágenes (URIs content:// de FileProvider).
data class ReferralExport(val pdf: String, val images: List<String>)

// Una imagen del paquete de derivación con su rótulo (RF-15).
data class ReferralAttachment(val label: String, val image: LesionImage)

// Paquete de derivación: lo que se exporta para adjuntar a la hoja de referencia o enviar
// por el canal del establecimiento (RF-15). Sin especialidad de destino: la app no la fija.
data class ReferralPackage(
    val patientId: String,
    val lesionId: String,
    val latest: Evaluation,
    val previous: Evaluation?,
    val attachments: List<ReferralAttachment>
)

object ReferralPackages {
    // Mínimo recomendado de imágenes: 2 a 3 (la de contexto, la dermoscópica y,
    // en un control, la dermoscópica anterior para comparar).
    fun build(patientId: String, lesionId: String, evaluationsOfLesion: List<Evaluation>): ReferralPackage? {
        val sorted = evaluationsOfLesion.forLesion(lesionId)
        val latest = sorted.lastOrNull() ?: return null
        val previous = sorted.getOrNull(sorted.size - 2)
        val attachments = buildList {
            latest.images.context?.let { add(ReferralAttachment("Foto clínica de contexto", it)) }
            add(ReferralAttachment("Imagen dermoscópica", latest.images.dermoscopic))
            previous?.let { add(ReferralAttachment("Dermoscópica anterior (${it.date})", it.images.dermoscopic)) }
        }
        return ReferralPackage(patientId, lesionId, latest, previous, attachments)
    }
}
