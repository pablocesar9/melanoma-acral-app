package pe.edu.pucp.melanomaacral.ui.screens.quality

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.LightingProblem
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.quality.ImageQualityAnalyzer
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.SimulatedNotice
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import pe.edu.pucp.melanomaacral.ui.theme.semantic

// P-06 Revisión de calidad — RF-03. Ya no es una pantalla aparte al final de la captura:
// cada foto se revisa apenas se obtiene, dentro de su paso (P-05).

// Veredicto de una foto, arriba y grande: lo primero que se lee.
@Composable
// advisory: en el primer plano, una foto con observaciones no se presenta como un error, sino como un consejo.
fun QualityVerdictCard(ok: Boolean, isContext: Boolean, modifier: Modifier = Modifier, advisory: Boolean = false) {
    val sem = MaterialTheme.semantic
    val scheme = MaterialTheme.colorScheme
    val container = when { ok -> sem.qualityOk.container; advisory -> sem.neutral.container; else -> scheme.errorContainer }
    val content = when { ok -> sem.qualityOk.onContainer; advisory -> sem.neutral.onContainer; else -> scheme.onErrorContainer }
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Space.l),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when { ok -> Icons.Filled.CheckCircle; advisory -> Icons.Filled.Info; else -> Icons.Filled.Warning },
                contentDescription = null,
                modifier = Modifier.size(Size.iconHero)
            )
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    when { ok -> "Calidad adecuada"; advisory -> "Puede mejorar"; else -> "Calidad insuficiente" },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    when {
                        ok && isContext -> "La foto clínica sirve. Sigue con la imagen dermoscópica."
                        ok -> "La imagen sirve para continuar."
                        advisory -> "La foto se puede usar. Si puedes, repítela siguiendo lo que se indica abajo."
                        else -> "Una imagen de mala calidad puede dar un resultado poco confiable. Se recomienda repetirla."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Mientras se revisa la foto recién obtenida.
@Composable
fun QualityCheckingCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Space.l),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(Size.iconInCard))
            Text("Revisando la calidad de la imagen…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Una foto con su revisión: miniatura, cada criterio con icono, causa y cómo corregir, y las medidas plegadas.
@Composable
fun PhotoQualityCard(
    title: String,
    image: LesionImage,
    report: QualityReport?,
    showFraming: Boolean,
    modifier: Modifier = Modifier,
    clinical: Boolean = false
) {
    // Umbrales con que se revisó esta foto: los de la foto clínica o los de la imagen dermoscópica.
    val t = if (clinical) ImageQualityAnalyzer.CLINICAL else ImageQualityAnalyzer.DERMOSCOPIC
    var showMeasures by rememberSaveable(title) { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            LesionImageView(
                image, label = title,
                modifier = Modifier.heightIn(max = 240.dp).clip(MaterialTheme.shapes.small)
            )
            if (report == null) return@Column
            CheckRow("Enfoque", report.focusOk, "Imagen borrosa. Repite la foto sin mover el teléfono.")
            CheckRow("Iluminación", report.lightingOk, lightingCause(report.lightingProblem))
            if (showFraming) {
                CheckRow("Encuadre", report.framingOk, "No hay contenido en el centro. Apoya el dermatoscopio sobre la lesión.")
            }
            CheckRow("Tamaño de la imagen", report.resolutionOk, "Imagen muy pequeña. Usa una imagen de mayor tamaño.")
            val measures = listOfNotNull(
                report.sharpness?.let {
                    "Nitidez: ${it.fmt1()} (mínimo ${t.minSharpness.fmt1()}). Cuánto contraste hay en los bordes; " +
                        "escala continua, más alto es más nítido. Las dermatoscopias dan valores bajos porque la piel es lisa."
                },
                report.sharpnessDenoised?.let {
                    "Nitidez sin ruido: ${it.fmt1()} (mínimo ${t.minSharpnessDenoised.fmt1()}). La misma medida tras " +
                        "suavizar el grano de la cámara; evita que una foto movida pase por nítida."
                },
                report.meanLuma?.let {
                    "Brillo: ${it.toInt()} (aceptable de ${t.minMeanLuma.toInt()} a ${t.maxMeanLuma.toInt()}; " +
                        "0 es negro y 255 blanco)."
                }
            )
            if (measures.isNotEmpty()) {
                TextButton(onClick = { showMeasures = !showMeasures }) {
                    Text(if (showMeasures) "Ocultar medidas" else "Ver medidas y umbrales")
                }
                AnimatedVisibility(visible = showMeasures) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        measures.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (clinical) "Los umbrales de la foto clínica se calibraron con fotos tomadas con la cámara del teléfono. " +
                                "Si la foto se ve bien, puedes continuar."
                            else "Los mínimos se calibraron con 600 imágenes dermatoscópicas de los datos de la tesis: solo el 2 % " +
                                "quedaba por debajo. Si la foto se ve bien, puedes continuar de todos modos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Avisos que acompañan la revisión: simulada o heurística.
@Composable
fun QualityMethodNotice(report: QualityReport) {
    if (report.isSimulated) {
        SimulatedNotice(title = "Revisión simulada", text = "Esta revisión de prueba siempre aprueba la imagen.")
    }
    if (report.isHeuristic) {
        NoticeCard(
            title = "Revisión automática simple",
            text = "Revisa enfoque, luz y tamaño con reglas fijas. No usa inteligencia artificial " +
                "y no juzga la lesión; solo avisa si la imagen es poco adecuada.",
            kind = NoticeKind.Info
        )
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, cause: String) {
    val sem = MaterialTheme.semantic
    Row(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.Top) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (ok) "Adecuado" else "No adecuado",
            tint = if (ok) sem.qualityOk.accent else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Size.iconInCard)
        )
        Column {
            Text(
                if (ok) "$label: adecuado" else "$label: no adecuado",
                style = MaterialTheme.typography.bodyLarge
            )
            if (!ok) {
                Text(cause, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun lightingCause(problem: LightingProblem) = when (problem) {
    LightingProblem.TOO_DARK -> "Imagen muy oscura. Acerca el dermatoscopio o enciende su luz."
    LightingProblem.TOO_BRIGHT -> "Imagen con exceso de luz o reflejos. Cambia el ángulo."
    LightingProblem.NONE -> "Luz insuficiente."
}

private fun Float.fmt1(): String = String.format(java.util.Locale("es", "PE"), "%.1f", this).removeSuffix(",0")
