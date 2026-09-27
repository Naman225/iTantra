package org.itantra.transceiver.emergency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.itantra.transceiver.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES = "itantra_messages"
        const val CHANNEL_EMERGENCY = "itantra_emergency"
        const val NOTIF_ID_MESSAGE = 1001
        const val NOTIF_ID_EMERGENCY = 9999
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Normal Transceiver Messages
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Radio Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Incoming walkie-talkie voice notes and messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }

            // Channel 2: Emergency SOS Alerts (High Priority with Alarm sound)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical life-safety distress broadcasts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
                setSound(alarmSound, audioAttributes)
            }

            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
        }
    }

    fun showMessageNotification(sender: String, message: String, isEmergency: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isEmergency) CHANNEL_EMERGENCY else CHANNEL_MESSAGES
        val title = if (isEmergency) "🚨 EMERGENCY SOS ALERT" else "📻 iTantra Message"
        val content = if (isEmergency) "EMERGENCY: $message" else "$sender: $message"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isEmergency) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isEmergency) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        val notifId = if (isEmergency) NOTIF_ID_EMERGENCY else (NOTIF_ID_MESSAGE + (1..1000).random())
        notificationManager.notify(notifId, builder.build())
    }
}
