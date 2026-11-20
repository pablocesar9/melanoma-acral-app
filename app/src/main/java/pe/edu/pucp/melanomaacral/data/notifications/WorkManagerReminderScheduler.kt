package pe.edu.pucp.melanomaacral.data.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import pe.edu.pucp.melanomaacral.domain.repository.ReminderScheduler
import pe.edu.pucp.melanomaacral.domain.rules.ReminderTiming
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

// WorkManager conserva la tarea aunque se cierre la app o se reinicie el teléfono.
// Un solo recordatorio por lesión: programar de nuevo reemplaza al anterior.
class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {

    override fun schedule(lesionId: String, followUpDate: LocalDate) {
        val delay = ReminderTiming.delayMillis(followUpDate, LocalDateTime.now())
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_LESION_ID to lesionId))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("control-$lesionId", ExistingWorkPolicy.REPLACE, request)
    }
}
