package pe.edu.pucp.melanomaacral.domain.repository

import java.time.LocalDate

// Programa el recordatorio del próximo control (RF-14). Funciona sin conexión.
interface ReminderScheduler {
    fun schedule(lesionId: String, followUpDate: LocalDate)
}
