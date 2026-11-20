package pe.edu.pucp.melanomaacral.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.ui.theme.Space
import java.util.Locale
import kotlin.math.abs

// Salida del modelo frente a su umbral (RF-04). El puntaje (logit) NO es una probabilidad calibrada
// (tesis, objetivo 2): por eso no se muestra un porcentaje, sino de qué lado del umbral cae y con qué margen.
object ModelScore {
    // Margen (puntaje - umbral) que llena media barra. Los casos claros de la tesis están a ±10 o más.
    const val ESCALA = 15f

    fun fmt(v: Float): String = String.format(Locale("es", "PE"), "%.1f", v).replace("-", "−")

    // Una línea para el historial, el texto de derivación y el PDF.
    fun summary(p: Prediction): String {
        val s = p.score; val t = p.scoreThreshold
        if (s == null || t == null) return "Probabilidad ${(p.probMelanoma * 100).toInt()} % (umbral ${(p.threshold * 100).toInt()} %)"
        val lado = if (p.suggestsMelanoma) "sugiere melanoma" else "no sugiere melanoma"
        return "El modelo $lado: puntaje ${fmt(s)} frente al umbral ${fmt(t)} (margen ${if (s >= t) "+" else ""}${fmt(s - t)})"
    }

    const val NOTE = "El puntaje no es una probabilidad: indica de qué lado del umbral del modelo cae la imagen y con qué margen."

    // Cómo se lee el puntaje (pantalla de resultado y «Acerca de»). Cifras de la tesis (objetivo 2, adenda 10).
    val HOW_TO_READ = listOf(
        "Puntaje: el modelo convierte la imagen en un número. Cuanto más alto, más se parece a los melanomas con los que " +
            "se entrenó; cuanto más bajo, a los nevos. Los casos claros suelen estar por encima de +10 o por debajo de −20.",
        "Umbral (−5,1): desde ese valor el modelo sugiere melanoma. Se fijó en la tesis para que, en los datos de " +
            "validación, el modelo reconociera como no melanoma a 9 de cada 10 nevos; es el promedio de 15 entrenamientos.",
        "Margen: la distancia entre el puntaje y el umbral. Un margen grande indica un caso claro para el modelo; cerca de " +
            "0, el modelo está en el límite y el resultado puede cambiar con otra foto de la misma lesión.",
        "La barra va de «no sugiere» a «sugiere melanoma»; la línea del centro es el umbral y la barra se llena del todo " +
            "con un margen de ±15.",
        "No es una probabilidad: el modelo no está calibrado (un puntaje de −8 no significa «0,03 % de riesgo»). Por eso " +
            "no se muestra un porcentaje.",
        "La recomendación final no depende solo del modelo: también usa los datos clínicos (diámetro mayor que 7 mm y " +
            "signos de alarma llevan a derivar aunque el modelo no sugiera melanoma)."
    )
}

// Barra horizontal: a la izquierda «no sugiere», a la derecha «sugiere»; la línea central es el umbral.
@Composable
fun ModelScoreBar(prediction: Prediction, modifier: Modifier = Modifier) {
    val s = prediction.score ?: return
    val t = prediction.scoreThreshold ?: return
    val margin = (s - t).coerceIn(-ModelScore.ESCALA, ModelScore.ESCALA)
    val pos = 0.5f + margin / (2 * ModelScore.ESCALA)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = if (prediction.suggestsMelanoma) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onSurface
    Column(modifier.semantics { contentDescription = ModelScore.summary(prediction) }, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val h = 10.dp.toPx(); val y = (size.height - h) / 2; val cx = size.width / 2; val px = size.width * pos
            drawRoundRect(track, Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
            val left = minOf(cx, px); drawRect(fill, Offset(left, y), Size(abs(px - cx), h))
            drawRect(mark, Offset(cx - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))        // umbral
            drawCircle(fill, radius = 8.dp.toPx(), center = Offset(px, size.height / 2))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("No sugiere", style = MaterialTheme.typography.labelMedium)
            Text("umbral", style = MaterialTheme.typography.labelMedium)
            Text("Sugiere melanoma", style = MaterialTheme.typography.labelMedium)
        }
    }
}
