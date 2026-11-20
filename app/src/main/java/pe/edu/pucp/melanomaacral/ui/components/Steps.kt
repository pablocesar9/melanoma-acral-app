package pe.edu.pucp.melanomaacral.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import pe.edu.pucp.melanomaacral.ui.theme.Space

// Pasos del flujo de evaluación (heurística 1: visibilidad del estado). El orden es el del flujo real.
// La calidad no es un paso aparte: cada foto se revisa dentro de su paso.
enum class EvaluationStep(val label: String) {
    ClinicalPhoto("Foto clínica"),
    Dermoscopic("Imagen dermoscópica"),
    ClinicalData("Datos clínicos"),
    Result("Resultado")
}

// Indicador de pasos: un segmento por paso y el texto «Paso n de 4: nombre». El texto asegura que
// el estado no dependa solo del color (WCAG 1.4.1) y lo lee TalkBack.
@Composable
fun StepIndicator(current: EvaluationStep, modifier: Modifier = Modifier) {
    val steps = EvaluationStep.entries
    val index = current.ordinal
    val description = "Paso ${index + 1} de ${steps.size}: ${current.label}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Space.s)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { i, _ -> Segment(done = i <= index) }
        }
        Text(
            description,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.Segment(done: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(6.dp)
            .background(
                if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(3.dp)
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun StepIndicatorPreview() {
    MelanomaAcralTheme {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            StepIndicator(EvaluationStep.ClinicalPhoto)
            StepIndicator(EvaluationStep.ClinicalData)
        }
    }
}
