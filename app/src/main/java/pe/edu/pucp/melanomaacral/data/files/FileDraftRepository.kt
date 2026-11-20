package pe.edu.pucp.melanomaacral.data.files

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pe.edu.pucp.melanomaacral.data.fake.InMemoryDraftRepository
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import java.io.File

// Evaluaciones sin terminar guardadas en un archivo JSON privado de la app (RNF de privacidad:
// no sale del teléfono). Las imágenes ya están en el almacenamiento privado; aquí solo sus rutas.
// Un archivo aparte, y no Room, para no cambiar el esquema de la base de datos.
class FileDraftRepository(context: Context) : InMemoryDraftRepository() {

    private val file = File(context.filesDir, "evaluaciones-sin-terminar.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val loadMutex = Mutex()
    private var loaded = false

    init {
        // Carga temprana, para que las listas muestren los pendientes al abrir la app.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { load() }
    }

    override suspend fun load() = loadMutex.withLock {
        if (loaded) return@withLock
        val all = withContext(Dispatchers.IO) {
            runCatching {
                if (file.isFile) json.decodeFromString<List<EvaluationDraft>>(file.readText()) else emptyList()
            }.getOrDefault(emptyList())
        }
        setLoaded(all.associateBy { it.lesionId })
        loaded = true
    }

    override suspend fun persist(all: Map<String, EvaluationDraft>) = withContext(Dispatchers.IO) {
        // Se escribe a un temporal y se renombra: si la app se cierra a medias, el archivo no queda roto.
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(all.values.toList()))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        Unit
    }
}
