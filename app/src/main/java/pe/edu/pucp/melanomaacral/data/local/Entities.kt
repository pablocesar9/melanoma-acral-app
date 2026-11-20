package pe.edu.pucp.melanomaacral.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tablas de la base de datos local (Room). Los tipos de fecha se guardan como día de época
// (LocalDate.toEpochDay) y los enum como su nombre, para que el esquema sea estable.
// Las imágenes se guardan por ruta de archivo interno, no como bytes (RNF-05, RNF-07).

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val createdOnEpochDay: Long
)

@Entity(
    tableName = "lesions",
    foreignKeys = [ForeignKey(PatientEntity::class, ["id"], ["patientId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("patientId")]
)
data class LesionEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val nextFollowUpEpochDay: Long?
)

@Entity(
    tableName = "evaluations",
    foreignKeys = [ForeignKey(LesionEntity::class, ["id"], ["lesionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("lesionId")]
)
data class EvaluationEntity(
    @PrimaryKey val id: String,
    val lesionId: String,
    val dateEpochDay: Long,

    // Imagen dermoscópica (RF-01) y su metadato técnico (RNF-07)
    @ColumnInfo(name = "derm_uri") val dermUri: String,
    @ColumnInfo(name = "derm_source") val dermSource: String,
    @ColumnInfo(name = "derm_width") val dermWidth: Int,
    @ColumnInfo(name = "derm_height") val dermHeight: Int,
    @ColumnInfo(name = "derm_size") val dermSize: Long,
    @ColumnInfo(name = "derm_captured_at") val dermCapturedAt: Long,
    @ColumnInfo(name = "derm_sharpness") val dermSharpness: Float?,
    @ColumnInfo(name = "derm_luma") val dermLuma: Float?,

    // Foto clínica de contexto (RF-02); todo null si no hay
    @ColumnInfo(name = "ctx_uri") val ctxUri: String?,
    @ColumnInfo(name = "ctx_source") val ctxSource: String?,
    @ColumnInfo(name = "ctx_width") val ctxWidth: Int?,
    @ColumnInfo(name = "ctx_height") val ctxHeight: Int?,
    @ColumnInfo(name = "ctx_size") val ctxSize: Long?,
    @ColumnInfo(name = "ctx_captured_at") val ctxCapturedAt: Long?,
    @ColumnInfo(name = "ctx_sharpness") val ctxSharpness: Float?,
    @ColumnInfo(name = "ctx_luma") val ctxLuma: Float?,

    // Datos clínicos (RF-06, RF-07, RF-09, RF-16)
    val location: String,
    val maxDiameterMm: Float,
    val isElevatedOrPalpable: Boolean,
    val isNodularOrBlue: Boolean,
    val hadPreviousProcedure: Boolean,
    val reportedChange: String,
    val evolutionTime: String,
    val ageYears: Int?,
    val sex: String?,
    val hasSkinCancerHistory: Boolean,
    val hasBleeding: Boolean,
    val hasPain: Boolean,
    val hasItching: Boolean,

    // Predicción (RNF-10)
    val probMelanoma: Float,
    val isOutOfScope: Boolean,
    val modelVersion: String,
    val threshold: Float,
    val isSimulated: Boolean,

    // Recomendación (RF-08, RF-09)
    val action: String,
    val reason: String,
    val followUpAllowed: Boolean,

    // Versión 2 del esquema (2026-11-17): anotaciones, puntaje del modelo y edición posterior. null en filas de la v1.
    val notes: String? = null,
    val score: Float? = null,
    val scoreThreshold: Float? = null,
    val editedAtMillis: Long? = null
)
