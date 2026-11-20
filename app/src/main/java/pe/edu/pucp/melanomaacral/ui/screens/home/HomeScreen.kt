package pe.edu.pucp.melanomaacral.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.pucp.melanomaacral.ui.components.BottomActionBar
import pe.edu.pucp.melanomaacral.ui.components.NoticeCard
import pe.edu.pucp.melanomaacral.ui.components.NoticeKind
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import pe.edu.pucp.melanomaacral.ui.theme.Space

// P-01 Inicio — RF-20, RNF-11 (aviso de apoyo siempre visible), RNF-01 (funciona sin Internet).
// No necesita ViewModel: no tiene datos.
@Composable
fun HomeScreen(
    onOpenPatients: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomActionBar(
                primaryLabel = "Ver pacientes",
                onPrimary = onOpenPatients,
                primaryIcon = Icons.Filled.Groups,
                secondaryLabel = "Acerca de y limitaciones",
                onSecondary = onOpenAbout,
                secondaryIsText = true
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.l, vertical = Space.xl)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Box(
                    modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Text("Melanoma acral", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Apoyo para evaluar y dar seguimiento a lesiones acrales pigmentadas en el primer nivel de atención.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NoticeCard(
                title = "Herramienta de apoyo e investigación",
                text = "No da diagnósticos ni reemplaza el juicio clínico ni la evaluación del especialista.",
                kind = NoticeKind.Support
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Funciona sin conexión a internet", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MelanomaAcralTheme {
        HomeScreen(onOpenPatients = {}, onOpenAbout = {})
    }
}
