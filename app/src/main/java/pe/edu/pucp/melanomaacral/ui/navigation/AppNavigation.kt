package pe.edu.pucp.melanomaacral.ui.navigation

import androidx.compose.runtime.Composable
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import pe.edu.pucp.melanomaacral.domain.model.DraftStage
import pe.edu.pucp.melanomaacral.ui.screens.about.AboutScreen
import pe.edu.pucp.melanomaacral.ui.screens.clinicaldata.ClinicalDataScreen
import pe.edu.pucp.melanomaacral.ui.screens.followup.ScheduleFollowUpScreen
import pe.edu.pucp.melanomaacral.ui.screens.home.HomeScreen
import pe.edu.pucp.melanomaacral.ui.screens.lesion.LesionScreen
import pe.edu.pucp.melanomaacral.ui.screens.obtainimage.ObtainImageScreen
import pe.edu.pucp.melanomaacral.ui.screens.patient.PatientScreen
import pe.edu.pucp.melanomaacral.ui.screens.patients.PatientsScreen
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralScreen
import pe.edu.pucp.melanomaacral.ui.screens.result.ResultScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    // La pila de navegación: una lista de rutas. La última es la pantalla visible.
    // Navegar = agregar a la lista; volver = quitar el último elemento.
    val backStack = rememberNavBackStack(HomeRoute)
    // Avisos breves que sobreviven al cambio de pantalla (p. ej., «Evaluación guardada» al volver a la lesión).
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String) {
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(message, withDismissAction = true) }
    }

    fun goTo(route: NavKey) {
        backStack.add(route)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }

    // Al terminar una evaluación, descarta las pantallas del flujo
    // (captura, calidad, datos, resultado) y vuelve a la lesión.
    fun backToLesion() {
        while (backStack.size > 1 && backStack.last() !is LesionRoute) {
            backStack.removeLastOrNull()
        }
    }

    // «Salir» en medio de una evaluación: la evaluación queda guardada como pendiente
    // y se vuelve a la lista de pacientes para atender a otro.
    fun exitToPatients() {
        while (backStack.size > 1 && backStack.last() !is PatientsRoute) {
            backStack.removeLastOrNull()
        }
    }

    // Abre la edición de una evaluación: la captura y, encima, los datos clínicos (Atrás vuelve a las fotos).
    fun openEditing(lesionId: String, stage: DraftStage, images: CapturedImages?) {
        backToLesion()
        goTo(ObtainImageRoute(lesionId))
        if (stage == DraftStage.CLINICAL_DATA && images != null) goTo(ClinicalDataRoute(lesionId, images))
    }

    Box(modifier) {
    NavDisplay(
        backStack = backStack,
        onBack = { goBack() },
        modifier = Modifier.fillMaxSize(),
        // Decoradores: cada pantalla de la pila recibe su propio estado guardable
        // y su propio ViewModel, que se destruye al sacarla de la pila.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen(
                    onOpenPatients = { goTo(PatientsRoute) },
                    onOpenAbout = { goTo(AboutRoute) }
                )
            }
            entry<PatientsRoute> {
                PatientsScreen(
                    onOpenPatient = { patientId -> goTo(PatientRoute(patientId)) },
                    onBack = { goBack() }
                )
            }
            entry<PatientRoute> { route ->
                PatientScreen(
                    patientId = route.patientId,
                    onOpenLesion = { lesionId -> goTo(LesionRoute(lesionId)) },
                    // Lesión nueva: se abre su historial (vacío) y encima la captura,
                    // así "Atrás" desde la captura vuelve a la lesión.
                    onLesionCreated = { lesionId ->
                        goTo(LesionRoute(lesionId))
                        goTo(ObtainImageRoute(lesionId))
                    },
                    onBack = { goBack() }
                )
            }
            entry<LesionRoute> { route ->
                LesionScreen(
                    lesionId = route.lesionId,
                    onNewCapture = { goTo(ObtainImageRoute(route.lesionId)) },
                    // Retoma la evaluación pendiente en el paso donde quedó.
                    onResume = { draft ->
                        goTo(ObtainImageRoute(route.lesionId))
                        val images = draft.images
                        if (draft.stage == DraftStage.CLINICAL_DATA && images != null) {
                            goTo(ClinicalDataRoute(route.lesionId, images))
                        }
                    },
                    onReferral = { goTo(ReferralRoute(route.lesionId)) },
                    onBack = { goBack() }
                )
            }
            entry<ObtainImageRoute> { route ->
                ObtainImageScreen(
                    lesionId = route.lesionId,
                    onImagesReady = { images -> goTo(ClinicalDataRoute(route.lesionId, images)) },
                    onBack = { goBack() },
                    onExit = { exitToPatients() }
                )
            }
            entry<ClinicalDataRoute> { route ->
                ClinicalDataScreen(
                    lesionId = route.lesionId,
                    onExit = { exitToPatients() },
                    onSubmit = { clinicalData -> goTo(ResultRoute(route.lesionId, route.images, clinicalData)) },
                    onBack = { goBack() }
                )
            }
            entry<ResultRoute> { route ->
                ResultScreen(
                    lesionId = route.lesionId,
                    images = route.images,
                    clinicalData = route.clinicalData,
                    onDone = { backToLesion(); notify("Evaluación guardada en el historial de la lesión.") },
                    onEdit = { stage -> openEditing(route.lesionId, stage, route.images) },
                    onScheduleFollowUp = { goTo(ScheduleFollowUpRoute(route.lesionId)) },
                    onReferral = { goTo(ReferralRoute(route.lesionId)) },
                    onShowLimitations = { goTo(AboutRoute) },
                    onBack = { goBack() }
                )
            }
            entry<ScheduleFollowUpRoute> { route ->
                ScheduleFollowUpScreen(
                    lesionId = route.lesionId,
                    // Vuelve a donde estaba (el resultado): el usuario cierra y guarda cuando termine.
                    onScheduled = { goBack(); notify("Control programado.") },
                    onBack = { goBack() }
                )
            }
            entry<ReferralRoute> { route ->
                ReferralScreen(
                    lesionId = route.lesionId,
                    onBack = { goBack() }
                )
            }
            entry<AboutRoute> {
                AboutScreen(onBack = { goBack() })
            }
        }
    )
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}
