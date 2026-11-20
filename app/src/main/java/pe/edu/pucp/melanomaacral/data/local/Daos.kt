package pe.edu.pucp.melanomaacral.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordsDao {
    @Query("SELECT * FROM patients ORDER BY id")
    fun observePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM lesions ORDER BY id")
    fun observeLesions(): Flow<List<LesionEntity>>

    @Query("SELECT * FROM evaluations ORDER BY dateEpochDay, id")
    fun observeEvaluations(): Flow<List<EvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLesion(lesion: LesionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvaluation(evaluation: EvaluationEntity)

    @Update
    suspend fun updateEvaluation(evaluation: EvaluationEntity)

    @Query("SELECT * FROM evaluations WHERE id = :id")
    suspend fun evaluation(id: String): EvaluationEntity?

    @Query("SELECT COUNT(*) FROM lesions")
    suspend fun lesionCount(): Int

    @Query("SELECT COUNT(*) FROM evaluations")
    suspend fun evaluationCount(): Int

    @Query("UPDATE lesions SET nextFollowUpEpochDay = :epochDay WHERE id = :lesionId")
    suspend fun setNextFollowUp(lesionId: String, epochDay: Long)
}
