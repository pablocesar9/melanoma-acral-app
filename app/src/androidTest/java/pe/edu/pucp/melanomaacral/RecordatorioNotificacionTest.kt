package pe.edu.pucp.melanomaacral

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.pucp.melanomaacral.data.notifications.ReminderWorker
import pe.edu.pucp.melanomaacral.data.notifications.WorkManagerReminderScheduler
import java.time.LocalDate

// RF-14 en un dispositivo o emulador: el recordatorio del control se programa y, cuando el trabajo corre, publica
// la notificación «Control programado» con el código de la lesión y sin datos del paciente (RNF-06).
@RunWith(AndroidJUnit4::class)
class RecordatorioNotificacionTest {

    @Before
    fun darPermisoDeNotificaciones() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val trabajos get() = WorkManager.getInstance(context)

    private fun esperar(id: java.util.UUID, estado: WorkInfo.State, ms: Long = 30_000): WorkInfo.State? {
        val limite = System.currentTimeMillis() + ms
        var actual: WorkInfo.State? = null
        while (System.currentTimeMillis() < limite) {
            actual = trabajos.getWorkInfoById(id).get()?.state
            if (actual == estado) return actual
            Thread.sleep(300)
        }
        return actual
    }

    @Test
    fun cuandoElTrabajoCorre_publicaLaNotificacionDelControl() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
        val pedido = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.KEY_LESION_ID to "LES-PRUEBA"))
            .build()
        trabajos.enqueue(pedido).result.get()

        assertEquals(WorkInfo.State.SUCCEEDED, esperar(pedido.id, WorkInfo.State.SUCCEEDED))
        val notificacion = manager.activeNotifications.firstOrNull { it.notification.extras.getString("android.title") == "Control programado" }
        assertTrue("No se publicó la notificación del control", notificacion != null)
        val texto = notificacion!!.notification.extras.getCharSequence("android.text").toString()
        assertTrue(texto, texto.contains("LES-PRUEBA"))
        manager.cancelAll()
    }

    @Test
    fun programarUnControl_dejaUnSoloRecordatorioPendiente() {
        val lesion = "LES-PRUEBA-2"
        val programador = WorkManagerReminderScheduler(context)
        programador.schedule(lesion, LocalDate.now().plusMonths(3))
        programador.schedule(lesion, LocalDate.now().plusMonths(6))

        val pendientes = trabajos.getWorkInfosForUniqueWork("control-$lesion").get()
            .filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals("Debe haber un solo recordatorio por lesión", 1, pendientes.size)
        trabajos.cancelUniqueWork("control-$lesion").result.get()
    }
}
