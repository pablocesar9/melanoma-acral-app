package pe.edu.pucp.melanomaacral.domain.model

import kotlinx.serialization.Serializable

// Hasta dónde llegó una evaluación sin terminar.
enum class DraftStage { CONTEXT, DERMOSCOPIC, CLINICAL_DATA }

// Formulario de datos clínicos a medio llenar, tal como lo dejó el usuario (texto incluido).
@Serializable
data class ClinicalFormDraft(
    val location: AcralLocation? = null,
    val diameterText: String = "",
    val isElevatedOrPalpable: Boolean = false,
    val isNodularOrBlue: Boolean = false,
    val hadPreviousProcedure: Boolean = false,
    val reportedChange: ReportedChange? = null,
    val evolutionTime: EvolutionTime? = null,
    val ageText: String = "",
    val sex: Sex? = null,
    val hasSkinCancerHistory: Boolean = false,
    val hasBleeding: Boolean = false,
    val hasPain: Boolean = false,
    val hasItching: Boolean = false,
    val notes: String = ""
)

// Evaluación empezada y no terminada de una lesión. Se guarda en el teléfono a cada paso,
// para poder salir del flujo, atender a otro paciente y retomarla después.
// Hay a lo más una por lesión; se borra al guardarse la evaluación o al descartarla.
@Serializable
data class EvaluationDraft(
    val lesionId: String,
    val stage: DraftStage = DraftStage.CONTEXT,
    val context: LesionImage? = null,
    val contextReport: QualityReport? = null,
    val dermoscopic: LesionImage? = null,
    val dermoscopicReport: QualityReport? = null,
    val clinicalForm: ClinicalFormDraft? = null,
    val updatedAtMillis: Long = 0,
    // Si no es null, el borrador corrige esa evaluación ya guardada: al terminar, la reemplaza.
    val editingEvaluationId: String? = null
) {
    // Las dos fotos con sus medidas de calidad (RNF-07), o null si falta alguna.
    val images: CapturedImages?
        get() {
            val d = dermoscopic ?: return null
            val c = context ?: return null
            return CapturedImages(dermoscopic = d.withMeasures(dermoscopicReport), context = c.withMeasures(contextReport))
        }

    // Todas las fotos del borrador (para borrarlas si se descarta).
    val allImages: List<LesionImage> get() = listOfNotNull(context, dermoscopic)

    // Lo que se muestra en las listas: en qué paso quedó.
    val stageLabel: String
        get() = when (stage) {
            DraftStage.CONTEXT -> "Quedó en la foto clínica"
            DraftStage.DERMOSCOPIC -> "Quedó en la imagen dermoscópica"
            DraftStage.CLINICAL_DATA -> "Quedó en los datos clínicos"
        } + (editingEvaluationId?.let { " (editando $it)" } ?: "")
}

fun LesionImage.withMeasures(report: QualityReport?): LesionImage =
    if (report == null) this else copy(sharpness = report.sharpness, meanLuma = report.meanLuma)
