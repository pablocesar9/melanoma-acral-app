package pe.edu.pucp.melanomaacral.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.pucp.melanomaacral.domain.model.EvaluationDraft

// Evaluaciones sin terminar, una por lesión como máximo.
interface DraftRepository {
    // Por ID de lesión.
    val drafts: StateFlow<Map<String, EvaluationDraft>>

    suspend fun get(lesionId: String): EvaluationDraft?

    // Cambia el borrador de la lesión de una sola vez (leer, modificar y guardar).
    // Si transform devuelve null, el borrador se borra.
    suspend fun update(lesionId: String, transform: (EvaluationDraft?) -> EvaluationDraft?)

    suspend fun delete(lesionId: String) = update(lesionId) { null }
}
