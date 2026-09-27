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
        const val CHANNEL_ALERT = "itantra_alert"
        const val CHANNEL_EMERGENCY = "itantra_emergency"
        const val NOTIF_ID_MESSAGE = 1001
        const val NOTIF_ID_ALERT = 5001
        const val NOTIF_ID_EMERGENCY = 9999
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Normal Transceiver Messages (Green tier)
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Radio Messages (Normal)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Incoming walkie-talkie voice notes and routine field messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }

            // Channel 2: Tactical Alerts (Yellow tier)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Tactical Warning Alerts (Yellow)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tactical danger warnings and urgent alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                lightColor = 0xFFF59E0B.toInt()
                enableLights(true)
            }

            // Channel 3: Emergency SOS Alerts (Red tier - High Priority with Alarm sound)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency SOS Distress (Red)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical life-safety distress broadcasts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
                setSound(alarmSound, audioAttributes)
                lightColor = 0xFFEA4335.toInt()
                enableLights(true)
            }

            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
        }
    }

    fun showMessageNotification(
        sender: String,
        message: String,
        isEmergency: Boolean = false,
        isAlert: Boolean = false
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when {
            isEmergency -> CHANNEL_EMERGENCY
            isAlert -> CHANNEL_ALERT
            else -> CHANNEL_MESSAGES
        }

        val title = when {
            isEmergency -> "🚨 RED SOS: EMERGENCY DISTRESS"
            isAlert -> "⚠️ YELLOW ALERT: TACTICAL WARNING"
            else -> "📻 $sender"
        }

        val content = when {
            isEmergency -> "SOS DISTRESS: $message"
            isAlert -> "TACTICAL ALERT: $message"
            else -> "$sender: $message"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(
                when {
                    isEmergency -> NotificationCompat.PRIORITY_MAX
                    isAlert -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isEmergency) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setColor(0xFFEA4335.toInt())
        } else if (isAlert) {
            builder.setCategory(NotificationCompat.CATEGORY_EVENT)
            builder.setColor(0xFFF59E0B.toInt())
        }

        val notifId = when {
            isEmergency -> NOTIF_ID_EMERGENCY
            isAlert -> NOTIF_ID_ALERT + (1..1000).random()
            else -> NOTIF_ID_MESSAGE + (1..1000).random()
        }
        notificationManager.notify(notifId, builder.build())
    }
}
