package pe.edu.pucp.melanomaacral.ui.screens.referral

import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.ui.description
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.components.ModelScore
import pe.edu.pucp.melanomaacral.ui.yesNo

// Arma el texto del paquete de derivación (RF-15).
// Es el contenido del documento de derivación: se muestra en pantalla, se comparte como texto
// y es el cuerpo del PDF (que además lleva las imágenes). Sin especialidad de destino:
// la derivación sigue el flujo del establecimiento (decisión D4).
object ReferralText {

    const val SIMULATED_WARNING = "*** RESULTADO SIMULADO — NO USAR PARA DECISIONES CLÍNICAS ***"

    fun build(patientId: String, lesionId: String, evaluation: Evaluation): String {
        val clinical = evaluation.clinicalData
        val prediction = evaluation.prediction
        val recommendation = evaluation.recommendation

        // buildString: arma un texto línea por línea.
        return buildString {
            if (prediction.isSimulated) {
                appendLine(SIMULATED_WARNING)
                appendLine()
            }
            appendLine("DERIVACIÓN — LESIÓN PIGMENTADA ACRAL")
            appendLine("Establecimiento de origen: ______________________")
            appendLine("Profesional que deriva: ______________________")
            appendLine("Destino: según el flujo de referencia de su establecimiento")
            appendLine("Código del paciente (del establecimiento, sin nombre ni documento): $patientId")
            appendLine("Lesión: $lesionId")
            appendLine("Fecha de la evaluación: ${evaluation.date.formatted()}")
            appendLine()
            appendLine("Datos clínicos")
            clinical.ageYears?.let { appendLine("- Edad: $it años") }
            clinical.sex?.let { appendLine("- Sexo: ${it.label()}") }
            appendLine("- Localización: ${clinical.location.label()}")
            appendLine("- Diámetro máximo: ${clinical.maxDiameterMm} mm")
            appendLine("- Elevada o palpable: ${clinical.isElevatedOrPalpable.yesNo()}")
            appendLine("- Nodular o azul: ${clinical.isNodularOrBlue.yesNo()}")
            appendLine("- Procedimiento previo en la zona: ${clinical.hadPreviousProcedure.yesNo()}")
            appendLine("- Cambio reportado por el paciente: ${clinical.reportedChange.label()}")
            appendLine("- Tiempo de evolución: ${clinical.evolutionTime.label()}")
            val symptoms = listOfNotNull(
                "sangra".takeIf { clinical.hasBleeding }, "duele".takeIf { clinical.hasPain }, "pica".takeIf { clinical.hasItching }
            )
            appendLine("- Síntomas: ${if (symptoms.isEmpty()) "ninguno" else symptoms.joinToString(", ")}")
            appendLine("- Antecedente de cáncer de piel (personal o familiar): ${clinical.hasSkinCancerHistory.yesNo()}")
            appendLine()
            appendLine("Resultado del modelo")
            appendLine("- ${ModelScore.summary(prediction)}")
            appendLine("- ${ModelScore.NOTE}")
            if (prediction.isOutOfScope) {
                appendLine("- Alerta: imagen fuera del alcance del modelo")
            }
            appendLine("- Recomendación: ${recommendation.action.label()}. ${recommendation.reason.description()}")
            appendLine("- Modelo: ${prediction.modelVersion}")
            if (clinical.notes.isNotBlank()) {
                appendLine()
                appendLine("Anotaciones del profesional")
                appendLine(clinical.notes)
            }
            appendLine()
            appendLine("Imágenes adjuntas (en el PDF y como archivos aparte)")
            appendLine("- Foto clínica de contexto: ${if (evaluation.images.context != null) "sí" else "no"}")
            appendLine("- Imagen dermoscópica: sí")
            val d = evaluation.images.dermoscopic
            if (d.widthPx > 0) {
                appendLine("- Datos técnicos de la dermoscópica: ${d.widthPx}x${d.heightPx} px, ${d.source.name.lowercase()}")
            }
            appendLine()
            append("Herramienta de apoyo: no reemplaza el juicio clínico.")
        }
    }
}
