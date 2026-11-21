package pe.edu.pucp.melanomaacral.data.local

import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

// RNF-05: pasa una base de datos sin cifrar a una cifrada con SQLCipher (sqlcipher_export), una sola vez.
object DatabaseEncryption {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun isPlain(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_HEADER.size) return false
        val head = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { it.read(head) }
        return head.contentEquals(SQLITE_HEADER)
    }

    fun encryptIfPlain(file: File, passphrase: ByteArray) {
        if (!isPlain(file)) return
        val tmp = File(file.path + ".cifrada")
        tmp.delete()
        val key = String(passphrase, Charsets.US_ASCII).replace("'", "''")
        // CREATE_IF_NECESSARY: la base adjunta hereda estas banderas y sin ella no se puede crear el archivo cifrado.
        val flags = SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        val db = SQLiteDatabase.openDatabase(file.path, "", null, flags, null, null)
        val version: Int
        val counts: Map<String, Long>
        try {
            version = db.rawQuery("PRAGMA user_version", null).use { c -> c.moveToFirst(); c.getInt(0) }
            counts = tableCounts(db, "main")
            // En el Honor (2026-11-20), con rawExecSQL el ATTACH no quedaba hecho («unknown database cifrada» y sin
            // archivo cifrado). execSQL trata el ATTACH aparte (deja una sola conexión) y rawQuery obliga a ejecutar
            // la exportación.
            db.execSQL("ATTACH DATABASE '${tmp.path}' AS cifrada KEY '$key'")
            db.rawQuery("SELECT sqlcipher_export('cifrada')", null).use { it.moveToFirst() }
            db.execSQL("PRAGMA cifrada.user_version = $version")
            check(tableCounts(db, "cifrada") == counts) { "La base cifrada no tiene los mismos registros" }
            db.execSQL("DETACH DATABASE cifrada")
        } finally {
            db.close()
        }
        check(tmp.isFile && !isPlain(tmp)) { "No se creó la base de datos cifrada" }
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
        check(tmp.renameTo(file)) { "No se pudo reemplazar la base de datos cifrada" }
    }

    // Filas por tabla (sin las tablas internas), para comprobar que la copia cifrada conserva todos los registros.
    private fun tableCounts(db: SQLiteDatabase, schema: String): Map<String, Long> {
        val tables = db.rawQuery(
            "SELECT name FROM $schema.sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('android_metadata', 'room_master_table')", null
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
        return tables.associateWith { t ->
            db.rawQuery("SELECT count(*) FROM $schema.`$t`", null).use { c -> c.moveToFirst(); c.getLong(0) }
        }
    }
}
