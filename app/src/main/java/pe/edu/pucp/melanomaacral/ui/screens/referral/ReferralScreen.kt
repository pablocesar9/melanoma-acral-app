package pe.edu.pucp.melanomaacral.ui.screens.referral

import android.content.Context
import android.content.ClipData
import android.content.Intent
import pe.edu.pucp.melanomaacral.domain.model.ReferralExport
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.EmptyState
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.components.SimulatedNotice
import pe.edu.pucp.melanomaacral.ui.theme.Space
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.pucp.melanomaacral.data.fake.SampleRecords
import pe.edu.pucp.melanomaacral.ui.components.AppTopBar
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.LoadingContent
import pe.edu.pucp.melanomaacral.ui.components.SectionTitle
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.time.LocalDate

// P-10 Paquete de derivación — RF-15.
@Composable
fun ReferralScreen(
    lesionId: String,
    onBack: () -> Unit,
    viewModel: ReferralViewModel = viewModel(factory = ReferralViewModel.factory(lesionId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ReferralContent(
        uiState = uiState,
        onShareText = { text -> shareText(context, subject = "Derivación lesión ${uiState.lesionId}", text = text) },
        onSharePdf = {
            viewModel.exportPdf { export -> sharePdf(context, subject = "Derivación lesión ${uiState.lesionId}", export = export) }
        },
        onBack = onBack
    )
}

@Composable
fun ReferralContent(
    uiState: ReferralUiState,
    onShareText: (String) -> Unit,
    onSharePdf: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = uiState.text
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AppTopBar(title = "Paquete de derivación", onBack = onBack, subtitle = "Lesión ${uiState.lesionId}") },
        bottomBar = {
            if (!uiState.isLoading && text != null) {
                BottomActionBar(
                    primaryLabel = if (uiState.isExporting) "Generando PDF…" else "Compartir PDF e imágenes",
                    onPrimary = onSharePdf,
                    primaryIcon = Icons.Filled.Share,
                    enabled = !uiState.isExporting,
                    secondaryLabel = "Compartir solo el texto",
                    onSecondary = { onShareText(text) },
                    secondaryIsText = true
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent("Preparando…", Modifier.padding(innerPadding))
            text == null -> EmptyState(
                icon = Icons.Filled.Description,
                title = "Nada que derivar todavía",
                text = "Esta lesión aún no tiene evaluaciones para derivar.",
                modifier = Modifier.padding(innerPadding).padding(top = Space.xxl)
            )
            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Space.l)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space.l)
            ) {
                NoticeCard(
                    title = "Documento para la hoja de referencia",
                    text = "Revisa el contenido antes de compartir. Se envía por el canal de tu establecimiento " +
                        "(correo institucional, WhatsApp institucional, impreso). La app no elige la especialidad de destino.",
                    kind = NoticeKind.Info
                )

                SectionTitle("Vista previa del PDF")
                DocumentPreview(text = text, attachments = uiState.attachments)

                uiState.errorMessage?.let { NoticeCard("No se pudo generar el PDF", it, NoticeKind.Error) }
            }
        }
    }
}

// Vista previa del documento: lo que irá en el PDF, ordenado por secciones.
@Composable
private fun DocumentPreview(text: String, attachments: List<pe.edu.pucp.melanomaacral.domain.model.ReferralAttachment>) {
    val blocks = text.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            for (block in blocks) {
                if (block.startsWith("***")) {
                    SimulatedNotice(title = "RESULTADO SIMULADO", text = "No usar para decisiones clínicas.")
                    continue
                }
                val lines = block.lines()
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(lines.first(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    lines.drop(1).forEach { line ->
                        Text(line.removePrefix("- "), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (attachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text("Imágenes que se adjuntan", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        attachments.forEach { attachment ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                                LesionImageView(
                                    attachment.image, label = attachment.label, maxSidePx = 400,
                                    modifier = Modifier.clip(MaterialTheme.shapes.small)
                                )
                                Text(attachment.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Abre el menú "Compartir" de Android (correo, WhatsApp, etc.). No necesita internet para prepararse.
private fun shareText(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Compartir derivación"))
}

// Abre el menú "Compartir" con el PDF y, como archivos aparte, las imágenes (foto clínica, dermoscópica, dermoscópica
// con el mapa y la anterior). FileProvider da permiso de lectura temporal al destino; nada se guarda en la galería (RNF-05).
private fun sharePdf(context: Context, subject: String, export: ReferralExport) {
    val uris = ArrayList((listOf(export.pdf) + export.images).map(Uri::parse))
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        clipData = ClipData.newRawUri(subject, uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Compartir derivación (PDF e imágenes)"))
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
fun ReferralPreview() {
    val evaluation = SampleRecords.evaluations(LocalDate.of(2026, 10, 27)).last()
    MelanomaAcralTheme {
        ReferralContent(
            uiState = ReferralUiState(
                isLoading = false,
                lesionId = evaluation.lesionId,
                text = ReferralText.build("PAC-002", evaluation.lesionId, evaluation)
            ),
            onShareText = {}, onSharePdf = {}, onBack = {}
        )
    }
}
