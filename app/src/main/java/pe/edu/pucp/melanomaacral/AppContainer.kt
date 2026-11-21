package pe.edu.pucp.melanomaacral

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.data.local.AppDatabase
import pe.edu.pucp.melanomaacral.data.local.RoomRecordsRepository
import pe.edu.pucp.melanomaacral.data.local.RoomAccessRepository
import pe.edu.pucp.melanomaacral.data.local.RoomConsentRepository
import pe.edu.pucp.melanomaacral.data.local.RoomDecisionRepository
import pe.edu.pucp.melanomaacral.domain.repository.AccessRepository
import pe.edu.pucp.melanomaacral.domain.repository.ConsentRepository
import pe.edu.pucp.melanomaacral.domain.repository.DecisionRepository
import pe.edu.pucp.melanomaacral.data.files.AndroidImageStorage
import pe.edu.pucp.melanomaacral.data.files.KeyVault
import pe.edu.pucp.melanomaacral.data.files.SecureImages
import pe.edu.pucp.melanomaacral.data.notifications.WorkManagerReminderScheduler
import pe.edu.pucp.melanomaacral.data.files.FileDraftRepository
import pe.edu.pucp.melanomaacral.data.files.HeuristicImageQualityRepository
import pe.edu.pucp.melanomaacral.data.files.PdfReferralExporter
import pe.edu.pucp.melanomaacral.data.real.AnalizadorB0
import pe.edu.pucp.melanomaacral.data.real.OnDeviceClassifierRepository
import pe.edu.pucp.melanomaacral.data.real.OnDeviceExplanationRepository
import pe.edu.pucp.melanomaacral.data.real.Runtime
import pe.edu.pucp.melanomaacral.domain.repository.ClassifierRepository
import pe.edu.pucp.melanomaacral.domain.repository.ExplanationRepository
import pe.edu.pucp.melanomaacral.domain.repository.ImageStorage
import pe.edu.pucp.melanomaacral.domain.repository.ReminderScheduler
import pe.edu.pucp.melanomaacral.domain.repository.ReferralExporter
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository
import pe.edu.pucp.melanomaacral.domain.repository.ImageQualityRepository
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository

// Inyección de dependencias manual (plan, sección 3.2): aquí se decide qué
// implementación usa la app. Pasar de una falsa a una real es cambiar una línea.
class AppContainer(context: Context) {
    // RNF-05: la base de datos (SQLCipher) y las imágenes (AES-GCM) se cifran con claves del Keystore de Android.
    private val database: AppDatabase by lazy {
        AppDatabase.create(context, KeyVault.databasePassphraseHex(context.noBackupFilesDir))
    }

    val imageStorage: ImageStorage = AndroidImageStorage(context)
    // Modelo final B0 en el teléfono (fase 4): una sola inferencia da la probabilidad y el Grad-CAM.
    // Runtime.LITERT o Runtime.ONNX; para volver a lo simulado: FakeClassifierRepository() y FakeExplanationRepository().
    // En el Honor (2026-11-16): ONNX Runtime 528 ms por inferencia frente a 1195 ms de LiteRT (ModeloB0EnDispositivoTest).
    private val analizador = AnalizadorB0(context, Runtime.ONNX)
    val explanationRepository: ExplanationRepository = OnDeviceExplanationRepository(analizador)
    val reminderScheduler: ReminderScheduler = WorkManagerReminderScheduler(context)
    val classifierRepository: ClassifierRepository = OnDeviceClassifierRepository(analizador)
    val imageQualityRepository: ImageQualityRepository = HeuristicImageQualityRepository()
    val draftRepository: DraftRepository = FileDraftRepository(context)
    // PDF de derivación: necesita el clasificador y el mapa para incluir la imagen con el mapa de calor.
    val referralExporter: ReferralExporter by lazy { PdfReferralExporter(context, explanationRepository) }

    // Trabajo que debe seguir aunque se cierre la pantalla que lo pidió.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // RNF-05: las imágenes se cifran con la clave de datos de la app, guardada cifrada con la del Keystore.
        val keystoreKey = KeyVault.imageKey()
        val dataKey = KeyVault.imageDataKey(context.noBackupFilesDir, keystoreKey)
        SecureImages.key = dataKey
        SecureImages.legacyKey = keystoreKey
        // Las imágenes sin cifrar o cifradas con el formato anterior pasan al actual una sola vez, en segundo plano.
        appScope.launch(Dispatchers.IO) {
            val n = SecureImages.encryptExisting(java.io.File(context.filesDir, "images"), dataKey, keystoreKey)
            if (n > 0) android.util.Log.w("SecureImages", "Imágenes pasadas al formato actual: $n")
        }
    }

    // Analiza la imagen dermoscópica en segundo plano (mientras se llenan los datos clínicos). El resultado queda en
    // memoria en AnalizadorB0, así que la pantalla de resultado no espera la inferencia otra vez.
    fun prepareModel(image: LesionImage) {
        appScope.launch { runCatching { classifierRepository.classify(image) } }
    }
    val recordsRepository: RecordsRepository by lazy { RoomRecordsRepository(database.recordsDao()) }

    // Uso con pacientes: acceso por usuario y registro de acciones (RNF-12), consentimiento (RF-17)
    // y decisión del profesional (RF-23).
    val accessRepository: AccessRepository by lazy { RoomAccessRepository(database.governanceDao()) }
    val consentRepository: ConsentRepository by lazy { RoomConsentRepository(database.governanceDao(), accessRepository) }
    val decisionRepository: DecisionRepository by lazy { RoomDecisionRepository(database.governanceDao(), accessRepository) }
}
