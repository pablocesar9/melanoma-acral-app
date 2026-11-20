package pe.edu.pucp.melanomaacral.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.Explanation
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.LightingProblem
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex
import pe.edu.pucp.melanomaacral.domain.rules.PatientIdError
import pe.edu.pucp.melanomaacral.ui.screens.about.AboutScreen
import pe.edu.pucp.melanomaacral.ui.screens.clinicaldata.ClinicalDataContent
import pe.edu.pucp.melanomaacral.ui.screens.clinicaldata.ClinicalDataUiState
import pe.edu.pucp.melanomaacral.ui.screens.followup.ScheduleFollowUpContent
import pe.edu.pucp.melanomaacral.ui.screens.followup.ScheduleFollowUpUiState
import pe.edu.pucp.melanomaacral.ui.screens.home.HomeScreen
import pe.edu.pucp.melanomaacral.ui.screens.lesion.LesionContent
import pe.edu.pucp.melanomaacral.ui.screens.lesion.LesionUiState
import pe.edu.pucp.melanomaacral.ui.screens.obtainimage.CaptureSlot
import pe.edu.pucp.melanomaacral.ui.screens.obtainimage.CaptureStep
import pe.edu.pucp.melanomaacral.ui.screens.obtainimage.ObtainImageContent
import pe.edu.pucp.melanomaacral.ui.screens.obtainimage.ObtainImageUiState
import pe.edu.pucp.melanomaacral.ui.screens.patient.LesionSummary
import pe.edu.pucp.melanomaacral.ui.screens.patient.PatientContent
import pe.edu.pucp.melanomaacral.ui.screens.patient.PatientUiState
import pe.edu.pucp.melanomaacral.ui.screens.patients.NewPatientDialogState
import pe.edu.pucp.melanomaacral.ui.screens.patients.PatientSummary
import pe.edu.pucp.melanomaacral.ui.screens.patients.PatientsContent
import pe.edu.pucp.melanomaacral.ui.screens.patients.PatientsUiState
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralContent
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralText
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralUiState
import pe.edu.pucp.melanomaacral.ui.screens.result.ResultContent
import pe.edu.pucp.melanomaacral.ui.screens.result.ResultUiState
import pe.edu.pucp.melanomaacral.ui.theme.MelanomaAcralTheme
import java.io.File
import java.time.LocalDate

