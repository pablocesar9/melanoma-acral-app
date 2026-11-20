package pe.edu.pucp.melanomaacral.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import pe.edu.pucp.melanomaacral.ui.theme.semantic

// Componentes que reutilizan varias pantallas. Todos usan solo roles del tema (colorScheme,
// typography, shapes, semantic) y llevan icono + texto, nunca solo color.

// Barra superior con título, subtítulo opcional y botón «Atrás» (icono estándar) si onBack no es null.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)?,
    subtitle: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(Size.minTouch)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// Tipos de aviso. Cada uno tiene su color, su icono y su texto.
// Saved: confirmación de guardado; neutra (el verde se reserva para la calidad de imagen, guía de diseño).
enum class NoticeKind { Support, Simulated, Error, Info, Warning, Saved }

@Composable
fun NoticeCard(title: String, text: String, kind: NoticeKind, modifier: Modifier = Modifier) {
    val sem = MaterialTheme.semantic
    val scheme = MaterialTheme.colorScheme
    val (container, content, icon) = when (kind) {
        NoticeKind.Support -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Filled.VerifiedUser)
        NoticeKind.Simulated -> Triple(sem.simulated.container, sem.simulated.onContainer, Icons.Filled.Science)
        NoticeKind.Error -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Filled.ErrorOutline)
        NoticeKind.Info -> Triple(scheme.surfaceContainerHigh, scheme.onSurface, Icons.Filled.Info)
        NoticeKind.Warning -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Filled.Warning)
        NoticeKind.Saved -> Triple(scheme.surfaceContainerHigh, scheme.onSurface, Icons.Filled.CheckCircle)
    }
    NoticeBox(title, text, container, content, icon, modifier)
}

@Composable
private fun NoticeBox(
    title: String,
    text: String,
    container: Color,
    content: Color,
    icon: ImageVector,
    modifier: Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(Space.l), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Size.iconInCard))
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Aviso para toda salida simulada (regla 6 del plan): color propio, icono y la palabra SIMULADO.
@Composable
fun SimulatedNotice(title: String, text: String, modifier: Modifier = Modifier) =
    NoticeCard(title, text, NoticeKind.Simulated, modifier)

// Tarjeta informativa.
@Composable
fun InfoCard(title: String, text: String, modifier: Modifier = Modifier) =
    NoticeCard(title, text, NoticeKind.Info, modifier)

// Encabezado de sección (accesible como encabezado para TalkBack).
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
fun LoadingContent(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.l)
        ) {
            CircularProgressIndicator()
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Recuadro que ocupa el lugar de una imagen cuando la imagen no está disponible (datos de ejemplo o archivo borrado).
@Composable
fun ImagePlaceholder(label: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Space.s)
        )
    }
}
