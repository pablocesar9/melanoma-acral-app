package pe.edu.pucp.melanomaacral.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Base de datos local, versión 5 (esquemas exportados en app/schemas/). v1 -> v2 (2026-11-17): columnas notes,
// score, scoreThreshold y editedAtMillis en evaluations, todas opcionales; las filas existentes quedan con null.
// v2 -> v3 (2026-11-20, RF-09): se quita isNodularOrBlue y se agregan isNotHealing e isGrowingRapidly (0 en las filas
// existentes); migración automática de Room. v3 -> v4 (2026-11-20, RF-02): columnas closeup_* de la foto clínica
// de primer plano, todas opcionales; las filas existentes quedan con null. v4 -> v5 (2026-11-20, RF-17, RF-23, RNF-12):
// tablas nuevas consents, decisions, users y audit_log; las tablas existentes no cambian.
// Cuando cambie el esquema: subir la versión y agregar un Migration (no usar
// fallbackToDestructiveMigration: borraría el historial de los pacientes).
@Database(
    entities = [
        PatientEntity::class, LesionEntity::class, EvaluationEntity::class,
        ConsentEntity::class, DecisionEntity::class, UserEntity::class, AuditEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migracion2a3::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao
    abstract fun governanceDao(): GovernanceDao

    @DeleteColumn(tableName = "evaluations", columnName = "isNodularOrBlue")
    class Migracion2a3 : AutoMigrationSpec

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

        // RNF-05: con passphrase, la base de datos se abre cifrada con SQLCipher; si existía una sin cifrar
        // (versiones anteriores), se cifra una sola vez conservando todos los registros.
        fun create(context: Context, passphrase: ByteArray? = null): AppDatabase {
            val builder = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
            if (passphrase != null) {
                System.loadLibrary("sqlcipher")
                DatabaseEncryption.encryptIfPlain(context.getDatabasePath(NAME), passphrase)
                builder.openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase))
            }
            return builder.build()
        }
    }
}
