package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft
import pe.edu.pucp.melanomaacral.domain.repository.DraftRepository

// Borradores en memoria (pruebas y vistas previas). Se pierden al cerrar la app.
open class InMemoryDraftRepository(initial: Map<String, EvaluationDraft> = emptyMap()) : DraftRepository {

    private val mutex = Mutex()
    private val _drafts = MutableStateFlow(initial)
    override val drafts: StateFlow<Map<String, EvaluationDraft>> = _drafts.asStateFlow()

    override suspend fun get(lesionId: String): EvaluationDraft? {
        load()
        return _drafts.value[lesionId]
    }

    override suspend fun update(lesionId: String, transform: (EvaluationDraft?) -> EvaluationDraft?) {
        load()
        mutex.withLock {
            val next = transform(_drafts.value[lesionId])
            val all = if (next == null) _drafts.value - lesionId
            else _drafts.value + (lesionId to next.copy(lesionId = lesionId, updatedAtMillis = System.currentTimeMillis()))
            if (all != _drafts.value) {
                _drafts.value = all
                persist(all)
            }
        }
    }

    // Puntos de extensión para la versión que guarda en un archivo.
    protected open suspend fun load() {}
    protected open suspend fun persist(all: Map<String, EvaluationDraft>) {}
    protected fun setLoaded(all: Map<String, EvaluationDraft>) { _drafts.value = all }
}
