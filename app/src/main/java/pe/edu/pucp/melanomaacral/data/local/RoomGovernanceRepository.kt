package pe.edu.pucp.melanomaacral.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import pe.edu.pucp.melanomaacral.domain.model.AuditAction
import pe.edu.pucp.melanomaacral.domain.model.AuditEntry
import pe.edu.pucp.melanomaacral.domain.model.ConsentType
import pe.edu.pucp.melanomaacral.domain.model.DecisionChoice
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.PatientConsent
import pe.edu.pucp.melanomaacral.domain.model.ProfessionalDecision
import pe.edu.pucp.melanomaacral.domain.repository.AccessRepository
import pe.edu.pucp.melanomaacral.domain.repository.ConsentRepository
import pe.edu.pucp.melanomaacral.domain.repository.DecisionRepository
import pe.edu.pucp.melanomaacral.domain.rules.AccessRules
import java.time.LocalDate

// RNF-12: sesión y registro de acciones. Cada acción sobre los datos queda con el usuario que la hizo.
class RoomAccessRepository(
    private val dao: GovernanceDao,
    private val clock: () -> Long = System::currentTimeMillis
) : AccessRepository {

    private val _currentUser = MutableStateFlow<String?>(null)
    override val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    override suspend fun hasUsers(): Boolean = dao.userCount() > 0

    override suspend fun createUser(username: String, pin: String): String? {
        val name = AccessRules.normalize(username)
        AccessRules.validate(name, pin)?.let { return it }
        if (dao.user(name) != null) return "Ya existe un usuario con ese nombre."
        val salt = AccessRules.newSalt()
        dao.insertUser(UserEntity(name, AccessRules.hash(pin, salt), salt, clock()))
        _currentUser.value = name
        dao.insertAudit(AuditEntity(username = name, action = AuditAction.CREATE.name, target = "Usuario $name", atMillis = clock()))
        dao.insertAudit(AuditEntity(username = name, action = AuditAction.LOGIN.name, target = "Sesión", atMillis = clock()))
        return null
    }

    override suspend fun login(username: String, pin: String): Boolean {
        val name = AccessRules.normalize(username)
        val user = dao.user(name) ?: return false
        if (!AccessRules.matches(pin, user.salt, user.pinHash)) return false
        _currentUser.value = name
        dao.insertAudit(AuditEntity(username = name, action = AuditAction.LOGIN.name, target = "Sesión", atMillis = clock()))
        return true
    }

    override suspend fun logout() {
        val name = _currentUser.value ?: return
        dao.insertAudit(AuditEntity(username = name, action = AuditAction.LOGOUT.name, target = "Sesión", atMillis = clock()))
        _currentUser.value = null
    }

    override suspend fun audit(action: AuditAction, target: String) {
        val name = _currentUser.value ?: return
        dao.insertAudit(AuditEntity(username = name, action = action.name, target = target, atMillis = clock()))
    }

    override val log: Flow<List<AuditEntry>> = dao.observeAudit().map { l ->
        l.map { AuditEntry(it.id, it.username, AuditAction.valueOf(it.action), it.target, it.atMillis) }
    }
}

// RF-17: consentimiento por paciente. Registrarlo también queda en el registro de acciones.
class RoomConsentRepository(
    private val dao: GovernanceDao,
    private val access: AccessRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now
) : ConsentRepository {

    override fun observe(patientId: String): Flow<PatientConsent?> = dao.observeConsent(patientId).map { it?.toDomain() }

    override suspend fun get(patientId: String): PatientConsent? = dao.consent(patientId)?.toDomain()

    override suspend fun record(patientId: String, type: ConsentType) {
        val user = access.currentUser.value ?: "sin sesión"
        dao.upsertConsent(ConsentEntity(patientId, type.name, today().toEpochDay(), user, clock()))
        access.audit(AuditAction.MODIFY, "Consentimiento de $patientId: ${type.name}")
    }

    private fun ConsentEntity.toDomain() =
        PatientConsent(patientId, ConsentType.valueOf(type), LocalDate.ofEpochDay(epochDay), recordedBy, recordedAtMillis)
}

// RF-23: decisión del profesional por evaluación. Se puede cambiar; queda la última, y cada cambio queda en el registro.
class RoomDecisionRepository(
    private val dao: GovernanceDao,
    private val access: AccessRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : DecisionRepository {

    override fun observe(evaluationId: String): Flow<ProfessionalDecision?> = dao.observeDecision(evaluationId).map { it?.toDomain() }

    override val all: Flow<List<ProfessionalDecision>> = dao.observeDecisions().map { l -> l.map { it.toDomain() } }

    override suspend fun save(evaluationId: String, choice: DecisionChoice, finalAction: ManagementAction?, reason: String) {
        val user = access.currentUser.value ?: "sin sesión"
        dao.upsertDecision(DecisionEntity(evaluationId, choice.name, finalAction?.name, reason.trim(), user, clock()))
        access.audit(AuditAction.MODIFY, "Decisión en $evaluationId: ${choice.name}")
    }

    private fun DecisionEntity.toDomain() = ProfessionalDecision(
        evaluationId, DecisionChoice.valueOf(choice), finalAction?.let(ManagementAction::valueOf), reason, decidedBy, decidedAtMillis
    )
}
