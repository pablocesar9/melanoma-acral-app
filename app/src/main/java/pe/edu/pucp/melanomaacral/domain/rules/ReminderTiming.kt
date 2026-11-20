package pe.edu.pucp.melanomaacral.domain.rules

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

// Cuándo se emite el recordatorio del control (RF-14): a las 9:00 del día del control.
// Si esa hora ya pasó (control de hoy o vencido), se emite de inmediato.
object ReminderTiming {
    const val REMINDER_HOUR = 9

    fun delayMillis(followUpDate: LocalDate, now: LocalDateTime): Long {
        val at = followUpDate.atTime(REMINDER_HOUR, 0)
        return maxOf(0L, Duration.between(now, at).toMillis())
    }
}
