package pe.edu.pucp.melanomaacral.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData

// Una ruta por pantalla del inventario (docs/requisitos-app-melanoma-acral.md, §6.2).
// @Serializable permite que Android guarde la pila de navegación si destruye la Activity
// (por ejemplo, al girar el teléfono o al liberar memoria) y la restaure después.
// Los datos de cada ruta son lo que la pantalla necesita para empezar.

@Serializable data object HomeRoute : NavKey                                         // P-01
@Serializable data object PatientsRoute : NavKey                                     // P-02
@Serializable data class PatientRoute(val patientId: String) : NavKey                // P-03
@Serializable data class LesionRoute(val lesionId: String) : NavKey                  // P-04

// Flujo de evaluación: cada paso agrega lo que obtuvo y lo pasa al siguiente.
@Serializable data class ObtainImageRoute(val lesionId: String) : NavKey             // P-05 y P-06 (calidad de cada foto)
@Serializable data class ClinicalDataRoute(                                          // P-07
    val lesionId: String,
    val images: CapturedImages
) : NavKey
@Serializable data class ResultRoute(                                                // P-08
    val lesionId: String,
    val images: CapturedImages,
    val clinicalData: ClinicalData
) : NavKey

@Serializable data class ScheduleFollowUpRoute(val lesionId: String) : NavKey        // P-09
@Serializable data class ReferralRoute(val lesionId: String) : NavKey                // P-10
@Serializable data object AboutRoute : NavKey                                        // P-11
