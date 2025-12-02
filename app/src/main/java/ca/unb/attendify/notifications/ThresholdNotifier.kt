package ca.unb.attendify.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


//This class is for the threshold notifying system if a class is bellow the required amount
object ThresholdNotifier {

    private const val CHANNEL_ID = "attendance_threshold_channel"
    private const val CHANNEL_NAME = "Attendance Threshold Alerts"
    private const val CHANNEL_DESC =
        "Alerts when course attendance drops below the required threshold"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyBelowThreshold(
        context: Context,
        courseCode: String,
        percentage: Int,
        required: Int
    ) {
        ensureChannel(context)

        val text = "Attendance in $courseCode is $percentage%, below required $required%."

        val notificationId = courseCode.hashCode()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Attendify threshold warning")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                return
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}