// Capturas de pantalla en JVM (Roborazzi sobre Robolectric) de cada XxxContent con datos de ejemplo,
// en claro y oscuro. Con `./gradlew recordRoborazziDebug` se guardan en docs/r3/despues/.
// En `testDebugUnitTest` normal solo comprueban que cada pantalla se dibuja sin fallar.
// Las imágenes de ejemplo son del dataset de investigación (src/test/resources), nunca de pacientes.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CapturasPantallasTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 14)
    private val outDir = System.getProperty("capturas.dir") ?: "../docs/r3/despues"

    private fun foto(n: Int) = LesionImage(File("src/test/resources/imagenes-ejemplo/ejemplo$n.jpg").absolutePath)

    // Dibuja el contenido y lo captura en claro y en oscuro. heightDp alto para pantallas que se desplazan.
    private fun shoot(name: String, heightDp: Int = 800, fontScale: Float = 1f, dialog: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("w360dp-h${heightDp}dp-xhdpi")
        RuntimeEnvironment.setFontScale(fontScale)
        var dark by mutableStateOf(false)
        // Con un cuadro de diálogo con campo de texto, el cursor parpadea sin fin: se controla el reloj a mano.
        if (dialog) compose.mainClock.autoAdvance = false
        compose.setContent {
            MelanomaAcralTheme(darkTheme = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
        val suffix = if (fontScale != 1f) "-fuente${fontScale}" else ""
        if (dialog) compose.mainClock.advanceTimeBy(600)
        capture("$outDir/$name$suffix-claro.png", dialog)
        dark = true
        if (dialog) compose.mainClock.advanceTimeBy(600) else compose.waitForIdle()
        capture("$outDir/$name$suffix-oscuro.png", dialog)
    }

    private fun capture(path: String, wholeScreen: Boolean) {
        if (wholeScreen) captureScreenRoboImage(path) else compose.onRoot().captureRoboImage(path)
    }

    // ---- datos de ejemplo ----
    private val sole = ClinicalData(
        location = AcralLocation.SOLE, maxDiameterMm = 9f, isElevatedOrPalpable = false,
        hadPreviousProcedure = false, reportedChange = ReportedChange.NO, evolutionTime = EvolutionTime.FROM_3_TO_12_MONTHS,
        ageYears = 58, sex = Sex.MALE
    )
    private val prediction = Prediction(0.18f, false, "simulado-0", 0.30f, true)
    private val refer = Recommendation(ManagementAction.REFER, RecommendationReason.DIAMETER_OVER_LIMIT, true)
    private val followUp = Recommendation(ManagementAction.FOLLOW_UP, RecommendationReason.SMALL_BELOW_THRESHOLD, true)
    private val notApt = Recommendation(ManagementAction.REFER, RecommendationReason.NOT_APT_FOR_FOLLOW_UP, false)
    private val outOfScope = Recommendation(ManagementAction.NONE, RecommendationReason.OUT_OF_SCOPE, false)
    private val images = CapturedImages(dermoscopic = foto(2), context = foto(1))
    private val twoEvaluations: List<Evaluation> = listOf(
        Evaluation("EV-001", "LES-001", today.minusMonths(3), CapturedImages(foto(3), foto(1)), sole, prediction, refer),
        Evaluation("EV-002", "LES-001", today, CapturedImages(foto(4), foto(1)), sole.copy(maxDiameterMm = 4f), prediction, followUp)
    )
    private val heat = Explanation(List(8) { r -> List(8) { c -> kotlin.math.max(0f, 1f - (kotlin.math.abs(r - 3) + kotlin.math.abs(c - 4)) / 4f) } }, isSimulated = true)
    private val filledForm = ClinicalDataUiState(
        location = AcralLocation.SOLE, diameterText = "9", ageText = "58", sex = Sex.MALE,
        reportedChange = ReportedChange.YES, evolutionTime = EvolutionTime.FROM_3_TO_12_MONTHS
    )
    private val okReport = QualityReport(focusOk = true, lightingOk = true, framingOk = true, isSimulated = false, isHeuristic = true,
        sharpness = 392f, meanLuma = 128f, sharpnessDenoised = 51f)
    private val badReport = QualityReport(focusOk = false, lightingOk = false, framingOk = true, isSimulated = false, isHeuristic = true,
        lightingProblem = LightingProblem.TOO_DARK, resolutionOk = false, sharpness = 1f, meanLuma = 61f, sharpnessDenoised = 1f)

    // ---- P-01 a P-11 ----
    @Test fun p01_inicio() = shoot("P-01-inicio") { HomeScreen({}, {}) }

    @Test fun p02_pacientes() = shoot("P-02-pacientes") {
        PatientsContent(
            PatientsUiState(listOf(
                PatientSummary("P-001", 2, today, hasOverdueFollowUp = true),
                PatientSummary("P-002", 1, today.minusDays(20), hasOverdueFollowUp = false),
                PatientSummary("P-003", 0, null, hasOverdueFollowUp = false)
            )), {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun p02_pacientes_vacio() = shoot("P-02-pacientes-vacio") {
        PatientsContent(PatientsUiState(), {}, {}, {}, {}, {}, {})
    }

    // El cuadro de diálogo con campo de texto (registro de paciente) no se captura aquí: su cursor
    // parpadea sin fin y Compose nunca queda «en reposo» en JVM. Se captura en el emulador
    // (docs/r3/despues/emulador/P-02-registro-error-dni.png).

    @Test fun p03_paciente() = shoot("P-03-paciente") {
        PatientContent(
            PatientUiState("P-001", listOf(
                LesionSummary("LES-001", AcralLocation.SOLE, 2, today, today.minusDays(12), true, ManagementAction.FOLLOW_UP),
                LesionSummary("LES-002", AcralLocation.PALM, 1, today.minusDays(30), null, false, ManagementAction.REFER),
                LesionSummary("LES-003", AcralLocation.DIGIT, 1, today.minusDays(9), today.plusMonths(2), false, ManagementAction.FOLLOW_UP)
            )), {}, {}, {}
        )
    }

    @Test fun p03_paciente_vacio() = shoot("P-03-paciente-vacio") { PatientContent(PatientUiState("P-004"), {}, {}, {}) }

    @Test fun p04_lesion() = shoot("P-04-lesion", heightDp = 1100) {
        LesionContent(
            LesionUiState("LES-001", "P-001", AcralLocation.SOLE, today.minusDays(12), true, twoEvaluations), {}, {}, {}
        )
    }

    @Test fun p04_lesion_sin_capturas() = shoot("P-04-lesion-sin-capturas") {
        LesionContent(LesionUiState("LES-005", "P-001"), {}, {}, {})
    }

    @Test fun p05_obtener_paso1() = shoot("P-05-obtener-paso1", heightDp = 900) {
        ObtainImageContent(ObtainImageUiState(), { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun p05_obtener_paso2_control() = shoot("P-05-obtener-paso2-control", heightDp = 1000) {
        ObtainImageContent(
            ObtainImageUiState(
                step = CaptureStep.DERMOSCOPIC, context = CaptureSlot(foto(1), okReport), closeUp = CaptureSlot(foto(1), okReport), previous = twoEvaluations.first()
            ),
            { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun p05_obtener_primer_plano() = shoot("P-05-obtener-primer-plano", heightDp = 900) {
        ObtainImageContent(
            ObtainImageUiState(step = CaptureStep.CLOSE_UP, context = CaptureSlot(foto(1), okReport)),
            { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

    // P-06: la calidad se ve dentro de cada paso de P-05, apenas se obtiene la foto.
    @Test fun p06_calidad_foto_clinica_adecuada() = shoot("P-06-calidad-paso1-adecuada", heightDp = 1500) {
        ObtainImageContent(ObtainImageUiState(context = CaptureSlot(foto(1), okReport)), { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun p06_calidad_adecuada() = shoot("P-06-calidad-adecuada", heightDp = 1500) {
        ObtainImageContent(
            ObtainImageUiState(step = CaptureStep.DERMOSCOPIC, context = CaptureSlot(foto(1), okReport), closeUp = CaptureSlot(foto(1), okReport), dermoscopic = CaptureSlot(foto(2), okReport)),
            { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun p06_calidad_insuficiente() = shoot("P-06-calidad-insuficiente", heightDp = 1800) {
        ObtainImageContent(
            ObtainImageUiState(step = CaptureStep.DERMOSCOPIC, context = CaptureSlot(foto(1), okReport), closeUp = CaptureSlot(foto(1), okReport), dermoscopic = CaptureSlot(foto(2), badReport)),
            { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun p07_datos_vacio() = shoot("P-07-datos-vacio", heightDp = 1900) {
        ClinicalDataContent(ClinicalDataUiState(), {}, {}, {})
    }

    @Test fun p07_datos_lleno() = shoot("P-07-datos-lleno", heightDp = 1900) {
        ClinicalDataContent(filledForm.copy(hasPain = true), {}, {}, {})
    }

    @Test fun p08_resultado_derivar() = shoot("P-08-resultado-derivar", heightDp = 2000) {
        ResultContent(ResultUiState.Success(prediction, refer, sole, images, heat), {}, {}, {}, {}, {})
    }

    @Test fun p08_resultado_control() = shoot("P-08-resultado-control", heightDp = 2000) {
        ResultContent(ResultUiState.Success(prediction, followUp, sole.copy(maxDiameterMm = 4f), images, heat), {}, {}, {}, {}, {})
    }

    @Test fun p08_resultado_no_apto() = shoot("P-08-resultado-no-apto", heightDp = 2000) {
        ResultContent(ResultUiState.Success(prediction, notApt, sole.copy(isElevatedOrPalpable = true), images, heat), {}, {}, {}, {}, {})
    }

    @Test fun p08_resultado_fuera_de_alcance() = shoot("P-08-resultado-fuera-de-alcance", heightDp = 1800) {
        ResultContent(
            ResultUiState.Success(prediction.copy(probMelanoma = 0.64f, isOutOfScope = true), outOfScope, sole, images, null),
            {}, {}, {}, {}, {}
        )
    }

    @Test fun p09_control() = shoot("P-09-control", heightDp = 1000) {
        ScheduleFollowUpContent(ScheduleFollowUpUiState(isLoading = false, today = today), {}, {}, {})
    }

    @Test fun p09_control_bloqueado() = shoot("P-09-control-bloqueado") {
        ScheduleFollowUpContent(ScheduleFollowUpUiState(isLoading = false, followUpAllowed = false, today = today), {}, {}, {})
    }

    @Test fun p10_derivacion() = shoot("P-10-derivacion", heightDp = 2200) {
        val evaluation = twoEvaluations.first()
        ReferralContent(
            ReferralUiState(
                isLoading = false, lesionId = "LES-001",
                text = ReferralText.build("P-001", "LES-001", evaluation),
                attachments = listOf(
                    pe.edu.pucp.melanomaacral.domain.model.ReferralAttachment("Foto clínica de contexto", foto(1)),
                    pe.edu.pucp.melanomaacral.domain.model.ReferralAttachment("Imagen dermoscópica", foto(3))
                )
            ), {}, {}, {}
        )
    }

    @Test fun p11_acerca() = shoot("P-11-acerca", heightDp = 1500) { AboutScreen({}) }

    // ---- escala de fuente 1.3 (accesibilidad) ----
    @Test fun fuente13_resultado() = shoot("P-08-resultado-derivar", heightDp = 2400, fontScale = 1.3f) {
        ResultContent(ResultUiState.Success(prediction, refer, sole, images, heat), {}, {}, {}, {}, {})
    }

    @Test fun fuente13_datos() = shoot("P-07-datos-lleno", heightDp = 2600, fontScale = 1.3f) {
        ClinicalDataContent(filledForm, {}, {}, {})
    }

    @Test fun fuente13_inicio() = shoot("P-01-inicio", fontScale = 1.3f) { HomeScreen({}, {}) }

    @Test fun fuente13_pacientes() = shoot("P-02-pacientes", fontScale = 1.3f) {
        PatientsContent(
            PatientsUiState(listOf(
                PatientSummary("P-001", 2, today, hasOverdueFollowUp = true),
                PatientSummary("P-002", 1, today.minusDays(20), hasOverdueFollowUp = false)
            )), {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun fuente13_calidad() = shoot("P-06-calidad-insuficiente", heightDp = 2400, fontScale = 1.3f) {
        ObtainImageContent(
            ObtainImageUiState(step = CaptureStep.DERMOSCOPIC, context = CaptureSlot(foto(1), okReport), closeUp = CaptureSlot(foto(1), okReport), dermoscopic = CaptureSlot(foto(2), badReport)),
            { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }

    @Test fun fuente13_obtener() = shoot("P-05-obtener-paso1", heightDp = 1300, fontScale = 1.3f) {
        ObtainImageContent(ObtainImageUiState(), { File("x") }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun fuente13_lesion() = shoot("P-04-lesion", heightDp = 1400, fontScale = 1.3f) {
        LesionContent(LesionUiState("LES-001", "P-001", AcralLocation.SOLE, today.minusDays(12), true, twoEvaluations), {}, {}, {})
    }

    @Test fun fuente13_control() = shoot("P-09-control", heightDp = 1200, fontScale = 1.3f) {
        ScheduleFollowUpContent(ScheduleFollowUpUiState(isLoading = false, today = today), {}, {}, {})
    }

    @Test fun fuente13_derivacion() = shoot("P-10-derivacion", heightDp = 2800, fontScale = 1.3f) {
        ReferralContent(
            ReferralUiState(isLoading = false, lesionId = "LES-001", text = ReferralText.build("P-001", "LES-001", twoEvaluations.first())),
            {}, {}, {}
        )
    }

    @Test fun fuente13_acerca() = shoot("P-11-acerca", heightDp = 2000, fontScale = 1.3f) { AboutScreen({}) }
}
