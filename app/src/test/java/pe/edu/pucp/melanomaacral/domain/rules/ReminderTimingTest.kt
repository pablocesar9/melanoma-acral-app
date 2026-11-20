package pe.edu.pucp.melanomaacral.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

// RF-14: el recordatorio sale a las 9:00 del día del control; si ya pasó, de inmediato.
class ReminderTimingTest {
    private val control = LocalDate.of(2027, 2, 14)

    @Test fun controlFuturo_esperaHastaLas9() {
        val now = LocalDateTime.of(2027, 2, 13, 9, 0)
        assertEquals(24L * 60 * 60 * 1000, ReminderTiming.delayMillis(control, now))
    }

    @Test fun controlDeHoyDespuesDeLas9_esInmediato() {
        assertEquals(0L, ReminderTiming.delayMillis(control, LocalDateTime.of(2027, 2, 14, 15, 0)))
    }

    @Test fun controlVencido_esInmediato() {
        assertEquals(0L, ReminderTiming.delayMillis(control, LocalDateTime.of(2027, 3, 1, 8, 0)))
    }
}
