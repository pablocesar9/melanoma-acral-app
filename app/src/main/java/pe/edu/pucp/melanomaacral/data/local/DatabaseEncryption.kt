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
        val db = SQLiteDatabase.openDatabase(file.path, "", null, SQLiteDatabase.OPEN_READWRITE, null, null)
        try {
            val version = db.rawQuery("PRAGMA user_version", null).use { c -> c.moveToFirst(); c.getInt(0) }
            db.rawExecSQL("ATTACH DATABASE '${tmp.path}' AS cifrada KEY '$key'")
            db.rawExecSQL("SELECT sqlcipher_export('cifrada')")
            db.rawExecSQL("PRAGMA cifrada.user_version = $version")
            db.rawExecSQL("DETACH DATABASE cifrada")
        } finally {
            db.close()
        }
        listOf("", "-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
        check(tmp.renameTo(file)) { "No se pudo reemplazar la base de datos cifrada" }
    }
}
