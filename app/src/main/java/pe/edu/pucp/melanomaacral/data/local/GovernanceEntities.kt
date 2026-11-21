package pe.edu.pucp.melanomaacral.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Tablas de la versión 5 del esquema (2026-11-20): consentimiento del paciente (RF-17), decisión del profesional
// (RF-23), usuarios locales y registro de acciones (RNF-12). Son tablas nuevas: no cambian las existentes.

@Entity(
    tableName = "consents",
    foreignKeys = [ForeignKey(PatientEntity::class, ["id"], ["patientId"], onDelete = ForeignKey.CASCADE)]
)
data class ConsentEntity(
    @PrimaryKey val patientId: String,
    val type: String,               // ConsentType.name
    val epochDay: Long,
    val recordedBy: String,
    val recordedAtMillis: Long
)

@Entity(
    tableName = "decisions",
    foreignKeys = [ForeignKey(EvaluationEntity::class, ["id"], ["evaluationId"], onDelete = ForeignKey.CASCADE)]
)
data class DecisionEntity(
    @PrimaryKey val evaluationId: String,
    val choice: String,             // DecisionChoice.name
    val finalAction: String?,       // ManagementAction.name o null si la descarta
    val reason: String,
    val decidedBy: String,
    val decidedAtMillis: Long
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val pinHash: ByteArray,
    val salt: ByteArray,
    val createdAtMillis: Long
)

@Entity(tableName = "audit_log", indices = [Index("atMillis")])
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val action: String,             // AuditAction.name
    val target: String,
    val atMillis: Long
)

@Dao
interface GovernanceDao {
    @Query("SELECT * FROM consents WHERE patientId = :patientId")
    fun observeConsent(patientId: String): Flow<ConsentEntity?>

    @Query("SELECT * FROM consents WHERE patientId = :patientId")
    suspend fun consent(patientId: String): ConsentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConsent(consent: ConsentEntity)

    @Query("SELECT * FROM decisions WHERE evaluationId = :evaluationId")
    fun observeDecision(evaluationId: String): Flow<DecisionEntity?>

    @Query("SELECT * FROM decisions")
    fun observeDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecision(decision: DecisionEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun userCount(): Int

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun user(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert
    suspend fun insertAudit(entry: AuditEntity)

    @Query("SELECT * FROM audit_log ORDER BY atMillis DESC, id DESC")
    fun observeAudit(): Flow<List<AuditEntity>>
}
