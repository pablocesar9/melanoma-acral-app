package pe.edu.pucp.melanomaacral.ui.screens.about

import pe.edu.pucp.melanomaacral.data.real.PosprocesadoB0
import pe.edu.pucp.melanomaacral.ui.components.ModelScore
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme

// P-11 Acerca de — RF-20 (limitaciones) y RNF-11 (posicionamiento regulatorio).
private val limitations = listOf(
    "No reemplaza el juicio clínico ni la evaluación del especialista.",
    "Se entrenó y evaluó con imágenes de tres fuentes públicas; no se ha validado con fotos tomadas con un dermatoscopio en " +
        "la práctica clínica. Su rendimiento puede cambiar con otro dispositivo o con otras condiciones de captura.",
    "Su salida no es una probabilidad calibrada, y el umbral único rinde distinto según la fuente de las imágenes.",
    "Su validación en piel oscura es limitada: los datos de entrenamiento no registran el fototipo.",
    "Está entrenado para lesiones acrales en imágenes dermoscópicas. La app avisa cuando una imagen no se parece a las " +
        "del entrenamiento (por ejemplo, una foto común) y entonces no muestra el resultado del modelo; ese aviso no se " +
        "probó con fotos clínicas sin dermatoscopio ni con dermatoscopias de otras zonas del cuerpo, que podrían no detectarse.",
    "No usa el historial de la lesión para predecir: cada imagen se analiza por separado.",
    "Solo evalúa las lesiones fotografiadas: no detecta lesiones nuevas ni las que no se registraron.",
    "El mapa de calor es una ayuda visual: puede confundir y su utilidad está pendiente de validación por especialistas."
)

// El modelo de IA (tesis, objetivo 2).
private val modelNotes = listOf(
    "Red EfficientNet-B0 entrenada con 2,230 imágenes dermatoscópicas de lesiones acrales (melanoma y nevo) de tres " +
        "fuentes públicas: BALD, DERM12345 y MEL-KOR.",
    "En la evaluación de la tesis distinguió melanoma de nevo con un AUC de 0,957 (0,935 cuando se evaluó en una fuente " +
        "que no vio al entrenar). El AUC va de 0,5 (azar) a 1 (perfecto).",
    "Corre dentro del teléfono, sin conexión, en alrededor de medio segundo. Prepara la imagen igual que en la tesis: la " +
        "lleva a 384 píxeles, conserva un círculo central y corrige el color de la luz.",
    "Versión del modelo: ${PosprocesadoB0.VERSION}; umbral: −5,14."
)

// El mapa de calor (análisis de explicabilidad de la tesis).
private val heatmapNotes = listOf(
    "Si el modelo sugiere melanoma, el mapa (Grad-CAM) muestra las zonas que lo llevaron a sugerirlo.",
    "Si no lo sugiere, muestra las zonas que lo llevaron a NO sugerirlo (evidencia en contra). En los nevos, el Grad-CAM " +
        "solo marcaba ruido, a menudo fuera de la lesión; la evidencia en contra sí refleja lo que usó el modelo.",
    "Va de azul (menor influencia) a rojo (mayor). Muestra dónde se apoyó el modelo, no dónde está la lesión ni sus bordes."
)

// La revisión de calidad de cada foto.
private val qualityNotes = listOf(
    "Revisa enfoque, luz, encuadre y tamaño con reglas fijas: no es inteligencia artificial y no juzga la lesión.",
    "Los mínimos de nitidez se calibraron con 600 imágenes dermatoscópicas de los datos de la tesis: solo el 2 % quedaba " +
        "por debajo. Si la foto se ve bien, puedes continuar de todos modos."
)

// Condiciones de uso del equipo (RNF-04). El prototipo las declara; no las verifica por código.
private val equipmentRequirements = listOf(
    "Dermoscopio acoplado al teléfono con luz polarizada.",
    "Acople estable: la imagen no debe moverse ni desenfocarse al capturar.",
    "Dispositivo con registro sanitario vigente (condición para el uso real)."
)

private val privacyNotes = listOf(
    "Los pacientes se identifican solo con un código del establecimiento: sin nombre ni documento.",
    "Las imágenes se guardan dentro de la app, no en la galería del teléfono, sin ubicación GPS.",
    "La app no tiene permiso de Internet: no envía datos a ningún servidor. La derivación solo se comparte cuando tú la envías.",
    "Al compartir una derivación se envían el PDF y las imágenes como archivos aparte; no se guardan en la galería."
)

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Acerca de", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Space.l)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            NoticeCard(
                title = "Uso previsto",
                text = "Herramienta de apoyo e investigación para personal de salud del primer nivel. No es un dispositivo de diagnóstico autónomo.",
                kind = NoticeKind.Support
            )

            AboutSection("Limitaciones del sistema", Icons.Filled.Warning, limitations)
            AboutSection("El modelo de IA", Icons.Filled.Memory, modelNotes)
            AboutSection("Cómo leer el resultado del modelo", Icons.Filled.Info, ModelScore.HOW_TO_READ)
            AboutSection("El mapa de calor", Icons.Filled.Layers, heatmapNotes)
            AboutSection("Revisión de calidad de la foto", Icons.Filled.CameraAlt, qualityNotes)
            AboutSection("Equipo necesario", Icons.Filled.PhoneAndroid, equipmentRequirements)
            AboutSection("Privacidad", Icons.Filled.Lock, privacyNotes)

            Text(
                "Versión ${appVersion()} · modelo ${PosprocesadoB0.VERSION} · prototipo de investigación de la tesis " +
                    "«Diseño e implementación de un aplicativo móvil impulsado por inteligencia artificial para el apoyo al " +
                    "tamizaje de melanoma acral» (Pontificia Universidad Católica del Perú, 2026).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Versión de la app (del manifiesto); si no se puede leer (vistas previas), no se inventa una.
@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "de desarrollo"
}

// Sección con encabezado, icono y una tarjeta con los puntos.
@Composable
private fun AboutSection(title: String, icon: ImageVector, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Size.iconInCard))
            SectionTitle(title)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                for (item in items) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.Top) {
                        Text("•", style = MaterialTheme.typography.bodyLarge)
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun AboutScreenPreview() {
    MelanomaAcralTheme {
        AboutScreen(onBack = {})
    }
}
