package pe.edu.pucp.melanomaacral.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Base de datos local, versión 2 (esquemas exportados en app/schemas/). v1 -> v2 (2026-11-17): columnas notes,
// score, scoreThreshold y editedAtMillis en evaluations, todas opcionales; las filas existentes quedan con null.
// Cuando cambie el esquema: subir la versión y agregar un Migration (no usar
// fallbackToDestructiveMigration: borraría el historial de los pacientes).
@Database(
    entities = [PatientEntity::class, LesionEntity::class, EvaluationEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao

    companion object {
        const val NAME = "melanoma-acral.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE evaluations ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE evaluations ADD COLUMN score REAL")
                db.execSQL("ALTER TABLE evaluations ADD COLUMN scoreThreshold REAL")
                db.execSQL("ALTER TABLE evaluations ADD COLUMN editedAtMillis INTEGER")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
