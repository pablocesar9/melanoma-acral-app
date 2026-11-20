package pe.edu.pucp.melanomaacral.ui.screens.followup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.SegmentedChoice
import pe.edu.pucp.melanomaacral.ui.theme.Size
import pe.edu.pucp.melanomaacral.ui.theme.Space
import pe.edu.pucp.melanomaacral.ui.theme.semantic
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.domain.model.FollowUpInterval
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.time.LocalDate

// P-09 Programar control — RF-09, RF-14.
@Composable
fun ScheduleFollowUpScreen(
    lesionId: String,
    onScheduled: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScheduleFollowUpViewModel = viewModel(factory = ScheduleFollowUpViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Android 13+: el recordatorio necesita permiso de notificaciones. Se pide al confirmar;
    // si lo niegan, el control igual queda programado y se ve como vencido dentro de la app.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.confirm(onDone = onScheduled)
    }
    val context = LocalContext.current
    ScheduleFollowUpContent(
        uiState = uiState,
        onSelectInterval = { viewModel.selectInterval(it) },
        onConfirm = {
            val needsAsk = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.confirm(onDone = onScheduled)
        },
        onBack = onBack
    )
}

@Composable
fun ScheduleFollowUpContent(
    uiState: ScheduleFollowUpUiState,
    onSelectInterval: (FollowUpInterval) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSchedule = !uiState.isLoading && uiState.followUpAllowed
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Programar control", onBack = onBack) },
        bottomBar = {
            if (canSchedule) {
                BottomActionBar(
                    primaryLabel = "Programar control",
                    onPrimary = onConfirm,
                    primaryIcon = Icons.Filled.Event
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent("Cargando…", Modifier.padding(innerPadding))
            !uiState.followUpAllowed -> BlockedContent(onBack = onBack, modifier = Modifier.padding(innerPadding))
            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Space.l)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space.xl)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    SectionTitle("Plazo del próximo control")
                    SegmentedChoice(
                        options = FollowUpInterval.entries,
                        selected = uiState.selectedInterval,
                        label = { "${it.months} meses" },
                        onSelect = onSelectInterval
                    )
                    Text(
                        "3 meses es corto plazo, para lesiones con atipia. 6 y 12 meses son largo plazo, para vigilancia de pacientes de alto riesgo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // La fecha resultante, bien visible.
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.semantic.followUp.container,
                        contentColor = MaterialTheme.semantic.followUp.onContainer
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Space.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.m)
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(Size.iconHero))
                        Column {
                            Text("Próximo control", style = MaterialTheme.typography.labelLarge)
                            Text(longDate(uiState.date), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                NoticeCard(
                    title = "Recordatorio",
                    text = "El teléfono te avisa con una notificación el día del control, a las 9:00. Al programar, Android puede pedirte permiso para mostrar notificaciones. Si no lo das, el control vencido se ve igual dentro de la app.",
                    kind = NoticeKind.Info
                )
            }
        }
    }
}

private val longDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))

private fun longDate(date: LocalDate): String = date.format(longDateFormatter)

// RF-09: las lesiones que cumplen un criterio de derivación del primer nivel no se monitorean.
@Composable
private fun BlockedContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(Space.l)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        NoticeCard(
            title = "No se puede programar control",
            text = "La última evaluación cumple un criterio de derivación (cambio de aspecto o elevación, síntomas, sangrado, lesión que no cura o que crece rápido). Estas lesiones se derivan, no se monitorean.",
            kind = NoticeKind.Error
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun ScheduleFollowUpPreview() {
    MelanomaAcralTheme {
        ScheduleFollowUpContent(
            uiState = ScheduleFollowUpUiState(isLoading = false, today = LocalDate.of(2026, 10, 27)),
            onSelectInterval = {}, onConfirm = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Bloqueado")
@Composable
fun ScheduleFollowUpBlockedPreview() {
    MelanomaAcralTheme {
        ScheduleFollowUpContent(
            uiState = ScheduleFollowUpUiState(isLoading = false, followUpAllowed = false),
            onSelectInterval = {}, onConfirm = {}, onBack = {}
        )
    }
}
