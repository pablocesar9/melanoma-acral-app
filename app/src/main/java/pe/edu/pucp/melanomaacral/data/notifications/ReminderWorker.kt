package pe.edu.pucp.melanomaacral.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pe.edu.pucp.melanomaacral.MainActivity
import pe.edu.pucp.melanomaacral.R

// Muestra la notificación del control. El texto no lleva datos del paciente: solo el
// código de la lesión (privacidad en la pantalla de bloqueo, RNF-06).
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lesionId = inputData.getString(KEY_LESION_ID) ?: return Result.failure()
        val context = applicationContext

        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()   // sin permiso: el control vencido igual se ve dentro de la app

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Controles programados", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = android.app.PendingIntent.getActivity(
            context, 0, android.content.Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Control programado")
            .setContentText("Hoy toca el control de la lesión $lesionId.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(lesionId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_LESION_ID = "lesionId"
        const val CHANNEL_ID = "controles"
    }
}
