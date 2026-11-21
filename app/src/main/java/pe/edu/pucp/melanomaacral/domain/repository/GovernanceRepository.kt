package pe.edu.pucp.melanomaacral.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.model.AuditEntry
import pe.edu.pucp.melanomaacral.domain.model.ConsentType
import pe.edu.pucp.melanomaacral.domain.model.DecisionChoice
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.PatientConsent
import pe.edu.pucp.melanomaacral.domain.model.ProfessionalDecision

// RF-17: consentimiento del paciente, registrado antes de capturar imágenes.
interface ConsentRepository {
    fun observe(patientId: String): Flow<PatientConsent?>
    suspend fun get(patientId: String): PatientConsent?
    suspend fun record(patientId: String, type: ConsentType)
}

// RF-23: decisión del profesional frente a la sugerencia de la aplicación.
interface DecisionRepository {
    fun observe(evaluationId: String): Flow<ProfessionalDecision?>
    val all: Flow<List<ProfessionalDecision>>
    suspend fun save(evaluationId: String, choice: DecisionChoice, finalAction: ManagementAction?, reason: String)
}

// RNF-12: usuarios locales con PIN y registro de las acciones sobre los datos.
interface AccessRepository {
    val currentUser: StateFlow<String?>
    suspend fun hasUsers(): Boolean
    // Crea el usuario y abre su sesión. Devuelve el motivo si no se pudo, o null si se creó.
    suspend fun createUser(username: String, pin: String): String?
    // Abre la sesión si el usuario y el PIN coinciden.
    suspend fun login(username: String, pin: String): Boolean
    suspend fun logout()
    // Registra una acción del usuario actual (no hace nada si no hay sesión).
    suspend fun audit(action: AuditAction, target: String)
    val log: Flow<List<AuditEntry>>
}
