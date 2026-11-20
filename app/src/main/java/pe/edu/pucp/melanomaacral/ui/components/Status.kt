package pe.edu.pucp.melanomaacral.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import pe.edu.pucp.melanomaacral.ui.theme.StatusColors
import pe.edu.pucp.melanomaacral.ui.theme.semantic
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun ManagementAction.status(sem: pe.edu.pucp.melanomaacral.ui.theme.SemanticColors): StatusColors = when (this) {
    ManagementAction.REFER -> sem.refer
    ManagementAction.FOLLOW_UP -> sem.followUp
    ManagementAction.NONE -> sem.neutral
}

private fun ManagementAction.icon(): ImageVector = when (this) {
    ManagementAction.REFER -> Icons.Filled.Warning
    ManagementAction.FOLLOW_UP -> Icons.Filled.Event
    ManagementAction.NONE -> Icons.Filled.Info
}

// Palabra corta de la recomendación (para listas y chips).
fun ManagementAction.shortLabel(): String = when (this) {
    ManagementAction.REFER -> "Derivar"
    ManagementAction.FOLLOW_UP -> "Control"
    ManagementAction.NONE -> "No evaluable"
}

private fun ManagementAction.whatToDo(): String = when (this) {
    ManagementAction.REFER -> "Derive al paciente con el especialista. Prepare el paquete de derivación."
    ManagementAction.FOLLOW_UP -> "Programe un control y repita la dermatoscopía en la fecha indicada."
    ManagementAction.NONE -> "La imagen no se pudo evaluar."
}

// Tarjeta de la recomendación: color + icono + palabra + qué hacer. Es la misma en el resultado,
// el historial y la derivación (heurística 4: consistencia). `reason` explica por qué.
@Composable
fun RecommendationCard(
    action: ManagementAction,
    reason: String?,
    modifier: Modifier = Modifier
) {
    val colors = action.status(MaterialTheme.semantic)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.onContainer),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "Recomendación: ${action.shortLabel()}" }
    ) {
        Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Icon(action.icon(), contentDescription = null, modifier = Modifier.size(Size.iconHero), tint = colors.accentOn())
                Column {
                    Text("Recomendación", style = MaterialTheme.typography.labelLarge)
                    Text(action.shortLabel(), style = MaterialTheme.typography.headlineMedium)
                }
            }
            Text(action.whatToDo(), style = MaterialTheme.typography.bodyLarge)
            if (reason != null) {
                Text(reason, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// El icono grande usa el color del texto del contenedor (contraste garantizado sobre el contenedor).
private fun StatusColors.accentOn(): Color = onContainer

// Versión compacta: una línea con icono y palabra, para listas e historial.
@Composable
fun RecommendationPill(action: ManagementAction, modifier: Modifier = Modifier) {
    val colors = action.status(MaterialTheme.semantic)
    Pill(action.shortLabel(), action.icon(), colors, modifier)
}

@Composable
private fun Pill(text: String, icon: ImageVector, colors: StatusColors, modifier: Modifier = Modifier) {
    Surface(color = colors.container, contentColor = colors.onContainer, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            modifier = Modifier.padding(horizontal = Space.s, vertical = Space.xs)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// Evaluación empezada y sin terminar (se retoma desde la lesión). Color neutro (secundario):
// no debe confundirse con los colores de la recomendación.
@Composable
fun PendingEvaluationPill(text: String = "Evaluación sin terminar", modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Pill(
        text, Icons.Filled.PendingActions,
        StatusColors(scheme.secondary, scheme.secondaryContainer, scheme.onSecondaryContainer), modifier
    )
}

// Estado del control de seguimiento de una lesión (pendiente, próximo, vencido).
@Composable
fun FollowUpStatusPill(nextFollowUp: LocalDate?, today: LocalDate = LocalDate.now(), modifier: Modifier = Modifier) {
    if (nextFollowUp == null) return
    val sem = MaterialTheme.semantic
    val days = ChronoUnit.DAYS.between(today, nextFollowUp)
    when {
        days < 0 -> Pill("Control vencido: ${nextFollowUp.formatted()}", Icons.Filled.EventBusy, sem.refer, modifier)
        days == 0L -> Pill("Control hoy", Icons.Filled.Event, sem.followUp, modifier)
        else -> Pill("Próximo control: ${nextFollowUp.formatted()}", Icons.Filled.Event, sem.neutral, modifier)
    }
}

// Estado vacío: icono, frase y, si hay, una acción.
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.s))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

// Barra de acción fija abajo: botón principal, opcional secundario y el motivo cuando está deshabilitado.
@Composable
fun BottomActionBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryIsText: Boolean = false
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = Space.l, vertical = Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.s)
        ) {
            if (!enabled && disabledReason != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(disabledReason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onPrimary, enabled = enabled, modifier = Modifier.fillMaxWidth().height(Size.minTouch + Space.s)) {
                if (primaryIcon != null) {
                    Icon(primaryIcon, contentDescription = null, modifier = Modifier.size(Size.iconInCard))
                    Spacer(Modifier.size(Space.s))
                }
                Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
            }
            if (secondaryLabel != null && onSecondary != null) {
                if (secondaryIsText) {
                    TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().height(Size.minTouch)) { Text(secondaryLabel) }
                } else {
                    OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().height(Size.minTouch)) { Text(secondaryLabel) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusPreview() {
    MelanomaAcralTheme {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            RecommendationCard(ManagementAction.REFER, "El diámetro supera los 7 mm.")
            RecommendationCard(ManagementAction.FOLLOW_UP, null)
            FollowUpStatusPill(LocalDate.now().minusDays(3))
        }
    }
}

// Aviso «Control vencido» para listas (rojo del tema + icono + palabra).
@Composable
fun OverdueFollowUpPill(modifier: Modifier = Modifier) {
    Pill("Control vencido", Icons.Filled.EventBusy, MaterialTheme.semantic.refer, modifier)
}
