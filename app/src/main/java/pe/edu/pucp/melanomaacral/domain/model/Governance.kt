package pe.edu.pucp.melanomaacral.domain.model

import java.time.LocalDate

// Datos para el uso con pacientes: consentimiento (RF-17), decisión del profesional (RF-23)
// y acceso por usuario con registro de acciones (RNF-12). Diseño: pantallas P-12, P-13 y P-14 (Anexo L).

// ---- RF-17 · Consentimiento informado del paciente (P-12) ----

// Cómo dio (o no) su consentimiento el paciente. Solo SIGNED y VERBAL habilitan la captura.
enum class ConsentType { SIGNED, VERBAL, REFUSED }

data class PatientConsent(
    val patientId: String,
    val type: ConsentType,
    val date: LocalDate,
    val recordedBy: String,          // usuario que lo registró
    val recordedAtMillis: Long
) {
    val allowsCapture: Boolean get() = type != ConsentType.REFUSED
}

// ---- RF-23 · Decisión del profesional frente a la sugerencia (P-13) ----

enum class DecisionChoice { ACCEPTS, MODIFIES, DISCARDS }

data class ProfessionalDecision(
    val evaluationId: String,
    val choice: DecisionChoice,
    val finalAction: ManagementAction?,   // la conducta que decide: la sugerida si acepta, la nueva si modifica, null si descarta
    val reason: String,
    val decidedBy: String,
    val decidedAtMillis: Long
)

// ---- RNF-12 · Acceso por usuario y registro de acciones (P-14) ----

// Acciones sobre los datos que quedan registradas con el usuario.
enum class AuditAction { LOGIN, LOGOUT, VIEW, CREATE, MODIFY, EXPORT }

data class AuditEntry(
    val id: Long,
    val username: String,
    val action: AuditAction,
    val target: String,              // qué se tocó, p. ej. «Paciente P-001» o «Evaluación EV-003»
    val atMillis: Long
)
